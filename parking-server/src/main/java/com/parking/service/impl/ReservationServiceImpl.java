package com.parking.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.parking.common.BusinessException;
import com.parking.dto.ReservationDTO;
import com.parking.entity.ParkingSpot;
import com.parking.entity.Reservation;
import com.parking.entity.SystemConfig;
import com.parking.mapper.ParkingSpotMapper;
import com.parking.mapper.ReservationMapper;
import com.parking.mapper.SystemConfigMapper;
import com.parking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationMapper reservationMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final SystemConfigMapper systemConfigMapper;

    @Override
    @Transactional
    public Reservation createReservation(Long userId, ReservationDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        if (dto.getStartTime().isBefore(now)) {
            throw new BusinessException("Reservation start time cannot be in the past");
        }
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new BusinessException("End time must be after start time");
        }
        long minutes = Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes();
        if (minutes < 60) throw new BusinessException("Minimum reservation is 1 hour");
        long hours = minutes / 60;
        long days = hours / 24;
        if (days > 30) throw new BusinessException("Maximum reservation is 30 days");
        ParkingSpot spot = parkingSpotMapper.selectById(dto.getSpotId());
        if (spot == null) throw new BusinessException("Parking spot not found");
        if (spot.getStatus() != 0) throw new BusinessException("Parking spot is not available");
        BigDecimal durationHours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
        BigDecimal totalAmount = spot.getPricePerHour().multiply(durationHours);
        Snowflake sf = IdUtil.getSnowflake(1, 3);
        Reservation reservation = new Reservation();
        reservation.setId(sf.nextId());
        reservation.setUserId(userId);
        reservation.setSpotId(dto.getSpotId());
        reservation.setReservationNo("RES" + System.currentTimeMillis());
        reservation.setStartTime(dto.getStartTime());
        reservation.setEndTime(dto.getEndTime());
        reservation.setDurationHours(durationHours);
        reservation.setTotalAmount(totalAmount);
        reservation.setStatus(1); // active
        reservationMapper.insert(reservation);
        spot.setStatus(2); // reserved
        parkingSpotMapper.updateById(spot);
        return reservation;
    }

    @Override
    @Transactional
    public void cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) throw new BusinessException("Reservation not found");
        if (!reservation.getUserId().equals(userId)) throw new BusinessException("Not your reservation");
        if (reservation.getStatus() != 1 && reservation.getStatus() != 0) {
            throw new BusinessException("Reservation cannot be cancelled in current status");
        }
        reservation.setStatus(3); // cancelled
        reservationMapper.updateById(reservation);
        ParkingSpot spot = parkingSpotMapper.selectById(reservation.getSpotId());
        if (spot != null && spot.getStatus() == 2) {
            spot.setStatus(0); // release
            parkingSpotMapper.updateById(spot);
        }
    }

    @Override
    public Reservation getById(Long id) {
        return reservationMapper.selectById(id);
    }
}
