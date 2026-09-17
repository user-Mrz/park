package com.parking.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.BusinessException;
import com.parking.common.IdentityType;
import com.parking.entity.*;
import com.parking.mapper.*;
import com.parking.service.AdminService;
import com.parking.service.CampusRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;
    private final VehicleRecordMapper vehicleRecordMapper;
    private final RechargeRecordMapper rechargeRecordMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final CampusRuleService campusRuleService;

    @Override
    public Page<User> getUsers(int page, int pageSize, String keyword, Integer status,
                              Integer identityType, Integer identityStatus) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getPhone, keyword).or().like(User::getNickname, keyword)
                    .or().like(User::getPlateNumber, keyword).or().like(User::getUserNo, keyword));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (identityType != null) {
            wrapper.eq(User::getIdentityType, identityType);
        }
        if (identityStatus != null) {
            wrapper.eq(User::getIdentityStatus, identityStatus);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return userMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public void updateUserIdentity(Long id, Integer identityStatus, String remark) {
        if (identityStatus == null || identityStatus < 0 || identityStatus > 2) {
            throw new BusinessException("审核状态不合法（0待审核 1通过 2驳回）");
        }
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        if (identityStatus == 1 && (user.getUserNo() == null || user.getUserNo().trim().isEmpty())
                && user.getIdentityType() != null && user.getIdentityType() <= 3) {
            throw new BusinessException("该用户未填写学号/工号，无法通过认证");
        }
        user.setIdentityStatus(identityStatus);
        user.setIdentityRemark(remark == null ? "" : remark.trim());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public Map<String, Object> getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("openid", user.getOpenid());
        result.put("nickname", user.getNickname());
        // 校园身份字段（用户信息抽屉依赖这些字段展示认证状态，避免误显示为「待审核」）
        result.put("userNo", user.getUserNo());
        result.put("identityType", user.getIdentityType());
        result.put("identityLabel", IdentityType.label(user.getIdentityType()));
        result.put("department", user.getDepartment());
        result.put("className", user.getClassName());
        result.put("identityStatus", user.getIdentityStatus());
        result.put("identityStatusLabel", IdentityType.auditLabel(user.getIdentityStatus()));
        result.put("identityRemark", user.getIdentityRemark());
        result.put("phone", user.getPhone());
        result.put("plateNumber", user.getPlateNumber());
        result.put("avatar", user.getAvatar());
        result.put("balance", user.getBalance());
        result.put("status", user.getStatus());
        result.put("createdAt", user.getCreatedAt());
        result.put("updatedAt", user.getUpdatedAt());

        // 业务统计
        long vehicleRecordCount = nullToZero(vehicleRecordMapper.selectCount(
                new LambdaQueryWrapper<VehicleRecord>().eq(VehicleRecord::getUserId, id)));
        long parkingNow = nullToZero(vehicleRecordMapper.selectCount(
                new LambdaQueryWrapper<VehicleRecord>().eq(VehicleRecord::getUserId, id).eq(VehicleRecord::getStatus, 0)));
        long reservationCount = nullToZero(reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getUserId, id)));
        long activeReservationCount = nullToZero(reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getUserId, id).eq(Reservation::getStatus, 1)));
        long rechargeCount = nullToZero(rechargeRecordMapper.selectCount(
                new LambdaQueryWrapper<RechargeRecord>().eq(RechargeRecord::getUserId, id).eq(RechargeRecord::getStatus, 1)));
        BigDecimal rechargeAmount = rechargeRecordMapper.selectList(
                        new LambdaQueryWrapper<RechargeRecord>().eq(RechargeRecord::getUserId, id).eq(RechargeRecord::getStatus, 1))
                .stream().map(RechargeRecord::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paymentAmount = paymentRecordMapper.selectList(
                        new LambdaQueryWrapper<PaymentRecord>().eq(PaymentRecord::getUserId, id))
                .stream().map(PaymentRecord::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        VehicleRecord lastRecord = vehicleRecordMapper.selectOne(
                new LambdaQueryWrapper<VehicleRecord>()
                        .eq(VehicleRecord::getUserId, id)
                        .orderByDesc(VehicleRecord::getEntryTime)
                        .last("limit 1"));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("vehicleRecordCount", vehicleRecordCount);
        stats.put("parkingNow", parkingNow);
        stats.put("reservationCount", reservationCount);
        stats.put("activeReservationCount", activeReservationCount);
        stats.put("rechargeCount", rechargeCount);
        stats.put("rechargeAmount", rechargeAmount);
        stats.put("paymentAmount", paymentAmount);
        stats.put("lastEntryTime", lastRecord != null ? lastRecord.getEntryTime() : null);
        stats.put("lastPlateNumber", lastRecord != null ? lastRecord.getPlateNumber() : null);
        result.put("stats", stats);
        return result;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    @Override
    @Transactional
    public void updateUserStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("User not found");
        user.setStatus(status);
        userMapper.updateById(user);
        // 禁用用户时，自动处理其未完成的业务，避免车辆/车位被卡住
        if (status != null && status == 0) {
            // 1. 取消该用户所有活跃预约，释放被占用的车位
            List<Reservation> activeReservations = reservationMapper.selectList(
                    new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getUserId, id)
                            .eq(Reservation::getStatus, 1));
            for (Reservation r : activeReservations) {
                updateReservationStatus(r.getId(), 3);
            }
            // 2. 自动结算该用户所有在停车辆（按出场逻辑计费并尝试扣款，释放车位）
            List<VehicleRecord> parkedRecords = vehicleRecordMapper.selectList(
                    new LambdaQueryWrapper<VehicleRecord>()
                            .eq(VehicleRecord::getUserId, id)
                            .eq(VehicleRecord::getStatus, 0));
            for (VehicleRecord vr : parkedRecords) {
                forceExit(vr.getId());
            }
        }
    }

    @Override
    public BigDecimal getTotalUserBalance() {
        return userMapper.sumActiveUserBalance();
    }

    @Override
    public Page<Reservation> getAllReservations(int page, int pageSize, Integer status) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Reservation::getStatus, status);
        wrapper.orderByDesc(Reservation::getCreatedAt);
        return reservationMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public void updateReservationStatus(Long id, Integer status) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) throw new BusinessException("Reservation not found");
        reservation.setStatus(status);
        reservationMapper.updateById(reservation);
        if (status == 3 || status == 2) {
            ParkingSpot spot = parkingSpotMapper.selectById(reservation.getSpotId());
            if (spot != null) { spot.setStatus(0); parkingSpotMapper.updateById(spot); }
        }
    }

    @Override
    public Page<VehicleRecord> getAllVehicleRecords(int page, int pageSize, Integer status) {
        LambdaQueryWrapper<VehicleRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(VehicleRecord::getStatus, status);
        wrapper.orderByDesc(VehicleRecord::getCreatedAt);
        return vehicleRecordMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    @Transactional
    public VehicleRecord updateVehicleRecord(Long id, VehicleRecord updateData) {
        VehicleRecord record = vehicleRecordMapper.selectById(id);
        if (record == null) throw new BusinessException("Record not found");
        if (updateData.getStatus() != null) record.setStatus(updateData.getStatus());
        if (updateData.getPaymentStatus() != null) record.setPaymentStatus(updateData.getPaymentStatus());
        if (updateData.getExitTime() != null) record.setExitTime(updateData.getExitTime());
        if (updateData.getTotalFee() != null) record.setTotalFee(updateData.getTotalFee());
        vehicleRecordMapper.updateById(record);
        return record;
    }

    @Override
    @Transactional
    public VehicleRecord forceExit(Long id) {
        VehicleRecord record = vehicleRecordMapper.selectById(id);
        if (record == null) throw new BusinessException("Record not found");
        // CAS: 原子抢占强制出场权 (status 0/2 -> 1), 防止并发重复结算/重复扣费
        int locked = vehicleRecordMapper.update(null,
                new LambdaUpdateWrapper<VehicleRecord>()
                        .eq(VehicleRecord::getId, id)
                        .in(VehicleRecord::getStatus, 0, 2)
                        .set(VehicleRecord::getStatus, 1));
        if (locked == 0) throw new BusinessException("Vehicle already exited");
        record.setExitTime(LocalDateTime.now());
        long minutes = Duration.between(record.getEntryTime(), record.getExitTime()).toMinutes();
        record.setDurationMinutes((int) minutes);
        ParkingSpot spot = parkingSpotMapper.selectById(record.getSpotId());
        // 校园身份费率：扣减身份免费时长 + 身份系数（学生折扣 / 访客加价）+ 假期折扣
        BigDecimal actualFee = campusRuleService.calculateParkingFee(
                userMapper.selectById(record.getUserId()), spot, minutes);
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
                payment.setVehicleRecordId(id);
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
        return record;
    }

    @Override
    public Page<RechargeRecord> getAllRechargeRecords(int page, int pageSize) {
        return rechargeRecordMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<RechargeRecord>().orderByDesc(RechargeRecord::getCreatedAt));
    }

    @Override
    public Page<PaymentRecord> getAllPaymentRecords(int page, int pageSize) {
        return paymentRecordMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<PaymentRecord>().orderByDesc(PaymentRecord::getCreatedAt));
    }

    @Override
    public Page<ParkingSpot> getAllSpots(int page, int pageSize) {
        return parkingSpotMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<ParkingSpot>().orderByAsc(ParkingSpot::getSpotCode));
    }

    @Override
    public ParkingSpot addSpot(ParkingSpot spot) {
        Snowflake sf = IdUtil.getSnowflake(1, 6);
        spot.setId(sf.nextId());
        if (spot.getPricePerHour() == null) spot.setPricePerHour(new BigDecimal("5.00"));
        if (spot.getStatus() == null) spot.setStatus(0);
        parkingSpotMapper.insert(spot);
        return spot;
    }

    @Override
    public ParkingSpot updateSpot(Long id, ParkingSpot updateData) {
        ParkingSpot spot = parkingSpotMapper.selectById(id);
        if (spot == null) throw new BusinessException("Spot not found");
        if (updateData.getSpotCode() != null) spot.setSpotCode(updateData.getSpotCode());
        if (updateData.getArea() != null) spot.setArea(updateData.getArea());
        if (updateData.getFloor() != null) spot.setFloor(updateData.getFloor());
        if (updateData.getPricePerHour() != null) spot.setPricePerHour(updateData.getPricePerHour());
        if (updateData.getRemark() != null) spot.setRemark(updateData.getRemark());
        parkingSpotMapper.updateById(spot);
        return spot;
    }

    @Override
    public void updateSpotStatus(Long id, Integer status) {
        ParkingSpot spot = parkingSpotMapper.selectById(id);
        if (spot == null) throw new BusinessException("Spot not found");
        spot.setStatus(status);
        parkingSpotMapper.updateById(spot);
    }

    @Override
    public Map<String, String> getAllConfig() {
        Map<String, String> map = new LinkedHashMap<>();
        systemConfigMapper.selectList(null).forEach(c -> map.put(c.getConfigKey(), c.getConfigValue()));
        return map;
    }

    @Override
    public void updateConfig(Map<String, String> configs) {
        configs.forEach((key, value) -> {
            validateConfigValue(key, value);
            SystemConfig config = systemConfigMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
            if (config != null) {
                config.setConfigValue(value == null ? "" : value.trim());
                systemConfigMapper.updateById(config);
            }
        });
    }

    /**
     * 校园规则配置值合法性校验：避免误填（例如把 30 分钟填成 300000）导致计费异常
     */
    private void validateConfigValue(String key, String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) return;
        try {
            if (key.endsWith("_free_minutes")) {
                int minutes = Integer.parseInt(v);
                if (minutes < 0 || minutes > 1440) {
                    throw new BusinessException("免费时长需在 0 ~ 1440 分钟之间（当前：" + v + "）");
                }
            } else if ("rate_student_discount".equals(key) || "holiday_discount".equals(key)) {
                BigDecimal d = new BigDecimal(v);
                if (d.compareTo(BigDecimal.ZERO) <= 0 || d.compareTo(new BigDecimal("5")) > 0) {
                    throw new BusinessException("折扣系数需在 0 ~ 5 之间（0.5 表示五折）");
                }
            } else if ("rate_visitor_multiplier".equals(key)) {
                BigDecimal m = new BigDecimal(v);
                if (m.compareTo(new BigDecimal("0.1")) < 0 || m.compareTo(new BigDecimal("10")) > 0) {
                    throw new BusinessException("加价系数需在 0.1 ~ 10 之间（1.5 表示加价 50%）");
                }
            } else if (key.startsWith("reserve_daily_limit_")) {
                int limit = Integer.parseInt(v);
                if (limit < 0 || limit > 50) {
                    throw new BusinessException("每日预约上限需在 0 ~ 50 次之间（0 表示不限）");
                }
            } else if (key.startsWith("rule_night_ban_")) {
                java.time.LocalTime.parse(v);
            } else if ("semester_mode".equals(key)) {
                if (!"0".equals(v) && !"1".equals(v)) {
                    throw new BusinessException("学期/假期模式只能填 0（学期）或 1（假期）");
                }
            } else if (key.endsWith("_grace_minutes") || key.endsWith("_minutes")) {
                int minutes = Integer.parseInt(v);
                if (minutes < 0 || minutes > 1440) {
                    throw new BusinessException("分钟数需在 0 ~ 1440 之间");
                }
            } else if ("default_hourly_rate".equals(key)) {
                BigDecimal rate = new BigDecimal(v);
                if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("1000")) > 0) {
                    throw new BusinessException("每小时费率需在 0 ~ 1000 元之间");
                }
            } else if ("max_reservation_days".equals(key)) {
                int days = Integer.parseInt(v);
                if (days < 1 || days > 365) throw new BusinessException("最大预约天数需在 1 ~ 365 之间");
            }
        } catch (NumberFormatException e) {
            throw new BusinessException("配置项 " + key + " 的值格式不正确：" + v);
        } catch (java.time.format.DateTimeParseException e) {
            throw new BusinessException("时间配置需为 HH:mm 格式，如 23:00");
        }
    }

    @Override
    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", userMapper.selectCount(null));
        data.put("totalUserBalance", userMapper.sumActiveUserBalance());
        data.put("totalSpots", parkingSpotMapper.selectCount(null));
        data.put("availableSpots", parkingSpotMapper.selectCount(
                new LambdaQueryWrapper<ParkingSpot>().eq(ParkingSpot::getStatus, 0)));
        data.put("parkedVehicles", vehicleRecordMapper.selectCount(
                new LambdaQueryWrapper<VehicleRecord>().eq(VehicleRecord::getStatus, 0)));
        data.put("todayReservations", reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>().ge(Reservation::getCreatedAt, LocalDateTime.now().toLocalDate())));
        data.put("todayRevenue", paymentRecordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecord>().ge(PaymentRecord::getCreatedAt, LocalDateTime.now().toLocalDate()))
                .stream().map(PaymentRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        return data;
    }

    @Override
    public Map<String, Object> getSpotMap() {
        Map<String, Object> result = new HashMap<>();
        
        List<ParkingSpot> allSpots = parkingSpotMapper.selectList(null);
        List<VehicleRecord> parkedRecords = vehicleRecordMapper.selectList(
                new LambdaQueryWrapper<VehicleRecord>().eq(VehicleRecord::getStatus, 0));
        List<Reservation> activeReservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getStatus, 1));

        // 车位 -> 在停车辆 / 进行中预约
        Map<Long, VehicleRecord> parkedBySpot = new HashMap<>();
        for (VehicleRecord r : parkedRecords) {
            if (r.getSpotId() != null) parkedBySpot.put(r.getSpotId(), r);
        }
        Map<Long, Reservation> reservationBySpot = new HashMap<>();
        for (Reservation r : activeReservations) {
            if (r.getSpotId() != null) reservationBySpot.put(r.getSpotId(), r);
        }
        // 在停车辆的车主信息（昵称 / 手机号）
        Set<Long> ownerIds = parkedRecords.stream().map(VehicleRecord::getUserId)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, User> ownerById = new HashMap<>();
        if (!ownerIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(ownerIds)) {
                ownerById.put(u.getId(), u);
            }
        }

        Set<Long> occupiedSpotIds = parkedBySpot.keySet();
        Set<Long> reservedSpotIds = reservationBySpot.keySet();

        List<Map<String, Object>> spots = new ArrayList<>();
        int vacantCount = 0, occupiedCount = 0, reservedUnparkedCount = 0, reservedParkedCount = 0, maintenanceCount = 0;

        for (ParkingSpot spot : allSpots) {
            Map<String, Object> spotInfo = new HashMap<>();
            spotInfo.put("id", String.valueOf(spot.getId())); // 雪花ID字符串化, 避免JS精度丢失
            spotInfo.put("spotCode", spot.getSpotCode());
            spotInfo.put("area", spot.getArea());
            spotInfo.put("zoneCode", spot.getZoneCode());
            spotInfo.put("allowIdentities", spot.getAllowIdentities());
            spotInfo.put("freeMinutes", spot.getFreeMinutes());
            spotInfo.put("floor", spot.getFloor());
            spotInfo.put("pricePerHour", spot.getPricePerHour());

            int displayStatus = spot.getStatus();
            if (spot.getStatus() == 3) {
                displayStatus = 4;
                maintenanceCount++;
            } else if (occupiedSpotIds.contains(spot.getId())) {
                displayStatus = 1;
                occupiedCount++;
                if (reservedSpotIds.contains(spot.getId())) {
                    displayStatus = 5;
                    reservedParkedCount++;
                    occupiedCount--;
                }
            } else if (reservedSpotIds.contains(spot.getId())) {
                displayStatus = 2;
                reservedUnparkedCount++;
            } else if (spot.getStatus() == 0) {
                displayStatus = 0;
                vacantCount++;
            }

            // ---- 在停车辆详情（车牌、入场时间、已停时长、预估费用、车主）----
            VehicleRecord parked = parkedBySpot.get(spot.getId());
            if (parked != null) {
                long minutes = parked.getEntryTime() == null ? 0L
                        : Math.max(0L, Duration.between(parked.getEntryTime(), LocalDateTime.now()).toMinutes());
                BigDecimal rate = spot.getPricePerHour() != null ? spot.getPricePerHour() : new BigDecimal("5.00");
                long billedHours = (minutes + 59) / 60;
                if (billedHours < 1) billedHours = 1;
                BigDecimal estimatedFee = rate.multiply(BigDecimal.valueOf(billedHours)).setScale(2, RoundingMode.HALF_UP);
                Reservation linked = parked.getReservationId() != null
                        ? reservationMapper.selectById(parked.getReservationId()) : null;
                if (linked != null && linked.getTotalAmount() != null
                        && linked.getTotalAmount().compareTo(estimatedFee) > 0) {
                    estimatedFee = linked.getTotalAmount();
                }

                Map<String, Object> vehicle = new LinkedHashMap<>();
                vehicle.put("recordId", String.valueOf(parked.getId()));
                vehicle.put("plateNumber", parked.getPlateNumber());
                vehicle.put("entryTime", parked.getEntryTime());
                vehicle.put("parkedMinutes", minutes);
                vehicle.put("estimatedFee", estimatedFee);
                vehicle.put("paymentStatus", parked.getPaymentStatus());
                vehicle.put("userId", parked.getUserId() == null ? null : String.valueOf(parked.getUserId()));
                User owner = parked.getUserId() == null ? null : ownerById.get(parked.getUserId());
                if (owner != null) {
                    vehicle.put("nickname", owner.getNickname());
                    vehicle.put("phone", owner.getPhone());
                }
                spotInfo.put("vehicle", vehicle);
            }

            // ---- 进行中的预约详情 ----
            Reservation activeRes = reservationBySpot.get(spot.getId());
            if (activeRes != null) {
                Map<String, Object> reservation = new LinkedHashMap<>();
                reservation.put("reservationId", String.valueOf(activeRes.getId()));
                reservation.put("reservationNo", activeRes.getReservationNo());
                reservation.put("startTime", activeRes.getStartTime());
                reservation.put("endTime", activeRes.getEndTime());
                reservation.put("durationHours", activeRes.getDurationHours());
                reservation.put("totalAmount", activeRes.getTotalAmount());
                reservation.put("status", activeRes.getStatus());
                spotInfo.put("reservation", reservation);
            }

            spotInfo.put("displayStatus", displayStatus);
            spots.add(spotInfo);
        }

        result.put("spots", spots);
        result.put("vacantCount", vacantCount);
        result.put("occupiedCount", occupiedCount);
        result.put("reservedUnparkedCount", reservedUnparkedCount);
        result.put("reservedParkedCount", reservedParkedCount);
        result.put("maintenanceCount", maintenanceCount);
        result.put("totalCount", allSpots.size());

        return result;
    }
}
