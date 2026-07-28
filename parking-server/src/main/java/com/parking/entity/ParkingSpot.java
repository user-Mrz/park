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
@TableName("t_parking_spot")
public class ParkingSpot {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String spotCode;
    private String area;
    private String floor;
    private Integer status;  // 0-vacant 1-occupied 2-reserved 3-maintenance
    private BigDecimal pricePerHour;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
