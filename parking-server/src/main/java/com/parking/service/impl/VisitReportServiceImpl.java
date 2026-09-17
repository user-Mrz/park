package com.parking.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.BusinessException;
import com.parking.common.IdentityType;
import com.parking.dto.VisitReportDTO;
import com.parking.dto.VehicleEnterDTO;
import com.parking.entity.ParkingSpot;
import com.parking.entity.User;
import com.parking.entity.VehicleRecord;
import com.parking.entity.VisitReport;
import com.parking.mapper.ParkingSpotMapper;
import com.parking.mapper.UserMapper;
import com.parking.mapper.VehicleRecordMapper;
import com.parking.mapper.VisitReportMapper;
import com.parking.service.AdminService;
import com.parking.service.CampusRuleService;
import com.parking.service.VehicleService;
import com.parking.service.VisitReportService;
import com.parking.service.ViolationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 访客与家长车辆报备实现
 *
 * <p>校园场景：学生/教师为来访家长或访客提前报备车牌，管理员审核后自动生成访客账号并分配
 * 「访客与家长临时停车区」车位；访客到达校门时由管理员登记入场，出场时按身份免费时长与费率结算
 * （家长接送默认 30 分钟免费）。</p>
 */
@Service
@RequiredArgsConstructor
public class VisitReportServiceImpl implements VisitReportService {

