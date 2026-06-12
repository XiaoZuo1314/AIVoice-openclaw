package com.cloudzao.aivoice.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步执行器配置。
 * <p>
 * 当前仅提供 {@code chatStreamExecutor}，专用于 SSE 流式对话场景：
 * Controller 立即把请求 submit 到该线程池后返回 {@link org.springframework.web.servlet.mvc.method.annotation.SseEmitter SseEmitter}，
 * 池里的工作线程负责拉取上游 OpenClaw 的 SSE 并逐 chunk 推给客户端。
 * </p>
 * <p>
 * 参数选择说明：
 * <ul>
 *   <li>{@code corePoolSize=4} —— MVP 阶段同时在线的流式会话数较少；</li>
 *   <li>{@code maxPoolSize=16} —— 突发并发上限，仍远低于 Tomcat 默认 200 工作线程；</li>
 *   <li>{@code queueCapacity=64} —— 满载后再排队，避免瞬时拒绝；</li>
 *   <li>{@code rejectedExecutionHandler=CallerRunsPolicy} —— 池+队列都满时回灌给 Tomcat 线程，自然降级；</li>
 *   <li>{@code waitForTasksToCompleteOnShutdown=true}+{@code awaitTerminationSeconds=30} —— 优雅停机，避免连接被强行掐断。</li>
 * </ul>
 * </p>
 *
 * @author cloudzao
 * @date 2026-06-12
 */
@Slf4j
@Configuration
public class AsyncConfig {

    /**
     * 流式聊天专用执行器。Bean 名等于字段名，Spring 会按名称注入到
     * {@code ChatController} 的同名构造参数。
     *
     * @return 已 initialize 的 {@link ThreadPoolTaskExecutor}
     */
    @Bean(name = "chatStreamExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor chatStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(64);
        executor.setThreadNamePrefix("chat-stream-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("chatStreamExecutor initialized: core=4, max=16, queue=64, prefix=chat-stream-");
        return executor;
    }
}
