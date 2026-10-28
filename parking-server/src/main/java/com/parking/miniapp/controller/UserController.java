package com.parking.miniapp.controller;

import com.parking.common.R;
import com.parking.common.UserContext;
import com.parking.system.dto.PasswordDTO;
import com.parking.system.dto.RechargeDTO;
import com.parking.system.dto.UpdateUserDTO;
import com.parking.system.dto.VisitReportDTO;
import com.parking.system.dto.WechatLoginDTO;
import com.parking.miniapp.service.UserService;
import com.parking.system.service.CampusRuleService;
import com.parking.system.service.UserRoleService;
import com.parking.system.service.VisitReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "User", description = "User APIs")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final VisitReportService visitReportService;
    private final CampusRuleService campusRuleService;
    private final UserRoleService userRoleService;

    @Operation(summary = "Get user profile")
    @GetMapping("/info")
    public R<?> info() {
        return R.ok(userService.getById(UserContext.getUserId()));
    }

    @Operation(summary = "Get current user role and visible parking zones (RBAC)")
    @GetMapping("/my-perms")
    public R<?> myPerms() {
        return R.ok(userRoleService.myPerms(UserContext.getUserId()));
    }

    @Operation(summary = "Update user profile (nickname / phone / plate / avatar)")
    @PutMapping("/info")
    public R<?> updateInfo(@RequestBody UpdateUserDTO dto) {
        return R.ok(userService.updateInfo(UserContext.getUserId(), dto));
    }

    @Operation(summary = "Change login password")
    @PutMapping("/password")
    public R<?> changePassword(@RequestBody PasswordDTO dto) {
        userService.changePassword(UserContext.getUserId(), dto);
        return R.ok();
    }

    @Operation(summary = "Upload user avatar")
    @PostMapping("/avatar")
    public R<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = userService.saveAvatar(UserContext.getUserId(), file);
        return R.ok(Map.of("url", url));
    }

    @Operation(summary = "Bind WeChat account to current user")
    @PostMapping("/bind-wechat")
    public R<?> bindWechat(@RequestBody WechatLoginDTO dto) {
        return R.ok(userService.bindWechat(UserContext.getUserId(), dto));
    }

    @Operation(summary = "Unbind WeChat account from current user")
    @PostMapping("/unbind-wechat")
    public R<?> unbindWechat() {
        return R.ok(userService.unbindWechat(UserContext.getUserId()));
    }

    @Operation(summary = "Get campus identity benefits (rate / free minutes / daily quota)")
    @GetMapping("/benefits")
    public R<?> benefits() {
        return R.ok(userService.benefits(UserContext.getUserId()));
    }

    @Operation(summary = "Submit visitor / parent vehicle report")
    @PostMapping("/visit-report")
    public R<?> createVisitReport(@RequestBody VisitReportDTO dto) {
        return R.ok(visitReportService.create(UserContext.getUserId(), dto));
    }

    @Operation(summary = "My visitor / parent vehicle reports")
    @GetMapping("/visit-reports")
    public R<?> myVisitReports(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(visitReportService.myReports(UserContext.getUserId(), page, pageSize));
    }

    @Operation(summary = "Cancel my visitor / parent vehicle report")
    @PostMapping("/visit-report/{id}/cancel")
    public R<?> cancelVisitReport(@PathVariable Long id) {
        visitReportService.cancel(UserContext.getUserId(), id);
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

    @Operation(summary = "Get campus parking rules (public whitelist only)")
    @GetMapping("/rules")
    public R<?> rules() {
        return R.ok(campusRuleService.getPublicRules());
    }
}
