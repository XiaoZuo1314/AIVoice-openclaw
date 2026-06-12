package com.cloudzao.aivoice.agent.llm;

import com.cloudzao.aivoice.agent.llm.dto.ChatCompletionRequest;
import com.cloudzao.aivoice.agent.llm.dto.ChatCompletionResponse;
import com.cloudzao.aivoice.common.exception.OpenClawException;

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
     * 调用 OpenClaw 模型列表接口，返回原始 JSON 字符串，主要用于健康探活。
     *
     * @return 上游返回的原始 JSON 字符串
     * @throws OpenClawException 上游非 2xx 或网络不可达时抛出
     */
    String listModelsRaw();
}
