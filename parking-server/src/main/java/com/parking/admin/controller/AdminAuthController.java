package com.parking.admin.controller;

import com.parking.admin.service.AdminAuthService;
import com.parking.common.R;
import com.parking.system.dto.AdminLoginDTO;
import com.parking.system.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端认证接口：验证码 + 管理员登录。
 * <p>
 * 验证码接口 /api/auth/captcha 由管理端与小程序端登录页共用（同一启动类、同一端口）。
 */
@Tag(name = "AdminAuth", description = "管理端认证 APIs")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final CaptchaService captchaService;

    @Operation(summary = "Generate captcha image (shared by admin & miniapp login)")
    @GetMapping("/captcha")
    public R<?> captcha() {
        return R.ok(captchaService.generateCaptcha());
    }

    @Operation(summary = "Admin login")
    @PostMapping("/admin/login")
    public R<?> adminLogin(@Valid @RequestBody AdminLoginDTO dto) {
        return R.ok(adminAuthService.adminLogin(dto));
    }
}
