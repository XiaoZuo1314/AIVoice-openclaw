package com.cloudzao.aivoice.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 / Swagger UI 配置。
 * <p>
 * 接入 springdoc-openapi-starter-webmvc-ui，将所有 {@code @RestController} 自动暴露为
 * OpenAPI 3 文档。运行后可访问：
 * <ul>
 *   <li>Swagger UI： {@code /swagger-ui.html}（重定向到 {@code /swagger-ui/index.html}）</li>
 *   <li>OpenAPI JSON： {@code /v3/api-docs}</li>
 *   <li>OpenAPI YAML： {@code /v3/api-docs.yaml}</li>
 * </ul>
 * 元数据（标题、版本、联系人）集中在本文件，避免散落在 {@code application.yaml}。
 * 预先声明 {@code bearerAuth} 安全方案，便于阶段三接入 JWT 鉴权时直接启用。
 * </p>
 *
 * @author cloudzao
 * @date 2026-06-11
 */
@Configuration
public class OpenApiConfig {

    /**
     * 应用名，从 {@code spring.application.name} 注入，便于多服务复用本配置。
     */
    @Value("${spring.application.name:AIVoice}")
    private String applicationName;

    /**
     * 服务监听端口，仅用于在 Swagger UI 中默认填充本地服务器地址。
     */
    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * 构造 OpenAPI Bean，被 springdoc 自动加载为根文档元数据。
     *
     * @return 已填充元信息与安全方案的 {@link OpenAPI} 实例
     */
    @Bean
    public OpenAPI aiVoiceOpenAPI() {
        Info info = new Info()
                .title(applicationName + " Backend API")
                .description("""
                        AIVoice 后端 REST API 文档（MVP 阶段一）。

                        - 链路：客户端 → Spring Boot 业务后端 → 本地 OpenClaw Gateway → LLM
                        - 错误响应统一使用 RFC 7807 `application/problem+json`
                        - 所有接口前缀：`/api/v1`
                        """)
                .version("v1.0")
                .contact(new Contact()
                        .name("cloudzao")
                        .url("https://aivoice.cloudzao.com"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://aivoice.cloudzao.com/license"));

        Server localServer = new Server()
                .url("http://127.0.0.1:" + serverPort)
                .description("本地开发环境");

        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("阶段三启用：在请求头携带 `Authorization: Bearer <jwt>`。");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer))
                .components(new Components().addSecuritySchemes("bearerAuth", bearerAuth));
    }
}
