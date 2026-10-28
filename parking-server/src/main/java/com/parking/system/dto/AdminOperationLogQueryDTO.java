package com.parking.system.dto;

import lombok.Data;

/**
 * 操作日志分页查询入参（所有字段可选）。
 */
@Data
public class AdminOperationLogQueryDTO {
    /** 模糊匹配用户名 */
    private String username;
    /** 精确匹配菜单 code */
    private String menuCode;
    /** CREATE / UPDATE / DELETE / AUDIT */
    private String type;
    /** 起始时间 yyyy-MM-dd HH:mm:ss */
    private String startTime;
    /** 结束时间 yyyy-MM-dd HH:mm:ss */
    private String endTime;
}
