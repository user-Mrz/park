package com.parking.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 潮汐（临时）车位批次。
 *
 * <p>用于开学迎新、考试、比赛等高峰期的临时停车需求：管理员新建一个批次即批量生成一批
 * 带有明确开放时间段的临时车位。开放时间到达前 {@code tidal_reserve_advance_days} 天
 * 起对用户端可见且仅可预约，进入开放时间段后自动可入场停车。</p>
 *
 * <p>注意：用户端可见性与可入场判定<b>一律按 openTime / closeTime 实时计算</b>，
 * status 字段只用于管理端展示与统计，不作为门禁判断依据（避免定时任务延迟导致误判）。</p>
 */
@Data
@TableName("t_tidal_batch")
public class TidalBatch {

    /** 批次状态：待开放 */
    public static final int STATUS_PENDING = 0;
    /** 批次状态：开放中 */
    public static final int STATUS_OPEN = 1;
    /** 批次状态：已结束 */
    public static final int STATUS_CLOSED = 2;
    /** 批次状态：已取消 */
    public static final int STATUS_CANCELLED = 3;
    /** 批次状态：已删除（软删除，管理端列表隐藏，但保留车位与历史记录以便溯源） */
    public static final int STATUS_DELETED = 4;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String batchName;
    /** 用户端展示的区域名，如「东门广场临时停车区」 */
    private String area;
    /** 分区编码，固定为 tidal */
    private String zoneCode;
    /** 车位编号前缀，最终编号形如 TD-001 */
    private String spotPrefix;
    private Integer spotCount;
    private String floor;
    private BigDecimal pricePerHour;
    /** 允许停放的身份类型，逗号分隔；为空表示不限 */
    private String allowIdentities;
    private Integer freeMinutes;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
