package com.cloudzao.aivoice.agent.llm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

/**
 * OpenAI 兼容的聊天补全流式响应单个 chunk。
 * <p>
 * 上游 OpenClaw Gateway 在 {@code stream=true} 时返回 SSE，
 * 每个 {@code data:} 行的 JSON 反序列化为本对象。最后会以
 * {@code data: [DONE]} 标记流结束（{@code [DONE]} 不是 JSON，
 * 由调用方在解析前过滤）。
 * </p>
 *
 * @param id      上游为本次请求生成的唯一 ID
 * @param model   实际响应的模型/Agent 标识
 * @param choices 候选 delta 列表，通常只取第一条
 * @author cloudzao
 * @date 2026-06-12
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionStreamChunk(
        String id,
        String model,
        List<DeltaChoice> choices
) {

    /**
     * 提取首个 delta 的增量文本内容。
     *
     * @return 当 choices / delta / content 任意一层为空时返回 {@link Optional#empty()}
     */
    public Optional<String> firstDeltaContent() {
        if (choices == null || choices.isEmpty()) {
            return Optional.empty();
        }
        DeltaChoice first = choices.get(0);
        if (first == null || first.delta() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(first.delta().content());
    }

    /**
     * 流式响应中的单个候选。
     *
     * @param index        候选序号
     * @param delta        增量内容
     * @param finishReason 结束原因，通常为 {@code null}（未结束）/ {@code stop} / {@code length}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeltaChoice(
            Integer index,
            Delta delta,
            @JsonProperty("finish_reason") String finishReason
    ) {
    }

    /**
     * 单个 chunk 的增量内容。首个 chunk 通常带 {@code role}，后续 chunk 仅带 {@code content}。
     *
     * @param role    角色，仅首个 chunk 出现
     * @param content 本次增量文本，UTF-8
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Delta(
            String role,
            String content
    ) {
    }
}
