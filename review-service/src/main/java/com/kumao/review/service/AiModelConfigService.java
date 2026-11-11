package com.kumao.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumao.common.exception.BizException;
import com.kumao.common.security.LoginUser;
import com.kumao.common.security.UserContext;
import com.kumao.review.config.DeepSeekProperties;
import com.kumao.review.dto.AiModelConfigRequest;
import com.kumao.review.dto.AiModelConfigView;
import com.kumao.review.entity.AiModelConfig;
import com.kumao.review.mapper.AiModelConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 三类 AI 能力的模型配置：审核、文本、图片识别。
 *
 * <p>配置存于共享库 {@code ai_model_config}，密钥加密保存。审核链路优先使用
 * 数据库中的启用配置，未配置时回退到原有环境变量，最后降级本地规则。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelConfigService {

    public static final String TYPE_REVIEW = "REVIEW";
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_VISION = "VISION";

    private static final Map<String, ModelMeta> METAS = new LinkedHashMap<>();
    private static final Map<String, ProviderPreset> PROVIDERS = new LinkedHashMap<>();

    static {
        METAS.put(TYPE_REVIEW, new ModelMeta(TYPE_REVIEW, "审核模型",
                "统一审核工作台的 PASS / REVIEW / REJECT 建议与风险判定。",
                "deepseek", "deepseek-chat", "https://api.deepseek.com", new BigDecimal("0.10")));
        METAS.put(TYPE_TEXT, new ModelMeta(TYPE_TEXT, "文本模型",
                "标题、简介、歌词与运营文案等文本生成场景。",
                "deepseek", "deepseek-chat", "https://api.deepseek.com", new BigDecimal("0.30")));
        METAS.put(TYPE_VISION, new ModelMeta(TYPE_VISION, "识别图片模型",
                "封面、广告素材与证件图片的内容识别场景。",
                "dashscope", "qwen-vl-max", "https://dashscope.aliyuncs.com/compatible-mode/v1",
                new BigDecimal("0.10")));

        PROVIDERS.put("deepseek", new ProviderPreset("deepseek", "DeepSeek",
                "https://api.deepseek.com", List.of("deepseek-chat", "deepseek-reasoner")));
        PROVIDERS.put("dashscope", new ProviderPreset("dashscope", "阿里云百炼",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                List.of("qwen-max", "qwen-plus", "qwen-vl-max", "qwen-vl-plus")));
        PROVIDERS.put("zhipu", new ProviderPreset("zhipu", "智谱 GLM",
                "https://open.bigmodel.cn/api/paas/v4",
                List.of("glm-4-plus", "glm-4v-plus", "glm-4-flash")));
        PROVIDERS.put("moonshot", new ProviderPreset("moonshot", "Moonshot",
                "https://api.moonshot.cn/v1", List.of("moonshot-v1-8k", "moonshot-v1-32k")));
        PROVIDERS.put("openai", new ProviderPreset("openai", "OpenAI",
                "https://api.openai.com/v1", List.of("gpt-4o-mini", "gpt-4o")));
        PROVIDERS.put("custom", new ProviderPreset("custom", "自定义",
                "", List.of()));
    }

    private final AiModelConfigMapper configMapper;
    private final DeepSeekProperties deepSeekProperties;
    private final SecretCipher secretCipher;
    private final ObjectMapper objectMapper;

    /** 当前生效的模型配置，供审核服务调用。 */
    public ResolvedModel resolve(String type) {
        String normalized = normalizeType(type);
        AiModelConfig config = find(normalized);
        if (config != null && Boolean.TRUE.equals(config.getEnabled())) {
            String key = safeDecrypt(config.getApiKey());
            if (StringUtils.hasText(key) && StringUtils.hasText(config.getModelName())
                    && StringUtils.hasText(config.getBaseUrl())) {
                return new ResolvedModel(normalized, config.getProvider(), config.getModelName(),
                        trimTrailingSlash(config.getBaseUrl()), key, valueOrDefault(config.getTemperature(),
                        METAS.get(normalized).temperature()).doubleValue(),
                        valueOrDefault(config.getTimeoutSeconds(), 30),
                        valueOrDefault(config.getMaxRetries(), 2), "DATABASE");
            }
        }
        if (TYPE_REVIEW.equals(normalized) && environmentConfigured()) {
            return new ResolvedModel(normalized, "deepseek", deepSeekProperties.getModel(),
                    trimTrailingSlash(deepSeekProperties.getBaseUrl()), deepSeekProperties.getApiKey(),
                    deepSeekProperties.getTemperature(), deepSeekProperties.getTimeoutSeconds(),
                    deepSeekProperties.getMaxRetries(), "ENVIRONMENT");
        }
        return null;
    }

    public List<AiModelConfigView> list() {
        List<AiModelConfigView> views = new ArrayList<>();
        for (ModelMeta meta : METAS.values()) {
            views.add(toView(meta, find(meta.type())));
        }
        return views;
    }

    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", list());
        result.put("presets", providerPresets());
        result.put("status", status());
        return result;
    }

    public Map<String, Object> status() {
        AiModelConfig config = find(TYPE_REVIEW);
        ResolvedModel active = resolve(TYPE_REVIEW);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", config != null && Boolean.TRUE.equals(config.getEnabled()));
        status.put("configured", active != null);
        status.put("mode", active == null ? "LOCAL_RULE" : "AI_MODEL");
        status.put("provider", active == null ? "none" : active.provider());
        status.put("providerLabel", active == null ? "未配置" : providerLabel(active.provider()));
        status.put("model", active == null
                ? valueOrDefault(config == null ? null : config.getModelName(), METAS.get(TYPE_REVIEW).modelName())
                : active.modelName());
        status.put("source", active == null ? "NONE" : active.source());
        status.put("timeoutSeconds", active == null ? 30 : active.timeoutSeconds());
        status.put("maxRetries", active == null ? 2 : active.maxRetries());
        status.put("environmentConfigured", environmentConfigured());
        status.put("fallbackReason", active == null ? unavailableReason(TYPE_REVIEW) : null);
        return status;
    }

    public AiModelConfigView save(String type, AiModelConfigRequest request) {
        String normalized = normalizeType(type);
        ModelMeta meta = METAS.get(normalized);
        AiModelConfig current = find(normalized);

        String provider = normalizeProvider(firstNonBlank(request.getProvider(),
                current == null ? meta.provider() : current.getProvider()));
        String modelName = trim(firstNonBlank(request.getModelName(),
                current == null ? meta.modelName() : current.getModelName()));
        String baseUrl = trim(firstNonBlank(request.getBaseUrl(),
                current == null ? meta.baseUrl() : current.getBaseUrl()));
        boolean enabled = request.getEnabled() != null
                ? request.getEnabled()
                : current != null && Boolean.TRUE.equals(current.getEnabled());

        if (!PROVIDERS.containsKey(provider)) {
            throw new BizException("不支持的服务商");
        }
        if (enabled) {
            if (!StringUtils.hasText(modelName)) throw new BizException("启用模型前请填写模型名称");
            if (!StringUtils.hasText(baseUrl)) throw new BizException("启用模型前请填写接口地址");
            String existingKey = current == null ? null : safeDecrypt(current.getApiKey());
            boolean willHaveKey = !request.isClearApiKey()
                    && (StringUtils.hasText(request.getApiKey()) || StringUtils.hasText(existingKey));
            if (!willHaveKey) {
                throw new BizException("启用模型前请填写 API Key");
            }
        }
        validateBaseUrl(baseUrl);

        AiModelConfig config = current == null ? new AiModelConfig() : current;
        config.setModelType(normalized);
        config.setProvider(provider);
        config.setModelName(modelName);
        config.setBaseUrl(baseUrl);
        config.setTemperature(clamp(request.getTemperature() == null
                ? (current == null ? meta.temperature() : current.getTemperature())
                : request.getTemperature(), BigDecimal.ZERO, new BigDecimal("2.00")));
        config.setTimeoutSeconds(clamp(request.getTimeoutSeconds() == null
                ? (current == null ? 30 : current.getTimeoutSeconds())
                : request.getTimeoutSeconds(), 1, 120));
        config.setMaxRetries(clamp(request.getMaxRetries() == null
                ? (current == null ? 2 : current.getMaxRetries())
                : request.getMaxRetries(), 0, 5));
        config.setEnabled(enabled);
        config.setRemark(limit(firstNonBlank(request.getRemark(),
                current == null ? null : current.getRemark()), 255));

        if (request.isClearApiKey()) {
            config.setApiKey(null);
        } else if (StringUtils.hasText(request.getApiKey())) {
            config.setApiKey(secretCipher.encrypt(request.getApiKey().trim()));
        }

        LoginUser admin = UserContext.get();
        if (admin != null) {
            config.setUpdateBy(admin.getUserId());
            config.setUpdateByName(limit(admin.getUsername(), 100));
        }
        config.setUpdateTime(java.time.LocalDateTime.now());
        if (current == null) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
        log.info("AI 模型配置已更新 type={} provider={} model={} enabled={} by={}",
                normalized, provider, modelName, enabled, admin == null ? "system" : admin.getUsername());
        return toView(meta, find(normalized));
    }

    /**
     * 读取服务商模型列表，供管理端在填写接口地址和 API Key 后直接选择模型。
     */
    public Map<String, Object> listModels(String type, AiModelConfigRequest draft) {
        String normalized = normalizeType(type);
        AiModelConfig saved = find(normalized);
        String baseUrl = trim(firstNonBlank(
                draft == null ? null : draft.getBaseUrl(),
                saved == null ? METAS.get(normalized).baseUrl() : saved.getBaseUrl()));
        String apiKey = null;
        if (draft != null && StringUtils.hasText(draft.getApiKey())) {
            apiKey = draft.getApiKey().trim();
        } else if (saved != null) {
            apiKey = safeDecrypt(saved.getApiKey());
        }
        Integer timeout = draft != null && draft.getTimeoutSeconds() != null
                ? draft.getTimeoutSeconds() : (saved == null ? 30 : valueOrDefault(saved.getTimeoutSeconds(), 30));

        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        if (!StringUtils.hasText(baseUrl)) return modelFailure(result, "请先填写接口地址", start);
        if (!StringUtils.hasText(apiKey)) return modelFailure(result, "请先填写 API Key", start);
        validateBaseUrl(baseUrl);

        RestTemplate restTemplate = restTemplate(timeout);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    trimTrailingSlash(baseUrl) + "/models", HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            List<String> models = parseModelIds(response.getBody());
            result.put("success", !models.isEmpty());
            result.put("models", models);
            result.put("message", models.isEmpty() ? "接口未返回可用模型，请手动输入模型 ID" : "已获取 " + models.size() + " 个模型");
            result.put("httpStatus", response.getStatusCode().value());
            result.put("latencyMs", System.currentTimeMillis() - start);
            return result;
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            if (status == 404 || status == 405) {
                return modelFailure(result, "该接口不提供 /models 列表，请手动输入模型 ID", start, status);
            }
            return modelFailure(result, httpMessage(status), start, status);
        } catch (ResourceAccessException e) {
            return modelFailure(result, "无法连接接口，请检查地址或网络", start);
        } catch (Exception e) {
            log.warn("获取模型列表失败 type={} baseUrl={}: {}", normalized, baseUrl, e.getMessage());
            return modelFailure(result, "获取模型列表失败：" + e.getMessage(), start);
        }
    }

    private Map<String, Object> modelFailure(Map<String, Object> result, String message, long start) {
        return modelFailure(result, message, start, null);
    }

    private Map<String, Object> modelFailure(Map<String, Object> result, String message, long start,
                                             Integer httpStatus) {
        result.put("success", false);
        result.put("models", List.of());
        result.put("message", message);
        result.put("httpStatus", httpStatus);
        result.put("latencyMs", System.currentTimeMillis() - start);
        return result;
    }

    /**
     * 测试连接：先调用 OpenAI 兼容的 /models，失败时再发一次最小对话请求。
     * 不保存任何配置，可测试草稿。
     */
    public Map<String, Object> test(String type, AiModelConfigRequest draft) {
        String normalized = normalizeType(type);
        AiModelConfig saved = find(normalized);
        String provider = normalizeProvider(firstNonBlank(
                draft == null ? null : draft.getProvider(),
                saved == null ? METAS.get(normalized).provider() : saved.getProvider()));
        String modelName = trim(firstNonBlank(
                draft == null ? null : draft.getModelName(),
                saved == null ? METAS.get(normalized).modelName() : saved.getModelName()));
        String baseUrl = trim(firstNonBlank(
                draft == null ? null : draft.getBaseUrl(),
                saved == null ? METAS.get(normalized).baseUrl() : saved.getBaseUrl()));
        String apiKey = null;
        if (draft != null && StringUtils.hasText(draft.getApiKey())) {
            apiKey = draft.getApiKey().trim();
        } else if (saved != null) {
            apiKey = safeDecrypt(saved.getApiKey());
        }
        Integer timeout = draft != null && draft.getTimeoutSeconds() != null
                ? draft.getTimeoutSeconds() : (saved == null ? 30 : valueOrDefault(saved.getTimeoutSeconds(), 30));

        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        if (!StringUtils.hasText(baseUrl)) return testFailure(result, "请先填写接口地址", start);
        if (!StringUtils.hasText(modelName)) return testFailure(result, "请先填写模型名称", start);
        if (!StringUtils.hasText(apiKey)) return testFailure(result, "请先填写 API Key", start);
        validateBaseUrl(baseUrl);

        RestTemplate restTemplate = restTemplate(timeout);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    trimTrailingSlash(baseUrl) + "/models", HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            List<String> models = parseModelIds(response.getBody());
            boolean found = models.stream().anyMatch(id -> id.equalsIgnoreCase(modelName));
            result.put("success", true);
            result.put("modelFound", found);
            result.put("models", models.stream().limit(10).toList());
            result.put("message", found
                    ? "连接成功，模型 " + modelName + " 可用"
                    : "接口鉴权成功；模型列表未包含 " + modelName + "，仍可直接调用");
            result.put("httpStatus", response.getStatusCode().value());
            result.put("latencyMs", System.currentTimeMillis() - start);
            return result;
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            // 部分兼容网关不提供 /models，改用一次最小对话请求探测。
            if (status == 404 || status == 405) {
                return testChat(restTemplate, baseUrl, modelName, apiKey, result, start);
            }
            return testFailure(result, httpMessage(status), start, status);
        } catch (ResourceAccessException e) {
            return testFailure(result, "无法连接接口，请检查地址或网络", start);
        } catch (Exception e) {
            log.warn("测试 AI 模型连接失败 type={} model={}: {}", normalized, modelName, e.getMessage());
            return testFailure(result, "连接测试失败：" + e.getMessage(), start);
        }
    }

    public String unavailableReason(String type) {
        String normalized = normalizeType(type);
        ModelMeta meta = METAS.get(normalized);
        AiModelConfig config = find(normalized);
        if (config == null) {
            return meta.label() + "尚未配置，请前往「模型配置」设置";
        }
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            if (TYPE_REVIEW.equals(normalized) && environmentConfigured()) return null;
            return meta.label() + "未启用，请前往「模型配置」启用";
        }
        if (!StringUtils.hasText(safeDecrypt(config.getApiKey()))) {
            return meta.label() + "未配置 API Key，请在「模型配置」中补全";
        }
        return meta.label() + "配置不完整，请检查接口地址与模型名称";
    }

    public List<Map<String, Object>> providerPresets() {
        List<Map<String, Object>> presets = new ArrayList<>();
        for (ProviderPreset preset : PROVIDERS.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", preset.value());
            item.put("label", preset.label());
            item.put("baseUrl", preset.baseUrl());
            item.put("models", preset.models());
            presets.add(item);
        }
        return presets;
    }

    public static String providerLabel(String provider) {
        if (!StringUtils.hasText(provider)) return "未配置";
        ProviderPreset preset = PROVIDERS.get(provider.toLowerCase(Locale.ROOT));
        return preset == null ? provider : preset.label();
    }

    private AiModelConfigView toView(ModelMeta meta, AiModelConfig config) {
        String masked = null;
        boolean keyConfigured = false;
        if (config != null && StringUtils.hasText(config.getApiKey())) {
            try {
                masked = secretCipher.mask(secretCipher.decrypt(config.getApiKey()));
                keyConfigured = StringUtils.hasText(masked);
            } catch (BizException e) {
                masked = "密钥不可用，请重新录入";
                keyConfigured = false;
            }
        }
        boolean enabled = config != null && Boolean.TRUE.equals(config.getEnabled());
        return AiModelConfigView.builder()
                .type(meta.type())
                .label(meta.label())
                .description(meta.description())
                .provider(config == null ? meta.provider() : config.getProvider())
                .providerLabel(providerLabel(config == null ? meta.provider() : config.getProvider()))
                .modelName(config == null ? meta.modelName() : config.getModelName())
                .baseUrl(config == null ? meta.baseUrl() : config.getBaseUrl())
                .apiKeyMasked(masked)
                .apiKeyConfigured(keyConfigured)
                .temperature(config == null || config.getTemperature() == null
                        ? meta.temperature() : config.getTemperature())
                .timeoutSeconds(config == null || config.getTimeoutSeconds() == null
                        ? 30 : config.getTimeoutSeconds())
                .maxRetries(config == null || config.getMaxRetries() == null ? 2 : config.getMaxRetries())
                .enabled(enabled)
                .active(enabled && keyConfigured)
                .environmentConfigured(TYPE_REVIEW.equals(meta.type()) && environmentConfigured())
                .remark(config == null ? null : config.getRemark())
                .updateTime(config == null ? null : config.getUpdateTime())
                .updateByName(config == null ? null : config.getUpdateByName())
                .build();
    }

    private Map<String, Object> testChat(RestTemplate restTemplate, String baseUrl, String modelName,
                                         String apiKey, Map<String, Object> result, long start) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelName);
            body.put("messages", List.of(Map.of("role", "user", "content", "ping")));
            body.put("max_tokens", 1);
            body.put("stream", false);
            ResponseEntity<String> response = restTemplate.exchange(
                    trimTrailingSlash(baseUrl) + "/chat/completions", HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);
            result.put("success", true);
            result.put("modelFound", true);
            result.put("models", List.of());
            result.put("message", "连接成功，模型已响应");
            result.put("httpStatus", response.getStatusCode().value());
            result.put("latencyMs", System.currentTimeMillis() - start);
            return result;
        } catch (HttpStatusCodeException e) {
            return testFailure(result, httpMessage(e.getStatusCode().value()), start,
                    e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            return testFailure(result, "无法连接接口，请检查地址或网络", start);
        } catch (Exception e) {
            return testFailure(result, "连接测试失败：" + e.getMessage(), start);
        }
    }

    private Map<String, Object> testFailure(Map<String, Object> result, String message, long start) {
        return testFailure(result, message, start, null);
    }

    private Map<String, Object> testFailure(Map<String, Object> result, String message, long start,
                                            Integer httpStatus) {
        result.put("success", false);
        result.put("modelFound", false);
        result.put("models", List.of());
        result.put("message", message);
        result.put("httpStatus", httpStatus);
        result.put("latencyMs", System.currentTimeMillis() - start);
        return result;
    }

    private String httpMessage(int status) {
        if (status == 401 || status == 403) return "鉴权失败（HTTP " + status + "），请检查 API Key";
        if (status == 404) return "接口不存在（HTTP 404），请检查接口地址";
        if (status == 429) return "请求被限流（HTTP 429），请稍后重试";
        if (status >= 500) return "模型服务异常（HTTP " + status + "）";
        return "模型服务返回 HTTP " + status;
    }

    private List<String> parseModelIds(String body) {
        List<String> models = new ArrayList<>();
        if (!StringUtils.hasText(body)) return models;
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            if (data.isArray()) {
                data.forEach(node -> {
                    String id = node.path("id").asText(null);
                    if (StringUtils.hasText(id)) models.add(id);
                });
            } else if (root.isArray()) {
                root.forEach(node -> {
                    String id = node.path("id").asText(null);
                    if (StringUtils.hasText(id)) models.add(id);
                });
            }
        } catch (Exception e) {
            log.debug("解析模型列表失败: {}", e.getMessage());
        }
        return models;
    }

    private AiModelConfig find(String type) {
        return configMapper.selectOne(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getModelType, normalizeType(type)));
    }

    private boolean environmentConfigured() {
        return deepSeekProperties.isEnabled() && StringUtils.hasText(deepSeekProperties.getApiKey());
    }

    private String safeDecrypt(String cipherText) {
        try {
            return secretCipher.decrypt(cipherText);
        } catch (BizException e) {
            log.warn("模型密钥解密失败，需要管理员重新录入");
            return null;
        }
    }

    private RestTemplate restTemplate(Integer timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = Math.max(1, timeoutSeconds) * 1000;
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!METAS.containsKey(normalized)) {
            throw new BizException("未知的模型类型");
        }
        return normalized;
    }

    private String normalizeProvider(String provider) {
        return StringUtils.hasText(provider) ? provider.trim().toLowerCase(Locale.ROOT) : "custom";
    }

    private void validateBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) return;
        try {
            URI uri = URI.create(baseUrl.trim());
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new BizException("接口地址必须使用 http 或 https");
            }
        } catch (IllegalArgumentException e) {
            throw new BizException("接口地址格式不正确");
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String limit(String value, int length) {
        if (value == null) return null;
        return value.length() <= length ? value : value.substring(0, length);
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) return min;
        if (value.compareTo(min) < 0) return min;
        if (value.compareTo(max) > 0) return max;
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private int clamp(Integer value, int min, int max) {
        if (value == null) return min;
        return Math.max(min, Math.min(max, value));
    }

    private <T> T valueOrDefault(T value, T fallback) {
        return value == null ? fallback : value;
    }

    /** 运行时生效模型。 */
    public record ResolvedModel(String type, String provider, String modelName, String baseUrl,
                                String apiKey, double temperature, int timeoutSeconds,
                                int maxRetries, String source) {
    }

    private record ModelMeta(String type, String label, String description, String provider,
                             String modelName, String baseUrl, BigDecimal temperature) {
    }

    private record ProviderPreset(String value, String label, String baseUrl, List<String> models) {
    }
}
