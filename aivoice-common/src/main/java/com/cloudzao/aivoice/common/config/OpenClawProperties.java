package com.cloudzao.aivoice.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * OpenClaw 网关连接参数配置。
 * <p>
 * 通过 {@code @ConfigurationProperties(prefix = "openclaw")} 自动绑定
 * {@code application.yaml} 中以 {@code openclaw.*} 开头的配置项。
 * 所有字段均为不可变（record），由 Spring Boot 在启动时一次性注入。
 * </p>
 *
 * @param baseUrl      OpenClaw Gateway 根地址，例如 {@code http://127.0.0.1:18789}
 * @param token        Gateway 共享密钥（Bearer Token），来源 {@code gateway.auth.token}
 *                     或环境变量 {@code OPENCLAW_GATEWAY_TOKEN}
 * @param defaultModel 默认模型名，OpenClaw 规范要求形如 {@code openclaw} 或 {@code openclaw/<agentId>}
 * @param endpoint     OpenClaw HTTP API 路径（聊天补全与模型列表）
 * @param timeout      HTTP 客户端连接超时与读超时
 * @author cloudzao
 * &#064;date  2026-06-11
 */
@ConfigurationProperties(prefix = "openclaw")
public record OpenClawProperties(
        String baseUrl,
        String token,
        String defaultModel,
        Endpoint endpoint,
        Timeout timeout
) {

    /**
     * OpenClaw HTTP API 端点路径集合。
     *
     * @param chat   聊天补全接口路径，OpenAI 兼容，默认 {@code /v1/chat/completions}
     * @param models 模型列表接口路径，用于健康探活，默认 {@code /v1/models}
     */
    public record Endpoint(String chat, String models) {
    }

    /**
     * HTTP 客户端超时配置。
     *
     * @param connect TCP 连接建立超时
     * @param read    读响应超时，LLM 推理较慢，建议 30 秒以上
     */
    public record Timeout(Duration connect, Duration read) {
    }
}
