package com.kumao.gateway.config;

import com.kumao.common.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayBeanConfig {

    @Bean
    public JwtUtils jwtUtils(@Value("${kumao.jwt.secret}") String secret,
                             @Value("${kumao.jwt.expire-seconds:604800}") long expireSeconds) {
        return new JwtUtils(secret, expireSeconds);
    }

    /** 按客户端 IP 限流 */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress() == null
                ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }

    /** 按用户限流 */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            return Mono.just(userId == null ? "anonymous" : userId);
        };
    }
}
