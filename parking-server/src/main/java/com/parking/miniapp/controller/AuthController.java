package com.parking.miniapp.controller;

import com.parking.common.R;
import com.parking.system.dto.LoginDTO;
import com.parking.system.dto.RegisterDTO;
import com.parking.system.dto.WechatLoginDTO;
import com.parking.miniapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序端认证接口：注册 / 手机号登录 / 微信登录。
 * 验证码接口位于管理端 {@link com.parking.admin.controller.AdminAuthController}，两端共用。
 */
@Tag(name = "Auth", description = "Authentication APIs")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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

    @Operation(summary = "WeChat mini program login")
    @PostMapping("/wechat-login")
    public R<?> wechatLogin(@Valid @RequestBody WechatLoginDTO dto) {
        return R.ok(authService.wechatLogin(dto));
    }
}
