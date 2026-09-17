package com.parking.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.BusinessException;
import com.parking.common.IdentityType;
import com.parking.dto.BlacklistDTO;
import com.parking.entity.Blacklist;
import com.parking.entity.ParkingSpot;
import com.parking.entity.PaymentRecord;
import com.parking.entity.Reservation;
import com.parking.entity.User;
import com.parking.entity.VehicleRecord;
import com.parking.entity.VisitReport;
import com.parking.entity.Violation;
import com.parking.mapper.BlacklistMapper;
import com.parking.mapper.ParkingSpotMapper;
import com.parking.mapper.PaymentRecordMapper;
import com.parking.mapper.ReservationMapper;
import com.parking.mapper.UserMapper;
import com.parking.mapper.VehicleRecordMapper;
import com.parking.mapper.VisitReportMapper;
import com.parking.mapper.ViolationMapper;
import com.parking.service.CampusAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

    private static final Map<String, String> ZONE_NAMES = new LinkedHashMap<String, String>() {{
        put("teaching", "教学楼区");
        put("library", "图书馆区");
        put("dorm", "宿舍区");
        put("gym", "体育馆区");
        put("visitor", "访客与家长临时停车区");
    }};

    private final UserMapper userMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final ReservationMapper reservationMapper;
    private final VehicleRecordMapper vehicleRecordMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final ViolationMapper violationMapper;
    private final VisitReportMapper visitReportMapper;
    private final BlacklistMapper blacklistMapper;

    // ==================== 校园统计 ====================

    @Override
    public Map<String, Object> campusStats() {
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
                VehicleRecord vr = vehicleRecordMapper.selectById(p.getVehicleRecordId());
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
        for (Map.Entry<String, long[]> e : zoneAgg.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("zoneCode", e.getKey());
            row.put("zoneName", ZONE_NAMES.getOrDefault(e.getKey(), e.getKey()));
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
        return result;
    }

    private long countAll(Long count) {
        return count == null ? 0 : count;
    }

    // ==================== 黑名单 ====================

    @Override
    public Page<Blacklist> listBlacklist(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<Blacklist> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Blacklist::getPlateNumber, keyword).or().like(Blacklist::getReason, keyword);
        }
        wrapper.orderByDesc(Blacklist::getCreatedAt);
        return blacklistMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Blacklist addBlacklist(BlacklistDTO dto) {
        if (dto == null) throw new BusinessException("参数不能为空");
        String plate = dto.getPlateNumber() == null ? "" : dto.getPlateNumber().replace(" ", "").toUpperCase();
        if (!StringUtils.hasText(plate) && dto.getUserId() == null) {
            throw new BusinessException("请填写车牌号或指定用户");
        }
        if (StringUtils.hasText(plate) && !PLATE_PATTERN.matcher(plate).matches()) {
            throw new BusinessException("车牌号格式不正确，例如：粤B12345");
        }
        if (StringUtils.hasText(plate)) {
            Long exists = blacklistMapper.selectCount(new LambdaQueryWrapper<Blacklist>()
                    .eq(Blacklist::getPlateNumber, plate));
            if (exists != null && exists > 0) throw new BusinessException("该车牌已在黑名单中");
        }
        Snowflake sf = IdUtil.getSnowflake(1, 10);
        Blacklist b = new Blacklist();
        b.setId(sf.nextId());
        b.setPlateNumber(StringUtils.hasText(plate) ? plate : null);
        b.setUserId(dto.getUserId());
        b.setReason(dto.getReason() == null ? "" : dto.getReason().trim());
        b.setUntilTime(dto.getUntilTime());
        b.setCreatedAt(LocalDateTime.now());
        blacklistMapper.insert(b);
        return b;
    }

    @Override
    public void removeBlacklist(Long id) {
        Blacklist b = blacklistMapper.selectById(id);
        if (b == null) throw new BusinessException("黑名单记录不存在");
        blacklistMapper.deleteById(id);
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
