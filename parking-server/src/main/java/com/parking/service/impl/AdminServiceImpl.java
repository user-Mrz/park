package com.parking.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.BusinessException;
import com.parking.entity.*;
import com.parking.mapper.*;
import com.parking.service.AdminService;
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

    @Override
    public Page<User> getUsers(int page, int pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getPhone, keyword).or().like(User::getNickname, keyword)
                    .or().like(User::getPlateNumber, keyword));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return userMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("User not found");
        user.setStatus(status);
        userMapper.updateById(user);
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
        if (record.getStatus() == 1) throw new BusinessException("Vehicle already exited");
        record.setExitTime(LocalDateTime.now());
        long minutes = Duration.between(record.getEntryTime(), record.getExitTime()).toMinutes();
        record.setDurationMinutes((int) minutes);
        ParkingSpot spot = parkingSpotMapper.selectById(record.getSpotId());
        BigDecimal hourlyRate = spot != null ? spot.getPricePerHour() : new BigDecimal("5.00");
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.UP);
        if (hours.compareTo(BigDecimal.ZERO) <= 0) hours = BigDecimal.ONE;
        record.setTotalFee(hourlyRate.multiply(hours).setScale(2, RoundingMode.HALF_UP));
        // Try auto-deduct
        User user = userMapper.selectById(record.getUserId());
        if (user != null && user.getBalance().compareTo(record.getTotalFee()) >= 0) {
            BigDecimal before = user.getBalance();
            user.setBalance(before.subtract(record.getTotalFee()));
            userMapper.updateById(user);
            record.setPaymentStatus(1);
            Snowflake sf = IdUtil.getSnowflake(1, 5);
            PaymentRecord payment = new PaymentRecord();
            payment.setId(sf.nextId());
            payment.setUserId(user.getId());
            payment.setVehicleRecordId(id);
            payment.setAmount(record.getTotalFee());
            payment.setBalanceBefore(before);
            payment.setBalanceAfter(user.getBalance());
            payment.setType("admin_force");
            paymentRecordMapper.insert(payment);
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
            SystemConfig config = systemConfigMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
            if (config != null) {
                config.setConfigValue(value);
                systemConfigMapper.updateById(config);
            }
        });
    }

    @Override
    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", userMapper.selectCount(null));
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
}
