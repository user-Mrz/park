package com.parking.admin.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.BusinessException;
import com.parking.common.IdentityType;
import com.parking.system.dto.BlacklistDTO;
import com.parking.system.dto.WhitelistDTO;
import com.parking.system.domain.Blacklist;
import com.parking.system.domain.ParkingSpot;
import com.parking.system.domain.ParkingZone;
import com.parking.system.domain.PaymentRecord;
import com.parking.system.domain.Reservation;
import com.parking.system.domain.User;
import com.parking.system.domain.VehicleRecord;
import com.parking.system.domain.VisitReport;
import com.parking.system.domain.Violation;
import com.parking.system.domain.Whitelist;
import com.parking.system.mapper.BlacklistMapper;
import com.parking.system.mapper.ParkingSpotMapper;
import com.parking.system.mapper.ParkingZoneMapper;
import com.parking.system.mapper.PaymentRecordMapper;
import com.parking.system.mapper.ReservationMapper;
import com.parking.system.mapper.UserMapper;
import com.parking.system.mapper.VehicleRecordMapper;
import com.parking.system.mapper.VisitReportMapper;
import com.parking.system.mapper.ViolationMapper;
import com.parking.system.mapper.WhitelistMapper;
import com.parking.admin.service.CampusAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 校园化管理统计实现
 *
 * <p>统计口径：按「校园身份」「院系」汇总用户数/认证情况/预约数/停车次数/消费金额；
 * 按「校园分区」汇总车位数与使用情况、分区收入。</p>
 */
@Service
@RequiredArgsConstructor
public class CampusAdminServiceImpl implements CampusAdminService {

