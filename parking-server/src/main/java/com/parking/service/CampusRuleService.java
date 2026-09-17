package com.parking.service;

import com.parking.entity.ParkingSpot;
import com.parking.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 校园规则服务：身份差异化费率、免费时长、预约限制、校园分区规则
 */
public interface CampusRuleService {

    /** 用户当前享受的校园权益与限制（供小程序展示） */
    Map<String, Object> benefitsOf(User user);

    /** 计算停车费（已扣减身份免费时长、应用身份系数与假期折扣） */
    BigDecimal calculateParkingFee(User user, ParkingSpot spot, long minutes);

    /** 预约金额（按完整时长计费；免费时长在出场结算时抵扣） */
    BigDecimal calculateReservationAmount(User user, ParkingSpot spot, BigDecimal durationHours);

    /** 预约前置校验：区域权限 / 每日上限 / 夜间禁停 / 假期模式 / 黑名单 */
    void validateReservation(User user, ParkingSpot spot, LocalDateTime start, LocalDateTime end);

    /** 今日剩余可预约次数（-1 表示不限） */
    int remainingDailyQuota(User user);

    /** 该身份享受的免费停车时长（分钟） */
    int freeMinutesOf(User user);

    /** 该车位所在区域是否允许该身份停放 */
    boolean zoneAllowed(ParkingSpot spot, Integer identityType);

    /** 该车位允许停放的身份名称（中文，用于提示语） */
    String zoneIdentityNames(ParkingSpot spot);

    /** 是否命中黑名单（车牌或用户） */
    boolean isBlacklisted(String plateNumber, Long userId);

    /** 读取校园规则配置值（带默认值） */
    String config(String key, String defaultValue);

    int intConfig(String key, int defaultValue);

    BigDecimal decimalConfig(String key, BigDecimal defaultValue);
}
