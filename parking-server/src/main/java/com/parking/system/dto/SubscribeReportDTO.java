package com.parking.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 前端 requestSubscribeMessage 授权结果上报 DTO。
 */
@Data
public class SubscribeReportDTO {
    /** 场景标识：reserve_remind / balance_low / identity_result / monthly_pass_expire */
    @NotBlank(message = "场景标识不能为空")
    private String scene;

    /** 本次上报对应的微信订阅消息模板 ID（前端从后端 /subscribe/templates 接口拿到） */
    @NotBlank(message = "模板 ID 不能为空")
    private String templateId;

    /** 1 = 用户同意订阅，0 = 用户拒绝 */
    @NotNull(message = "订阅结果不能为空")
    private Integer subscribed;

    /** 关联业务号，如预约号 RESxxx 或用户 ID 字符串，用于后续定时发送时匹配目标 */
    private String bizRef;
}
