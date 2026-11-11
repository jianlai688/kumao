package com.kumao.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.kumao.common.result.R;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel 网关限流 + 熔断降级 + 统一友好提示
 * <p>
 * sentinelGatewayFilter / sentinelGatewayBlockExceptionHandler 由
 * SentinelSCGAutoConfiguration 自动注册，这里不再重复定义（否则 Bean 名冲突导致启动失败）。
 */
@Configuration
public class SentinelConfig {

    @PostConstruct
    public void init() {
        initApiDefinitions();
        initBlockHandler();
        initGatewayRules();
    }

    /** 统一限流响应：HTTP 429 + JSON 提示（BlockRequestHandler 返回 Mono&lt;ServerResponse&gt;） */
    private void initBlockHandler() {
        GatewayCallbackManager.setBlockHandler((exchange, t) -> ServerResponse
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(R.fail(429, "访问过于频繁，请稍后再试")));
    }

    private void initApiDefinitions() {
        Set<ApiDefinition> definitions = new HashSet<>();

        Set<ApiPredicateItem> allItems = new HashSet<>();
        allItems.add(new ApiPathPredicateItem().setPattern("/**")
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
        definitions.add(new ApiDefinition("kumao_gateway").setPredicateItems(allItems));

        Set<ApiPredicateItem> writeItems = new HashSet<>();
        writeItems.add(new ApiPathPredicateItem().setPattern("/api/comment/**")
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
        writeItems.add(new ApiPathPredicateItem().setPattern("/api/live/live/danmaku/**")
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
        writeItems.add(new ApiPathPredicateItem().setPattern("/api/file/**")
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
        definitions.add(new ApiDefinition("kumao_write").setPredicateItems(writeItems));

        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }

    private void initGatewayRules() {
        // 全局限流：单 IP 每秒 200 次；高频写接口更严格
        Set<GatewayFlowRule> rules = new HashSet<>();
        rules.add(new GatewayFlowRule("kumao_gateway")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(200)
                .setIntervalSec(1)
                .setBurst(50));
        rules.add(new GatewayFlowRule("kumao_write")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(30)
                .setIntervalSec(1));
        GatewayRuleManager.loadRules(rules);
    }
}
