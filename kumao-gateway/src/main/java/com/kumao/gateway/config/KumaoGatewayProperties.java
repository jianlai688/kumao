package com.kumao.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关业务配置（@Value 无法绑定 YAML 列表，改用 ConfigurationProperties）
 * <p>
 * 注意：类名不能叫 GatewayProperties，否则会与 Spring Cloud Gateway 自带的
 * {@code org.springframework.cloud.gateway.config.GatewayProperties} 生成同名 Bean，导致启动失败。
 */
@Data
@Component
@ConfigurationProperties(prefix = "kumao.gateway")
public class KumaoGatewayProperties {

    /** 完全放行的路径（任意方法）：登录注册、短信、SRS 回调、健康检查、WS */
    private List<String> whitelist = new ArrayList<>();
    /** 仅 GET 放行的公开只读接口 */
    private List<String> publicRead = new ArrayList<>();
    /** 仅 APP 可访问 */
    private List<String> appOnly = new ArrayList<>();
    /** 仅桌面客户端可访问 */
    private List<String> desktopOnly = new ArrayList<>();
}
