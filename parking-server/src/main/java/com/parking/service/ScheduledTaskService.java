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
import org.springframework.transaction.annotation.Transactional;

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
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireReservations() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(gracePeriodMinutes);
            // Find active reservations where endTime + grace period is in the past
            List<Reservation> expiredList = reservationMapper.selectList(
                    new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getStatus, 1)       // active
                            .lt(Reservation::getEndTime, cutoff)  // endTime < now - grace
            );
            if (expiredList.isEmpty()) return;
            for (Reservation r : expiredList) {
                r.setStatus(4); // expired
                reservationMapper.updateById(r);
                // Release the parking spot if it's still in reserved status
                ParkingSpot spot = parkingSpotMapper.selectById(r.getSpotId());
                if (spot != null && spot.getStatus() == 2) {
                    spot.setStatus(0); // back to vacant
                    parkingSpotMapper.updateById(spot);
                }
            }
            log.info("Expired {} reservation(s) at {}", expiredList.size(), LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to expire reservations: {}", e.getMessage(), e);
        }
    }
}
