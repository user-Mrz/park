package com.parking.miniapp.service;

import com.parking.system.dto.ReservationDTO;
import com.parking.system.domain.Reservation;

public interface ReservationService {
    Reservation createReservation(Long userId, ReservationDTO dto);
    void cancelReservation(Long userId, Long reservationId);
    Reservation getById(Long id);

    /** 查询"本人"的预约详情，非本人预约抛出 403，防止越权查看他人停车信息 */
    Reservation getMyReservation(Long userId, Long id);
}
