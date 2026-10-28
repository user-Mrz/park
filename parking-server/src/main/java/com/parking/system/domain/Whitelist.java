package com.parking.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 白名单（按车牌或用户）：豁免门禁与预约限制，访客报备自动通过
 */
@Data
@TableName("t_whitelist")
public class Whitelist {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String plateNumber;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String reason;
    /** 生效截止时间，NULL 表示永久 */
    private LocalDateTime untilTime;
    /** 来源：manual 手动 / system 系统 */
    private String source;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
