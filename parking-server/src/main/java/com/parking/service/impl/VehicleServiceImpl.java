package com.parking.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.parking.common.BusinessException;
import com.parking.dto.VehicleEnterDTO;
import com.parking.entity.*;
import com.parking.mapper.*;
import com.parking.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRecordMapper vehicleRecordMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final UserMapper userMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ReservationMapper reservationMapper;

    @Override
    @Transactional
    public VehicleRecord enter(Long userId, VehicleEnterDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("User not found");
        // Check if user already has an active parking record
        Long count = vehicleRecordMapper.selectCount(new LambdaQueryWrapper<VehicleRecord>()
                .eq(VehicleRecord::getUserId, userId).eq(VehicleRecord::getStatus, 0));
        if (count > 0) throw new BusinessException("You already have a vehicle parked");
        ParkingSpot spot = parkingSpotMapper.selectById(dto.getSpotId());
        if (spot == null) throw new BusinessException("Parking spot not found");
        // Validate reservation if provided
        Reservation reservation = null;
        if (dto.getReservationId() != null) {
            reservation = reservationMapper.selectById(dto.getReservationId());
            if (reservation == null) throw new BusinessException("Reservation not found");
            if (!reservation.getUserId().equals(userId)) throw new BusinessException("This reservation does not belong to you");
            if (reservation.getStatus() != 1) throw new BusinessException("Reservation is not active");
            if (!reservation.getSpotId().equals(dto.getSpotId())) throw new BusinessException("Reserved spot does not match selected spot");
            // Allow entry even if slightly past endTime (grace period)
            if (reservation.getEndTime().isBefore(LocalDateTime.now().minusMinutes(15))) {
                throw new BusinessException("Reservation has expired. Please make a new reservation");
            }
        }
        if (spot.getStatus() != 0 && spot.getStatus() != 2) throw new BusinessException("Spot is occupied");
        // CAS: 原子更新车位状态，防止并发入场
        int spotRows = parkingSpotMapper.update(null,
                new LambdaUpdateWrapper<ParkingSpot>()
                        .eq(ParkingSpot::getId, dto.getSpotId())
                        .in(ParkingSpot::getStatus, 0, 2)
                        .set(ParkingSpot::getStatus, 1));
        if (spotRows == 0) throw new BusinessException("Spot was just occupied by another vehicle");
        String plateNumber = dto.getPlateNumber() != null ? dto.getPlateNumber() : user.getPlateNumber();
        if (plateNumber == null || plateNumber.isEmpty()) throw new BusinessException("Please set your license plate first");
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
        return record;
    }

    @Override
    @Transactional
    public VehicleRecord exit(Long userId, Long recordId) {
        VehicleRecord record = vehicleRecordMapper.selectById(recordId);
        if (record == null) throw new BusinessException("Record not found");
        if (!record.getUserId().equals(userId)) throw new BusinessException("Not your record");
        if (record.getStatus() != 0) throw new BusinessException("Vehicle has already exited");
        record.setExitTime(LocalDateTime.now());
        long minutes = Duration.between(record.getEntryTime(), record.getExitTime()).toMinutes();
        record.setDurationMinutes((int) minutes);
        // Calculate fee
        ParkingSpot spot = parkingSpotMapper.selectById(record.getSpotId());
        BigDecimal hourlyRate = spot != null ? spot.getPricePerHour() : new BigDecimal("5.00");
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.UP);
        if (hours.compareTo(BigDecimal.ZERO) <= 0) hours = BigDecimal.ONE;
        BigDecimal totalFee = hourlyRate.multiply(hours).setScale(2, RoundingMode.HALF_UP);
        record.setTotalFee(totalFee);
        // Auto deduct from balance
        User user = userMapper.selectById(userId);
        if (user.getBalance().compareTo(totalFee) < 0) {
            throw new BusinessException("Insufficient balance. Please recharge. Required: " + totalFee + " yuan");
        }
        BigDecimal balanceBefore = user.getBalance();
        int rows = userMapper.deductBalance(userId, totalFee);
        if (rows == 0) {
            throw new BusinessException("Balance insufficient during deduction");
        }
        user = userMapper.selectById(userId);
        record.setPaymentStatus(1); // paid
        record.setStatus(1); // exited
        vehicleRecordMapper.updateById(record);
        // Release spot
        if (spot != null) {
            spot.setStatus(0);
            parkingSpotMapper.updateById(spot);
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
        payment.setType("auto_deduct");
        paymentRecordMapper.insert(payment);
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
        }
        return record;
    }
}
