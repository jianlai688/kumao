package com.kumao.common.config;

import com.kumao.common.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 各微服务共用的 JwtUtils
 */
@Configuration
public class JwtConfig {

    @Bean
    @ConditionalOnMissingBean
    public JwtUtils jwtUtils(@Value("${kumao.jwt.secret:kumao-music-platform-jwt-secret-key-2026-please-change}") String secret,
                             @Value("${kumao.jwt.expire-seconds:604800}") long expireSeconds) {
        return new JwtUtils(secret, expireSeconds);
    }
}
