package com.parking.system.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.BusinessException;
import com.parking.common.IdentityType;
import com.parking.system.dto.VehicleEnterDTO;
import com.parking.system.domain.*;
import com.parking.system.mapper.*;
import com.parking.system.service.CampusRuleService;
import com.parking.system.service.SpotCacheService;
import com.parking.system.service.SpotRealtimeService;
import com.parking.system.service.SubscribeMessageService;
import com.parking.system.service.TidalSpotService;
import com.parking.system.service.VehicleService;
import com.parking.system.service.ViolationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRecordMapper vehicleRecordMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final UserMapper userMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    
    private final ReservationMapper reservationMapper;
    private final VisitReportMapper visitReportMapper;
    private final CampusRuleService campusRuleService;
    private final TidalSpotService tidalSpotService;
    private final ViolationService violationService;
    private final SpotCacheService spotCacheService;
    private final SubscribeMessageService subscribeMessageService;
    private final SpotRealtimeService spotRealtimeService;

    @Override
    @Transactional
    public VehicleRecord enter(Long userId, VehicleEnterDTO dto) {
        // 悲观锁锁定用户行, 串行化同一用户的并发入场, 防止 TOCTOU 导致同一用户同时停多辆车 (Bug-3)
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, userId).last("FOR UPDATE"));
        if (user == null) throw new BusinessException("用户不存在");
        // Check if user already has an active parking record
        Long count = vehicleRecordMapper.selectCount(new LambdaQueryWrapper<VehicleRecord>()
                .eq(VehicleRecord::getUserId, userId).eq(VehicleRecord::getStatus, 0));
        if (count > 0) throw new BusinessException("您已有车辆在校园内停放，请先办理出场");
        ParkingSpot spot = parkingSpotMapper.selectById(dto.getSpotId());
        if (spot == null) throw new BusinessException("车位不存在");

        // 潮汐车位门禁：开放时间前仅可预约不可入场，结束时间后不可入场（按时间实时判定，不依赖定时任务）
        tidalSpotService.validateEnterWindow(spot);

        // 校园门禁校验 1：黑名单车辆/账号禁止入场
        String plateNumber = StringUtils.hasText(dto.getPlateNumber()) ? dto.getPlateNumber() : user.getPlateNumber();
        if (campusRuleService.isBlacklisted(plateNumber, userId)) {
            throw new BusinessException("该车牌或账号已被列入黑名单，禁止入场，请联系停车管理处");
        }
        // 校园门禁校验 2：按身份限制可停区域（白名单车辆豁免）
        boolean whitelisted = campusRuleService.isWhitelisted(plateNumber, userId);
        Integer identityType = user.getIdentityType() == null ? IdentityType.STUDENT : user.getIdentityType();
        boolean identityVerified = user.getIdentityStatus() != null
                && user.getIdentityStatus() == IdentityType.AUDIT_APPROVED;
        int effectiveIdentity = identityVerified ? identityType : IdentityType.VISITOR;
        if (!whitelisted && !campusRuleService.zoneAllowed(spot, effectiveIdentity)) {
            violationService.recordIllegalZone(userId, plateNumber, spot.getId(),
                    IdentityType.label(identityType) + "（" + (identityVerified ? "已认证" : "未认证")
                            + "）驶入「" + spot.getArea() + "」");
            throw new BusinessException("当前身份不可停放在「" + spot.getArea()
                    + "」，该区域仅限" + campusRuleService.zoneIdentityNames(spot) + "使用");
        }
        // 已预约(2)车位的排他性: 仅允许持有该车位"活跃预约"的用户入场,
        // 防止未预约用户抢停他人已预约车位 (Bug-2)
        // 校园场景补充: 已通过审核的访客/家长报备(其访客账号+分配车位)同样允许入场
        if (spot.getStatus() == 2) {
            Reservation holder = reservationMapper.selectOne(new LambdaQueryWrapper<Reservation>()
                    .eq(Reservation::getSpotId, dto.getSpotId())
                    .eq(Reservation::getStatus, 1)
                    .last("LIMIT 1"));
            boolean isReservationHolder = holder != null && holder.getUserId().equals(userId);
            boolean hasApprovedReport = false;
            if (!isReservationHolder) {
                Long reports = visitReportMapper.selectCount(new LambdaQueryWrapper<VisitReport>()
                        .eq(VisitReport::getVisitorUserId, userId)
                        .eq(VisitReport::getSpotId, dto.getSpotId())
                        .eq(VisitReport::getStatus, 1));
                hasApprovedReport = reports != null && reports > 0;
            }
            if (!isReservationHolder && !hasApprovedReport) {
                throw new BusinessException("该车位已被他人预约，请选择其他车位");
            }
            // 入场请求未显式携带预约ID时, 自动采用该车位的活跃预约(预约者本人入场)
            if (isReservationHolder && dto.getReservationId() == null) {
                dto.setReservationId(holder.getId());
            }
        }
        // Validate reservation if provided
        Reservation reservation = null;
        if (dto.getReservationId() != null) {
            reservation = reservationMapper.selectById(dto.getReservationId());
            if (reservation == null) throw new BusinessException("预约记录不存在");
            if (!reservation.getUserId().equals(userId)) throw new BusinessException("该预约不属于当前账号");
            if (reservation.getStatus() != 1) throw new BusinessException("该预约已失效");
            if (!reservation.getSpotId().equals(dto.getSpotId())) throw new BusinessException("预约车位与所选车位不一致");
            // Allow entry even if slightly past endTime (grace period)
            if (reservation.getEndTime().isBefore(LocalDateTime.now().minusMinutes(15))) {
                throw new BusinessException("预约已过期，请重新预约");
            }
        }
        // 状态校验 + CAS：高峰期走 Redis 缓存 + Lua CAS + 异步回写 MySQL；非高峰走 MySQL CAS（保持原逻辑）
        boolean peak = spotCacheService.isPeakNow();
        Integer currentStatus = peak ? spotCacheService.getStatus(spot.getId()) : spot.getStatus();
        if (currentStatus == null || (currentStatus != 0 && currentStatus != 2)) {
            throw new BusinessException("该车位已被占用");
        }
        boolean casOk;
        if (peak) {
            casOk = spotCacheService.tryEnter(spot.getId());
            if (casOk) {
                spotCacheService.asyncSyncToDb(spot.getId(), 1);
                log.debug("Peak enter: spot {} CAS ok via Redis, async sync to MySQL", spot.getId());
            }
        } else {
            int spotRows = parkingSpotMapper.update(null,
                    new LambdaUpdateWrapper<ParkingSpot>()
                            .eq(ParkingSpot::getId, dto.getSpotId())
                            .in(ParkingSpot::getStatus, 0, 2)
                            .set(ParkingSpot::getStatus, 1));
            casOk = spotRows > 0;
            spotCacheService.setStatus(spot.getId(), 1);
        }
        if (!casOk) throw new BusinessException("该车位刚被其他车辆占用，请重新选择");
        if (plateNumber == null || plateNumber.isEmpty()) throw new BusinessException("请先在「账号信息」中绑定车牌号");
        Snowflake sf = IdUtil.getSnowflake(1, 4);
        VehicleRecord record = new VehicleRecord();
        record.setId(sf.nextId());
        record.setUserId(userId);
        record.setSpotId(dto.getSpotId());
        record.setReservationId(dto.getReservationId());
        record.setPlateNumber(plateNumber);
        record.setEntryTime(LocalDateTime.now());
        record.setStatus(0); // parked
        record.setPaymentStatus(0); // unpaid
        vehicleRecordMapper.insert(record);
        // Mark reservation as completed if using one
        if (reservation != null) {
            reservation.setStatus(2); // completed
            reservationMapper.updateById(reservation);
        }
        spotRealtimeService.notifyChanged();
        return record;
    }

    @Override
    @Transactional
    public VehicleRecord exit(Long userId, Long recordId) {
        VehicleRecord record = vehicleRecordMapper.selectById(recordId);
        if (record == null) throw new BusinessException("停车记录不存在");
        if (!record.getUserId().equals(userId)) throw new BusinessException("该停车记录不属于当前账号");
        // CAS: 原子抢占出场权 (status 0 -> 1), 防止并发重复结算/重复扣费
        int locked = vehicleRecordMapper.update(null,
                new LambdaUpdateWrapper<VehicleRecord>()
                        .eq(VehicleRecord::getId, recordId)
                        .eq(VehicleRecord::getStatus, 0)
                        .set(VehicleRecord::getStatus, 1));
        if (locked == 0) throw new BusinessException("该车辆已出场");
        record.setExitTime(LocalDateTime.now());
        long minutes = Duration.between(record.getEntryTime(), record.getExitTime()).toMinutes();
        record.setDurationMinutes((int) minutes);
        // Calculate fee —— 校园身份费率：先扣减身份免费时长，再应用身份系数与假期折扣
        ParkingSpot spot = parkingSpotMapper.selectById(record.getSpotId());
        User feeUser = userMapper.selectById(userId);
        BigDecimal actualFee;
        // 月卡豁免：若车牌命中用户有效月卡，出场按次停车费直接归零（预约费不豁免）
        if (campusRuleService.hasActiveMonthlyPass(userId, record.getPlateNumber())) {
            actualFee = BigDecimal.ZERO;
        } else {
            actualFee = campusRuleService.calculateParkingFee(feeUser, spot, minutes);
        }
        // 预约结算: 若停车记录关联预约, 费用取 max(实际停车费, 预约金额)。
        // 预约时段已被锁定, 即使提前离场也应按预约金额结算; 超时停车则按实际时长(多占多付)。
        BigDecimal totalFee = actualFee;
        boolean settledByReservation = false;
        if (record.getReservationId() != null) {
            Reservation reservation = reservationMapper.selectById(record.getReservationId());
            if (reservation != null && reservation.getTotalAmount() != null
                    && reservation.getTotalAmount().compareTo(totalFee) > 0) {
                totalFee = reservation.getTotalAmount();
                settledByReservation = true;
            }
        }
        record.setTotalFee(totalFee);
        // Auto deduct from balance
        User user = userMapper.selectById(userId);
        if (user.getBalance().compareTo(totalFee) < 0) {
            throw new BusinessException("余额不足，请先充值。本次需支付 " + totalFee + " 元");
        }
        BigDecimal balanceBefore = user.getBalance();
        int rows = userMapper.deductBalance(userId, totalFee);
        if (rows == 0) {
            throw new BusinessException("扣款失败：余额不足");
        }
        user = userMapper.selectById(userId);
        record.setPaymentStatus(1); // paid
        record.setStatus(1); // exited
        vehicleRecordMapper.updateById(record);
        // Release spot：高峰走 Redis 释放 + 异步回写；非高峰走 MySQL update（保持原逻辑）
        if (spot != null) {
            boolean peak = spotCacheService.isPeakNow();
            if (peak) {
                spotCacheService.release(spot.getId());
                spotCacheService.asyncSyncToDb(spot.getId(), 0);
                log.debug("Peak exit: spot {} released via Redis, async sync to MySQL", spot.getId());
            } else {
                spot.setStatus(0);
                parkingSpotMapper.updateById(spot);
                spotCacheService.setStatus(spot.getId(), 0);
            }
        }
        // Create payment record
        Snowflake sf = IdUtil.getSnowflake(1, 5);
        PaymentRecord payment = new PaymentRecord();
        payment.setId(sf.nextId());
        payment.setUserId(userId);
        payment.setVehicleRecordId(recordId);
        payment.setAmount(totalFee);
        payment.setBalanceBefore(balanceBefore);
        payment.setBalanceAfter(user.getBalance());
        payment.setType(settledByReservation ? "reservation_settle" : "auto_deduct");
        paymentRecordMapper.insert(payment);

        // 余额不足提醒：扣费后若剩余余额低于阈值，异步触发订阅消息（需前端已授权）
        try {
            BigDecimal threshold = campusRuleService.decimalConfig("wx_balance_warning_threshold", new BigDecimal("5.00"));
            BigDecimal afterBalance = user.getBalance();
            if (afterBalance != null && afterBalance.compareTo(threshold) < 0 && totalFee.compareTo(BigDecimal.ZERO) > 0) {
                subscribeMessageService.sendBalanceLow(userId, afterBalance);
            }
        } catch (Exception e) {
            log.warn("余额不足订阅消息触发异常（不影响主流程）: {}", e.getMessage());
        }

        // 校园违规判定：预约超时占用（超过预约结束时间 + 宽限期）自动记录违规
        if (record.getReservationId() != null) {
            Reservation res = reservationMapper.selectById(record.getReservationId());
            if (res != null) {
                violationService.checkOvertime(userId, record.getPlateNumber(), recordId, record.getSpotId(),
                        res.getEndTime(), record.getExitTime(), "预约停车");
            }
        }
        spotRealtimeService.notifyChanged();
        return record;
    }

    @Override
    @Transactional
    public VehicleRecord forceExit(Long recordId) {
        VehicleRecord record = vehicleRecordMapper.selectById(recordId);
        if (record == null) throw new BusinessException("停车记录不存在");
        // CAS: 原子抢占强制出场权 (status 0/2 -> 1), 防止并发重复结算/重复扣费
        int locked = vehicleRecordMapper.update(null,
                new LambdaUpdateWrapper<VehicleRecord>()
                        .eq(VehicleRecord::getId, recordId)
                        .in(VehicleRecord::getStatus, 0, 2)
                        .set(VehicleRecord::getStatus, 1));
        if (locked == 0) throw new BusinessException("该车辆已出场");
        record.setExitTime(LocalDateTime.now());
        long minutes = Duration.between(record.getEntryTime(), record.getExitTime()).toMinutes();
        record.setDurationMinutes((int) minutes);
        ParkingSpot spot = parkingSpotMapper.selectById(record.getSpotId());
        // 校园身份费率：扣减身份免费时长 + 身份系数（学生折扣 / 访客加价）+ 假期折扣
        BigDecimal actualFee;
        // 月卡豁免：若车牌命中用户有效月卡，出场按次停车费直接归零（预约费不豁免）
        User forceExitUser = userMapper.selectById(record.getUserId());
        if (forceExitUser != null && campusRuleService.hasActiveMonthlyPass(forceExitUser.getId(), record.getPlateNumber())) {
            actualFee = BigDecimal.ZERO;
        } else {
            actualFee = campusRuleService.calculateParkingFee(forceExitUser, spot, minutes);
        }
        // 预约结算: 关联预约时费用取 max(实际停车费, 预约金额), 提前离场按预约金额结算
        BigDecimal totalFee = actualFee;
        if (record.getReservationId() != null) {
            Reservation reservation = reservationMapper.selectById(record.getReservationId());
            if (reservation != null && reservation.getTotalAmount() != null
                    && reservation.getTotalAmount().compareTo(totalFee) > 0) {
                totalFee = reservation.getTotalAmount();
            }
        }
        record.setTotalFee(totalFee);
        // Try auto-deduct
        User user = userMapper.selectById(record.getUserId());
        if (user != null && user.getBalance().compareTo(record.getTotalFee()) >= 0) {
            BigDecimal before = user.getBalance();
            int rows = userMapper.deductBalance(record.getUserId(), record.getTotalFee());
            if (rows > 0) {
                record.setPaymentStatus(1);
                Snowflake sf = IdUtil.getSnowflake(1, 5);
                PaymentRecord payment = new PaymentRecord();
                payment.setId(sf.nextId());
                payment.setUserId(user.getId());
                payment.setVehicleRecordId(recordId);
                payment.setAmount(record.getTotalFee());
                payment.setBalanceBefore(before);
                payment.setBalanceAfter(userMapper.selectById(record.getUserId()).getBalance());
                payment.setType("admin_force");
                paymentRecordMapper.insert(payment);
            } else {
                record.setPaymentStatus(0);
            }
        } else {
            record.setPaymentStatus(0);
        }
        record.setStatus(1);
        vehicleRecordMapper.updateById(record);
        if (spot != null) { spot.setStatus(0); parkingSpotMapper.updateById(spot); }
        spotRealtimeService.notifyChanged();
        return record;
    }

    @Override
    public VehicleRecord getCurrentParking(Long userId) {
        VehicleRecord record = vehicleRecordMapper.selectOne(new LambdaQueryWrapper<VehicleRecord>()
                .eq(VehicleRecord::getUserId, userId)
                .eq(VehicleRecord::getStatus, 0)
                .orderByDesc(VehicleRecord::getEntryTime)
                .last("LIMIT 1"));
        if (record != null) {
            ParkingSpot spot = parkingSpotMapper.selectById(record.getSpotId());
            if (spot != null) {
                record.setSpotPricePerHour(spot.getPricePerHour());
            }
            // 返回关联预约金额, 供前端展示"提前离场按预约结算"的预计费用
            if (record.getReservationId() != null) {
                Reservation reservation = reservationMapper.selectById(record.getReservationId());
                if (reservation != null) {
                    record.setReservationAmount(reservation.getTotalAmount());
                }
            }
        }
        return record;
    }

    @Override
    public Page<Violation> getMyViolations(Long userId, int page, int pageSize) {
        return violationService.myViolations(userId, page, pageSize);
    }
}
