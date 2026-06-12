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
        String model = StringUtils.hasText(request.model()) ? request.model() : properties.defaultModel();
        String systemPrompt = StringUtils.hasText(request.systemPrompt())
                ? request.systemPrompt()
                : DEFAULT_SYSTEM_PROMPT;

        List<ChatMessage> messages = new ArrayList<>(2);
        messages.add(ChatMessage.system(systemPrompt));
        messages.add(ChatMessage.user(request.message()));

        ChatCompletionRequest upstream = new ChatCompletionRequest(
                model,
                messages,
                Boolean.FALSE,
                request.temperature());

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
}
