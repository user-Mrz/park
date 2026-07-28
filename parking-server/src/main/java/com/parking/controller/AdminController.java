package com.parking.controller;

import com.parking.common.R;
import com.parking.common.UserContext;
import com.parking.entity.ParkingSpot;
import com.parking.entity.VehicleRecord;
import com.parking.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Admin", description = "Admin management APIs")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // --- Dashboard ---
    @Operation(summary = "Get dashboard statistics")
    @GetMapping("/dashboard")
    public R<?> dashboard() {
        return R.ok(adminService.getDashboard());
    }

    // --- User Management ---
    @Operation(summary = "Get user list")
    @GetMapping("/users")
    public R<?> users(@RequestParam(defaultValue = "1") int page,
                      @RequestParam(defaultValue = "10") int pageSize,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Integer status) {
        return R.ok(adminService.getUsers(page, pageSize, keyword, status));
    }

    @Operation(summary = "Get user detail")
    @GetMapping("/user/{id}")
    public R<?> userDetail(@PathVariable Long id) {
        return R.ok(adminService.getUserById(id));
    }

    @Operation(summary = "Update user status")
    @PutMapping("/user/{id}/status")
    public R<?> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateUserStatus(id, status);
        return R.ok();
    }

    // --- Reservation Management ---
    @Operation(summary = "Get all reservations")
    @GetMapping("/reservations")
    public R<?> reservations(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int pageSize,
                              @RequestParam(required = false) Integer status) {
        return R.ok(adminService.getAllReservations(page, pageSize, status));
    }

    @Operation(summary = "Update reservation status")
    @PutMapping("/reservation/{id}/status")
    public R<?> updateReservationStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateReservationStatus(id, status);
        return R.ok();
    }

    // --- Vehicle Records ---
    @Operation(summary = "Get all vehicle records")
    @GetMapping("/vehicle-records")
    public R<?> vehicleRecords(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) Integer status) {
        return R.ok(adminService.getAllVehicleRecords(page, pageSize, status));
    }

    @Operation(summary = "Update vehicle record")
    @PutMapping("/vehicle-record/{id}")
    public R<?> updateVehicleRecord(@PathVariable Long id, @RequestBody VehicleRecord record) {
        return R.ok(adminService.updateVehicleRecord(id, record));
    }

    @Operation(summary = "Force exit vehicle")
    @PostMapping("/vehicle-record/{id}/force-exit")
    public R<?> forceExit(@PathVariable Long id) {
        return R.ok(adminService.forceExit(id));
    }

    // --- Recharge Records ---
    @Operation(summary = "Get all recharge records")
    @GetMapping("/recharge-records")
    public R<?> rechargeRecords(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(adminService.getAllRechargeRecords(page, pageSize));
    }

    // --- Payment Records ---
    @Operation(summary = "Get all payment records")
    @GetMapping("/payment-records")
    public R<?> paymentRecords(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(adminService.getAllPaymentRecords(page, pageSize));
    }

    // --- Parking Spots ---
    @Operation(summary = "Get all parking spots")
    @GetMapping("/spots")
    public R<?> spots(@RequestParam(defaultValue = "1") int page,
                      @RequestParam(defaultValue = "50") int pageSize) {
        return R.ok(adminService.getAllSpots(page, pageSize));
    }

    @Operation(summary = "Add parking spot")
    @PostMapping("/spot")
    public R<?> addSpot(@RequestBody ParkingSpot spot) {
        return R.ok(adminService.addSpot(spot));
    }

    @Operation(summary = "Update parking spot")
    @PutMapping("/spot/{id}")
    public R<?> updateSpot(@PathVariable Long id, @RequestBody ParkingSpot spot) {
        return R.ok(adminService.updateSpot(id, spot));
    }

    @Operation(summary = "Update spot status")
    @PutMapping("/spot/{id}/status")
    public R<?> updateSpotStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateSpotStatus(id, status);
        return R.ok();
    }

    // --- Spot Map ---
    @Operation(summary = "Get spot status map")
    @GetMapping("/spot-map")
    public R<?> spotMap() {
        return R.ok(adminService.getSpotMap());
    }

    // --- System Config ---
    @Operation(summary = "Get system config")
    @GetMapping("/config")
    public R<?> config() {
        return R.ok(adminService.getAllConfig());
    }

    @Operation(summary = "Update system config")
    @PutMapping("/config")
    public R<?> updateConfig(@RequestBody Map<String, String> configs) {
        adminService.updateConfig(configs);
        return R.ok();
    }
}