    private static final Pattern PLATE_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5][A-Z][A-Z0-9]{5,6}$");
    private static final String VISITOR_ZONE = "visitor";

    private final VisitReportMapper visitReportMapper;
    private final UserMapper userMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final VehicleRecordMapper vehicleRecordMapper;
    private final VehicleService vehicleService;
    private final AdminService adminService;
    private final CampusRuleService campusRuleService;
    private final ViolationService violationService;

    @Override
    @Transactional
    public VisitReport create(Long ownerUserId, VisitReportDTO dto) {
        User owner = userMapper.selectById(ownerUserId);
        if (owner == null) throw new BusinessException("用户不存在");
        // 仅校内已认证成员可代为报备
        if (owner.getIdentityStatus() == null || owner.getIdentityStatus() != IdentityType.AUDIT_APPROVED
                || (owner.getIdentityType() != null && owner.getIdentityType() >= IdentityType.VISITOR)) {
            throw new BusinessException("仅通过认证的校内师生可提交访客/家长车辆报备");
        }
        if (dto.getVisitType() == null || (dto.getVisitType() != 1 && dto.getVisitType() != 2)) {
            throw new BusinessException("请选择来访类型（1访客 2家长）");
        }
        String plate = dto.getPlateNumber() == null ? "" : dto.getPlateNumber().replace(" ", "").toUpperCase();
        if (!PLATE_PATTERN.matcher(plate).matches()) {
            throw new BusinessException("车牌号格式不正确，例如：粤B12345");
        }
        if (dto.getExpectedStart() == null || dto.getExpectedEnd() == null) {
            throw new BusinessException("请选择预计到校与离校时间");
        }
        if (dto.getExpectedStart().isBefore(LocalDateTime.now())) {
            throw new BusinessException("预计到校时间不能早于当前时间");
        }
        if (!dto.getExpectedEnd().isAfter(dto.getExpectedStart())) {
            throw new BusinessException("预计离校时间必须晚于到校时间");
        }
        long hours = Duration.between(dto.getExpectedStart(), dto.getExpectedEnd()).toHours();
        if (hours > 24) throw new BusinessException("单次报备时长不能超过 24 小时");
        if (campusRuleService.isBlacklisted(plate, null)) {
            throw new BusinessException("该车牌已在黑名单中，无法报备");
        }
        // 同一车牌不允许存在多份未完成报备
        Long pending = visitReportMapper.selectCount(new LambdaQueryWrapper<VisitReport>()
                .eq(VisitReport::getPlateNumber, plate)
                .in(VisitReport::getStatus, 0, 1, 3));
        if (pending != null && pending > 0) {
            throw new BusinessException("该车牌已有进行中的报备，请等待完成或先撤销");
        }

        Snowflake sf = IdUtil.getSnowflake(1, 7);
        VisitReport report = new VisitReport();
        report.setId(sf.nextId());
        report.setOwnerUserId(ownerUserId);
        report.setVisitType(dto.getVisitType());
        report.setVisitorName(dto.getVisitorName() == null ? "" : dto.getVisitorName().trim());
        report.setPlateNumber(plate);
        report.setPurpose(dto.getPurpose() == null ? "" : dto.getPurpose().trim());
        report.setExpectedStart(dto.getExpectedStart());
        report.setExpectedEnd(dto.getExpectedEnd());
        report.setStatus(0); // 待审核
        visitReportMapper.insert(report);
        return report;
    }

    @Override
    public Page<VisitReport> myReports(Long ownerUserId, int page, int pageSize) {
        Page<VisitReport> result = visitReportMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<VisitReport>()
                        .eq(VisitReport::getOwnerUserId, ownerUserId)
                        .orderByDesc(VisitReport::getCreatedAt));
        return enrich(result);
    }

    /** 补齐展示字段：报备人姓名（学号）与车位编号 */
    private Page<VisitReport> enrich(Page<VisitReport> page) {
        List<VisitReport> records = page.getRecords();
        if (records == null || records.isEmpty()) return page;
        java.util.Set<Long> ownerIds = new java.util.HashSet<>();
        java.util.Set<Long> spotIds = new java.util.HashSet<>();
        for (VisitReport r : records) {
            if (r.getOwnerUserId() != null) ownerIds.add(r.getOwnerUserId());
            if (r.getSpotId() != null) spotIds.add(r.getSpotId());
        }
        Map<Long, String> ownerNames = new LinkedHashMap<>();
        if (!ownerIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(ownerIds)) {
                String label = (u.getNickname() == null ? "未知" : u.getNickname())
                        + (StringUtils.hasText(u.getUserNo()) ? "（" + u.getUserNo() + "）" : "");
                ownerNames.put(u.getId(), label);
            }
        }
        Map<Long, String> spotCodes = new LinkedHashMap<>();
        if (!spotIds.isEmpty()) {
            for (ParkingSpot s : parkingSpotMapper.selectBatchIds(spotIds)) {
                spotCodes.put(s.getId(), s.getSpotCode());
            }
        }
        for (VisitReport r : records) {
            r.setOwnerName(ownerNames.get(r.getOwnerUserId()));
            r.setSpotCode(spotCodes.get(r.getSpotId()));
        }
        return page;
    }

    @Override
    public void cancel(Long ownerUserId, Long reportId) {
        VisitReport report = visitReportMapper.selectById(reportId);
        if (report == null) throw new BusinessException("报备记录不存在");
        if (!report.getOwnerUserId().equals(ownerUserId)) throw new BusinessException("只能撤销自己的报备");
        if (report.getStatus() != 0 && report.getStatus() != 1) {
            throw new BusinessException("当前状态不可撤销");
        }
        // 若已分配车位且未入场，释放车位
        if (report.getStatus() == 1 && report.getSpotId() != null) {
            ParkingSpot spot = parkingSpotMapper.selectById(report.getSpotId());
            if (spot != null && spot.getStatus() == 2) {
                spot.setStatus(0);
                parkingSpotMapper.updateById(spot);
            }
        }
        report.setStatus(6); // 已撤销
        visitReportMapper.updateById(report);
    }

    @Override
    public Page<VisitReport> listAll(int page, int pageSize, Integer status, String keyword) {
        LambdaQueryWrapper<VisitReport> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(VisitReport::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(VisitReport::getPlateNumber, keyword)
                    .or().like(VisitReport::getVisitorName, keyword));
        }
        wrapper.orderByDesc(VisitReport::getCreatedAt);
        return enrich(visitReportMapper.selectPage(new Page<>(page, pageSize), wrapper));
    }

    @Override
    @Transactional
    public VisitReport approve(Long reportId, Long reviewerId, Long spotId, String remark) {
        VisitReport report = visitReportMapper.selectById(reportId);
        if (report == null) throw new BusinessException("报备记录不存在");
        if (report.getStatus() != 0) throw new BusinessException("该报备已审核过");

        ParkingSpot spot = null;
        if (spotId != null) {
            spot = parkingSpotMapper.selectById(spotId);
        }
        if (spot == null) {
            // 自动分配访客区的空闲车位
            List<ParkingSpot> free = parkingSpotMapper.selectList(new LambdaQueryWrapper<ParkingSpot>()
                    .eq(ParkingSpot::getZoneCode, VISITOR_ZONE)
                    .eq(ParkingSpot::getStatus, 0)
                    .orderByAsc(ParkingSpot::getSpotCode)
                    .last("limit 1"));
            if (free.isEmpty()) {
                throw new BusinessException("访客与家长临时停车区暂无可分配车位，请稍后再试或手动指定车位");
            }
            spot = free.get(0);
        }
        if (!VISITOR_ZONE.equals(spot.getZoneCode())) {
            throw new BusinessException("访客/家长车辆只能分配「访客与家长临时停车区」的车位");
        }

        // 创建访客账号（用于停车记录与计费）
        User visitor = new User();
        Snowflake sf = IdUtil.getSnowflake(1, 8);
        visitor.setId(sf.nextId());
        visitor.setNickname(StringUtils.hasText(report.getVisitorName())
                ? report.getVisitorName() : report.getPlateNumber());
        visitor.setPlateNumber(report.getPlateNumber());
        visitor.setIdentityType(report.getVisitType() == 2 ? IdentityType.PARENT : IdentityType.VISITOR);
        visitor.setIdentityStatus(IdentityType.AUDIT_APPROVED);
        visitor.setIdentityRemark(report.getVisitType() == 2 ? "家长接送临时车辆" : "来访访客车辆");
        visitor.setPhone(null);
        visitor.setBalance(BigDecimal.ZERO);
        visitor.setStatus(1);
        userMapper.insert(visitor);

        // 占位：将车位置为已预约，等待访客到达
        spot.setStatus(2);
        parkingSpotMapper.updateById(spot);

        report.setStatus(1);
        report.setVisitorUserId(visitor.getId());
        report.setSpotId(spot.getId());
        report.setReviewerId(reviewerId);
        report.setReviewTime(LocalDateTime.now());
        report.setReviewRemark(remark == null ? "" : remark.trim());
        visitReportMapper.updateById(report);
        return report;
    }

    @Override
    public VisitReport reject(Long reportId, Long reviewerId, String remark) {
        VisitReport report = visitReportMapper.selectById(reportId);
        if (report == null) throw new BusinessException("报备记录不存在");
        if (report.getStatus() != 0) throw new BusinessException("该报备已审核过");
        report.setStatus(2);
        report.setReviewerId(reviewerId);
        report.setReviewTime(LocalDateTime.now());
        report.setReviewRemark(remark == null ? "不符合入校条件" : remark.trim());
        visitReportMapper.updateById(report);
        return report;
    }

    @Override
    @Transactional
    public VisitReport checkIn(Long reportId) {
        VisitReport report = visitReportMapper.selectById(reportId);
        if (report == null) throw new BusinessException("报备记录不存在");
        if (report.getStatus() != 1) throw new BusinessException("仅已通过的报备可登记入场");
        if (report.getVisitorUserId() == null || report.getSpotId() == null) {
            throw new BusinessException("报备缺少访客账号或车位信息，请重新审核");
        }
        VehicleEnterDTO enterDTO = new VehicleEnterDTO();
        enterDTO.setSpotId(report.getSpotId());
        enterDTO.setReservationId(null);
        enterDTO.setPlateNumber(report.getPlateNumber());
        VehicleRecord record = vehicleService.enter(report.getVisitorUserId(), enterDTO);
        report.setStatus(3); // 已入场
        report.setVehicleRecordId(record.getId());
        visitReportMapper.updateById(report);
        return report;
    }

    @Override
    @Transactional
    public VisitReport checkOut(Long reportId) {
        VisitReport report = visitReportMapper.selectById(reportId);
        if (report == null) throw new BusinessException("报备记录不存在");
        if (report.getStatus() != 3 || report.getVehicleRecordId() == null) {
            throw new BusinessException("该报备没有在停车辆");
        }
        // 复用管理端强制出场：按身份免费时长 + 费率结算并释放车位
        VehicleRecord record = adminService.forceExit(report.getVehicleRecordId());
        // 校园违规判定：家长/访客超过预计离校时间 + 宽限期
        violationService.checkOvertime(report.getVisitorUserId(), report.getPlateNumber(),
                record.getId(), record.getSpotId(), report.getExpectedEnd(),
                record.getExitTime() == null ? LocalDateTime.now() : record.getExitTime(),
                report.getVisitType() == 2 ? "家长接送临时停车" : "访客临时停车");
        report.setStatus(4); // 已完成
        visitReportMapper.updateById(report);
        return report;
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pending", countByStatus(0));
        result.put("approved", countByStatus(1));
        result.put("parked", countByStatus(3));
        result.put("finished", countByStatus(4));
        result.put("rejected", countByStatus(2));
        result.put("visitorZoneFree", parkingSpotMapper.selectCount(new LambdaQueryWrapper<ParkingSpot>()
                .eq(ParkingSpot::getZoneCode, VISITOR_ZONE)
                .eq(ParkingSpot::getStatus, 0)));
        result.put("parentFreeMinutes", campusRuleService.config("rate_parent_free_minutes", "30"));
        result.put("visitorFreeMinutes", campusRuleService.config("rate_visitor_free_minutes", "15"));
        return result;
    }

    private long countByStatus(int status) {
        Long count = visitReportMapper.selectCount(new LambdaQueryWrapper<VisitReport>()
                .eq(VisitReport::getStatus, status));
        return count == null ? 0 : count;
    }
}
