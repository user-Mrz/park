package com.parking.miniapp.service;

import java.util.Map;

/**
 * 第三方校园管理系统单点认证服务：验签通过后直接使用其身份信息完成登录。
 * <p>与手机号/微信登录并存，不替代原有认证与管理员身份审核流程。</p>
 */
public interface CampusSsoService {

    /**
     * 校园单点认证登录。
     *
     * @param appId     第三方应用标识（请求头 X-Campus-App-Id）
     * @param timestamp 请求时间戳毫秒（请求头 X-Campus-Timestamp）
     * @param nonce     随机串（请求头 X-Campus-Nonce），用于防重放
     * @param signature HMAC-SHA256 签名（请求头 X-Campus-Signature）
     * @param rawBody   原始 JSON 请求体（参与签名）
     * @return token 与用户信息
     */
    Map<String, Object> login(String appId, String timestamp, String nonce, String signature, String rawBody);
}
