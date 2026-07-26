package com.parking.controller;

import com.parking.common.R;
import com.parking.dto.AdminLoginDTO;
import com.parking.dto.LoginDTO;
import com.parking.dto.RegisterDTO;
import com.parking.dto.WechatLoginDTO;
import com.parking.service.AuthService;
import com.parking.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Authentication APIs")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @Operation(summary = "Generate captcha image")
    @GetMapping("/captcha")
    public R<?> captcha() {
        return R.ok(captchaService.generateCaptcha());
    }

    @Operation(summary = "User registration")
    @PostMapping("/register")
    public R<?> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok(authService.register(dto));
    }

    @Operation(summary = "User login")
    @PostMapping("/login")
    public R<?> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @Operation(summary = "Admin login")
    @PostMapping("/admin/login")
    public R<?> adminLogin(@Valid @RequestBody AdminLoginDTO dto) {
        return R.ok(authService.adminLogin(dto));
    }

    @Operation(summary = "WeChat mini program login")
    @PostMapping("/wechat-login")
    public R<?> wechatLogin(@Valid @RequestBody WechatLoginDTO dto) {
        return R.ok(authService.wechatLogin(dto));
    }
}
