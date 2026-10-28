package com.parking.admin.controller;

import com.parking.common.R;
import com.parking.common.UserContext;
import com.parking.common.annotation.OperationLog;
import com.parking.common.annotation.OperationType;
import com.parking.system.dto.BlacklistDTO;
import com.parking.system.dto.WhitelistDTO;
import com.parking.system.domain.Department;
import com.parking.system.dto.ServerInfo;
import com.parking.system.domain.ParkingSpot;
import com.parking.system.domain.VehicleRecord;
import com.parking.admin.service.AdminService;
import com.parking.admin.service.CampusAdminService;
import com.parking.system.service.DepartmentService;
import com.parking.system.service.VehicleService;
import com.parking.system.service.VisitReportService;
import com.parking.system.service.ViolationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
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
    private final VehicleService vehicleService;

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
    @OperationLog(value = "启用/禁用用户", menuCode = "users", type = OperationType.UPDATE)
    public R<?> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateUserStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "Audit campus identity (1 approve / 2 reject)")
    @PutMapping("/user/{id}/identity")
    @OperationLog(value = "校园身份审核", menuCode = "users", type = OperationType.AUDIT)
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
                              @RequestParam(required = false) Integer status,
                              @RequestParam(required = false) String reservationNo,
                              @RequestParam(required = false) Long userId,
                              @RequestParam(required = false) Long spotId) {
        return R.ok(adminService.getAllReservations(page, pageSize, status, reservationNo, userId, spotId));
    }

    @Operation(summary = "Update reservation status")
    @PutMapping("/reservation/{id}/status")
    @OperationLog(value = "更新预约状态", menuCode = "reservations", type = OperationType.UPDATE)
    public R<?> updateReservationStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateReservationStatus(id, status);
        return R.ok();
    }

    // --- Vehicle Records ---
    @Operation(summary = "Get all vehicle records")
    @GetMapping("/vehicle-records")
    public R<?> vehicleRecords(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) Integer status,
                                @RequestParam(required = false) String plateNumber,
                                @RequestParam(required = false) Long userId,
                                @RequestParam(required = false) Long spotId) {
        return R.ok(adminService.getAllVehicleRecords(page, pageSize, status, plateNumber, userId, spotId));
    }

    @Operation(summary = "Update vehicle record")
    @PutMapping("/vehicle-record/{id}")
    public R<?> updateVehicleRecord(@PathVariable Long id, @RequestBody VehicleRecord record) {
        return R.ok(adminService.updateVehicleRecord(id, record));
    }

    @Operation(summary = "Force exit vehicle")
    @PostMapping("/vehicle-record/{id}/force-exit")
    @OperationLog(value = "强制抬杆放行", menuCode = "vehicles", type = OperationType.UPDATE)
    public R<?> forceExit(@PathVariable Long id) {
        return R.ok(vehicleService.forceExit(id));
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

    // --- Monthly Pass（月卡 / 长租） ---
    @Operation(summary = "Get monthly pass list (filter by keyword / status / passType)")
    @GetMapping("/monthly-passes")
    public R<?> monthlyPasses(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int pageSize,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Integer status,
                              @RequestParam(required = false) Integer passType) {
        return R.ok(adminService.getMonthlyPasses(page, pageSize, keyword, status, passType));
    }

    @Operation(summary = "Monthly pass business stats (active / expiring / revenue)")
    @GetMapping("/monthly-passes/stats")
    public R<?> monthlyPassStats() {
        return R.ok(adminService.getMonthlyPassStats());
    }

    // --- Parking Spots ---
    @Operation(summary = "Get all parking spots")
    @GetMapping("/spots")
    public R<?> spots(@RequestParam(defaultValue = "1") int page,
                      @RequestParam(defaultValue = "50") int pageSize,
                      @RequestParam(required = false) Integer spotType,
                      @RequestParam(required = false) String spotCode,
                      @RequestParam(required = false) String area,
                      @RequestParam(required = false) String zoneCode) {
        return R.ok(adminService.getAllSpots(page, pageSize, spotType, spotCode, area, zoneCode));
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

    @Operation(summary = "Delete parking spot (occupied/reserved spots cannot be deleted)")
    @DeleteMapping("/spot/{id}")
    public R<?> deleteSpot(@PathVariable Long id) {
        adminService.deleteSpot(id);
        return R.ok();
    }

    
    @Operation(summary = "Get server hardware info: CPU / memory / JVM / disk")
    @GetMapping("/server-monitor")
    public R<?> serverMonitor() throws Exception {
        return R.ok(new ServerInfo().copyTo());
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

    @Operation(summary = "Daily statistics for a date range (revenue / vehicle visits)")
    @GetMapping("/campus-stats/daily")
    public R<?> campusDailyStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(campusAdminService.dailyStats(startDate, endDate));
    }

    @Operation(summary = "Blacklist page")
    @GetMapping("/blacklist")
    public R<?> blacklist(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String source) {
        return R.ok(campusAdminService.listBlacklist(page, pageSize, keyword, status, source));
    }

    @Operation(summary = "Add plate/user to blacklist")
    @PostMapping("/blacklist")
    @OperationLog(value = "加入黑名单", menuCode = "blacklist", type = OperationType.CREATE)
    public R<?> addBlacklist(@RequestBody BlacklistDTO dto) {
        return R.ok(campusAdminService.addBlacklist(dto, UserContext.getUserId()));
    }

    @Operation(summary = "Remove blacklist record")
    @DeleteMapping("/blacklist/{id}")
    @OperationLog(value = "移出黑名单", menuCode = "blacklist", type = OperationType.DELETE)
    public R<?> removeBlacklist(@PathVariable Long id) {
        campusAdminService.removeBlacklist(id);
        return R.ok();
    }

    @Operation(summary = "Whitelist page")
    @GetMapping("/whitelist")
    public R<?> whitelist(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String status) {
        return R.ok(campusAdminService.listWhitelist(page, pageSize, keyword, status));
    }

    @Operation(summary = "Add plate/user to whitelist")
    @PostMapping("/whitelist")
    @OperationLog(value = "加入白名单", menuCode = "blacklist", type = OperationType.CREATE)
    public R<?> addWhitelist(@RequestBody WhitelistDTO dto) {
        return R.ok(campusAdminService.addWhitelist(dto, UserContext.getUserId()));
    }

    @Operation(summary = "Remove whitelist record")
    @DeleteMapping("/whitelist/{id}")
    @OperationLog(value = "移出白名单", menuCode = "blacklist", type = OperationType.DELETE)
    public R<?> removeWhitelist(@PathVariable Long id) {
        campusAdminService.removeWhitelist(id);
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
    @OperationLog(value = "创建学院", menuCode = "departments", type = OperationType.CREATE)
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
    @OperationLog(value = "更新系统配置", menuCode = "config", type = OperationType.UPDATE)
    public R<?> updateConfig(@RequestBody Map<String, String> configs) {
        adminService.updateConfig(configs);
        return R.ok();
    }
}
