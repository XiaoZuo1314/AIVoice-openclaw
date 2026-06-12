package com.cloudzao.aivoice.agent.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 对外暴露的聊天响应 DTO。
 *
 * @param model 实际响应的模型/Agent 标识
 * @param reply 模型回复的纯文本内容
 * @param usage 本次调用的 Token 用量
 * @author cloudzao
 * @date 2026-06-11
 */
@Schema(name = "ChatResponse", description = "同步对话响应体")
public record ChatResponse(

        @Schema(description = "实际响应的模型 / Agent 标识", example = "openclaw")
        String model,

        @Schema(description = "模型回复的纯文本内容", example = "你好，我是 AIVoice 助手……")
        String reply,

        @Schema(description = "本次调用的 Token 用量")
        TokenUsage usage
) {

    /**
     * Token 用量明细。
     */
    @Schema(name = "TokenUsage", description = "OpenAI 兼容的 Token 用量明细；上游未返回时全部为 null")
    public record TokenUsage(

            @Schema(description = "提示词消耗的 Token 数", example = "23")
            Integer promptTokens,

            @Schema(description = "补全内容消耗的 Token 数", example = "41")
            Integer completionTokens,

            @Schema(description = "本次请求的 Token 总数", example = "64")
            Integer totalTokens
    ) {
    }
}
