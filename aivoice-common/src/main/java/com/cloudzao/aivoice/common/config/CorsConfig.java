package com.cloudzao.aivoice.common.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 全局跨域（CORS）配置。
 * <p>
 * 通过 {@link WebMvcConfigurer#addCorsMappings(CorsRegistry)} 把 {@link CorsProperties}
 * 配置注册到 Spring MVC 的 CORS 处理链：
 * <ul>
 *   <li>对 {@link CorsProperties#pathPattern()}（默认 {@code /api/**}）路径下的请求自动添加
 *       {@code Access-Control-Allow-*} 响应头</li>
 *   <li>自动响应 {@code OPTIONS} 预检请求，无需在每个 Controller 上手写</li>
 *   <li>SSE 流式接口（{@code /api/v1/agents/chat/stream}）也由本配置统一覆盖，
 *       浏览器 {@code EventSource} 会按 SSE 标准走 CORS 流程</li>
 * </ul>
 * </p>
 *
 * <h3>为什么不用 {@link org.springframework.web.cors.CorsConfigurationSource}</h3>
 * <ul>
 *   <li>当前阶段没有 Spring Security，全局 {@link WebMvcConfigurer} 路径最简洁</li>
 *   <li>阶段三接入 Spring Security 时再迁移到
 *       {@code CorsConfigurationSource} Bean，让 Security 过滤器链统一接管</li>
 * </ul>
 *
 * <h3>关闭 CORS</h3>
 * 通过设置 {@code aivoice.cors.enabled=false}（或环境变量 {@code AIVOICE_CORS_ENABLED=false}）
 * 整体禁用本配置，{@link ConditionalOnProperty} 会让本类不被加载。
 *
 * @author cloudzao
 * @date 2026-06-12
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CorsProperties.class)
@ConditionalOnProperty(prefix = "aivoice.cors", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProperties properties;

    /**
     * 容器启动阶段对 CORS 关键字段做 fail-fast 校验，避免运行期才发现配置缺失。
     *
     * @throws IllegalStateException 当 {@code path-pattern} 或 {@code allowed-origin-patterns} 缺失，
     *                               或 {@code allow-credentials=true} 时仍允许 {@code "*"} origin
     */
    @PostConstruct
    void validate() {
        if (properties.pathPattern() == null || properties.pathPattern().isBlank()) {
            throw new IllegalStateException(
                    "aivoice.cors.path-pattern must not be empty (e.g. \"/api/**\")");
        }
        if (CollectionUtils.isEmpty(properties.allowedOriginPatterns())) {
            throw new IllegalStateException(
                    "aivoice.cors.allowed-origin-patterns must contain at least one entry; "
                            + "use \"http://localhost:[*]\" for local dev or set AIVOICE_CORS_ALLOWED_ORIGINS in prod");
        }
        if (Boolean.TRUE.equals(properties.allowCredentials())
                && properties.allowedOriginPatterns().stream().anyMatch("*"::equals)) {
            throw new IllegalStateException(
                    "aivoice.cors.allow-credentials=true is incompatible with allowed-origin-patterns containing \"*\"; "
                            + "narrow the patterns to specific hosts (e.g. \"https://*.aivoice.cloudzao.com\")");
        }
        log.info("CORS configured: pathPattern={}, allowedOriginPatterns={}, methods={}, allowCredentials={}, maxAge={}",
                properties.pathPattern(),
                properties.allowedOriginPatterns(),
                properties.allowedMethods(),
                properties.allowCredentials(),
                properties.maxAge());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var registration = registry.addMapping(properties.pathPattern())
                .allowedOriginPatterns(toArray(properties.allowedOriginPatterns()))
                .allowedMethods(toArray(defaultIfEmpty(properties.allowedMethods(),
                        List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"))))
                .allowedHeaders(toArray(defaultIfEmpty(properties.allowedHeaders(), List.of("*"))))
                .allowCredentials(Boolean.TRUE.equals(properties.allowCredentials()));

        if (!CollectionUtils.isEmpty(properties.exposedHeaders())) {
            registration.exposedHeaders(toArray(properties.exposedHeaders()));
        }
        if (properties.maxAge() != null) {
            registration.maxAge(properties.maxAge().getSeconds());
        }
    }

    private static String[] toArray(List<String> list) {
        return list.toArray(String[]::new);
    }

    private static List<String> defaultIfEmpty(List<String> list, List<String> fallback) {
        return CollectionUtils.isEmpty(list) ? fallback : list;
    }
}
