package com.parking.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 月卡 / 长租停车权益。
 * 有效期内对指定车牌免按次计费（出场结算返回 0）。
 */
@Data
@TableName("t_monthly_pass")
public class MonthlyPass {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String plateNumber;
    /** 1 月卡 2 季卡 3 年卡 */
    private Integer passType;
    /** 允许停放区域，空字符串表示全场 */
    private String zoneCode;
    private LocalDate startDate;
    private LocalDate expireDate;
    private BigDecimal pricePaid;
    /** 1 生效 2 已过期 3 已退款 */
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 展示用：passType 中文标签，非持久化字段 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String passTypeLabel;

    /** 展示用：剩余天数（今天起），非持久化字段 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Long remainingDays;

    /** 展示用（管理端）：车主昵称，非持久化字段 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String nickname;

    /** 展示用（管理端）：车主手机号，非持久化字段 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String phone;

    /** 展示用（管理端）：车主学号/工号，非持久化字段 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String userNo;

    /** 展示用（管理端）：车主校园身份，非持久化字段 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer identityType;
}
