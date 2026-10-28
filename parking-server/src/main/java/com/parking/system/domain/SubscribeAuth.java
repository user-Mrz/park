package com.parking.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 微信订阅消息授权上报记录。
 * 前端 requestSubscribeMessage 后调 reportSubscription，后端落库后才有权限 sendSubscribeMessage。
 */
@Data
@TableName("t_subscribe_auth")
public class SubscribeAuth {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String templateId;
    /** reserve_remind / balance_low / identity_result / monthly_pass_expire */
    private String scene;
    /** 1 同意订阅 0 拒绝 */
    private Integer subscribed;
    /** 0 未发送 1 发送成功 2 发送失败 */
    private Integer sendStatus;
    private LocalDateTime sendTime;
    /** 关联业务号（预约号 / 业务 ID 字符串等） */
    private String bizRef;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
