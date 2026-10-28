package com.parking.system.service;

import com.parking.system.domain.ParkingSpot;
import com.parking.system.domain.User;

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

    /** 给定的「允许停放身份串」是否包含该身份（空串表示不限身份） */
    boolean identityAllowed(String allowIdentities, Integer identityType);

    /** 生效身份：已认证返回用户真实身份，未认证按访客处理（与实际计费口径一致） */
    int effectiveIdentityType(User user);

    /** 该车位允许停放的身份名称（中文，用于提示语） */
    String zoneIdentityNames(ParkingSpot spot);

    /** 把 "1,2,3" 形式的身份串转成中文名称（空表示不限身份） */
    String identityNames(String allowIdentities);

    /** 是否命中黑名单（车牌或用户） */
    boolean isBlacklisted(String plateNumber, Long userId);

    /** 是否命中白名单（车牌或用户） */
    boolean isWhitelisted(String plateNumber, Long userId);

    /** 违规累计达到阈值时自动加入黑名单（白名单对象跳过） */
    void autoBlacklistIfViolationReached(String plateNumber, Long userId, String scene);

    /** 判断指定用户对指定车牌是否有有效的月卡（有效期内且状态为生效） */
    boolean hasActiveMonthlyPass(Long userId, String plateNumber);

    /** 用户端「停车规则」公开配置：仅返回费率/规则类键，不含微信凭据等敏感项 */
    Map<String, String> getPublicRules();

    /** 读取校园规则配置值（带默认值） */
    String config(String key, String defaultValue);

    int intConfig(String key, int defaultValue);

    BigDecimal decimalConfig(String key, BigDecimal defaultValue);
}
