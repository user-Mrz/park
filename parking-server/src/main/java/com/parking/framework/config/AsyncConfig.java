package com.parking.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置：为车位缓存异步回写 MySQL 提供专用线程池。
 *
 * <p>使用 {@link ThreadPoolTaskExecutor} 配合有界队列 + 拒绝策略，
 * 防止高峰期回写任务堆积耗尽内存；队列满时记录失败由对账任务兜底。</p>
 *
 * <p><b>容量说明</b>：core=16 / max=64 / queue=2000 适配 8核 16G 服务器，
 * 单条回写任务通常 <5ms，64 线程可承载上万 QPS 的回写需求；
 * 若部署在低配服务器（2核2G），请按硬件同比缩减为 core=4 / max=16 / queue=500。</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 缓存回写 MySQL 的线程池。
     * <ul>
     *   <li>core 16 / max 64 / queue 2000：高并发下保证回写不丢任务</li>
     *   <li>CallerRunsPolicy：队列满时由调用线程直接执行（自然限流，避免丢任务）</li>
     *   <li>线程名 parking-async-：便于线程 dump 与日志排查</li>
     * </ul>
     */
    @Bean("parkingSyncExecutor")
    public Executor parkingSyncExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(16);
        exec.setMaxPoolSize(64);
        exec.setQueueCapacity(2000);
        exec.setKeepAliveSeconds(60); // 空闲线程 60s 回收至 core 大小
        exec.setThreadNamePrefix("parking-async-");
        // 队列满时让调用线程直接同步执行（自然限流，避免丢任务）
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅关闭：等待已提交任务完成
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(30);
        exec.initialize();
        return exec;
    }
}
