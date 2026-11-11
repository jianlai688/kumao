package com.kumao.review.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumao.review.dto.AiReviewRequest;
import com.kumao.review.dto.AiReviewResult;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 审核建议服务。
 *
 * <p>模型来自管理员在「AI 模型配置」中启用的审核模型；未配置时回退环境变量，
 * 仍不可用时使用可解释本地规则。任何结果都只是建议，不执行审核动作。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewService {

    private final AiModelConfigService modelConfigService;
    private final LocalReviewEngine localReviewEngine;
    private final ObjectMapper objectMapper;

    public AiReviewResult review(AiReviewRequest request) {
        long start = System.currentTimeMillis();
        AiModelConfigService.ResolvedModel config = modelConfigService.resolve(AiModelConfigService.TYPE_REVIEW);
        if (config == null) {
            String reason = modelConfigService.unavailableReason(AiModelConfigService.TYPE_REVIEW);
            return localReviewEngine.review(request,
                    StringUtils.hasText(reason) ? reason : "审核模型未配置，已降级本地规则");
        }

        String url = config.baseUrl() + "/chat/completions";
        String prompt = buildPrompt(request);
        String providerName = AiModelConfigService.providerLabel(config.provider());
        String lastError = null;
        int attempts = Math.max(1, config.maxRetries() + 1);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                Map<String, Object> body = buildRequestBody(config, prompt);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(config.apiKey());
                ResponseEntity<String> response = restTemplate(config.timeoutSeconds()).exchange(
                        url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
                AiReviewResult result = parseResponse(response.getBody(), start, config);
                if (result != null) return result;
                lastError = providerName + " 返回格式无法解析";
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                lastError = providerName + " HTTP " + status;
                log.warn("AI 审核模型调用失败 attempt={} status={} body={}", attempt, status,
                        truncate(e.getResponseBodyAsString(), 300));
                // 鉴权/参数错误重试也不会成功，直接降级；429/5xx 可以重试。
                if (status == 401 || status == 403 || status == 400) break;
            } catch (ResourceAccessException e) {
                lastError = providerName + " 请求超时或网络不可达";
                log.warn("AI 审核模型网络异常 attempt={}: {}", attempt, e.getMessage());
            } catch (Exception e) {
                lastError = providerName + " 调用异常：" + e.getMessage();
                log.warn("AI 审核模型调用异常 attempt={}", attempt, e);
            }
            if (attempt < attempts) {
                sleepQuietly(150L * attempt);
            }
        }
        return localReviewEngine.review(request,
                lastError == null ? "AI 模型不可用，已降级本地规则" : lastError + "，已降级本地规则");
    }

    private Map<String, Object> buildRequestBody(AiModelConfigService.ResolvedModel config, String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.modelName());
        body.put("temperature", config.temperature());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content", prompt)
        ));
        // OpenAI 兼容接口尽可能要求 JSON 对象；部分模型服务不支持时会自动忽略/报错并走本地规则。
        body.put("response_format", Map.of("type", "json_object"));
        return body;
    }

    private String systemPrompt() {
        return """
                你是酷猫音乐平台的审核辅助员。你的输出只是给管理员参考的建议，不能代替管理员做最终决定。
                请严格只返回一个 JSON 对象，不要 Markdown、不要输出 JSON 以外的文字：
                {"decision":"PASS|REVIEW|REJECT","riskScore":0-100,"reasons":"中文可解释理由","signals":["可复核信号"]}
                规则：
                1. PASS 表示未发现明显问题，仍由管理员确认；
                2. REVIEW 表示需要人工重点复核；
                3. REJECT 表示存在较高内容/资质风险，建议驳回；
                4. 不要臆造用户未提供的资质、版权或身份信息；
                5. 注意主播/音乐人/歌手资质、广告合规、歌曲/MV版权、评论/弹幕骚扰与导流、直播间安全；
                6. 不输出个人敏感信息。
                """;
    }

    private String buildPrompt(AiReviewRequest request) {
        String metadata = request.getMetadata() == null ? "{}" : request.getMetadata().toString();
        return "审核类型：" + request.getCategory() + "\n"
                + "对象ID：" + safe(request.getItemId()) + "\n"
                + "标题：" + safe(request.getTitle()) + "\n"
                + "内容：" + safe(request.getContent()) + "\n"
                + "附加信息（已脱敏）：" + LocalReviewEngine.maskSensitive(metadata);
    }

    private AiReviewResult parseResponse(String body, long start,
                                         AiModelConfigService.ResolvedModel config) {
        if (!StringUtils.hasText(body)) return null;
        String content = null;
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) return null;
            content = choices.get(0).path("message").path("content").asText(null);
            if (!StringUtils.hasText(content)) return null;
            String json = content.trim()
                    .replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "");
            int jsonStart = json.indexOf('{');
            int jsonEnd = json.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                json = json.substring(jsonStart, jsonEnd + 1);
            }
            JsonNode result = objectMapper.readTree(json);
            String decision = result.path("decision").asText("REVIEW").trim().toUpperCase();
            if (!List.of("PASS", "REVIEW", "REJECT").contains(decision)) decision = "REVIEW";
            int risk = Math.max(0, Math.min(100, result.path("riskScore").asInt(50)));
            String reasons = result.path("reasons").asText("");
            List<String> signals = new ArrayList<>();
            JsonNode signalNode = result.path("signals");
            if (signalNode.isArray()) {
                signalNode.forEach(node -> {
                    if (node.isTextual() && StringUtils.hasText(node.asText())) signals.add(node.asText());
                });
            }
            if (!StringUtils.hasText(reasons)) {
                reasons = signals.isEmpty() ? "模型未给出具体理由，请人工复核" : String.join("；", signals);
            }
            return AiReviewResult.builder()
                    .decision(decision)
                    .riskScore(risk)
                    .reasons(reasons)
                    .signals(signals)
                    .source("AI_MODEL")
                    .provider(config.provider())
                    .model(config.modelName())
                    .fallbackReason(null)
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception e) {
            log.warn("解析 AI 审核结果失败: {}", e.getMessage());
            // 部分 OpenAI 兼容模型会返回自然语言审核结论，尽量从文本里提取最终判定，避免完全丢失 AI 结果。
            return StringUtils.hasText(content) ? parseLooseResponse(content, start, config) : null;
        }
    }

    private AiReviewResult parseLooseResponse(String content, long start,
                                              AiModelConfigService.ResolvedModel config) {
        String text = content.trim();
        if (!StringUtils.hasText(text)) return null;
        String lower = text.toLowerCase();
        String compact = text.replaceAll("\\s+", "");

        // 优先读取明确的标题式结论，例如「审核结果：违规」。
        java.util.regex.Matcher verdictMatcher = java.util.regex.Pattern.compile(
                "(?:审核结果|判定结果|判定|结论|结果|decision|result)\\s*[:：]\\s*(违规|拒绝|不通过|正常|通过|合规|pass|reject|review)",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(compact);
        if (verdictMatcher.find()) {
            String verdict = verdictMatcher.group(1).toLowerCase();
            if (verdict.contains("违规") || verdict.contains("拒绝") || verdict.contains("不通过") || verdict.contains("reject")) {
                return looseResult("REJECT", 85, text, start, config);
            }
            if (verdict.contains("正常") || verdict.contains("通过") || verdict.contains("合规") || verdict.contains("pass")) {
                return looseResult("PASS", 15, text, start, config);
            }
        }

        boolean pass = lower.contains("未发现违规") || lower.contains("无明显违规")
                || lower.contains("内容正常") || lower.contains("整体正常")
                || (lower.contains("通过") && !lower.contains("不通过"))
                || lower.contains("pass");
        boolean reject = lower.contains("违规") || lower.contains("拒绝") || lower.contains("高风险")
                || lower.contains("不通过") || lower.contains("封禁") || lower.contains("禁言")
                || lower.contains("reject");
        String decision = pass ? "PASS" : reject ? "REJECT" : "REVIEW";
        int risk = "PASS".equals(decision) ? 15 : "REJECT".equals(decision) ? 85 : 55;
        return looseResult(decision, risk, text, start, config);
    }

    private AiReviewResult looseResult(String decision, int risk, String text, long start,
                                       AiModelConfigService.ResolvedModel config) {
        return AiReviewResult.builder()
                .decision(decision)
                .riskScore(risk)
                .reasons("模型返回文本审核结论：" + truncate(text, 240))
                .signals(new ArrayList<>())
                .source("AI_MODEL_TEXT")
                .provider(config.provider())
                .model(config.modelName())
                .fallbackReason(null)
                .latencyMs(System.currentTimeMillis() - start)
                .build();
    }

    private RestTemplate restTemplate(int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = Math.max(1, timeoutSeconds) * 1000;
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }

    private String safe(String value) {
        return value == null ? "" : LocalReviewEngine.maskSensitive(value);
    }

    private String truncate(String value, int length) {
        if (value == null) return "";
        return value.length() <= length ? value : value.substring(0, length);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
