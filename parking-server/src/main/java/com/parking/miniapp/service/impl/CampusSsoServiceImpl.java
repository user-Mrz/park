package com.parking.miniapp.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.common.BusinessException;
import com.parking.common.IdentityType;
import com.parking.system.dto.CampusSsoLoginDTO;
import com.parking.system.domain.User;
import com.parking.system.mapper.UserMapper;
import com.parking.miniapp.service.CampusSsoService;
import com.parking.system.service.UserRoleService;
import com.parking.common.util.CampusSignatureUtil;
import com.parking.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 校园管理系统单点认证实现。
 * <p>
 * 安全模型：预共享 appId + secret，请求头携带时间戳、随机串与 HMAC-SHA256 签名，
 * 签名原文为 {@code appId + "\n" + timestamp + "\n" + nonce + "\n" + rawBody}。
 * 时间戳允许 ±5 分钟偏差，nonce 通过 Redis 做 5 分钟防重放。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampusSsoServiceImpl implements CampusSsoService {

    private static final Pattern USER_NO_PATTERN = Pattern.compile("^[A-Za-z0-9]{4,20}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    /** 请求时间戳允许的偏差（毫秒） */
    private static final long TIMESTAMP_WINDOW_MS = 5 * 60 * 1000L;
    /** 防重放 nonce 的 Redis 保留时长 */
    private static final long NONCE_TTL_MINUTES = 5;
    /** nonce 最大长度 */
    private static final int MAX_NONCE_LENGTH = 64;
    /** 请求体上限，避免异常大报文参与签名计算 */
    private static final int MAX_BODY_LENGTH = 8 * 1024;
    /** 防重放 nonce 的 Redis key 前缀 */
    private static final String NONCE_KEY_PREFIX = "campus:sso:nonce:";

    private final UserMapper userMapper;
    private final UserRoleService userRoleService;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 对接方应用标识，未配置时接口不可用 */
    @Value("${parking.campus-sso.app-id:}")
    private String configuredAppId;

    /** 预共享签名密钥，未配置时接口不可用 */
    @Value("${parking.campus-sso.secret:}")
    private String configuredSecret;

    @Override
    @Transactional
    public Map<String, Object> login(String appId, String timestamp, String nonce,
                                     String signature, String rawBody) {
        verifySignature(appId, timestamp, nonce, signature, rawBody);
        CampusSsoLoginDTO dto = parseBody(rawBody);
        validateIdentity(dto);

        String userNo = dto.getUserNo().trim();
        int identityType = dto.getIdentityType();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserNo, userNo));
        boolean created = false;
        if (user == null) {
            user = createUser(userNo, identityType, dto, appId);
            created = true;
            log.info("[CampusSSO] 新建账号 appId={}, userNo={}", appId, userNo);
        } else {
            updateUser(user, identityType, dto, appId);
            log.info("[CampusSSO] 同步并登录已有账号 appId={}, userNo={}", appId, userNo);
        }

        String token = jwtUtil.generateToken(user.getId(),
                user.getPhone() != null ? user.getPhone() : "", "user");
        return buildResult(user, token, created);
    }

    // ===================== 验签 =====================

    /** 验签流程：配置 -> 请求头 -> 请求体 -> 时间戳 -> 签名 -> nonce 防重放 */
    private void verifySignature(String appId, String timestamp, String nonce,
                                 String signature, String rawBody) {
        assertConfigured();
        assertHeadersValid(appId, timestamp, nonce, signature);
        assertBodyValid(rawBody);
        assertTimestampFresh(timestamp);
        assertSignatureValid(appId, timestamp, nonce, signature, rawBody);
        consumeNonce(appId, nonce);
    }

    private void assertConfigured() {
        if (!StringUtils.hasText(configuredAppId) || !StringUtils.hasText(configuredSecret)) {
            throw new BusinessException(503, "校园单点认证未配置，请联系停车系统管理员");
        }
    }

    private void assertHeadersValid(String appId, String timestamp, String nonce, String signature) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce) || !StringUtils.hasText(signature)) {
            throw new BusinessException(401, "缺少校园单点认证请求头");
        }
        if (!configuredAppId.equals(appId)) {
            throw new BusinessException(401, "校园应用标识无效");
        }
        if (nonce.length() > MAX_NONCE_LENGTH) {
            throw new BusinessException(401, "请求随机串不合法");
        }
    }

    private void assertBodyValid(String rawBody) {
        if (rawBody == null || rawBody.length() > MAX_BODY_LENGTH) {
            throw new BusinessException(400, "请求体缺失或过大");
        }
    }

    private void assertTimestampFresh(String timestamp) {
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new BusinessException(401, "请求时间戳不合法");
        }
        if (Math.abs(System.currentTimeMillis() - ts) > TIMESTAMP_WINDOW_MS) {
            throw new BusinessException(401, "请求时间戳已过期");
        }
    }

    private void assertSignatureValid(String appId, String timestamp, String nonce,
                                      String signature, String rawBody) {
        String expected = CampusSignatureUtil.hmacSha256Hex(configuredSecret,
                CampusSignatureUtil.canonicalString(appId, timestamp, nonce, rawBody));
        // 常量时间比较，避免通过响应耗时猜测签名
        boolean matched = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.toLowerCase().getBytes(StandardCharsets.UTF_8));
        if (!matched) {
            throw new BusinessException(401, "签名校验失败");
        }
    }

    /** 签名通过后再占用 nonce，签名失败的请求不会消耗对接方的随机串 */
    private void consumeNonce(String appId, String nonce) {
        Boolean fresh = redisTemplate.opsForValue().setIfAbsent(
                NONCE_KEY_PREFIX + appId + ":" + nonce, "1",
                NONCE_TTL_MINUTES, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(fresh)) {
            throw new BusinessException(401, "请求已失效（nonce 重复）");
        }
    }

    // ===================== 请求解析与校验 =====================

    private CampusSsoLoginDTO parseBody(String rawBody) {
        try {
            CampusSsoLoginDTO dto = objectMapper.readValue(rawBody, CampusSsoLoginDTO.class);
            if (dto == null) throw new BusinessException("请求体不能为空");
            return dto;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("请求体不是合法的 JSON");
        }
    }

    private void validateIdentity(CampusSsoLoginDTO dto) {
        String userNo = dto.getUserNo() == null ? "" : dto.getUserNo().trim();
        if (!USER_NO_PATTERN.matcher(userNo).matches()) {
            throw new BusinessException("学号/工号格式不正确（4-20 位字母或数字）");
        }
        Integer type = dto.getIdentityType();
        if (type == null || type < IdentityType.STUDENT || type > IdentityType.STAFF) {
            throw new BusinessException("身份类型不合法（仅支持 1学生 2教师 3职工）");
        }
        if (StringUtils.hasText(dto.getRealName()) && dto.getRealName().trim().length() > 64) {
            throw new BusinessException("姓名长度不能超过 64 个字符");
        }
        String phone = dto.getPhone() == null ? "" : dto.getPhone().trim();
        if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("手机号格式不正确");
        }
        if (dto.getDepartment() != null && dto.getDepartment().trim().length() > 64) {
            throw new BusinessException("院系/部门名称过长");
        }
        if (dto.getClassName() != null && dto.getClassName().trim().length() > 64) {
            throw new BusinessException("班级名称过长");
        }
    }

    // ===================== 账号落库 =====================

    private User createUser(String userNo, int identityType, CampusSsoLoginDTO dto, String appId) {
        Snowflake sf = IdUtil.getSnowflake(1, 1);
        User user = new User();
        user.setId(sf.nextId());
        user.setUserNo(userNo);
        user.setIdentityType(identityType);
        // 校园系统为权威数据源：直接标记已认证，无需管理员二次审核
        user.setIdentityStatus(IdentityType.AUDIT_APPROVED);
        user.setIdentityRemark("校园管理系统统一认证（" + appId + "）");
        user.setNickname(StringUtils.hasText(dto.getRealName()) ? dto.getRealName().trim() : userNo);
        user.setPhone(resolveBindablePhone(dto.getPhone(), null));
        user.setDepartment(dto.getDepartment() == null ? "" : dto.getDepartment().trim());
        user.setClassName(dto.getClassName() == null ? "" : dto.getClassName().trim());
        user.setPlateNumber("");
        user.setBalance(BigDecimal.ZERO);
        user.setStatus(1);
        user.setRoleCode(userRoleService.resolveRoleCodeByIdentity(identityType));
        userMapper.insert(user);
        return user;
    }

    private void updateUser(User user, int identityType, CampusSsoLoginDTO dto, String appId) {
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        // 身份变化或角色缺失时按新身份重新映射；已有手动调整的角色保持不变
        if (user.getIdentityType() == null || !user.getIdentityType().equals(identityType)
                || !StringUtils.hasText(user.getRoleCode())) {
            user.setRoleCode(userRoleService.resolveRoleCodeByIdentity(identityType));
        }
        user.setIdentityType(identityType);
        user.setIdentityStatus(IdentityType.AUDIT_APPROVED);
        user.setIdentityRemark("校园管理系统统一认证（" + appId + "）");
        if (StringUtils.hasText(dto.getDepartment())) {
            user.setDepartment(dto.getDepartment().trim());
        }
        if (StringUtils.hasText(dto.getClassName())) {
            user.setClassName(dto.getClassName().trim());
        }
        // 不覆盖用户自定义昵称，仅在昵称为空或仍为微信默认值时使用真实姓名
        if (StringUtils.hasText(dto.getRealName())
                && (!StringUtils.hasText(user.getNickname()) || "WeChat User".equals(user.getNickname()))) {
            user.setNickname(dto.getRealName().trim());
        }
        // 仅在尚未绑定手机号时补绑，且不抢占其他账号已使用的号码
        if (!StringUtils.hasText(user.getPhone())) {
            String phone = resolveBindablePhone(dto.getPhone(), user.getId());
            if (phone != null) user.setPhone(phone);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /** 手机号未被其他账号占用时返回可写入值，否则返回 null（不阻断校园认证登录） */
    private String resolveBindablePhone(String phone, Long selfId) {
        if (!StringUtils.hasText(phone)) return null;
        String p = phone.trim();
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, p)
                .ne(selfId != null, User::getId, selfId));
        if (count != null && count > 0) {
            log.warn("[CampusSSO] 手机号已被其他账号占用，跳过绑定");
            return null;
        }
        return p;
    }

    private Map<String, Object> buildResult(User user, String token, boolean created) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        // Long 雪花 ID 转字符串，避免第三方 JSON 解析丢精度
        result.put("userId", String.valueOf(user.getId()));
        result.put("userNo", user.getUserNo());
        result.put("nickname", user.getNickname());
        result.put("phone", user.getPhone());
        result.put("plateNumber", user.getPlateNumber());
        result.put("balance", user.getBalance());
        result.put("identityType", user.getIdentityType());
        result.put("identityLabel", IdentityType.label(user.getIdentityType()));
        result.put("identityStatus", user.getIdentityStatus());
        result.put("verified", true);
        result.put("department", user.getDepartment());
        result.put("className", user.getClassName());
        result.put("created", created);
        return result;
    }
}
