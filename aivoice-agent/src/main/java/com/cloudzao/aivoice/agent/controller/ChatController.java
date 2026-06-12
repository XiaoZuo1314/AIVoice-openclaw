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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;

/**
 * 通用 Agent 对话 REST 控制器。
 *
 * @author cloudzao
 * @date 2026-06-11
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agents")
@Validated
@Tag(name = "Agents", description = "通用 Agent 同步对话相关接口")
public class ChatController {

    /**
     * 流式接口的客户端连接超时；OpenClaw 端 read-timeout 60s，
     * 这里给 5min 留足整段流式回复空间。
     */
    private static final long STREAM_EMITTER_TIMEOUT_MS = Duration.ofMinutes(5).toMillis();

    private final ChatService chatService;

    /**
     * 流式聊天专用执行器。Bean 名 {@code chatStreamExecutor} 与字段名一致，
     * Spring 按名称注入，避免与默认 {@code TaskExecutor} 冲突。
     */
    private final ThreadPoolTaskExecutor chatStreamExecutor;

    public ChatController(ChatService chatService, ThreadPoolTaskExecutor chatStreamExecutor) {
        this.chatService = chatService;
        this.chatStreamExecutor = chatStreamExecutor;
    }

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

    /**
     * 流式对话接口（SSE）。
     * <p>
     * 工作流程：
     * <ol>
     *   <li>立即创建 {@link SseEmitter} 并返回，Tomcat 工作线程随即释放；</li>
     *   <li>{@code chatStreamExecutor} 池里的工作线程接管 emitter，串接 OpenClaw 上游；</li>
     *   <li>每条 OpenClaw delta {@code content} 即时通过 {@code data:} 行下发给客户端；</li>
     *   <li>上游 {@code [DONE]} 后调用 {@code emitter.complete()} 关闭连接。</li>
     * </ol>
     * </p>
     *
     * @param request 用户请求体
     * @return 已注册回调的 {@link SseEmitter}
     */
    @Operation(
            summary = "流式对话（SSE）",
            description = """
                    与 `/chat` 同步接口请求结构完全一致，但响应以 Server-Sent Events 形式逐 chunk 返回。

                    每个 `data:` 行内容为本次 LLM 增量文本（已在服务端剥离 OpenAI 协议外壳，
                    客户端拿到的是纯文本，不需要再解析 JSON）。
                    上游 `[DONE]` 哨兵已被服务端吸收，**不会**透传给客户端 ——
                    客户端按 SSE 标准等待连接关闭即可。

                    示例响应（按时间顺序）：
                    ```
                    data: 你

                    data: 好

                    data: ，
                    ```
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 流式回复，每行 data 字段为 LLM 增量文本",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                            schema = @Schema(type = "string", example = "data: 你\n\ndata: 好\n\n"))),
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
    @PostMapping(value = "/chat/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_EMITTER_TIMEOUT_MS);
        emitter.onTimeout(() -> {
            log.info("SSE emitter timed out after {}ms, completing", STREAM_EMITTER_TIMEOUT_MS);
            emitter.complete();
        });
        emitter.onError(throwable ->
                log.warn("SSE emitter error (likely client disconnect): {}", throwable.getMessage()));
        chatStreamExecutor.execute(() -> chatService.chatStream(request, emitter));
        return emitter;
    }
}
