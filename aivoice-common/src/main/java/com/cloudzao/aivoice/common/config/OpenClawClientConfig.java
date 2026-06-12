package com.cloudzao.aivoice.common.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * OpenClaw 客户端 Spring 配置类。
 * <p>
 * 主要职责：
 * <ol>
 *   <li>启用 {@link OpenClawProperties} 配置绑定；</li>
 *   <li>通过 {@link PostConstruct} 在容器启动阶段对关键配置做 fail-fast 校验，
 *       缺失配置时立即抛出 {@link IllegalStateException}，避免运行时调用才报错；</li>
 *   <li>暴露名为 {@code openClawRestClient} 的 {@link RestClient} Bean，
 *       预绑定 {@code Authorization: Bearer ${token}} 与 Accept 头、统一连接与读超时。</li>
 * </ol>
 * </p>
 *
 * @author cloudzao
 * @date 2026-06-11
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(OpenClawProperties.class)
public class OpenClawClientConfig {

    /**
     * OpenClaw 网关连接参数，由 Spring Boot 自动注入。
     */
    private final OpenClawProperties properties;

    /**
     * 容器启动阶段校验 OpenClaw 关键配置是否齐全。
     *
     * @throws IllegalStateException 当 {@code base-url}、{@code token}、{@code endpoint} 或
     *                               {@code timeout} 缺失时抛出
     */
    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(properties.baseUrl())) {
            throw new IllegalStateException(
                    "openclaw.base-url must not be empty (set OPENCLAW_GATEWAY_URL or openclaw.base-url)");
        }
        if (!StringUtils.hasText(properties.token())) {
            throw new IllegalStateException(
                    "openclaw.token must not be empty (set OPENCLAW_GATEWAY_TOKEN or openclaw.token); "
                            + "run `openclaw doctor --generate-gateway-token` on the Gateway host to obtain one");
        }
        if (properties.endpoint() == null
                || !StringUtils.hasText(properties.endpoint().chat())
                || !StringUtils.hasText(properties.endpoint().models())) {
            throw new IllegalStateException("openclaw.endpoint.chat and openclaw.endpoint.models must be set");
        }
        if (properties.timeout() == null
                || properties.timeout().connect() == null
                || properties.timeout().read() == null) {
            throw new IllegalStateException("openclaw.timeout.connect and openclaw.timeout.read must be set");
        }
        log.info("OpenClaw client configured: baseUrl={}, defaultModel={}, connectTimeout={}, readTimeout={}",
                properties.baseUrl(), properties.defaultModel(),
                properties.timeout().connect(), properties.timeout().read());
    }

    /**
     * 构造对接 OpenClaw 网关的 {@link RestClient} Bean。
     *
     * @return 已配置好的 {@link RestClient} 实例
     */
    @Bean
    public RestClient openClawRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.timeout().connect().toMillis());
        factory.setReadTimeout((int) properties.timeout().read().toMillis());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
