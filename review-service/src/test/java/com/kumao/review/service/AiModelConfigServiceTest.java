package com.kumao.review.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumao.review.config.DeepSeekProperties;
import com.kumao.review.dto.AiModelConfigRequest;
import com.kumao.review.dto.AiModelConfigView;
import com.kumao.review.entity.AiModelConfig;
import com.kumao.review.mapper.AiModelConfigMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiModelConfigServiceTest {

    @Test
    void shouldSaveEncryptedKeyAndResolveActiveModel() {
        InMemoryMapper mapper = new InMemoryMapper();
        AiModelConfigService service = service(mapper);

        AiModelConfigRequest request = new AiModelConfigRequest();
        request.setProvider("deepseek");
        request.setModelName("deepseek-chat");
        request.setBaseUrl("https://api.deepseek.com");
        request.setApiKey("sk-test-1234567890");
        request.setEnabled(true);
        request.setTemperature(new BigDecimal("0.20"));
        request.setTimeoutSeconds(15);
        request.setMaxRetries(1);

        AiModelConfigView view = service.save(AiModelConfigService.TYPE_REVIEW, request);

        assertTrue(view.isEnabled());
        assertTrue(view.isActive());
        assertEquals("sk-****7890", view.getApiKeyMasked());
        assertTrue(mapper.row.getApiKey().startsWith("enc:v1:"));
        assertFalse(mapper.row.getApiKey().contains("1234567890"));

        AiModelConfigService.ResolvedModel resolved = service.resolve(AiModelConfigService.TYPE_REVIEW);
        assertNotNull(resolved);
        assertEquals("sk-test-1234567890", resolved.apiKey());
        assertEquals("deepseek-chat", resolved.modelName());
        assertEquals("DATABASE", resolved.source());
    }

    @Test
    void shouldClearKeyAndFallBackToLocalRule() {
        InMemoryMapper mapper = new InMemoryMapper();
        AiModelConfigService service = service(mapper);

        AiModelConfigRequest enable = new AiModelConfigRequest();
        enable.setModelName("deepseek-chat");
        enable.setBaseUrl("https://api.deepseek.com");
        enable.setApiKey("sk-test-1234567890");
        enable.setEnabled(true);
        service.save(AiModelConfigService.TYPE_REVIEW, enable);

        AiModelConfigRequest clear = new AiModelConfigRequest();
        clear.setEnabled(false);
        clear.setClearApiKey(true);
        AiModelConfigView view = service.save(AiModelConfigService.TYPE_REVIEW, clear);

        assertNull(view.getApiKeyMasked());
        assertFalse(view.isActive());
        assertNull(service.resolve(AiModelConfigService.TYPE_REVIEW));
        assertTrue(service.unavailableReason(AiModelConfigService.TYPE_REVIEW).contains("未启用"));
    }

    @Test
    void shouldRejectEnablingWithoutApiKey() {
        AiModelConfigService service = service(new InMemoryMapper());
        AiModelConfigRequest request = new AiModelConfigRequest();
        request.setModelName("deepseek-chat");
        request.setBaseUrl("https://api.deepseek.com");
        request.setEnabled(true);

        try {
            service.save(AiModelConfigService.TYPE_TEXT, request);
            throw new AssertionError("expected BizException");
        } catch (com.kumao.common.exception.BizException e) {
            assertTrue(e.getMessage().contains("API Key"));
        }
    }

    @Test
    void shouldRejectEnablingWhenSavedKeyIsCleared() {
        InMemoryMapper mapper = new InMemoryMapper();
        AiModelConfigService service = service(mapper);

        AiModelConfigRequest enable = new AiModelConfigRequest();
        enable.setModelName("deepseek-chat");
        enable.setBaseUrl("https://api.deepseek.com");
        enable.setApiKey("sk-test-1234567890");
        enable.setEnabled(true);
        service.save(AiModelConfigService.TYPE_TEXT, enable);

        AiModelConfigRequest clear = new AiModelConfigRequest();
        clear.setEnabled(true);
        clear.setClearApiKey(true);
        try {
            service.save(AiModelConfigService.TYPE_TEXT, clear);
            throw new AssertionError("expected BizException");
        } catch (com.kumao.common.exception.BizException e) {
            assertTrue(e.getMessage().contains("API Key"));
        }
    }

    private AiModelConfigService service(InMemoryMapper mapper) {
        return new AiModelConfigService(mapper.mapper(), new DeepSeekProperties(),
                new SecretCipher("unit-test-secret"), new ObjectMapper());
    }

    /** 只实现配置服务用到的三个方法，避免依赖数据库。 */
    private static final class InMemoryMapper {

        private AiModelConfig row;

        private AiModelConfigMapper mapper() {
            return (AiModelConfigMapper) Proxy.newProxyInstance(
                    AiModelConfigMapper.class.getClassLoader(),
                    new Class<?>[]{AiModelConfigMapper.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "selectOne":
                                return row;
                            case "insert":
                                row = (AiModelConfig) args[0];
                                row.setId(1L);
                                return 1;
                            case "updateById":
                                row = (AiModelConfig) args[0];
                                return 1;
                            case "toString":
                                return "InMemoryMapper";
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "equals":
                                return proxy == args[0];
                            default:
                                return null;
                        }
                    });
        }
    }
}
