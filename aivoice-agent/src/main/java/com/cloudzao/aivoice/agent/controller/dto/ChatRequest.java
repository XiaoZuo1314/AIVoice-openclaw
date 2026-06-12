package com.cloudzao.aivoice.agent.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 对外暴露的聊天请求 DTO。
 *
 * @param message      用户输入的问题/指令，不可为空，最大 8000 字符
 * @param model        模型/Agent 标识，可选
 * @param systemPrompt 系统提示词，可选
 * @param temperature  采样温度，可选
 * @author cloudzao
 * @date 2026-06-11
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ChatRequest", description = "同步对话请求体")
public record ChatRequest(

        @Schema(
                description = "用户输入的问题或指令",
                example = "你好，介绍一下自己",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 8000
        )
        @NotBlank @Size(max = 8000) String message,

        @Schema(
                description = "模型 / Agent 标识；留空则使用 `openclaw.default-model` 配置",
                example = "openclaw",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String model,

        @Schema(
                description = "系统提示词；留空则使用内置 AIVoice 默认人设",
                example = "你是一个简洁的中文助手",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String systemPrompt,

        @Schema(
                description = "采样温度，范围一般 0.0 ~ 2.0；留空则使用模型默认值",
                example = "0.7",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                minimum = "0.0",
                maximum = "2.0"
        )
        Double temperature
) {
}
