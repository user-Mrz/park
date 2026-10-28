package com.parking.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 潮汐车位批次响应对象（管理端与用户端共用）。
 *
 * <p>phase / canReserve / canEnter 由 openTime、closeTime 与「提前预约天数」实时计算，
 * 不依赖数据库中的 status，避免定时任务延迟造成状态误判。</p>
 */
@Data
public class TidalBatchVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String batchName;
    private String area;
    private String zoneCode;
    private String spotPrefix;
    private Integer spotCount;
    private String floor;
    private BigDecimal pricePerHour;
    private String allowIdentities;
    /** 允许停放身份的中文名，如「学生/教师/职工」 */
    private String zoneIdentityNames;
    private Integer freeMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime openTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime closeTime;
    /** 开放时间前 N 天（N = 提前预约天数），用户端从此刻起可见并可预约 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime reserveFromTime;

    /** upcoming 开放前·仅可预约 / open 开放中·可入场 / ended 已结束 */
    private String phase;
    private String phaseLabel;
    private Boolean canReserve;
    private Boolean canEnter;
    /** 距开放还有几天（向上取整），已开放或已结束为 0 */
    private Integer daysUntilOpen;
    /** 提前预约天数（便于前端展示，不硬编码） */
    private Integer advanceDays;

    /** 库中状态：0待开放 1开放中 2已结束 3已取消 */
    private Integer status;
    private String statusLabel;

    private Integer vacantCount;
    private Integer occupiedCount;
    private Integer reservedCount;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    /** 批次下的车位（用户端列表与「查看车位」抽屉使用） */
    private List<SpotItem> spots;

    @Data
    public static class SpotItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String spotCode;
        private Integer status;
        private String floor;
    }
}
