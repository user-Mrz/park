package com.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_reservation")
public class Reservation {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long userId;
    private Long spotId;
    private String reservationNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal durationHours;
    private BigDecimal totalAmount;
    private Integer status;  // 0-pending 1-active 2-completed 3-cancelled 4-expired
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
