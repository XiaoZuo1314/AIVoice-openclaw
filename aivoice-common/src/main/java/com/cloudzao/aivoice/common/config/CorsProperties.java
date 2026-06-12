package com.cloudzao.aivoice.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * 全局跨域（CORS）配置项。
 * <p>
 * 通过 {@code @ConfigurationProperties(prefix = "aivoice.cors")} 自动绑定
 * {@code application.yaml} 中以 {@code aivoice.cors.*} 开头的配置项。
 * 默认仅对 {@link #pathPattern()} 路径（默认 {@code /api/**}）生效，不影响
 * {@code /actuator/**}、{@code /swagger-ui/**}、{@code /v3/api-docs/**} 等同源访问端点。
 * </p>
 *
 * <h3>关键字段说明</h3>
 * <ul>
 *   <li>{@link #allowedOriginPatterns()} —— 使用 {@code allowedOriginPatterns} 而非
 *       {@code allowedOrigins}。Spring 6.0+ 在 {@code allowCredentials=true} 时禁止
 *       {@code allowedOrigins} 包含 {@code *}，但 patterns 形式（{@code http://localhost:[*]}）
 *       仍然支持通配，更适合多端口本地开发场景。</li>
 *   <li>{@link #allowCredentials()} —— 默认 {@code false}。当前阶段以
 *       {@code Authorization: Bearer <jwt>} header 完成鉴权，不依赖 cookie；
 *       阶段三若引入 session/cookie 鉴权再行打开。</li>
 *   <li>{@link #maxAge()} —— 浏览器对预检（OPTIONS）结果的缓存时长，默认 1 小时，
 *       减少高频调用时的预检往返。</li>
 * </ul>
 *
 * @param enabled              CORS 总开关；置 {@code false} 时本配置类不注册任何
 *                             {@code WebMvcConfigurer}，恢复 Spring 默认（无 CORS 头）行为
 * @param pathPattern          CORS 生效的路径模式，默认 {@code /api/**}
 * @param allowedOriginPatterns 允许的来源（origin）模式列表，支持 {@code [*]} 端口通配
 * @param allowedMethods       允许的 HTTP 方法列表
 * @param allowedHeaders       允许的请求 header 列表，{@code "*"} 表示放行所有
 * @param exposedHeaders       响应中允许 JS 读取的 header（默认仅 6 个 simple header 可读，
 *                             如需自定义 header 必须在此显式声明）
 * @param allowCredentials     是否允许携带凭证（cookie / Authorization）。开启后
 *                             {@link #allowedOriginPatterns()} 不能为 {@code "*"}
 * @param maxAge               预检请求结果缓存时长
 * @author cloudzao
 * @date 2026-06-12
 */
@ConfigurationProperties(prefix = "aivoice.cors")
public record CorsProperties(
        Boolean enabled,
        String pathPattern,
        List<String> allowedOriginPatterns,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        List<String> exposedHeaders,
        Boolean allowCredentials,
        Duration maxAge
) {
}
