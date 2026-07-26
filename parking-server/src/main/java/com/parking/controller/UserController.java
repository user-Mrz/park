package com.parking.controller;

import com.parking.common.R;
import com.parking.common.UserContext;
import com.parking.dto.PageDTO;
import com.parking.dto.RechargeDTO;
import com.parking.dto.UpdateUserDTO;
import com.parking.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "User APIs")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get user profile")
    @GetMapping("/info")
    public R<?> info() {
        return R.ok(userService.getById(UserContext.getUserId()));
    }

    @Operation(summary = "Update user profile")
    @PutMapping("/info")
    public R<?> updateInfo(@RequestBody UpdateUserDTO dto) {
        userService.updateInfo(UserContext.getUserId(), dto);
        return R.ok();
    }

    @Operation(summary = "Recharge balance")
    @PostMapping("/recharge")
    public R<?> recharge(@Valid @RequestBody RechargeDTO dto) {
        return R.ok(userService.recharge(UserContext.getUserId(), dto));
    }

    @Operation(summary = "Get recharge records")
    @GetMapping("/recharge/records")
    public R<?> rechargeRecords(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(userService.getRechargeRecords(UserContext.getUserId(), page, pageSize));
    }

    @Operation(summary = "Get payment/deduction records")
    @GetMapping("/payment/records")
    public R<?> paymentRecords(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(userService.getPaymentRecords(UserContext.getUserId(), page, pageSize));
    }

    @Operation(summary = "Get my reservations")
    @GetMapping("/reservations")
    public R<?> myReservations(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(userService.getUserReservations(UserContext.getUserId(), page, pageSize));
    }

    @Operation(summary = "Get my vehicle records")
    @GetMapping("/vehicle-records")
    public R<?> myVehicleRecords(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(userService.getUserVehicleRecords(UserContext.getUserId(), page, pageSize));
    }
}
