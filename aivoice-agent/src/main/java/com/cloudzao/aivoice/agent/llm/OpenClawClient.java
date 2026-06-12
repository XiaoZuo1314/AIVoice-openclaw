package com.cloudzao.aivoice.agent.llm;

import com.cloudzao.aivoice.agent.llm.dto.ChatCompletionRequest;
import com.cloudzao.aivoice.agent.llm.dto.ChatCompletionResponse;
import com.cloudzao.aivoice.common.exception.OpenClawException;

import java.util.function.Consumer;

/**
 * OpenClaw 客户端抽象接口。
 *
 * @author cloudzao
 * @date 2026-06-11
 */
public interface OpenClawClient {

    /**
     * 调用 OpenClaw 聊天补全接口，同步返回完整结果。
     *
     * @param request 请求体
     * @return 上游返回的完整响应；不为 {@code null}
     * @throws OpenClawException 上游非 2xx 或网络不可达时抛出
     */
    ChatCompletionResponse chatCompletion(ChatCompletionRequest request);

    /**
     * 调用 OpenClaw 聊天补全接口，以 SSE 流式方式拉取增量内容。
     * <p>
     * 实现需保证：
     * <ul>
     *   <li>无论 {@code request.stream()} 是何值，向上游发送时必须强制为 {@code true}；</li>
     *   <li>对每个有效的 delta {@code content} 调用一次 {@code onContent}（按上游顺序）；</li>
     *   <li>遇上游 {@code data: [DONE]} 行后调用一次 {@code onComplete}；</li>
     *   <li>任何上游/网络/解析异常均回调 {@code onError}（异常被包装为 {@link OpenClawException}），
     *       且不再调用 {@code onComplete}；</li>
     *   <li>方法返回前必须释放底层 HTTP 连接资源。</li>
     * </ul>
     * 本方法是同步阻塞的，调用方应自行放在工作线程中执行。
     * </p>
     *
     * @param request    请求体（{@code stream} 字段会被实现强制覆盖）
     * @param onContent  每个 delta 增量文本的回调
     * @param onComplete 流正常结束时的回调
     * @param onError    流异常结束时的回调
     */
    void chatCompletionStream(
            ChatCompletionRequest request,
            Consumer<String> onContent,
            Runnable onComplete,
            Consumer<Throwable> onError);

    /**
     * 调用 OpenClaw 模型列表接口，返回原始 JSON 字符串，主要用于健康探活。
     *
     * @return 上游返回的原始 JSON 字符串
     * @throws OpenClawException 上游非 2xx 或网络不可达时抛出
     */
    String listModelsRaw();
}
