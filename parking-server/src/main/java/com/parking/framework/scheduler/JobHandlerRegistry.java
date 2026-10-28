package com.parking.framework.scheduler;

import com.parking.system.service.ScheduledTaskService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 可被定时任务调度的执行器白名单。
 * <p>
 * 管理端只能从该列表中选择执行器，避免通过数据库配置任意类名 / 方法名。
 */
@Component
@RequiredArgsConstructor
public class JobHandlerRegistry {

    private final ScheduledTaskService scheduledTaskService;
    private final Map<String, JobHandler> handlers = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        register(new JobHandler("expireReservations", "预约超时自动释放",
                "扫描已过期预约，释放车位并标记预约状态", "0 * * * * ?", scheduledTaskService::expireReservations));
        register(new JobHandler("syncTidalBatches", "潮汐批次状态同步",
                "同步待开放 / 开放中 / 已结束状态，并清理已结束批次车位", "0 * * * * ?", scheduledTaskService::syncTidalBatches));
        register(new JobHandler("reconcileSpotCache", "车位缓存对账",
                "比对 Redis 与 MySQL 车位状态，修复缓存漂移", "*/30 * * * * ?", scheduledTaskService::reconcileSpotCache));
        register(new JobHandler("expireMonthlyPasses", "月卡到期流转",
                "将已到期的生效月卡批量置为已过期", "0 5 0 * * ?", scheduledTaskService::expireMonthlyPasses));
    }

    private void register(JobHandler handler) {
        handlers.put(handler.key(), handler);
    }

    /** 按执行器 key 获取；不存在返回 null */
    public JobHandler get(String key) {
        return key == null ? null : handlers.get(key);
    }

    /** 全部执行器（只读） */
    public Collection<JobHandler> all() {
        return Collections.unmodifiableCollection(handlers.values());
    }
}
