package com.kumao.review.controller;

import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.review.dto.AiModelConfigRequest;
import com.kumao.review.dto.AiModelConfigView;
import com.kumao.review.service.AiModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员模型配置接口。网关已要求 ADMIN，服务端再次校验。
 */
@RestController
@RequestMapping("/ai/models")
@RequiredArgsConstructor
public class AiModelController {

    private final AiModelConfigService modelConfigService;

    @GetMapping
    public R<Map<String, Object>> list() {
        UserContext.requireAdmin();
        return R.ok(modelConfigService.overview());
    }

    @PutMapping("/{type}")
    public R<AiModelConfigView> save(@PathVariable String type,
                                     @RequestBody AiModelConfigRequest request) {
        UserContext.requireAdmin();
        return R.ok(modelConfigService.save(type, request));
    }

    @PostMapping("/{type}/test")
    public R<Map<String, Object>> test(@PathVariable String type,
                                       @RequestBody(required = false) AiModelConfigRequest request) {
        UserContext.requireAdmin();
        Map<String, Object> result = modelConfigService.test(type, request);
        // 连接测试的失败原因属于业务结果，返回 200 + success=false，便于页面展示详情。
        return R.ok("测试完成", result);
    }

    @PostMapping("/{type}/models")
    public R<Map<String, Object>> models(@PathVariable String type,
                                         @RequestBody(required = false) AiModelConfigRequest request) {
        UserContext.requireAdmin();
        return R.ok(modelConfigService.listModels(type, request));
    }
}
