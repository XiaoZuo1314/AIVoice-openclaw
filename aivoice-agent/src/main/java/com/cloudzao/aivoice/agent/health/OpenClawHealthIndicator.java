package com.cloudzao.aivoice.agent.health;

import com.cloudzao.aivoice.agent.llm.OpenClawClient;
import com.cloudzao.aivoice.common.config.OpenClawProperties;
import com.cloudzao.aivoice.common.exception.OpenClawException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * OpenClaw 网关连通性健康指示器。
 *
 * @author cloudzao
 * @date 2026-06-11
 */
@Slf4j
@Component("openclaw")
@RequiredArgsConstructor
public class OpenClawHealthIndicator implements HealthIndicator {

    private final OpenClawClient client;
    private final OpenClawProperties properties;

    @Override
    public Health health() {
        long start = System.currentTimeMillis();
        try {
            String body = client.listModelsRaw();
            long latency = System.currentTimeMillis() - start;
            return Health.up()
                    .withDetail("baseUrl", properties.baseUrl())
                    .withDetail("endpoint", properties.endpoint().models())
                    .withDetail("latencyMs", latency)
                    .withDetail("responseSize", body == null ? 0 : body.length())
                    .build();
        } catch (OpenClawException ex) {
            return Health.down()
                    .withDetail("baseUrl", properties.baseUrl())
                    .withDetail("upstreamStatus", ex.getStatus())
                    .withDetail("error", ex.getMessage())
                    .build();
        } catch (Exception ex) {
            log.warn("OpenClaw health check failed: {}", ex.getMessage());
            return Health.down(ex)
                    .withDetail("baseUrl", properties.baseUrl())
                    .build();
        }
    }
}