    private static final Pattern PLATE_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5][A-Z][A-Z0-9]{5,6}$");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 区间统计最多跨度，防止一次查询过大 */
    private static final int DAILY_STATS_MAX_DAYS = 92;

    private final UserMapper userMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final ParkingZoneMapper parkingZoneMapper;
    private final ReservationMapper reservationMapper;
    private final VehicleRecordMapper vehicleRecordMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final ViolationMapper violationMapper;
    private final VisitReportMapper visitReportMapper;
    private final BlacklistMapper blacklistMapper;
    private final WhitelistMapper whitelistMapper;

    /** 校园统计缓存：全量聚合允许 30 秒延迟，避免首页 10 秒自动刷新反复全表扫描 */
    private static final long CAMPUS_STATS_CACHE_TTL_MS = 30_000L;
    private volatile Map<String, Object> campusStatsCache;
    private volatile long campusStatsCacheExpireAt;

    // ==================== 校园统计 ====================

    /** 区域 code -> 名称（来自「停车区域管理」配置） */
    private Map<String, String> zoneNameMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (ParkingZone zone : parkingZoneMapper.selectList(new LambdaQueryWrapper<ParkingZone>()
                .orderByAsc(ParkingZone::getSortOrder)
                .orderByAsc(ParkingZone::getId))) {
            map.put(zone.getZoneCode(), zone.getZoneName());
        }
        return map;
    }

    @Override
    public Map<String, Object> campusStats() {
        long now = System.currentTimeMillis();
        Map<String, Object> cached = campusStatsCache;
        if (cached != null && now < campusStatsCacheExpireAt) {
            return cached;
        }
        List<User> users = userMapper.selectList(null);
        List<ParkingSpot> spots = parkingSpotMapper.selectList(null);
        List<Reservation> reservations = reservationMapper.selectList(null);
        List<VehicleRecord> records = vehicleRecordMapper.selectList(null);
        List<PaymentRecord> payments = paymentRecordMapper.selectList(null);

        Map<Long, User> userById = new HashMap<>();
        for (User u : users) userById.put(u.getId(), u);
        Map<Long, ParkingSpot> spotById = new HashMap<>();
        for (ParkingSpot s : spots) spotById.put(s.getId(), s);

        // 用户维度聚合
        Map<Integer, long[]> identityAgg = new LinkedHashMap<>();   // [用户数, 已认证, 待审核]
        Map<Integer, BigDecimal> identityRevenue = new LinkedHashMap<>();
        Map<Integer, Long> identityReservations = new LinkedHashMap<>();
        Map<Integer, Long> identityRecords = new LinkedHashMap<>();
        Map<String, long[]> deptAgg = new LinkedHashMap<>();        // [用户数, 已认证, 待审核]
        Map<String, BigDecimal> deptRevenue = new LinkedHashMap<>();
        Map<String, Long> deptReservations = new LinkedHashMap<>();
        Map<String, Long> deptRecords = new LinkedHashMap<>();

        for (User u : users) {
            int identity = u.getIdentityType() == null ? IdentityType.STUDENT : u.getIdentityType();
            boolean verified = u.getIdentityStatus() != null && u.getIdentityStatus() == IdentityType.AUDIT_APPROVED;
            boolean pending = u.getIdentityStatus() == null || u.getIdentityStatus() == IdentityType.AUDIT_PENDING;
            long[] ia = identityAgg.computeIfAbsent(identity, k -> new long[3]);
            ia[0]++;
            if (verified) ia[1]++;
            if (pending) ia[2]++;
            identityRevenue.putIfAbsent(identity, BigDecimal.ZERO);
            identityReservations.putIfAbsent(identity, 0L);
            identityRecords.putIfAbsent(identity, 0L);

            String dept = StringUtils.hasText(u.getDepartment()) ? u.getDepartment() : "未填写院系";
            long[] da = deptAgg.computeIfAbsent(dept, k -> new long[3]);
            da[0]++;
            if (verified) da[1]++;
            if (pending) da[2]++;
            deptRevenue.putIfAbsent(dept, BigDecimal.ZERO);
            deptReservations.putIfAbsent(dept, 0L);
            deptRecords.putIfAbsent(dept, 0L);
        }

        for (Reservation r : reservations) {
            User u = userById.get(r.getUserId());
            if (u == null) continue;
            int identity = u.getIdentityType() == null ? IdentityType.STUDENT : u.getIdentityType();
            identityReservations.merge(identity, 1L, Long::sum);
            String dept = StringUtils.hasText(u.getDepartment()) ? u.getDepartment() : "未填写院系";
            deptReservations.merge(dept, 1L, Long::sum);
        }

        // 分区聚合
        Map<String, long[]> zoneAgg = new LinkedHashMap<>();  // [总数, 空闲, 占用, 预约中, 维护]
        Map<String, BigDecimal> zoneRevenue = new LinkedHashMap<>();
        for (ParkingSpot s : spots) {
            String zone = s.getZoneCode() == null ? "unknown" : s.getZoneCode();
            long[] za = zoneAgg.computeIfAbsent(zone, k -> new long[5]);
            za[0]++;
            int st = s.getStatus() == null ? 0 : s.getStatus();
            if (st == 0) za[1]++;
            else if (st == 1) za[2]++;
            else if (st == 2) za[3]++;
            else if (st == 3) za[4]++;
            zoneRevenue.putIfAbsent(zone, BigDecimal.ZERO);
        }

        for (VehicleRecord r : records) {
            User u = userById.get(r.getUserId());
            if (u != null) {
                int identity = u.getIdentityType() == null ? IdentityType.STUDENT : u.getIdentityType();
                identityRecords.merge(identity, 1L, Long::sum);
                String dept = StringUtils.hasText(u.getDepartment()) ? u.getDepartment() : "未填写院系";
                deptRecords.merge(dept, 1L, Long::sum);
            }
        }

        // 扣费记录关联的停车记录批量加载，避免逐条 selectById 造成 N+1
        Set<Long> linkedRecordIds = payments.stream()
                .map(PaymentRecord::getVehicleRecordId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, VehicleRecord> linkedRecordById = new HashMap<>();
        if (!linkedRecordIds.isEmpty()) {
            for (VehicleRecord vr : vehicleRecordMapper.selectBatchIds(linkedRecordIds)) {
                linkedRecordById.put(vr.getId(), vr);
            }
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (PaymentRecord p : payments) {
            BigDecimal amount = p.getAmount() == null ? BigDecimal.ZERO : p.getAmount();
            totalRevenue = totalRevenue.add(amount);
            User u = userById.get(p.getUserId());
            if (u != null) {
                int identity = u.getIdentityType() == null ? IdentityType.STUDENT : u.getIdentityType();
                identityRevenue.merge(identity, amount, BigDecimal::add);
                String dept = StringUtils.hasText(u.getDepartment()) ? u.getDepartment() : "未填写院系";
                deptRevenue.merge(dept, amount, BigDecimal::add);
            }
            // 分区收入：扣费记录 -> 停车记录 -> 车位
            if (p.getVehicleRecordId() != null) {
                VehicleRecord vr = linkedRecordById.get(p.getVehicleRecordId());
                if (vr != null && vr.getSpotId() != null) {
                    ParkingSpot spot = spotById.get(vr.getSpotId());
                    if (spot != null && spot.getZoneCode() != null) {
                        zoneRevenue.merge(spot.getZoneCode(), amount, BigDecimal::add);
                    }
                }
            }
        }

        List<Map<String, Object>> byIdentity = new ArrayList<>();
        for (Map.Entry<Integer, long[]> e : identityAgg.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("identityType", e.getKey());
            row.put("identityLabel", IdentityType.label(e.getKey()));
            row.put("userCount", e.getValue()[0]);
            row.put("verifiedCount", e.getValue()[1]);
            row.put("pendingCount", e.getValue()[2]);
            row.put("reservationCount", identityReservations.getOrDefault(e.getKey(), 0L));
            row.put("vehicleRecordCount", identityRecords.getOrDefault(e.getKey(), 0L));
            row.put("revenue", identityRevenue.getOrDefault(e.getKey(), BigDecimal.ZERO));
            byIdentity.add(row);
        }
        byIdentity.sort(Comparator.comparingInt(r -> (Integer) r.get("identityType")));

        List<Map<String, Object>> byDepartment = new ArrayList<>();
        for (Map.Entry<String, long[]> e : deptAgg.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("department", e.getKey());
            row.put("userCount", e.getValue()[0]);
            row.put("verifiedCount", e.getValue()[1]);
            row.put("pendingCount", e.getValue()[2]);
            row.put("reservationCount", deptReservations.getOrDefault(e.getKey(), 0L));
            row.put("vehicleRecordCount", deptRecords.getOrDefault(e.getKey(), 0L));
            row.put("revenue", deptRevenue.getOrDefault(e.getKey(), BigDecimal.ZERO));
            byDepartment.add(row);
        }
        byDepartment.sort((a, b) -> Long.compare((Long) b.get("vehicleRecordCount"), (Long) a.get("vehicleRecordCount")));

        List<Map<String, Object>> byZone = new ArrayList<>();
        Map<String, String> zoneNames = zoneNameMap();
        for (Map.Entry<String, long[]> e : zoneAgg.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("zoneCode", e.getKey());
            row.put("zoneName", zoneNames.getOrDefault(e.getKey(), e.getKey()));
            row.put("spotCount", e.getValue()[0]);
            row.put("vacantCount", e.getValue()[1]);
            row.put("occupiedCount", e.getValue()[2]);
            row.put("reservedCount", e.getValue()[3]);
            row.put("maintenanceCount", e.getValue()[4]);
            row.put("revenue", zoneRevenue.getOrDefault(e.getKey(), BigDecimal.ZERO));
            byZone.add(row);
        }

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("users", users.size());
        totals.put("verifiedUsers", users.stream().filter(u -> u.getIdentityStatus() != null
                && u.getIdentityStatus() == IdentityType.AUDIT_APPROVED).count());
        totals.put("pendingUsers", users.stream().filter(u -> u.getIdentityStatus() == null
                || u.getIdentityStatus() == IdentityType.AUDIT_PENDING).count());
        totals.put("spots", spots.size());
        totals.put("reservations", reservations.size());
        totals.put("vehicleRecords", records.size());
        totals.put("revenue", totalRevenue);
        totals.put("violations", countAll(violationMapper.selectCount(null)));
        totals.put("pendingViolations", countAll(violationMapper.selectCount(
                new LambdaQueryWrapper<Violation>().eq(Violation::getStatus, 0))));
        totals.put("visitReports", countAll(visitReportMapper.selectCount(null)));
        totals.put("pendingVisitReports", countAll(visitReportMapper.selectCount(
                new LambdaQueryWrapper<VisitReport>().eq(VisitReport::getStatus, 0))));
        totals.put("blacklist", countAll(blacklistMapper.selectCount(null)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totals", totals);
        result.put("byIdentity", byIdentity);
        result.put("byDepartment", byDepartment);
        result.put("byZone", byZone);
        campusStatsCache = result;
        campusStatsCacheExpireAt = now + CAMPUS_STATS_CACHE_TTL_MS;
        return result;
    }

    private long countAll(Long count) {
        return count == null ? 0 : count;
    }

    // ==================== 区间日统计（首页柱状图） ====================

    @Override
    public Map<String, Object> dailyStats(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (endDate == null || endDate.isAfter(today)) endDate = today;
        if (startDate == null) startDate = endDate.minusDays(6);
        if (startDate.isAfter(endDate)) startDate = endDate;
        if (startDate.isBefore(endDate.minusDays(DAILY_STATS_MAX_DAYS - 1L))) {
            startDate = endDate.minusDays(DAILY_STATS_MAX_DAYS - 1L);
        }

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.plusDays(1).atStartOfDay();

        Map<String, BigDecimal> revenueByDay = new HashMap<>();
        for (Map<String, Object> row : paymentRecordMapper.sumAmountByDay(from, to)) {
            revenueByDay.put(String.valueOf(row.get("d")), new BigDecimal(String.valueOf(row.get("v"))));
        }
        Map<String, Long> vehiclesByDay = new HashMap<>();
        for (Map<String, Object> row : vehicleRecordMapper.countByDay(from, to)) {
            vehiclesByDay.put(String.valueOf(row.get("d")), Long.valueOf(String.valueOf(row.get("v"))));
        }

        List<Map<String, Object>> days = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalVehicles = 0;
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            String date = d.toString();
            BigDecimal revenue = revenueByDay.getOrDefault(date, BigDecimal.ZERO);
            long vehicles = vehiclesByDay.getOrDefault(date, 0L);
            totalRevenue = totalRevenue.add(revenue);
            totalVehicles += vehicles;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", date);
            row.put("revenue", revenue);
            row.put("vehicleCount", vehicles);
            days.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("days", days);
        result.put("totalRevenue", totalRevenue);
        result.put("totalVehicleCount", totalVehicles);
        return result;
    }

    // ==================== 黑白名单 ====================

    @Override
    public Page<Blacklist> listBlacklist(int page, int pageSize, String keyword, String status, String source) {
        LambdaQueryWrapper<Blacklist> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Blacklist::getPlateNumber, keyword)
                    .or().like(Blacklist::getReason, keyword));
        }
        applyExpiryFilter(wrapper, status, Blacklist::getUntilTime);
        if (StringUtils.hasText(source) && !"all".equalsIgnoreCase(source)) {
            wrapper.eq(Blacklist::getSource, source.trim());
        }
        wrapper.orderByDesc(Blacklist::getCreatedAt);
        return blacklistMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Blacklist addBlacklist(BlacklistDTO dto, Long operatorId) {
        if (dto == null) throw new BusinessException("参数不能为空");
        String plate = normalizePlate(dto.getPlateNumber());
        if (!StringUtils.hasText(plate) && dto.getUserId() == null) {
            throw new BusinessException("请填写车牌号或指定用户");
        }
        validatePlate(plate);
        if (StringUtils.hasText(plate) && existsBlacklist(plate, null)) {
            throw new BusinessException("该车牌已在黑名单中");
        }
        if (dto.getUserId() != null && existsBlacklist(null, dto.getUserId())) {
            throw new BusinessException("该用户已在黑名单中");
        }
        Blacklist b = new Blacklist();
        b.setId(IdUtil.getSnowflake(1, 10).nextId());
        b.setPlateNumber(StringUtils.hasText(plate) ? plate : null);
        b.setUserId(dto.getUserId());
        b.setReason(dto.getReason() == null ? "" : dto.getReason().trim());
        b.setUntilTime(dto.getUntilTime());
        b.setSource("manual");
        b.setOperatorId(operatorId);
        b.setCreatedAt(LocalDateTime.now());
        b.setUpdatedAt(LocalDateTime.now());
        blacklistMapper.insert(b);
        return b;
    }

    @Override
    public void removeBlacklist(Long id) {
        Blacklist b = blacklistMapper.selectById(id);
        if (b == null) throw new BusinessException("黑名单记录不存在");
        blacklistMapper.deleteById(id);
    }

    @Override
    public Page<Whitelist> listWhitelist(int page, int pageSize, String keyword, String status) {
        LambdaQueryWrapper<Whitelist> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Whitelist::getPlateNumber, keyword)
                    .or().like(Whitelist::getReason, keyword));
        }
        applyExpiryFilter(wrapper, status, Whitelist::getUntilTime);
        wrapper.orderByDesc(Whitelist::getCreatedAt);
        return whitelistMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Whitelist addWhitelist(WhitelistDTO dto, Long operatorId) {
        if (dto == null) throw new BusinessException("参数不能为空");
        String plate = normalizePlate(dto.getPlateNumber());
        if (!StringUtils.hasText(plate) && dto.getUserId() == null) {
            throw new BusinessException("请填写车牌号或指定用户");
        }
        validatePlate(plate);
        if (StringUtils.hasText(plate) && existsWhitelist(plate, null)) {
            throw new BusinessException("该车牌已在白名单中");
        }
        if (dto.getUserId() != null && existsWhitelist(null, dto.getUserId())) {
            throw new BusinessException("该用户已在白名单中");
        }
        Whitelist w = new Whitelist();
        w.setId(IdUtil.getSnowflake(1, 10).nextId());
        w.setPlateNumber(StringUtils.hasText(plate) ? plate : null);
        w.setUserId(dto.getUserId());
        w.setReason(dto.getReason() == null ? "" : dto.getReason().trim());
        w.setUntilTime(dto.getUntilTime());
        w.setSource("manual");
        w.setOperatorId(operatorId);
        w.setCreatedAt(LocalDateTime.now());
        w.setUpdatedAt(LocalDateTime.now());
        whitelistMapper.insert(w);
        return w;
    }

    @Override
    public void removeWhitelist(Long id) {
        Whitelist w = whitelistMapper.selectById(id);
        if (w == null) throw new BusinessException("白名单记录不存在");
        whitelistMapper.deleteById(id);
    }

    private String normalizePlate(String raw) {
        return raw == null ? "" : raw.replace(" ", "").toUpperCase();
    }

    private void validatePlate(String plate) {
        if (StringUtils.hasText(plate) && !PLATE_PATTERN.matcher(plate).matches()) {
            throw new BusinessException("车牌号格式不正确，例如：粤B12345");
        }
    }

    private boolean existsBlacklist(String plate, Long userId) {
        LambdaQueryWrapper<Blacklist> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(plate)) wrapper.eq(Blacklist::getPlateNumber, plate);
        if (userId != null) wrapper.eq(Blacklist::getUserId, userId);
        Long count = blacklistMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private boolean existsWhitelist(String plate, Long userId) {
        LambdaQueryWrapper<Whitelist> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(plate)) wrapper.eq(Whitelist::getPlateNumber, plate);
        if (userId != null) wrapper.eq(Whitelist::getUserId, userId);
        Long count = whitelistMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    /** 统一处理 active / expired / all 状态筛选 */
    private <T> void applyExpiryFilter(LambdaQueryWrapper<T> wrapper, String status,
                                       com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, LocalDateTime> untilColumn) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) return;
        if ("active".equalsIgnoreCase(status)) {
            wrapper.and(w -> w.isNull(untilColumn).or().ge(untilColumn, LocalDateTime.now()));
        } else if ("expired".equalsIgnoreCase(status)) {
            wrapper.isNotNull(untilColumn).lt(untilColumn, LocalDateTime.now());
        }
    }

    // ==================== CSV 导出 ====================

    @Override
    public String exportCsv(String type) {
        String t = type == null ? "" : type.toLowerCase();
        switch (t) {
            case "users":
                return exportUsers();
            case "reservations":
                return exportReservations();
            case "payments":
                return exportPayments();
            case "violations":
                return exportViolations();
            case "visits":
                return exportVisits();
            default:
                throw new BusinessException("不支持的导出类型：" + type);
        }
    }

    private String csv(String... cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            String v = cells[i] == null ? "" : cells[i];
            // 防 CSV 公式注入：以 = + - @ 制表符 回车开头的单元格会被 Excel 当作公式执行，
            // 前置单引号强制按文本处理（昵称等字段由用户自填，不可信）
            if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
                v = "'" + v;
            }
            if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
                v = '"' + v.replace("\"", "\"\"") + '"';
            }
            sb.append(v);
        }
        return sb.append("\r\n").toString();
    }

    private String exportUsers() {
        StringBuilder sb = new StringBuilder();
        sb.append(csv("用户ID", "昵称", "学号/工号", "身份", "认证状态", "院系", "班级", "手机号", "车牌号", "余额", "账号状态", "注册时间"));
        for (User u : userMapper.selectList(null)) {
            sb.append(csv(String.valueOf(u.getId()), u.getNickname(), u.getUserNo(),
                    IdentityType.label(u.getIdentityType()), IdentityType.auditLabel(u.getIdentityStatus()),
                    u.getDepartment(), u.getClassName(), u.getPhone(), u.getPlateNumber(),
                    u.getBalance() == null ? "0.00" : u.getBalance().toPlainString(),
                    u.getStatus() != null && u.getStatus() == 1 ? "正常" : "禁用",
                    u.getCreatedAt() == null ? "" : u.getCreatedAt().format(DT)));
        }
        return sb.toString();
    }

    private String exportReservations() {
        StringBuilder sb = new StringBuilder();
        sb.append(csv("预约ID", "预约编号", "用户ID", "昵称", "学号/工号", "身份", "车位", "开始时间", "结束时间", "时长(小时)", "金额", "状态"));
        Map<Long, User> users = mapUsers();
        Map<Long, String> spotCodes = mapSpotCodes();
        for (Reservation r : reservationMapper.selectList(null)) {
            User u = users.get(r.getUserId());
            sb.append(csv(String.valueOf(r.getId()), r.getReservationNo(), String.valueOf(r.getUserId()),
                    u == null ? "" : u.getNickname(), u == null ? "" : u.getUserNo(),
                    u == null ? "" : IdentityType.label(u.getIdentityType()),
                    spotCodes.getOrDefault(r.getSpotId(), ""),
                    r.getStartTime() == null ? "" : r.getStartTime().format(DT),
                    r.getEndTime() == null ? "" : r.getEndTime().format(DT),
                    r.getDurationHours() == null ? "" : r.getDurationHours().toPlainString(),
                    r.getTotalAmount() == null ? "" : r.getTotalAmount().toPlainString(),
                    reservationStatus(r.getStatus())));
        }
        return sb.toString();
    }

    private String exportPayments() {
        StringBuilder sb = new StringBuilder();
        sb.append(csv("扣费ID", "用户ID", "昵称", "学号/工号", "身份", "停车记录ID", "金额", "扣费前余额", "扣费后余额", "类型", "时间"));
        Map<Long, User> users = mapUsers();
        for (PaymentRecord p : paymentRecordMapper.selectList(null)) {
            User u = users.get(p.getUserId());
            sb.append(csv(String.valueOf(p.getId()), String.valueOf(p.getUserId()),
                    u == null ? "" : u.getNickname(), u == null ? "" : u.getUserNo(),
                    u == null ? "" : IdentityType.label(u.getIdentityType()),
                    p.getVehicleRecordId() == null ? "" : String.valueOf(p.getVehicleRecordId()),
                    p.getAmount() == null ? "" : p.getAmount().toPlainString(),
                    p.getBalanceBefore() == null ? "" : p.getBalanceBefore().toPlainString(),
                    p.getBalanceAfter() == null ? "" : p.getBalanceAfter().toPlainString(),
                    p.getType(), p.getCreatedAt() == null ? "" : p.getCreatedAt().format(DT)));
        }
        return sb.toString();
    }

    private String exportViolations() {
        StringBuilder sb = new StringBuilder();
        sb.append(csv("违规ID", "车牌号", "用户ID", "类型", "说明", "扣分", "罚款", "状态", "记录时间", "处理时间"));
        for (Violation v : violationMapper.selectList(null)) {
            sb.append(csv(String.valueOf(v.getId()), v.getPlateNumber(),
                    v.getUserId() == null ? "" : String.valueOf(v.getUserId()),
                    v.getType(), v.getDescription(),
                    v.getPoints() == null ? "0" : String.valueOf(v.getPoints()),
                    v.getFineAmount() == null ? "0.00" : v.getFineAmount().toPlainString(),
                    v.getStatus() != null && v.getStatus() == 1 ? "已处理" : "未处理",
                    v.getCreatedAt() == null ? "" : v.getCreatedAt().format(DT),
                    v.getHandledAt() == null ? "" : v.getHandledAt().format(DT)));
        }
        return sb.toString();
    }

    private String exportVisits() {
        StringBuilder sb = new StringBuilder();
        sb.append(csv("报备ID", "类型", "来访人", "车牌号", "事由", "预计到校", "预计离校", "状态", "分配车位", "审核意见", "提交时间"));
        Map<Long, String> spotCodes = mapSpotCodes();
        for (VisitReport r : visitReportMapper.selectList(null)) {
            sb.append(csv(String.valueOf(r.getId()), r.getVisitType() != null && r.getVisitType() == 2 ? "家长" : "访客",
                    r.getVisitorName(), r.getPlateNumber(), r.getPurpose(),
                    r.getExpectedStart() == null ? "" : r.getExpectedStart().format(DT),
                    r.getExpectedEnd() == null ? "" : r.getExpectedEnd().format(DT),
                    visitStatus(r.getStatus()),
                    spotCodes.getOrDefault(r.getSpotId(), ""),
                    r.getReviewRemark(),
                    r.getCreatedAt() == null ? "" : r.getCreatedAt().format(DT)));
        }
        return sb.toString();
    }

    private Map<Long, User> mapUsers() {
        Map<Long, User> map = new HashMap<>();
        for (User u : userMapper.selectList(null)) map.put(u.getId(), u);
        return map;
    }

    private Map<Long, String> mapSpotCodes() {
        Map<Long, String> map = new HashMap<>();
        for (ParkingSpot s : parkingSpotMapper.selectList(null)) map.put(s.getId(), s.getSpotCode());
        return map;
    }

    private String reservationStatus(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待支付";
            case 1: return "进行中";
            case 2: return "已完成";
            case 3: return "已取消";
            case 4: return "已过期";
            default: return String.valueOf(status);
        }
    }

    private String visitStatus(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待审核";
            case 1: return "已通过";
            case 2: return "已驳回";
            case 3: return "已入场";
            case 4: return "已完成";
            case 5: return "已过期";
            case 6: return "已撤销";
            default: return String.valueOf(status);
        }
    }
}
