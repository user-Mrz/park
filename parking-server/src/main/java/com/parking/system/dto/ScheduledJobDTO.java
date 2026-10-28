package com.parking.system.dto;

import lombok.Data;

/**
 * 定时任务新增 / 修改表单。
 */
@Data
public class ScheduledJobDTO {

    private String jobName;

    private String jobGroup;

    /** 执行器标识，必须是后端白名单中的 key */
    private String handlerKey;

    /** Cron 表达式（秒 分 时 日 月 周） */
    private String cronExpression;

    /** 0 暂停，1 运行中 */
    private Integer status;

    private String remark;
}
