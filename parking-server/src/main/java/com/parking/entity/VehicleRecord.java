package com.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_vehicle_record")
public class VehicleRecord {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long spotId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reservationId;
    private String plateNumber;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime entryTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime exitTime;
    private Integer durationMinutes;
    private BigDecimal totalFee;
    private Integer paymentStatus;  // 0-unpaid 1-paid
    private Integer status;  // 0-parked 1-exited 2-admin-override
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Transient field - hourly rate of the parking spot, set when querying current record */
    @TableField(exist = false)
    private BigDecimal spotPricePerHour;

    /** Transient field - linked reservation amount, set when record is linked to a reservation */
    @TableField(exist = false)
    private BigDecimal reservationAmount;
}
