package com.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 违规记录（超时占用 / 违规区域 / 占用他人车位等）
 */
@Data
@TableName("t_violation")
public class Violation {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String plateNumber;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long vehicleRecordId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long spotId;
    /** overtime超时占用 / illegal_zone违规区域 / occupied_others占位 */
    private String type;
    private String description;
    private Integer points;
    private BigDecimal fineAmount;
    /** 0未处理 1已处理 */
    private Integer status;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operatorId;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
