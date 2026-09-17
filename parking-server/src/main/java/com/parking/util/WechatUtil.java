package com.parking.util;

import org.springframework.util.StringUtils;

/**
 * 微信授权工具类。
 *
 * <p>开发阶段未接入真实 code2session：用小程序传入的稳定设备标识（deviceId）生成 mock openid，
 * 保证同一台设备每次微信登录/绑定得到同一个 openid。正式接入后只需替换 {@link #resolveOpenid} 的实现。</p>
 */
public final class WechatUtil {

    private WechatUtil() {
    }

    /**
     * 解析用户微信 openid。优先使用 deviceId（稳定），否则退化为使用登录 code。
     */
    public static String resolveOpenid(String code, String deviceId) {
        return StringUtils.hasText(deviceId) ? "wx_" + deviceId : "wx_" + code;
    }
}
