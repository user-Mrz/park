package com.parking.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.admin.service.ScheduledJobService;
import com.parking.common.BusinessException;
import com.parking.framework.scheduler.JobHandler;
import com.parking.framework.scheduler.JobHandlerRegistry;
import com.parking.framework.scheduler.ScheduledJobManager;
import com.parking.system.domain.ScheduledJob;
import com.parking.system.dto.ScheduledJobDTO;
import com.parking.system.mapper.ScheduledJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定时任务管理实现：数据库配置 + Cron 动态调度。
 * <p>
 * 应用启动完成后自动加载 status=1 的任务；新增/修改/启停会即时重新调度。
 * 同一任务上一次尚未执行完成时，本次触发会被跳过，避免任务堆叠。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledJobServiceImpl implements ScheduledJobService {

    private final ScheduledJobMapper scheduledJobMapper;
    private final JobHandlerRegistry jobHandlerRegistry;
    private final ScheduledJobManager scheduledJobManager;

    /** 正在执行中的 jobId，用于跳过重叠触发 */
    private final Set<Long> runningJobs = ConcurrentHashMap.newKeySet();

    @Override
    public Page<ScheduledJob> page(int page, int pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<ScheduledJob> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(ScheduledJob::getJobName, kw)
                    .or().like(ScheduledJob::getHandlerKey, kw)
                    .or().like(ScheduledJob::getRemark, kw));
        }
        if (status != null) {
            wrapper.eq(ScheduledJob::getStatus, status);
        }
        wrapper.orderByAsc(ScheduledJob::getId);
        return scheduledJobMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public List<Map<String, Object>> handlers() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JobHandler handler : jobHandlerRegistry.all()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", handler.key());
            item.put("name", handler.name());
            item.put("description", handler.description());
            item.put("defaultCron", handler.defaultCron());
            list.add(item);
        }
        return list;
    }

    @Override
    public ScheduledJob create(ScheduledJobDTO dto) {
        ScheduledJob job = new ScheduledJob();
        apply(dto, job);
        if (job.getStatus() == null) {
            job.setStatus(0);
        }
        ensureNameUnique(job.getJobName(), null);
        scheduledJobMapper.insert(job);
        ScheduledJob saved = scheduledJobMapper.selectById(job.getId());
        if (isEnabled(saved)) {
            schedule(saved);
        }
        return saved;
    }

    @Override
    public ScheduledJob update(Long id, ScheduledJobDTO dto) {
        ScheduledJob job = require(id);
        apply(dto, job);
        ensureNameUnique(job.getJobName(), id);
        scheduledJobMapper.updateById(job);
        ScheduledJob saved = scheduledJobMapper.selectById(id);
        if (isEnabled(saved)) {
            schedule(saved);
        } else {
            scheduledJobManager.cancel(id);
        }
        return saved;
    }

    @Override
    public void delete(Long id) {
        require(id);
        scheduledJobManager.cancel(id);
        runningJobs.remove(id);
        scheduledJobMapper.deleteById(id);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("任务状态不合法（0 暂停 / 1 运行）");
        }
        ScheduledJob job = require(id);
        job.setStatus(status);
        scheduledJobMapper.updateById(job);
        if (status == 1) {
            schedule(job);
        } else {
            scheduledJobManager.cancel(id);
        }
    }

    @Override
    public void runOnce(Long id) {
        ScheduledJob job = require(id);
        JobHandler handler = jobHandlerRegistry.get(job.getHandlerKey());
        if (handler == null) {
            throw new BusinessException("执行器不存在，无法执行");
        }
        scheduledJobManager.runOnce(wrapper(job, handler));
    }

    /**
     * 应用启动完成后加载启用的任务。
     * 表不存在或单条任务配置异常只记录日志，不阻塞应用启动。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadEnabledJobs() {
        List<ScheduledJob> jobs;
        try {
            jobs = scheduledJobMapper.selectList(
                    new LambdaQueryWrapper<ScheduledJob>().eq(ScheduledJob::getStatus, 1));
        } catch (Exception e) {
            log.warn("Load scheduled jobs failed (table t_scheduled_job missing?): {}", e.getMessage());
            return;
        }
        int loaded = 0;
        for (ScheduledJob job : jobs) {
            try {
                JobHandler handler = jobHandlerRegistry.get(job.getHandlerKey());
                if (handler == null) {
                    log.warn("Scheduled job [{}] skipped: unknown handler '{}'", job.getJobName(), job.getHandlerKey());
                    continue;
                }
                if (!CronExpression.isValidExpression(job.getCronExpression())) {
                    log.warn("Scheduled job [{}] skipped: invalid cron '{}'", job.getJobName(), job.getCronExpression());
                    continue;
                }
                scheduledJobManager.schedule(job.getId(), job.getCronExpression(), wrapper(job, handler));
                loaded++;
            } catch (Exception e) {
                log.warn("Scheduled job [{}] failed to start: {}", job.getJobName(), e.getMessage());
            }
        }
        log.info("Loaded {} enabled scheduled job(s)", loaded);
    }

    // ------------------------------------------------------------------

    private void schedule(ScheduledJob job) {
        JobHandler handler = jobHandlerRegistry.get(job.getHandlerKey());
        if (handler == null) {
            throw new BusinessException("执行器不存在，无法调度");
        }
        if (!CronExpression.isValidExpression(job.getCronExpression())) {
            throw new BusinessException("Cron 表达式不合法");
        }
        scheduledJobManager.schedule(job.getId(), job.getCronExpression(), wrapper(job, handler));
    }

    /** 包装执行逻辑：防重叠 + 记录上次执行时间与结果 */
    private Runnable wrapper(ScheduledJob job, JobHandler handler) {
        return () -> {
            if (!runningJobs.add(job.getId())) {
                log.warn("Scheduled job [{}] is still running, skip this trigger", job.getJobName());
                return;
            }
            long start = System.currentTimeMillis();
            String result;
            try {
                handler.task().run();
                result = "成功（耗时 " + (System.currentTimeMillis() - start) + " ms）";
            } catch (Exception e) {
                log.error("Scheduled job [{}] failed: {}", job.getJobName(), e.getMessage(), e);
                result = "失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            } finally {
                runningJobs.remove(job.getId());
            }
            recordRunResult(job.getId(), result);
        };
    }

    private void recordRunResult(Long jobId, String result) {
        try {
            scheduledJobMapper.update(null, new LambdaUpdateWrapper<ScheduledJob>()
                    .eq(ScheduledJob::getId, jobId)
                    .set(ScheduledJob::getLastRunTime, LocalDateTime.now())
                    .set(ScheduledJob::getLastRunResult, truncate(result, 500)));
        } catch (Exception e) {
            log.warn("Record scheduled job result failed: {}", e.getMessage());
        }
    }

    private void apply(ScheduledJobDTO dto, ScheduledJob job) {
        if (dto == null) {
            throw new BusinessException("任务信息不能为空");
        }
        String name = dto.getJobName() == null ? "" : dto.getJobName().trim();
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("任务名称不能为空");
        }
        if (name.length() > 100) {
            throw new BusinessException("任务名称不能超过 100 个字符");
        }
        String handlerKey = dto.getHandlerKey() == null ? "" : dto.getHandlerKey().trim();
        if (jobHandlerRegistry.get(handlerKey) == null) {
            throw new BusinessException("执行器不存在或已下线");
        }
        String cron = dto.getCronExpression() == null ? "" : dto.getCronExpression().trim();
        if (!StringUtils.hasText(cron)) {
            throw new BusinessException("Cron 表达式不能为空");
        }
        if (cron.length() > 100 || !CronExpression.isValidExpression(cron)) {
            throw new BusinessException("Cron 表达式不合法，示例：0 */5 * * * ?");
        }
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw new BusinessException("任务状态不合法（0 暂停 / 1 运行）");
        }
        job.setJobName(name);
        job.setJobGroup(StringUtils.hasText(dto.getJobGroup()) ? dto.getJobGroup().trim() : "SYSTEM");
        job.setHandlerKey(handlerKey);
        job.setCronExpression(cron);
        job.setRemark(dto.getRemark() == null ? "" : dto.getRemark().trim());
        if (dto.getStatus() != null) {
            job.setStatus(dto.getStatus());
        }
    }

    private void ensureNameUnique(String jobName, Long excludeId) {
        Long count = scheduledJobMapper.selectCount(new LambdaQueryWrapper<ScheduledJob>()
                .eq(ScheduledJob::getJobName, jobName)
                .ne(excludeId != null, ScheduledJob::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("任务名称已存在");
        }
    }

    private ScheduledJob require(Long id) {
        ScheduledJob job = scheduledJobMapper.selectById(id);
        if (job == null) {
            throw new BusinessException("定时任务不存在");
        }
        return job;
    }

    private boolean isEnabled(ScheduledJob job) {
        return job != null && job.getStatus() != null && job.getStatus() == 1;
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
