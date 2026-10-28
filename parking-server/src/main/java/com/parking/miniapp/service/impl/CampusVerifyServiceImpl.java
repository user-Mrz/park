package com.parking.miniapp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.system.dto.CampusVerifyResponse;
import com.parking.system.dto.CampusVerifyResult;
import com.parking.miniapp.service.CampusVerifyService;
import com.parking.common.util.CampusSignatureUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 校园身份核验实现：以 HMAC 签名的服务端请求调用校园管理系统核验接口。
 * <p>
 * 请求约定：POST {url}，JSON 体 {@code {"userNo":"...","identityType":1}}，
 * 携带 X-Campus-App-Id / X-Campus-Timestamp / X-Campus-Nonce / X-Campus-Signature；
 * 签名口径见 {@link CampusSignatureUtil}。
 * </p>
 * <p>
 * 容错策略：未配置、调用失败、响应异常均返回 {@link CampusVerifyResult.Status#UNAVAILABLE}，
 * 由业务侧保持原有人工审核流程；仅当校园系统明确未匹配时才返回 NOT_FOUND。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampusVerifyServiceImpl implements CampusVerifyService {

    private static final int DEFAULT_TIMEOUT_MS = 3000;
    private static final int SUCCESS_CODE = 200;
    /** 校园系统回传文本的最大采纳长度，超长视为无效 */
    private static final int MAX_TEXT_LENGTH = 64;
    /** 核验通过结果缓存时长：同一学号/身份在 TTL 内重复提交时不再请求校园接口 */
    private static final long SUCCESS_CACHE_TTL_MS = 5 * 60 * 1000L;

    private final ObjectMapper objectMapper;

    /** 核验通过结果缓存：userNo:identityType -> 结果 */
    private final ConcurrentHashMap<String, CachedResult> successCache = new ConcurrentHashMap<>();

    /** 校园管理系统核验接口地址，未配置时自动转人工审核 */
    @Value("${parking.campus-verify.url:}")
    private String url;

    /** 对接方应用标识 */
    @Value("${parking.campus-verify.app-id:}")
    private String appId;

    /** 对接方预共享签名密钥 */
    @Value("${parking.campus-verify.secret:}")
    private String secret;

    /** 连接与读取超时（毫秒） */
    @Value("${parking.campus-verify.timeout-ms:3000}")
    private int timeoutMs;

    private RestClient restClient;

    @PostConstruct
    void initRestClient() {
        int timeout = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
        // JDK HttpClient 自带连接池与 keep-alive，重复核验时复用 TCP 连接，避免每次重新握手
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeout))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(timeout));
        restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public CampusVerifyResult verify(String userNo, int identityType) {
        String cacheKey = userNo + ":" + identityType;
        CachedResult cached = successCache.get(cacheKey);
        if (cached != null && cached.expireAt > System.currentTimeMillis()) {
            return cached.result;
        }

        CampusVerifyResult result = doVerify(userNo, identityType);
        if (result.matched()) {
            successCache.put(cacheKey,
                    new CachedResult(result, System.currentTimeMillis() + SUCCESS_CACHE_TTL_MS));
        } else {
            // 未通过不缓存：校园系统补录数据后用户可立即重试
            successCache.remove(cacheKey);
        }
        return result;
    }

    private CampusVerifyResult doVerify(String userNo, int identityType) {
        if (!isConfigured()) {
            return CampusVerifyResult.unavailable();
        }
        try {
            String responseBody = postVerifyRequest(userNo, identityType);
            return parseResponse(responseBody, userNo, identityType);
        } catch (Exception e) {
            log.warn("[CampusVerify] 校园核验接口调用失败，转人工审核：{}", e.getMessage());
            return CampusVerifyResult.unavailable();
        }
    }

    private boolean isConfigured() {
        return StringUtils.hasText(url) && StringUtils.hasText(appId) && StringUtils.hasText(secret);
    }

    /** 发送带签名的核验请求，返回原始响应体 */
    private String postVerifyRequest(String userNo, int identityType) throws JsonProcessingException {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String body = objectMapper.writeValueAsString(
                Map.of("userNo", userNo, "identityType", identityType));
        String signature = CampusSignatureUtil.hmacSha256Hex(secret,
                CampusSignatureUtil.canonicalString(appId, timestamp, nonce, body));

        ResponseEntity<String> response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header(CampusSignatureUtil.HEADER_APP_ID, appId)
                .header(CampusSignatureUtil.HEADER_TIMESTAMP, timestamp)
                .header(CampusSignatureUtil.HEADER_NONCE, nonce)
                .header(CampusSignatureUtil.HEADER_SIGNATURE, signature)
                .body(body)
                .retrieve()
                .toEntity(String.class);
        return response.getBody();
    }

    /** 解析校园系统响应：业务码异常视为接口不可用，明确未匹配则视为查无此人 */
    private CampusVerifyResult parseResponse(String responseBody, String userNo, int identityType) {
        CampusVerifyResponse response;
        try {
            response = objectMapper.readValue(responseBody == null ? "" : responseBody,
                    CampusVerifyResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("校园核验响应不是合法的 JSON", e);
        }

        if (response == null || response.getCode() != SUCCESS_CODE) {
            log.warn("[CampusVerify] 校园系统返回异常状态 code={}",
                    response == null ? null : response.getCode());
            return CampusVerifyResult.unavailable();
        }

        CampusVerifyResponse.IdentityData data = response.getData();
        if (data == null || !data.isMatched()) {
            log.info("[CampusVerify] 校园系统未查询到身份 userNo={}, identityType={}", userNo, identityType);
            return CampusVerifyResult.notFound();
        }

        // 返回的学号/工号与身份类型必须与提交一致，避免学生冒用教师/职工身份
        if (!isSameIdentity(data, userNo, identityType)) {
            log.warn("[CampusVerify] 校园系统返回身份与提交不一致，转人工审核 "
                            + "userNo={}, returnedNo={}, identityType={}, returnedType={}",
                    userNo, data.getUserNo(), identityType, data.getIdentityType());
            return CampusVerifyResult.notFound();
        }

        return CampusVerifyResult.matched(
                normalizeText(data.getDepartment()),
                normalizeText(data.getClassName()));
    }

    private boolean isSameIdentity(CampusVerifyResponse.IdentityData data,
                                   String userNo, int identityType) {
        return userNo.equalsIgnoreCase(trim(data.getUserNo()))
                && data.getIdentityType() != null
                && data.getIdentityType() == identityType;
    }

    /** 校园系统返回的可选文本：空白或超长时视为未提供 */
    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) return null;
        String text = value.trim();
        return text.length() <= MAX_TEXT_LENGTH ? text : null;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /** 核验通过结果缓存条目 */
    private record CachedResult(CampusVerifyResult result, long expireAt) {
    }
}
