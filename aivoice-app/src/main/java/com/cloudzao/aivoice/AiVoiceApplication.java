package com.cloudzao.aivoice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AIVoice 应用启动类。
 * <p>
 * 通过 {@code @SpringBootApplication} 自动扫描 {@code com.cloudzao.aivoice} 及其所有子包，
 * 聚合 aivoice-common / aivoice-user / aivoice-agent 三个模块的 Bean。
 * </p>
 *
 * @author cloudzao
 * @date 2026-06-11
 */
@SpringBootApplication
public class AiVoiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiVoiceApplication.class, args);
    }
}
