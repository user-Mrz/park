package com.parking.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访客 / 家长车辆报备（由校内师生代为报备，管理员审核）
 */
@Data
@TableName("t_visit_report")
public class VisitReport {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 报备人（校内师生） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ownerUserId;
    /** 1访客 2家长 */
    private Integer visitType;
    private String visitorName;
    private String plateNumber;
    private String purpose;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime expectedStart;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime expectedEnd;
    /** 0待审核 1已通过 2已驳回 3已入场 4已完成 5已过期 */
    private Integer status;
    /** 审核通过后自动创建的访客账号 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long visitorUserId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long spotId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long vehicleRecordId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reviewerId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime reviewTime;
    private String reviewRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 展示用：报备人昵称（学号/工号） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String ownerName;
    /** 展示用：分配车位编号 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String spotCode;
}
