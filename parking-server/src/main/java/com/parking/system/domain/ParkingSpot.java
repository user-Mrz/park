package com.parking.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_parking_spot")
public class ParkingSpot {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String spotCode;
    private String area;
    /** 校园分区：teaching教学楼区 / library图书馆区 / dorm宿舍区 / gym体育馆区 / visitor访客与家长临时区 */
    private String zoneCode;
    /** 允许停放的身份类型，逗号分隔，如 "1,2,3" */
    private String allowIdentities;
    /** 本区域免费时长（分钟），0 表示按身份规则 */
    private Integer freeMinutes;
    /** 车位类型：0 固定车位 / 1 潮汐临时车位 */
    private Integer spotType;
    /** 潮汐车位所属批次 ID（固定车位为 null） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tidalBatchId;
    /** 潮汐车位开放时间（固定车位为 null）：到点后自动可入场 */
    private LocalDateTime openTime;
    /** 潮汐车位结束时间（固定车位为 null）：过期后不再对用户端展示 */
    private LocalDateTime closeTime;
    private String floor;
    private Integer status;  // 0-vacant 1-occupied 2-reserved 3-maintenance
    private BigDecimal pricePerHour;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
