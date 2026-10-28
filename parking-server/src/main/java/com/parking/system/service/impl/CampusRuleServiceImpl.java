package com.parking.system.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.common.BusinessException;
import com.parking.common.IdentityType;
import com.parking.common.user.UserVisibility;
import com.parking.system.domain.Blacklist;
import com.parking.system.domain.MonthlyPass;
import com.parking.system.domain.ParkingSpot;
import com.parking.system.domain.ParkingZone;
import com.parking.system.domain.Reservation;
import com.parking.system.domain.SystemConfig;
import com.parking.system.domain.User;
import com.parking.system.domain.Violation;
import com.parking.system.domain.Whitelist;
import com.parking.system.mapper.BlacklistMapper;
import com.parking.system.mapper.MonthlyPassMapper;
import com.parking.system.mapper.ParkingZoneMapper;
import com.parking.system.mapper.ReservationMapper;
import com.parking.system.mapper.SystemConfigMapper;
import com.parking.system.mapper.ViolationMapper;
import com.parking.system.mapper.WhitelistMapper;
import com.parking.system.service.CampusRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 校园规则实现：所有身份相关规则集中在此，便于统一调整与测试
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampusRuleServiceImpl implements CampusRuleService {

    private final SystemConfigMapper systemConfigMapper;
    private final ReservationMapper reservationMapper;
    private final BlacklistMapper blacklistMapper;
    private final WhitelistMapper whitelistMapper;
    private final ViolationMapper violationMapper;
    private final MonthlyPassMapper monthlyPassMapper;
    private final ParkingZoneMapper parkingZoneMapper;

    /** 用户端可见的规则类配置键（完整匹配） */
    private static final Set<String> PUBLIC_RULE_KEYS = Set.of(
            "default_hourly_rate", "max_reservation_days", "min_reservation_hours",
            "grace_period_minutes", "semester_mode", "holiday_discount",
            "violation_overtime_grace_minutes", "tidal_reserve_advance_days");

    /** 用户端可见的规则类配置前缀（身份费率 / 每日预约上限 / 夜间禁停） */
    private static final List<String> PUBLIC_RULE_PREFIXES = List.of(
            "rate_", "reserve_daily_limit_", "rule_night_ban_");

    @Override
    public Map<String, String> getPublicRules() {
        Map<String, String> result = new LinkedHashMap<>();
        systemConfigMapper.selectList(null).forEach(c -> {
            String key = c.getConfigKey();
            if (key == null) return;
            // 白名单过滤：避免将微信凭据等敏感配置暴露给小程序用户端
            boolean visible = PUBLIC_RULE_KEYS.contains(key)
                    || PUBLIC_RULE_PREFIXES.stream().anyMatch(key::startsWith);
            if (visible) result.put(key, c.getConfigValue());
        });
        return result;
    }

    @Override
    public String config(String key, String defaultValue) {
        SystemConfig cfg = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        if (cfg == null || !StringUtils.hasText(cfg.getConfigValue())) return defaultValue;
        return cfg.getConfigValue().trim();
    }

    @Override
    public int intConfig(String key, int defaultValue) {
        try {
            return Integer.parseInt(config(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public BigDecimal decimalConfig(String key, BigDecimal defaultValue) {
        try {
            return new BigDecimal(config(key, defaultValue.toPlainString()));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 身份是否已认证（未认证不享受优惠） */
    private boolean verified(User user) {
        return user != null && user.getIdentityStatus() != null
                && user.getIdentityStatus() == IdentityType.AUDIT_APPROVED;
    }

    /** 生效身份：未认证时按访客规则处理 */
    private int effectiveIdentity(User user) {
        return effectiveIdentityType(user);
    }

    @Override
    public int effectiveIdentityType(User user) {
        if (user == null || !verified(user)) return IdentityType.VISITOR;
        Integer t = user.getIdentityType();
        return t == null ? IdentityType.VISITOR : t;
    }

    @Override
    public int freeMinutesOf(User user) {
        int minutes;
        switch (effectiveIdentity(user)) {
            case IdentityType.TEACHER: minutes = intConfig("rate_teacher_free_minutes", 120); break;
            case IdentityType.STAFF: minutes = intConfig("rate_staff_free_minutes", 60); break;
            case IdentityType.STUDENT: minutes = intConfig("rate_student_free_minutes", 15); break;
            case IdentityType.PARENT: minutes = intConfig("rate_parent_free_minutes", 30); break;
            default: minutes = intConfig("rate_visitor_free_minutes", 15); break;
        }
        // 兜底：免费时长限制在 0 ~ 24 小时，避免配置被误填（如 300000）导致计费异常
        if (minutes < 0) minutes = 0;
        if (minutes > 1440) minutes = 1440;
        return minutes;
    }

    private BigDecimal identityMultiplier(User user) {
        switch (effectiveIdentity(user)) {
            case IdentityType.STUDENT:
                return decimalConfig("rate_student_discount", new BigDecimal("0.50"));
            case IdentityType.VISITOR:
                return decimalConfig("rate_visitor_multiplier", new BigDecimal("1.50"));
            default:
                return BigDecimal.ONE;
        }
    }

    /** 假期模式统一折扣系数（学期模式为 1） */
    private BigDecimal seasonMultiplier() {
        if (intConfig("semester_mode", 0) == 1) {
            return decimalConfig("holiday_discount", new BigDecimal("0.80"));
        }
        return BigDecimal.ONE;
    }

    private boolean holidayMode() {
        return intConfig("semester_mode", 0) == 1;
    }

    @Override
    public boolean hasActiveMonthlyPass(Long userId, String plateNumber) {
        if (userId == null || !StringUtils.hasText(plateNumber)) return false;
        Long count = monthlyPassMapper.selectCount(new LambdaQueryWrapper<MonthlyPass>()
                .eq(MonthlyPass::getUserId, userId)
                .eq(MonthlyPass::getPlateNumber, plateNumber)
                .eq(MonthlyPass::getStatus, 1)
                .ge(MonthlyPass::getExpireDate, LocalDate.now()));
        return count != null && count > 0;
    }

    @Override
    public BigDecimal calculateParkingFee(User user, ParkingSpot spot, long minutes) {
        if (spot == null || spot.getPricePerHour() == null) return BigDecimal.ZERO;
        long freeMinutes = Math.max(freeMinutesOf(user), spot.getFreeMinutes() == null ? 0 : spot.getFreeMinutes());
        long billable = Math.max(0L, minutes - freeMinutes);
        if (billable <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long billedHours = (billable + 59) / 60;   // 不足 1 小时按 1 小时（与出场结算口径一致）
        BigDecimal base = spot.getPricePerHour().multiply(BigDecimal.valueOf(billedHours));
        BigDecimal fee = base.multiply(identityMultiplier(user)).multiply(seasonMultiplier());
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateReservationAmount(User user, ParkingSpot spot, BigDecimal durationHours) {
        if (spot == null || spot.getPricePerHour() == null || durationHours == null) return BigDecimal.ZERO;
        BigDecimal base = spot.getPricePerHour().multiply(durationHours);
        BigDecimal amount = base.multiply(identityMultiplier(user)).multiply(seasonMultiplier());
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean zoneAllowed(ParkingSpot spot, Integer identityType) {
        if (spot == null) return false;
        return identityAllowed(spot.getAllowIdentities(), identityType);
    }

    @Override
    public boolean identityAllowed(String allowIdentities, Integer identityType) {
        return UserVisibility.identityAllowed(allowIdentities, identityType);
    }

    @Override
    public String zoneIdentityNames(ParkingSpot spot) {
        return spot == null ? "所有身份" : identityNames(spot.getAllowIdentities());
    }

    @Override
    public boolean isBlacklisted(String plateNumber, Long userId) {
        LambdaQueryWrapper<Blacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> {
            boolean any = false;
            if (StringUtils.hasText(plateNumber)) {
                w.eq(Blacklist::getPlateNumber, plateNumber);
                any = true;
            }
            if (userId != null) {
                if (any) w.or();
                w.eq(Blacklist::getUserId, userId);
            }
        });
        wrapper.and(w -> w.isNull(Blacklist::getUntilTime).or().ge(Blacklist::getUntilTime, LocalDateTime.now()));
        Long count = blacklistMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    public boolean isWhitelisted(String plateNumber, Long userId) {
        if (!StringUtils.hasText(plateNumber) && userId == null) return false;
        LambdaQueryWrapper<Whitelist> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> {
            boolean any = false;
            if (StringUtils.hasText(plateNumber)) {
                w.eq(Whitelist::getPlateNumber, plateNumber);
                any = true;
            }
            if (userId != null) {
                if (any) w.or();
                w.eq(Whitelist::getUserId, userId);
            }
        });
        wrapper.and(w -> w.isNull(Whitelist::getUntilTime).or().ge(Whitelist::getUntilTime, LocalDateTime.now()));
        Long count = whitelistMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    public void autoBlacklistIfViolationReached(String plateNumber, Long userId, String scene) {
        try {
            if (intConfig("blacklist_auto_enabled", 1) != 1) return;
            if (!StringUtils.hasText(plateNumber) && userId == null) return;
            // 白名单车辆不参与自动拉黑；黑名单已存在则无需重复加入
            if (isWhitelisted(plateNumber, userId)) return;
            if (isBlacklisted(plateNumber, userId)) return;

            int windowDays = Math.max(1, intConfig("blacklist_auto_window_days", 30));
            int threshold = Math.max(1, intConfig("blacklist_auto_violation_threshold", 3));
            LambdaQueryWrapper<Violation> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(plateNumber)) {
                wrapper.eq(Violation::getPlateNumber, plateNumber);
            } else {
                wrapper.eq(Violation::getUserId, userId);
            }
            wrapper.ge(Violation::getCreatedAt, LocalDateTime.now().minusDays(windowDays));
            Long count = violationMapper.selectCount(wrapper);
            if (count == null || count < threshold) return;

            int durationDays = intConfig("blacklist_auto_duration_days", 30);
            String sceneLabel = "illegal_zone".equals(scene) ? "违规区域"
                    : "overtime".equals(scene) ? "超时占用" : (scene == null ? "违规" : scene);
            Blacklist b = new Blacklist();
            b.setId(IdUtil.getSnowflake(1, 10).nextId());
            b.setPlateNumber(StringUtils.hasText(plateNumber) ? plateNumber : null);
            b.setUserId(StringUtils.hasText(plateNumber) ? null : userId);
            b.setReason("系统自动拉黑：近 " + windowDays + " 天累计 " + count + " 次违规（" + sceneLabel + "）");
            b.setSource("auto_violation");
            b.setUntilTime(durationDays <= 0 ? null : LocalDateTime.now().plusDays(durationDays));
            b.setCreatedAt(LocalDateTime.now());
            b.setUpdatedAt(LocalDateTime.now());
            blacklistMapper.insert(b);
            log.info("Auto blacklist added: plate={}, userId={}, count={}", plateNumber, userId, count);
        } catch (Exception e) {
            // 自动拉黑失败不能影响违规记录本身的写入
            log.warn("Auto blacklist check failed: {}", e.getMessage());
        }
    }

    @Override
    public String identityNames(String allowIdentities) {
        if (!StringUtils.hasText(allowIdentities)) return "所有身份";
        List<String> names = new ArrayList<>();
        for (String s : allowIdentities.split(",")) {
            try {
                names.add(IdentityType.label(Integer.parseInt(s.trim())));
            } catch (NumberFormatException ignore) {
                // 忽略非法配置
            }
        }
        return names.isEmpty() ? "所有身份" : String.join("/", names);
    }

    /** 夜间禁停时段：[start, end)，支持跨午夜 */
    private boolean inNightBanWindow(LocalDateTime start, LocalDateTime end) {
        String startStr = config("rule_night_ban_start", "23:00");
        String endStr = config("rule_night_ban_end", "06:00");
        if (!StringUtils.hasText(startStr) || !StringUtils.hasText(endStr)) return false;
        LocalTime banStart = LocalTime.parse(startStr);
        LocalTime banEnd = LocalTime.parse(endStr);

        LocalDateTime cursor = start;
        // 以 30 分钟步进取样，判断预约时段是否与禁停窗口重叠（最长 30 天）
        while (!cursor.isAfter(end)) {
            LocalTime t = cursor.toLocalTime();
            boolean inWindow = banStart.isBefore(banEnd)
                    ? (t.compareTo(banStart) >= 0 && t.isBefore(banEnd))
                    : (t.compareTo(banStart) >= 0 || t.isBefore(banEnd));
            if (inWindow) return true;
            cursor = cursor.plusMinutes(30);
        }
        return false;
    }

    @Override
    public int remainingDailyQuota(User user) {
        int identity = effectiveIdentity(user);
        int limit;
        switch (identity) {
            case IdentityType.TEACHER: limit = intConfig("reserve_daily_limit_teacher", 5); break;
            case IdentityType.STAFF: limit = intConfig("reserve_daily_limit_staff", 3); break;
            case IdentityType.STUDENT: limit = intConfig("reserve_daily_limit_student", 2); break;
            default: limit = intConfig("reserve_daily_limit_visitor", 1); break;
        }
        if (limit <= 0) return -1;   // 0 或负数表示不限
        Long used = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, user.getId())
                .ge(Reservation::getCreatedAt, LocalDate.now().atStartOfDay()));
        long usedCount = used == null ? 0 : used;
        return (int) Math.max(0, limit - usedCount);
    }

    private int dailyLimitOf(User user) {
        switch (effectiveIdentity(user)) {
            case IdentityType.TEACHER: return intConfig("reserve_daily_limit_teacher", 5);
            case IdentityType.STAFF: return intConfig("reserve_daily_limit_staff", 3);
            case IdentityType.STUDENT: return intConfig("reserve_daily_limit_student", 2);
            default: return intConfig("reserve_daily_limit_visitor", 1);
        }
    }

    @Override
    public void validateReservation(User user, ParkingSpot spot, LocalDateTime start, LocalDateTime end) {
        if (spot == null) throw new BusinessException("车位不存在");
        int identity = effectiveIdentity(user);

        // 黑名单优先：命中后直接拒绝，白名单不能抵消
        if (isBlacklisted(user.getPlateNumber(), user.getId())) {
            throw new BusinessException("当前车牌或账号已被列入黑名单，无法预约，请联系停车管理处");
        }
        boolean white = isWhitelisted(user.getPlateNumber(), user.getId());

        if (!white && !verified(user)) {
            // 未认证用户不享受校内优惠，按访客规则预约访客区
            throw new BusinessException("身份尚未通过认证，暂无法预约车位；请在「账号信息」中补全学号/工号等待管理员审核");
        }

        if (!white && !zoneAllowed(spot, identity)) {
            throw new BusinessException("该区域（" + zoneName(spot.getZoneCode()) + "）仅限"
                    + identityNames(spot.getAllowIdentities()) + "停放，请选择其他区域");
        }

        if (!white && holidayMode() && ("teaching".equals(spot.getZoneCode()) || "library".equals(spot.getZoneCode()))) {
            throw new BusinessException("假期模式期间教学楼区、图书馆区不开放预约，请选择宿舍区或访客区");
        }

        // 潮汐车位的开放时间段由管理员显式设定（迎新、考试、夜间赛事等可能覆盖到夜间），
        // 此时以管理员设定的时段为准，不再套用全局夜间禁停规则
        boolean tidalSpot = spot.getSpotType() != null && spot.getSpotType() == 1;
        if (!white && !tidalSpot && inNightBanWindow(start, end)) {
            throw new BusinessException("夜间（" + config("rule_night_ban_start", "23:00") + " - "
                    + config("rule_night_ban_end", "06:00") + "）禁止停车与预约，请调整预约时段");
        }

        int limit = dailyLimitOf(user);
        if (!white && limit > 0) {
            Long used = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                    .eq(Reservation::getUserId, user.getId())
                    .ge(Reservation::getCreatedAt, LocalDate.now().atStartOfDay()));
            long usedCount = used == null ? 0 : used;
            if (usedCount >= limit) {
                throw new BusinessException("今日预约次数已达上限（" + limit + " 次），请明天再试");
            }
        }
    }

    /** 区域名称：以「停车区域管理」的配置为准，缺失时回退为原始编码 */
    private String zoneName(String zoneCode) {
        if (zoneCode == null) return "未知区域";
        ParkingZone zone = parkingZoneMapper.selectOne(new LambdaQueryWrapper<ParkingZone>()
                .eq(ParkingZone::getZoneCode, zoneCode)
                .last("LIMIT 1"));
        return zone == null ? zoneCode : zone.getZoneName();
    }

    @Override
    public Map<String, Object> benefitsOf(User user) {
        int identity = user == null || user.getIdentityType() == null
                ? IdentityType.STUDENT : user.getIdentityType();
        boolean ok = verified(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userNo", user == null ? null : user.getUserNo());
        result.put("identityType", identity);
        result.put("identityLabel", IdentityType.label(identity));
        result.put("identityStatus", user == null ? 0 : user.getIdentityStatus());
        result.put("identityStatusLabel", IdentityType.auditLabel(user == null ? 0 : user.getIdentityStatus()));
        result.put("identityRemark", user == null ? "" : user.getIdentityRemark());
        result.put("verified", ok);
        result.put("department", user == null ? "" : user.getDepartment());
        result.put("className", user == null ? "" : user.getClassName());

        // 未认证：按访客规则展示（与实际计费口径一致）
        result.put("effectiveIdentityLabel", IdentityType.label(effectiveIdentity(user)));
        result.put("freeMinutes", freeMinutesOf(user));
        result.put("discount", identityMultiplier(user));
        result.put("dailyLimit", dailyLimitOf(user));
        result.put("remainingQuota", remainingDailyQuota(user));
        result.put("semesterMode", intConfig("semester_mode", 0));
        result.put("holidayDiscount", decimalConfig("holiday_discount", new BigDecimal("0.80")));
        result.put("nightBanStart", config("rule_night_ban_start", "23:00"));
        result.put("nightBanEnd", config("rule_night_ban_end", "06:00"));
        result.put("overtimeGraceMinutes", intConfig("violation_overtime_grace_minutes", 30));

        // 月卡信息（权益页展示）
        MonthlyPass mp = null;
        if (user != null && user.getId() != null) {
            mp = monthlyPassMapper.selectOne(new LambdaQueryWrapper<MonthlyPass>()
                    .eq(MonthlyPass::getUserId, user.getId())
                    .eq(MonthlyPass::getStatus, 1)
                    .ge(MonthlyPass::getExpireDate, LocalDate.now())
                    .orderByAsc(MonthlyPass::getExpireDate)
                    .last("LIMIT 1"));
        }
        result.put("monthlyPassActive", mp != null);
        result.put("monthlyPassExpireDate", mp == null ? null : mp.getExpireDate());
        result.put("monthlyPassPlate", mp == null ? null : mp.getPlateNumber());

        // 区域目录来自「停车区域管理」，仅返回启用中的区域（按展示顺序）
        Map<String, Object> zones = new LinkedHashMap<>();
        for (ParkingZone zone : parkingZoneMapper.selectList(new LambdaQueryWrapper<ParkingZone>()
                .eq(ParkingZone::getStatus, 1)
                .orderByAsc(ParkingZone::getSortOrder)
                .orderByAsc(ParkingZone::getId))) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", zone.getZoneName());
            item.put("sortOrder", zone.getSortOrder());
            zones.put(zone.getZoneCode(), item);
        }
        result.put("zones", zones);
        return result;
    }
}
