package com.kumao.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumao.common.constant.CommonConstants;
import com.kumao.common.result.R;
import com.kumao.common.security.JwtUtils;
import com.kumao.common.security.LoginUser;
import com.kumao.gateway.config.KumaoGatewayProperties;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关鉴权过滤器：
 * 1. 白名单接口放行（GET 只读 + 登录相关），若带 token 则解析并透传身份（匿名可访问）
 * 2. 非白名单接口强制 Token 校验，未登录直接拦截
 * 3. 设备类型识别，APP / 桌面端专属接口隔离，后台 /admin 接口 ADMIN 校验
 * 4. 基础请求日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;
    private final KumaoGatewayProperties gatewayProperties;

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();
        long start = System.currentTimeMillis();

        log.info("[网关] {} {} from {}", method, path, request.getRemoteAddress());

        String token = JwtUtils.resolveToken(request.getHeaders().getFirst(CommonConstants.TOKEN_HEADER));
        LoginUser user = parseQuietly(token);

        boolean adminPath = isAdminPath(path);
        boolean readOnly = HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method)
                || HttpMethod.OPTIONS.equals(method);
        boolean openPath = matchesAny(gatewayProperties.getWhitelist(), path)
                || (readOnly && matchesAny(gatewayProperties.getPublicRead(), path));

        // 后台接口一律要求登录 + ADMIN
        if (adminPath) {
            if (user == null) {
                return unauthorized(exchange, "未登录，请先登录");
            }
            if (!user.hasRole(CommonConstants.ROLE_ADMIN)) {
                return forbidden(exchange, "无后台管理权限");
            }
        } else if (openPath) {
            // 公开接口：匿名放行；已登录则透传身份（用于收藏状态、VIP 鉴权等）
            return chain.filter(inject(exchange, user))
                    .then(Mono.fromRunnable(() -> log.info("[网关] {} {} -> {} ({}ms)",
                            method, path, exchange.getResponse().getStatusCode(),
                            System.currentTimeMillis() - start)));
        }

        if (user == null) {
            return unauthorized(exchange, token == null ? "未登录，请先登录" : "登录已过期，请重新登录");
        }

        String device = user.getDevice() == null ? CommonConstants.DEVICE_PC : user.getDevice();
        if (matchesAny(gatewayProperties.getAppOnly(), path) && !CommonConstants.DEVICE_APP.equals(device)) {
            return forbidden(exchange, "该接口仅 APP 端可用");
        }
        if (matchesAny(gatewayProperties.getDesktopOnly(), path) && !CommonConstants.DEVICE_DESKTOP.equals(device)) {
            return forbidden(exchange, "该接口仅桌面客户端可用");
        }

        return chain.filter(inject(exchange, user))
                .then(Mono.fromRunnable(() -> log.info("[网关] {} {} -> {} ({}ms) userId={}",
                        method, path, exchange.getResponse().getStatusCode(),
                        System.currentTimeMillis() - start, user.getUserId())));
    }

    /**
     * 将登录身份写入下游请求头；游客则清除伪造头
     */
    private ServerWebExchange inject(ServerWebExchange exchange, LoginUser user) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(CommonConstants.HEADER_USER_ID);
                    h.remove(CommonConstants.HEADER_USERNAME);
                    h.remove(CommonConstants.HEADER_ROLE);
                    h.remove(CommonConstants.HEADER_DEVICE);
                    if (user != null) {
                        String device = user.getDevice() == null ? CommonConstants.DEVICE_PC : user.getDevice();
                        h.add(CommonConstants.HEADER_USER_ID, String.valueOf(user.getUserId()));
                        h.add(CommonConstants.HEADER_USERNAME, user.getUsername() == null ? "" : user.getUsername());
                        h.add(CommonConstants.HEADER_ROLE, user.getRole() == null ? CommonConstants.ROLE_USER : user.getRole());
                        h.add(CommonConstants.HEADER_DEVICE, device);
                    }
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private LoginUser parseQuietly(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = jwtUtils.parse(token);
            return new LoginUser(
                    claims.get("userId", Number.class).longValue(),
                    claims.get("username", String.class),
                    claims.get("role", String.class),
                    claims.get("device", String.class));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAdminPath(String path) {
        return path.matches(".*/admin(/.*)?");
    }

    private boolean matchesAny(List<String> patterns, String path) {
        if (patterns == null || patterns.isEmpty() || path == null) {
            return false;
        }
        for (String pattern : patterns) {
            if (matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return write(exchange, HttpStatus.UNAUTHORIZED, R.fail(401, message));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        return write(exchange, HttpStatus.FORBIDDEN, R.fail(403, message));
    }

    private Mono<Void> write(ServerWebExchange exchange, HttpStatus status, R<?> body) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            DataBuffer buffer = response.bufferFactory().wrap(
                    ("{\"code\":" + status.value() + ",\"message\":\"请求失败\"}")
                            .getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
