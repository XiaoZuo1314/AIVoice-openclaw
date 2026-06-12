package com.cloudzao.aivoice.agent.llm;

import com.cloudzao.aivoice.agent.llm.dto.ChatCompletionRequest;
import com.cloudzao.aivoice.agent.llm.dto.ChatCompletionResponse;
import com.cloudzao.aivoice.agent.llm.dto.ChatCompletionStreamChunk;
import com.cloudzao.aivoice.common.config.OpenClawProperties;
import com.cloudzao.aivoice.common.exception.OpenClawException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * {@link OpenClawClient} 的 HTTP 实现，基于 Spring {@link RestClient}。
 *
 * @author cloudzao
 * @date 2026-06-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenClawHttpClient implements OpenClawClient {

    /**
     * SSE 终止哨兵。OpenAI 兼容协议在最后一行发送 {@code data: [DONE]} 标记流结束，
     * 这一行不是合法 JSON，需在解析前过滤。
     */
    private static final String SSE_DONE_TOKEN = "[DONE]";

    /**
     * SSE 帧字段前缀。本实现仅消费 {@code data:} 前缀的行，其它前缀（如 {@code event:}、
     * {@code id:}、{@code retry:}）忽略。
     */
    private static final String SSE_DATA_PREFIX = "data:";

    private final RestClient openClawRestClient;
    private final OpenClawProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ChatCompletionResponse chatCompletion(ChatCompletionRequest request) {
        long startMs = System.currentTimeMillis();
        try {
            ChatCompletionResponse response = openClawRestClient.post()
                    .uri(properties.endpoint().chat())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String body = readBody(res.getBody());
                        throw new OpenClawException(res.getStatusCode().value(), body);
                    })
                    .body(ChatCompletionResponse.class);
            long elapsed = System.currentTimeMillis() - startMs;
            if (response != null && response.usage() != null) {
                log.info("OpenClaw chat ok: model={}, latencyMs={}, promptTokens={}, completionTokens={}, totalTokens={}",
                        response.model(), elapsed,
                        response.usage().promptTokens(),
                        response.usage().completionTokens(),
                        response.usage().totalTokens());
            } else {
                log.info("OpenClaw chat ok: model={}, latencyMs={}, usage=unknown",
                        response != null ? response.model() : "n/a", elapsed);
            }
            return response;
        } catch (OpenClawException e) {
            log.warn("OpenClaw chat upstream error: status={}, body={}", e.getStatus(), e.getUpstreamBody());
            throw e;
        } catch (ResourceAccessException e) {
            log.warn("OpenClaw Gateway unreachable: {}", e.getMessage());
            throw new OpenClawException(503, "OpenClaw Gateway unreachable: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatCompletionStream(
            ChatCompletionRequest request,
            Consumer<String> onContent,
            Runnable onComplete,
            Consumer<Throwable> onError) {
        long startMs = System.currentTimeMillis();
        ChatCompletionRequest streamRequest = new ChatCompletionRequest(
                request.model(),
                request.messages(),
                Boolean.TRUE,
                request.temperature());
        try {
            openClawRestClient.post()
                    .uri(properties.endpoint().chat())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(streamRequest)
                    .exchange((req, res) -> {
                        if (res.getStatusCode().isError()) {
                            String body = readBody(res.getBody());
                            throw new OpenClawException(res.getStatusCode().value(), body);
                        }
                        consumeSseStream(res.getBody(), onContent);
                        return Boolean.TRUE;
                    });
            long elapsed = System.currentTimeMillis() - startMs;
            log.info("OpenClaw chat stream ok: model={}, latencyMs={}", streamRequest.model(), elapsed);
            onComplete.run();
        } catch (OpenClawException e) {
            log.warn("OpenClaw chat stream upstream error: status={}, body={}", e.getStatus(), e.getUpstreamBody());
            onError.accept(e);
        } catch (ResourceAccessException e) {
            log.warn("OpenClaw Gateway unreachable (stream): {}", e.getMessage());
            onError.accept(new OpenClawException(503, "OpenClaw Gateway unreachable: " + e.getMessage(), e));
        } catch (Exception e) {
            log.error("OpenClaw chat stream unexpected error", e);
            onError.accept(new OpenClawException(500, "Stream processing failed: " + e.getMessage(), e));
        }
    }

    /**
     * 从上游 SSE 字节流中逐行解析 chunk，回调增量文本。
     * <p>
     * 仅消费 {@code data:} 前缀的行；遇 {@code data: [DONE]} 后立刻 break，
     * 不再读取后续字节。单条 chunk 解析失败仅 WARN 日志后跳过，避免一行坏数据
     * 拖死整条会话。
     * </p>
     *
     * @param body      上游响应字节流
     * @param onContent 每个 delta {@code content} 的回调
     * @throws IOException 当底层流读取失败时抛出，由调用方转为 {@link OpenClawException}
     */
    private void consumeSseStream(InputStream body, Consumer<String> onContent) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || !line.startsWith(SSE_DATA_PREFIX)) {
                    continue;
                }
                String payload = line.substring(SSE_DATA_PREFIX.length()).trim();
                if (SSE_DONE_TOKEN.equals(payload)) {
                    return;
                }
                try {
                    ChatCompletionStreamChunk chunk =
                            objectMapper.readValue(payload, ChatCompletionStreamChunk.class);
                    chunk.firstDeltaContent().ifPresent(onContent);
                } catch (JsonProcessingException pe) {
                    log.warn("OpenClaw stream chunk parse failed, skip line: {}", payload);
                }
            }
        }
    }

    @Override
    public String listModelsRaw() {
        try {
            return openClawRestClient.get()
                    .uri(properties.endpoint().models())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String body = readBody(res.getBody());
                        throw new OpenClawException(res.getStatusCode().value(), body);
                    })
                    .body(String.class);
        } catch (ResourceAccessException e) {
            throw new OpenClawException(503, "OpenClaw Gateway unreachable: " + e.getMessage(), e);
        }
    }

    private String readBody(InputStream in) {
        try {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "<unreadable>";
        }
    }
}
