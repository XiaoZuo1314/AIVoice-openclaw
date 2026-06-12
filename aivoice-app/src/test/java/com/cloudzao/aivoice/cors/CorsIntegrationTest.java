package com.cloudzao.aivoice.cors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局 CORS 配置端到端验证。
 * <p>
 * 验证 {@link com.cloudzao.aivoice.common.config.CorsConfig} 在容器启动后是否
 * 正确把 {@code aivoice.cors.*} 配置应用到 {@code /api/**} 路径上。仅覆盖
 * OPTIONS 预检场景（最能体现 CORS 配置生效与否，且不会触达 OpenClaw 上游）。
 * </p>
 *
 * @author cloudzao
 * @date 2026-06-12
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsIntegrationTest {

    /**
     * 业务路径下任一已注册的 endpoint 即可，预检与具体方法实现无关。
     */
    private static final String API_PATH = "/api/v1/agents/chat";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("OPTIONS 预检：允许的 origin pattern（http://localhost:[*]）应放行并返回完整 CORS 响应头")
    void preflight_allowedLocalhostPort_returnsExpectedHeaders() throws Exception {
        mvc.perform(options(API_PATH)
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Max-Age", "3600"))
                .andExpect(header().string("Vary", containsString("Origin")));
    }

    @Test
    @DisplayName("OPTIONS 预检：不在 origin pattern 白名单内的来源应被拒绝（403）")
    void preflight_disallowedOrigin_returnsForbidden() throws Exception {
        mvc.perform(options(API_PATH)
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OPTIONS 预检：generic 域名 pattern（https://app.test.aivoice.cloudzao.com）应放行")
    void preflight_allowedExactDomain_returnsExpectedHeaders() throws Exception {
        mvc.perform(options(API_PATH)
                        .header(HttpHeaders.ORIGIN, "https://app.test.aivoice.cloudzao.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin",
                        "https://app.test.aivoice.cloudzao.com"));
    }

    @Test
    @DisplayName("OPTIONS 预检：actuator 路径不在 /api/** 范围内，CORS 不生效（无 Allow-Origin 头）")
    void preflight_actuatorPath_noCorsHeaders() throws Exception {
        mvc.perform(options("/actuator/health")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
