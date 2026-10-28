package com.parking.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 潮汐车位批次的创建 / 修改请求体。
 * 开放与结束时间精确到分钟，前端以 "yyyy-MM-dd HH:mm:ss" 提交。
 */
@Data
public class TidalBatchDTO {

    private String batchName;
    /** 用户端展示的区域名，如「东门广场临时停车区」 */
    private String area;
    /** 车位编号前缀，最终编号形如 TD-001 */
    private String spotPrefix;
    private Integer spotCount;
    private String floor;
    private BigDecimal pricePerHour;
    /** 允许停放的身份类型，逗号分隔；为空表示不限 */
    private String allowIdentities;
    private Integer freeMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime openTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime closeTime;

    private String remark;
}
