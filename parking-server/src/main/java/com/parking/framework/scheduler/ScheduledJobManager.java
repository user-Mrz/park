package com.parking.framework.scheduler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态定时任务调度器。
 * <p>
 * 内部维护独立的线程池，不注册为 Spring 的 TaskScheduler Bean，
 * 避免影响 {@code @Scheduled} 注解驱动的固定任务（车位推送、订阅消息队列等）。
 */
@Slf4j
@Component
public class ScheduledJobManager {

    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    /** jobId -> 调度句柄；取消 / 修改时用于停止旧调度 */
    private final Map<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("scheduled-job-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
    }

    /** 按 Cron 表达式调度任务；同一 jobId 的旧调度会先被取消 */
    public void schedule(Long jobId, String cronExpression, Runnable task) {
        cancel(jobId);
        ScheduledFuture<?> future = scheduler.schedule(task, new CronTrigger(cronExpression));
        if (future != null) {
            futures.put(jobId, future);
        }
    }

    /** 取消指定任务的调度 */
    public void cancel(Long jobId) {
        ScheduledFuture<?> future = futures.remove(jobId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /** 立即异步执行一次（不影响原有 Cron 调度） */
    public void runOnce(Runnable task) {
        scheduler.execute(task);
    }

    public boolean isScheduled(Long jobId) {
        ScheduledFuture<?> future = futures.get(jobId);
        return future != null && !future.isCancelled() && !future.isDone();
    }

    @PreDestroy
    public void shutdown() {
        futures.values().forEach(future -> future.cancel(false));
        futures.clear();
        scheduler.shutdown();
    }
}
