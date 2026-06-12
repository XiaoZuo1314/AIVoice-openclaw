package com.cloudzao.aivoice.agent.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * OpenAI 兼容的聊天补全请求体。
 *
 * @param model       模型/Agent 标识，OpenClaw 规范要求形如 {@code openclaw} 或 {@code openclaw/<agentId>}
 * @param messages    对话消息序列，至少包含一条 user 消息
 * @param stream      是否启用流式响应；MVP 阶段固定传 {@code false}
 * @param temperature 采样温度，传 {@code null} 时使用模型默认
 * @author cloudzao
 * @date 2026-06-11
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        Boolean stream,
        Double temperature
) {
}
