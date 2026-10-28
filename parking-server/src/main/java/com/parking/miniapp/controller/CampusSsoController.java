package com.parking.miniapp.controller;

import com.parking.common.R;
import com.parking.miniapp.service.CampusSsoService;
import com.parking.common.util.CampusSignatureUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 第三方校园管理系统对接接口（服务端调用，不面向小程序）。
 * <p>
 * 请求头：X-Campus-App-Id / X-Campus-Timestamp / X-Campus-Nonce / X-Campus-Signature。
 * 签名算法：hex(HMAC-SHA256(secret, appId + "\n" + timestamp + "\n" + nonce + "\n" + rawBody))。
 * 该接口不走 JWT，鉴权完全由签名保证；原有注册、手机号登录、微信登录与管理员审核流程保持不变。
 * </p>
 */
@Tag(name = "CampusSSO", description = "第三方校园管理系统单点认证（签名鉴权）")
@RestController
@RequestMapping("/api/open/campus/sso")
@RequiredArgsConstructor
public class CampusSsoController {

    private final CampusSsoService campusSsoService;

    @Operation(summary = "校园身份单点认证登录（免二次身份认证）")
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public R<?> login(
            @RequestHeader(value = CampusSignatureUtil.HEADER_APP_ID, required = false) String appId,
            @RequestHeader(value = CampusSignatureUtil.HEADER_TIMESTAMP, required = false) String timestamp,
            @RequestHeader(value = CampusSignatureUtil.HEADER_NONCE, required = false) String nonce,
            @RequestHeader(value = CampusSignatureUtil.HEADER_SIGNATURE, required = false) String signature,
            @RequestBody(required = false) String rawBody) {
        return R.ok(campusSsoService.login(appId, timestamp, nonce, signature, rawBody));
    }
}
