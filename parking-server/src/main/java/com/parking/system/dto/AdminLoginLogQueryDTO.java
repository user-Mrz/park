package com.parking.system.dto;

import lombok.Data;

/**
 * 登录日志分页查询入参（所有字段可选）。
 */
@Data
public class AdminLoginLogQueryDTO {
    /** 模糊匹配用户名 */
    private String username;
    /** SUCCESS / FAIL */
    private String loginStatus;
    /** 起始时间 yyyy-MM-dd HH:mm:ss */
    private String startTime;
    /** 结束时间 yyyy-MM-dd HH:mm:ss */
    private String endTime;
}
