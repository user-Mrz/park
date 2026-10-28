package com.parking.miniapp.controller;

import com.parking.common.R;
import com.parking.common.UserContext;
import com.parking.system.dto.ReservationDTO;
import com.parking.miniapp.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reservation", description = "Reservation APIs")
@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "Create reservation")
    @PostMapping("/create")
    public R<?> create(@Valid @RequestBody ReservationDTO dto) {
        return R.ok(reservationService.createReservation(UserContext.getUserId(), dto));
    }

    @Operation(summary = "Cancel reservation")
    @PostMapping("/cancel/{id}")
    public R<?> cancel(@PathVariable Long id) {
        reservationService.cancelReservation(UserContext.getUserId(), id);
        return R.ok();
    }

    @Operation(summary = "Get reservation detail (own reservation only)")
    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(reservationService.getMyReservation(UserContext.getUserId(), id));
    }
}
