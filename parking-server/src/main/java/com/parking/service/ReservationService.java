package com.parking.service;

import com.parking.dto.ReservationDTO;
import com.parking.entity.Reservation;

public interface ReservationService {
    Reservation createReservation(Long userId, ReservationDTO dto);
    void cancelReservation(Long userId, Long reservationId);
    Reservation getById(Long id);
}
