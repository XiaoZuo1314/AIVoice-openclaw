package com.cloudzao.aivoice.common.exception;

import lombok.Getter;

/**
 * OpenClaw 上游调用异常。
 * <p>
 * 在调用 OpenClaw 网关 HTTP 接口时，遇到任一非 2xx 响应、网络不可达或解析失败，
 * 均统一包装为本异常抛出。携带原始 HTTP 状态码与响应体，便于
 * {@link com.cloudzao.aivoice.common.exception.GlobalExceptionHandler} 转换为
 * 标准的 {@code application/problem+json} 响应。
 * </p>
 *
 * @author cloudzao
 * @date 2026-06-11
 */
@Getter
public class OpenClawException extends RuntimeException {

    /**
     * 原始上游 HTTP 状态码；网络层错误时人为映射为 503。
     */
    private final int status;

    /**
     * 上游返回的响应体（通常为 JSON 字符串），用于日志排查与对外暴露原因。
     */
    private final String upstreamBody;

    /**
     * 构造异常实例。
     *
     * @param status       上游 HTTP 状态码
     * @param upstreamBody 上游返回的响应体（原文）
     */
    public OpenClawException(int status, String upstreamBody) {
        super("OpenClaw upstream error: status=" + status + ", body=" + upstreamBody);
        this.status = status;
        this.upstreamBody = upstreamBody;
    }

    /**
     * 构造异常实例，并保留底层原因。
     *
     * @param status       上游 HTTP 状态码
     * @param upstreamBody 上游返回的响应体（原文）
     * @param cause        触发本异常的底层原因
     */
    public OpenClawException(int status, String upstreamBody, Throwable cause) {
        super("OpenClaw upstream error: status=" + status + ", body=" + upstreamBody, cause);
        this.status = status;
        this.upstreamBody = upstreamBody;
    }
}
