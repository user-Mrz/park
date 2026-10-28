package com.parking.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访客 / 家长车辆报备请求
 */
@Data
public class VisitReportDTO {
    /** 1访客 2家长（接送） */
    private Integer visitType;
    /** 来访人姓名 */
    private String visitorName;
    /** 来访车牌 */
    private String plateNumber;
    /** 来访事由 */
    private String purpose;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime expectedStart;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime expectedEnd;
}
