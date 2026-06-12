package com.cloudzao.aivoice.agent.service;

import com.cloudzao.aivoice.agent.controller.dto.ChatRequest;
import com.cloudzao.aivoice.agent.controller.dto.ChatResponse;
import com.cloudzao.aivoice.agent.llm.OpenClawClient;
import com.cloudzao.aivoice.agent.llm.dto.ChatCompletionRequest;
import com.cloudzao.aivoice.agent.llm.dto.ChatCompletionResponse;
import com.cloudzao.aivoice.agent.llm.dto.ChatMessage;
import com.cloudzao.aivoice.common.config.OpenClawProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用聊天业务编排服务。
 *
 * @author cloudzao
 * @date 2026-06-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are AIVoice assistant, a helpful AI bound to the YoooClaw C·ONE companion. "
                    + "Answer concisely in the user's language.";

    private final OpenClawClient client;
    private final OpenClawProperties properties;

    /**
     * 执行一次同步对话。
     *
     * @param request 对外 DTO，已通过 Bean Validation 校验
     * @return 对外响应 DTO；不为 {@code null}
     */
    public ChatResponse chat(ChatRequest request) {
        ChatCompletionRequest upstream = buildUpstreamRequest(request, Boolean.FALSE);
        ChatCompletionResponse response = client.chatCompletion(upstream);
        String reply = response.firstContent().orElse("");

        ChatResponse.TokenUsage usage = response.usage() == null
                ? new ChatResponse.TokenUsage(null, null, null)
                : new ChatResponse.TokenUsage(
                response.usage().promptTokens(),
                response.usage().completionTokens(),
                response.usage().totalTokens());

        return new ChatResponse(response.model(), reply, usage);
    }

    /**
     * 执行一次流式对话，将 OpenClaw 上游 SSE 增量文本逐 chunk 转发给 {@link SseEmitter}。
     * <p>
     * 本方法是阻塞的；调用方（{@code ChatController}）会把它 submit 到
     * {@code chatStreamExecutor} 线程池中执行，并把 {@code emitter} 立即返回给客户端，
     * 让 Tomcat 工作线程不会被流式生命周期占用。
     * </p>
     * <p>
     * 不允许在本方法签名抛异常 —— 任何异常都通过 {@link SseEmitter#completeWithError(Throwable)}
     * 传给框架，由 Spring 决定是否中断响应（响应头此时通常已发出，无法再走 GlobalExceptionHandler）。
     * </p>
     *
     * @param request 对外 DTO，已通过 Bean Validation 校验
     * @param emitter 已建立的流式发射器，由 Controller 创建并交给本方法接管生命周期
     */
    public void chatStream(ChatRequest request, SseEmitter emitter) {
        ChatCompletionRequest upstream = buildUpstreamRequest(request, Boolean.TRUE);
        client.chatCompletionStream(
                upstream,
                content -> sendOrFail(emitter, content),
                emitter::complete,
                emitter::completeWithError);
    }

    /**
     * 把对外 DTO 转换为 OpenClaw 上游协议请求体。
     * <p>
     * 为同步与流式两个调用路径共用，避免参数解析逻辑漂移。
     * </p>
     *
     * @param request 对外请求
     * @param stream  是否流式（仅决定本字段，最终会由 {@code OpenClawHttpClient} 在流式路径上再次强制覆盖）
     * @return 已组装好的上游请求体
     */
    private ChatCompletionRequest buildUpstreamRequest(ChatRequest request, Boolean stream) {
        String model = StringUtils.hasText(request.model()) ? request.model() : properties.defaultModel();
        String systemPrompt = StringUtils.hasText(request.systemPrompt())
                ? request.systemPrompt()
                : DEFAULT_SYSTEM_PROMPT;

        List<ChatMessage> messages = new ArrayList<>(2);
        messages.add(ChatMessage.system(systemPrompt));
        messages.add(ChatMessage.user(request.message()));

        return new ChatCompletionRequest(model, messages, stream, request.temperature());
    }

    /**
     * 向 emitter 发送一条 data 事件，IO 异常时通过 {@link SseEmitter#completeWithError(Throwable)} 上报。
     * <p>
     * 出现 {@link IOException} 通常意味着客户端已断开，继续读上游 SSE 也无意义；
     * Spring 在接收到 completeWithError 后会自动中断处理。
     * </p>
     *
     * @param emitter 流式发射器
     * @param content 单个 chunk 增量文本
     */
    private void sendOrFail(SseEmitter emitter, String content) {
        try {
            emitter.send(SseEmitter.event().data(content));
        } catch (IOException e) {
            log.info("SSE client disconnected, aborting stream: {}", e.getMessage());
            emitter.completeWithError(e);
        }
    }
}
