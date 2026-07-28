package com.parking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.entity.ParkingSpot;
import com.parking.entity.Reservation;
import com.parking.mapper.ParkingSpotMapper;
import com.parking.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled tasks for the parking system.
 * Runs periodic cleanup of expired reservations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final ReservationMapper reservationMapper;
    private final ParkingSpotMapper parkingSpotMapper;

    @Value("${parking.reservation.grace-period-minutes:15}")
    private int gracePeriodMinutes;

    /**
     * Every 60 seconds, scan for active reservations whose endTime + grace period has passed,
     * mark them as expired, and release the parking spot.
     * Each reservation is processed independently to prevent one failure from rolling back all.
     */
    @Scheduled(fixedRate = 60000)
    public void expireReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(gracePeriodMinutes);
        List<Reservation> expiredList = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getStatus, 1)
                        .lt(Reservation::getEndTime, cutoff)
        );
        if (expiredList.isEmpty()) return;
        int count = 0;
        for (Reservation r : expiredList) {
            try {
                expireOne(r);
                count++;
            } catch (Exception e) {
                log.error("Failed to expire reservation {}: {}", r.getId(), e.getMessage());
            }
        }
        if (count > 0) {
            log.info("Expired {} reservation(s) at {}", count, LocalDateTime.now());
        }
    }

    private void expireOne(Reservation r) {
        r.setStatus(4); // expired
        reservationMapper.updateById(r);
        ParkingSpot spot = parkingSpotMapper.selectById(r.getSpotId());
        if (spot != null && spot.getStatus() == 2) {
            spot.setStatus(0); // back to vacant
            parkingSpotMapper.updateById(spot);
        }
    }
}
