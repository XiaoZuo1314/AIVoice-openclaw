package com.cloudzao.aivoice.agent.controller;

import com.cloudzao.aivoice.agent.controller.dto.ChatRequest;
import com.cloudzao.aivoice.agent.controller.dto.ChatResponse;
import com.cloudzao.aivoice.agent.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用 Agent 对话 REST 控制器。
 *
 * @author cloudzao
 * @date 2026-06-11
 */
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Validated
@Tag(name = "Agents", description = "通用 Agent 同步对话相关接口")
public class ChatController {

    private final ChatService chatService;

    /**
     * 同步对话接口。
     *
     * @param request 用户请求体
     * @return HTTP 200 + 模型回复内容与 token 用量
     */
    @Operation(
            summary = "同步对话",
            description = """
                    向 OpenClaw 网关下游 LLM 发起一次同步问答。
                    
                    - `model` 留空时使用配置项 `openclaw.default-model`
                    - `systemPrompt` 留空时使用 AIVoice 内置默认人设
                    - 上游调用错误统一以 RFC 7807 `application/problem+json` 返回
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "对话成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ChatResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数校验失败",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "OpenClaw 上游错误（鉴权失败 / 路由不存在 / 5xx）",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "OpenClaw 网关不可达或超时",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(value = "/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(request));
    }
}
