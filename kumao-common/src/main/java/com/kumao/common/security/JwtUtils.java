package com.kumao.common.security;

import com.kumao.common.constant.CommonConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具，网关与 user-service 共用
 */
@Slf4j
public class JwtUtils {

    private final SecretKey secretKey;
    private final long expireSeconds;

    public JwtUtils(String secret, long expireSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    public String createToken(Long userId, String username, String role, String device) {
        Map<String, Object> claims = new HashMap<>(8);
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("device", device);
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireSeconds * 1000))
                .signWith(secretKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    public LoginUser parseToLoginUser(String token) {
        Claims claims = parse(token);
        return new LoginUser(
                claims.get("userId", Number.class).longValue(),
                claims.get("username", String.class),
                claims.get("role", String.class),
                claims.get("device", String.class));
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public static String resolveToken(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        if (header.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return header.substring(CommonConstants.TOKEN_PREFIX.length()).trim();
        }
        return header.trim();
    }
}
