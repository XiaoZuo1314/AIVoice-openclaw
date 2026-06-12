package com.cloudzao.aivoice.agent.llm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 一次 LLM 调用的 Token 用量统计。
 *
 * @param promptTokens     输入 Token 数
 * @param completionTokens 输出 Token 数
 * @param totalTokens      总 Token 数
 * @author cloudzao
 * @date 2026-06-11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Usage(
        @JsonProperty("prompt_tokens") Integer promptTokens,
        @JsonProperty("completion_tokens") Integer completionTokens,
        @JsonProperty("total_tokens") Integer totalTokens
) {
}
