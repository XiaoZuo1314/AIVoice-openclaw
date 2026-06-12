package com.cloudzao.aivoice.agent.llm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

/**
 * OpenAI 兼容的聊天补全响应体。
 *
 * @param id      上游为本次请求生成的唯一 ID
 * @param model   实际响应的模型/Agent 标识
 * @param choices 候选回复列表，通常只取第一条
 * @param usage   Token 用量统计；可能为 {@code null}
 * @author cloudzao
 * @date 2026-06-11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(
        String id,
        String model,
        List<Choice> choices,
        Usage usage
) {

    public Optional<String> firstContent() {
        if (choices == null || choices.isEmpty()) {
            return Optional.empty();
        }
        Choice first = choices.get(0);
        if (first == null || first.message() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(first.message().content());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(
            Integer index,
            ChatMessage message,
            @JsonProperty("finish_reason") String finishReason
    ) {
    }
}
