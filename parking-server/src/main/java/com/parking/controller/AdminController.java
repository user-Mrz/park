package com.parking.controller;

import com.parking.common.R;
import com.parking.common.UserContext;
import com.parking.dto.BlacklistDTO;
import com.parking.entity.Department;
import com.parking.entity.ParkingSpot;
import com.parking.entity.VehicleRecord;
import com.parking.service.AdminService;
import com.parking.service.CampusAdminService;
import com.parking.service.DepartmentService;
import com.parking.service.VisitReportService;
import com.parking.service.ViolationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Tag(name = "Admin", description = "Admin management APIs")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final VisitReportService visitReportService;
    private final ViolationService violationService;
    private final CampusAdminService campusAdminService;
    private final DepartmentService departmentService;

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
                      @RequestParam(required = false) Integer status,
                      @RequestParam(required = false) Integer identityType,
                      @RequestParam(required = false) Integer identityStatus) {
        return R.ok(adminService.getUsers(page, pageSize, keyword, status, identityType, identityStatus));
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

    @Operation(summary = "Audit campus identity (1 approve / 2 reject)")
    @PutMapping("/user/{id}/identity")
    public R<?> updateUserIdentity(@PathVariable Long id,
                                    @RequestParam Integer identityStatus,
                                    @RequestParam(required = false) String remark) {
        adminService.updateUserIdentity(id, identityStatus, remark);
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

    // --- Visitor / Parent Vehicle Reports ---
    @Operation(summary = "Visitor / parent vehicle report list")
    @GetMapping("/visit-reports")
    public R<?> visitReports(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int pageSize,
                             @RequestParam(required = false) Integer status,
                             @RequestParam(required = false) String keyword) {
        return R.ok(visitReportService.listAll(page, pageSize, status, keyword));
    }

    @Operation(summary = "Visitor / parent report statistics")
    @GetMapping("/visit-reports/stats")
    public R<?> visitReportStats() {
        return R.ok(visitReportService.stats());
    }

    @Operation(summary = "Approve visitor / parent report")
    @PutMapping("/visit-report/{id}/approve")
    public R<?> approveVisitReport(@PathVariable Long id,
                                   @RequestParam(required = false) Long spotId,
                                   @RequestParam(required = false) String remark) {
        return R.ok(visitReportService.approve(id, UserContext.getUserId(), spotId, remark));
    }

    @Operation(summary = "Reject visitor / parent report")
    @PutMapping("/visit-report/{id}/reject")
    public R<?> rejectVisitReport(@PathVariable Long id,
                                  @RequestParam(required = false) String remark) {
        return R.ok(visitReportService.reject(id, UserContext.getUserId(), remark));
    }

    @Operation(summary = "Check in visitor vehicle")
    @PostMapping("/visit-report/{id}/check-in")
    public R<?> checkInVisit(@PathVariable Long id) {
        return R.ok(visitReportService.checkIn(id));
    }

    @Operation(summary = "Check out visitor vehicle (settle by campus rate)")
    @PostMapping("/visit-report/{id}/check-out")
    public R<?> checkOutVisit(@PathVariable Long id) {
        return R.ok(visitReportService.checkOut(id));
    }

    // --- Violations ---
    @Operation(summary = "Violation list")
    @GetMapping("/violations")
    public R<?> violations(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize,
                           @RequestParam(required = false) Integer status,
                           @RequestParam(required = false) String keyword) {
        return R.ok(violationService.list(page, pageSize, status, keyword));
    }

    @Operation(summary = "Violation statistics")
    @GetMapping("/violations/stats")
    public R<?> violationStats() {
        return R.ok(violationService.stats());
    }

    @Operation(summary = "Handle violation record")
    @PutMapping("/violation/{id}/handle")
    public R<?> handleViolation(@PathVariable Long id, @RequestParam(required = false) String remark) {
        violationService.handle(id, UserContext.getUserId(), remark);
        return R.ok();
    }

    // --- Campus statistics / Blacklist / Export ---
    @Operation(summary = "Campus statistics by identity / department / zone")
    @GetMapping("/campus-stats")
    public R<?> campusStats() {
        return R.ok(campusAdminService.campusStats());
    }

    @Operation(summary = "Blacklist page")
    @GetMapping("/blacklist")
    public R<?> blacklist(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String keyword) {
        return R.ok(campusAdminService.listBlacklist(page, pageSize, keyword));
    }

    @Operation(summary = "Add plate/user to blacklist")
    @PostMapping("/blacklist")
    public R<?> addBlacklist(@RequestBody BlacklistDTO dto) {
        return R.ok(campusAdminService.addBlacklist(dto));
    }

    @Operation(summary = "Remove blacklist record")
    @DeleteMapping("/blacklist/{id}")
    public R<?> removeBlacklist(@PathVariable Long id) {
        campusAdminService.removeBlacklist(id);
        return R.ok();
    }

    @Operation(summary = "Export CSV report (users / reservations / payments / violations / visits)")
    @GetMapping("/export/{type}")
    public void export(@PathVariable String type, HttpServletResponse response) throws IOException {
        String csv = campusAdminService.exportCsv(type);
        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + type + "_" + System.currentTimeMillis() + ".csv");
        // 前置 BOM，保证 Excel 正确识别 UTF-8 中文
        response.getWriter().write('\uFEFF');
        response.getWriter().write(csv);
        response.getWriter().flush();
    }

    // --- Campus departments ---
    @Operation(summary = "Department list (all)")
    @GetMapping("/departments")
    public R<?> departments() {
        return R.ok(departmentService.list(false));
    }

    @Operation(summary = "Create department")
    @PostMapping("/department")
    public R<?> createDepartment(@RequestBody Department department) {
        return R.ok(departmentService.create(department));
    }

    @Operation(summary = "Update department")
    @PutMapping("/department/{id}")
    public R<?> updateDepartment(@PathVariable Long id, @RequestBody Department department) {
        return R.ok(departmentService.update(id, department));
    }

    @Operation(summary = "Enable / disable department")
    @PutMapping("/department/{id}/status")
    public R<?> updateDepartmentStatus(@PathVariable Long id, @RequestParam Integer status) {
        departmentService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "Delete department (only when no user belongs to it)")
    @DeleteMapping("/department/{id}")
    public R<?> deleteDepartment(@PathVariable Long id) {
        departmentService.remove(id);
        return R.ok();
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
