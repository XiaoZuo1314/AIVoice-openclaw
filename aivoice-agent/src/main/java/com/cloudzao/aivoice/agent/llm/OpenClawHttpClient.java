package com.cloudzao.aivoice.agent.llm;

import com.cloudzao.aivoice.agent.llm.dto.ChatCompletionRequest;
import com.cloudzao.aivoice.agent.llm.dto.ChatCompletionResponse;
import com.cloudzao.aivoice.common.config.OpenClawProperties;
import com.cloudzao.aivoice.common.exception.OpenClawException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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

    private final RestClient openClawRestClient;
    private final OpenClawProperties properties;

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
