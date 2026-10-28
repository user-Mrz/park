package com.parking.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * 微信授权 & 订阅消息工具类。
 *
 * <p>开发阶段：resolveOpenid 仍用 deviceId 生成 mock openid（保证同一设备稳定），
 * access_token 与 sendSubscribeMessage 在 appid/secret 未配置时自动降级为 log-only，
 * 不会抛出异常影响主业务流程。</p>
 */
@Slf4j
@Component
public class WechatUtil {

    /** access_token 在 Redis 中的 key */
    private static final String ACCESS_TOKEN_KEY = "wx:access_token";
    /** 微信返回 access_token 有效期 7200 秒，提前 200 秒过期 */
    private static final long ACCESS_TOKEN_TTL_SECONDS = 7000L;

    @Value("${parking.wechat.appid:}")
    private String appid;

    @Value("${parking.wechat.secret:}")
    private String secret;

    private final StringRedisTemplate stringRedisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WechatUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 解析用户微信 openid（静态方法，兼容现有调用）。
     * 开发阶段返回 mock openid；正式接入时可在微信小程序端通过 wx.login 获取 code，
     * 后端调 code2session 接口替换实现。
     */
    public static String resolveOpenid(String code, String deviceId) {
        return StringUtils.hasText(deviceId) ? "wx_" + deviceId : "wx_" + code;
    }

    /**
     * 获取微信接口调用凭证 access_token（Redis 缓存，7000 秒过期）。
     * 未配置 appid/secret 时返回 null。
     */
    public String getAccessToken() {
        if (!StringUtils.hasText(appid) || !StringUtils.hasText(secret)) {
            log.warn("[Wechat] appid/secret 未配置，access_token 获取降级为 null");
            return null;
        }
        // 先查 Redis 缓存
        try {
            String cached = stringRedisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
            if (StringUtils.hasText(cached)) return cached;
        } catch (Exception e) {
            log.warn("[Wechat] Redis 读 access_token 失败，回源请求官方接口: {}", e.getMessage());
        }
        // 回源请求微信官方接口
        try {
            String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                    + appid + "&secret=" + secret;
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null || !resp.containsKey("access_token")) {
                log.error("[Wechat] 获取 access_token 失败: {}", resp);
                return null;
            }
            String token = String.valueOf(resp.get("access_token"));
            try {
                stringRedisTemplate.opsForValue().set(ACCESS_TOKEN_KEY, token, Duration.ofSeconds(ACCESS_TOKEN_TTL_SECONDS));
            } catch (Exception e) {
                log.warn("[Wechat] Redis 写 access_token 失败，不影响本次返回: {}", e.getMessage());
            }
            return token;
        } catch (Exception e) {
            log.error("[Wechat] 请求 access_token 异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 发送微信订阅消息。
     *
     * @param openid      接收者 openid
     * @param templateId  订阅消息模板 ID
     * @param data        模板数据（微信要求的 keyword 字段 Map，值用 {"value":"xxx"} 包裹）
     * @param page        点击消息跳转的小程序页面（可选）
     * @return true 发送成功 / false 发送失败或降级跳过
     */
    public boolean sendSubscribeMessage(String openid, String templateId, Map<String, Object> data, String page) {
        if (!StringUtils.hasText(openid)) {
            log.warn("[Wechat] sendSubscribeMessage 跳过：openid 为空");
            return false;
        }
        if (!StringUtils.hasText(templateId)) {
            log.warn("[Wechat] sendSubscribeMessage 跳过：templateId 为空（请在 SystemConfig 中配置 wx_template_*）");
            return false;
        }
        String accessToken = getAccessToken();
        if (!StringUtils.hasText(accessToken)) {
            log.info("[Wechat] sendSubscribeMessage 降级：access_token 未就绪（appid/secret 未配置？）"
                    + " templateId={} openid={}", templateId, openid);
            return false;
        }
        try {
            String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;
            Map<String, Object> body = Map.of(
                    "touser", openid,
                    "template_id", templateId,
                    "page", page == null ? "" : page,
                    "data", data
            );
            String jsonBody = objectMapper.writeValueAsString(body);
            String resp = restTemplate.postForObject(url, jsonBody, String.class);
            log.info("[Wechat] sendSubscribeMessage templateId={} openid={} resp={}", templateId, openid, resp);
            if (resp != null && resp.contains("\"errcode\":0")) {
                return true;
            }
            log.warn("[Wechat] sendSubscribeMessage 失败 resp={}", resp);
            return false;
        } catch (Exception e) {
            log.error("[Wechat] sendSubscribeMessage 异常: {}", e.getMessage());
            return false;
        }
    }
}
