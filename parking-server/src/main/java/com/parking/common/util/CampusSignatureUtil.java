package com.parking.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * 校园管理系统对接签名工具，入站验签与出站调用共用同一口径。
 * <p>
 * 签名原文：{@code appId + "\n" + timestamp + "\n" + nonce + "\n" + body}；
 * 签名算法：{@code hex(HMAC-SHA256(secret, 原文))}，输出小写十六进制。
 * </p>
 */
public final class CampusSignatureUtil {

    public static final String HEADER_APP_ID = "X-Campus-App-Id";
    public static final String HEADER_TIMESTAMP = "X-Campus-Timestamp";
    public static final String HEADER_NONCE = "X-Campus-Nonce";
    public static final String HEADER_SIGNATURE = "X-Campus-Signature";

    private CampusSignatureUtil() {
    }

    /** 拼接待签名原文 */
    public static String canonicalString(String appId, String timestamp, String nonce, String body) {
        return appId + "\n" + timestamp + "\n" + nonce + "\n" + body;
    }

    /** 计算小写十六进制 HMAC-SHA256 签名 */
    public static String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 签名计算失败", e);
        }
    }
}
