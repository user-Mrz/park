package com.parking.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 黑名单新增请求：车牌与用户二选一
 */
@Data
public class BlacklistDTO {
    /** 车牌号（与 userId 至少填一个） */
    private String plateNumber;
    /** 用户 ID */
    private Long userId;
    /** 加入原因 */
    private String reason;
    /** 生效截止时间，为空表示永久 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime untilTime;
}
