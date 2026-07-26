package com.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_vehicle_record")
public class VehicleRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long spotId;
    private Long reservationId;
    private String plateNumber;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Integer durationMinutes;
    private BigDecimal totalFee;
    private Integer paymentStatus;  // 0-unpaid 1-paid
    private Integer status;  // 0-parked 1-exited 2-admin-override
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
