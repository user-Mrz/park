package com.parking.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 开通 / 续费月卡请求 DTO。
 */
@Data
public class MonthlyPassOpenDTO {
    /** 1 月卡 2 季卡 3 年卡 */
    @NotNull(message = "请选择卡类型")
    private Integer passType;

    /** 生效车牌（默认取用户已绑定车牌，也可显式指定） */
    private String plateNumber;

    /** 允许停放区域，空字符串表示全场 */
    private String zoneCode;

    /** 可选，手动指定起始日期；不填则以当日为起点 */
    private String startDate;
}
