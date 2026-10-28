package com.parking.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    /** 仓库内置的演示密钥：仅允许本地开发使用，生产 profile 启动时会被拒绝 */
    private static final String DEFAULT_SECRET =
            "parking-system-secret-key-2024-parking-lot-management-jwt-token";

    @Value("${parking.jwt.secret}")
    private String secret;

    @Value("${parking.jwt.expiration}")
    private long expiration;

    @Value("${parking.jwt.admin-expiration}")
    private long adminExpiration;

    private final Environment environment;

    public JwtUtil(Environment environment) {
        this.environment = environment;
    }

    /**
     * 启动校验：
     * 1. 密钥缺失或长度不足 32 字节（HS256 下限）直接启动失败；
     * 2. 生产 profile 下仍使用仓库内置默认密钥时拒绝启动，避免公开密钥被用于伪造管理员令牌。
     */
    @PostConstruct
    public void validateSecret() {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT 密钥未配置或长度不足 32 字节，请通过环境变量 JWT_SECRET 注入随机密钥");
        }
        if (DEFAULT_SECRET.equals(secret)) {
            boolean prod = Arrays.stream(environment.getActiveProfiles())
                    .anyMatch(p -> "prod".equalsIgnoreCase(p));
            if (prod) {
                throw new IllegalStateException(
                        "检测到生产环境仍在使用仓库默认 JWT 密钥，请通过环境变量 JWT_SECRET 注入随机密钥后再启动");
            }
            log.warn("[安全告警] 当前使用仓库内置默认 JWT 密钥，仅可用于本地开发；部署前请注入 JWT_SECRET");
        }
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成小程序用户令牌（userType=user）。
     */
    public String generateToken(Long userId, String phone, String role) {
        return buildToken(userId, phone, role, "user", expiration);
    }

    /**
     * 生成后台管理员令牌（userType=admin）。
     * <p>
     * roleCode 为 RBAC 角色编码（t_role.role_code），管理员令牌使用较短的过期时间。
     */
    public String generateAdminToken(Long adminId, String username, String roleCode) {
        return buildToken(adminId, username, roleCode, "admin", adminExpiration);
    }

    /**
     * 统一构造令牌：phone 字段兼容旧逻辑（管理员存用户名），userType 区分用户/管理员域。
     */
    private String buildToken(Long userId, String name, String role, String userType, long expMillis) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("phone", name)
                .claim("role", role)
                .claim("userType", userType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expMillis))
                .signWith(getKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        try {
            parseToken(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }
}
