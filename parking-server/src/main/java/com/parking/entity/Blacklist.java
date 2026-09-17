package com.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 黑名单（按车牌或用户）
 */
@Data
@TableName("t_blacklist")
public class Blacklist {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String plateNumber;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String reason;
    /** 生效截止时间，NULL 表示永久 */
    private LocalDateTime untilTime;
    private LocalDateTime createdAt;
}
