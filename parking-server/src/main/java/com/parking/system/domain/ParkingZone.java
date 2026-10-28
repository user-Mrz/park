package com.parking.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 校园停车区域（教学楼区 / 图书馆区 / 宿舍区 …）。
 * <p>车位通过 t_parking_spot.zone_code 归属到区域。</p>
 */
@Data
@TableName("t_parking_zone")
public class ParkingZone {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 区域编码，小写，创建后不可修改（车位通过该编码归属区域） */
    private String zoneCode;
    private String zoneName;
    private String description;
    private Integer sortOrder;
    /** 1 启用 0 停用 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 展示用：该区域下的车位数，非持久化字段 */
    @TableField(exist = false)
    private Long spotCount;

    /** 展示用：是否为系统内置区域（内置区域不允许删除或停用），非持久化字段 */
    @TableField(exist = false)
    private Boolean builtin;
}
