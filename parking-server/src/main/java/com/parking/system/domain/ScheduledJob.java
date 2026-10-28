package com.parking.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态定时任务（管理端可创建 / 修改 / 删除 / 启停 / 立即执行）。
 * <p>
 * 实际执行逻辑由 {@code JobHandlerRegistry} 中的白名单执行器提供，
 * 不允许在数据库中配置任意类名或方法名，避免命令执行风险。
 */
@Data
@TableName("t_scheduled_job")
public class ScheduledJob {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务名称（唯一） */
    private String jobName;

    /** 任务分组，如 SYSTEM / BUSINESS */
    private String jobGroup;

    /** 执行器标识（白名单 key） */
    private String handlerKey;

    /** Cron 表达式（秒 分 时 日 月 周） */
    private String cronExpression;

    /** 0 暂停，1 运行中 */
    private Integer status;

    private String remark;

    private LocalDateTime lastRunTime;

    private String lastRunResult;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
