package com.kumao.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.common.security.JwtUtils;
import com.kumao.review.config.AutoReviewProperties;
import com.kumao.review.dto.AiReviewRequest;
import com.kumao.review.dto.AiReviewResult;
import com.kumao.review.entity.AiReviewTask;
import com.kumao.review.mapper.AiReviewTaskMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 自动审核调度器。
 *
 * <p>定时通过网关读取资质、广告、歌曲/MV、歌手等业务待审队列，调用审核模型生成建议：
 * AI 返回 PASS、风险低于阈值且分类允许时自动落业务审核状态；其余情况写入
 * {@code ai_review_task}，由管理员在工作台复核。</p>
 *
 * <p>评论与直播弹幕在用户提交时由业务服务同步审核，PASS 直接发布，
 * REVIEW / REJECT 保留为待管理员复核，不进入本定时任务。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoReviewService {

    private static final List<CategorySpec> CATEGORIES = List.of(
            new CategorySpec("ANCHOR_APPLY", "/api/users/admin/anchor-applies",
                    Map.of("status", "PENDING"), HttpMethod.POST,
                    "/api/users/admin/anchor-applies/{id}/audit", true),
            new CategorySpec("MUSICIAN", "/api/musician/musician/admin/page",
                    Map.of("status", "PENDING"), HttpMethod.POST,
                    "/api/musician/musician/admin/{id}/audit", true),
            new CategorySpec("ADVERTISER", "/api/ad/ad/admin/page",
                    Map.of("status", "PENDING"), HttpMethod.POST,
                    "/api/ad/ad/admin/{id}/audit", true),
            new CategorySpec("AD_MATERIAL", "/api/ad/ad/admin/materials",
                    Map.of("status", "PENDING"), HttpMethod.POST,
                    "/api/ad/ad/admin/materials/{id}/audit", true),
            new CategorySpec("AD_PLAN", "/api/ad/ad/admin/plans",
                    Map.of("status", "PENDING"), HttpMethod.POST,
                    "/api/ad/ad/admin/plans/{id}/audit", true),
            new CategorySpec("SONG", "/api/music/songs",
                    Map.of("status", "0"), HttpMethod.PUT,
                    "/api/music/songs/admin/{id}/status", false),
            new CategorySpec("MV", "/api/music/mv/admin/page",
                    Map.of("auditStatus", "PENDING"), HttpMethod.POST,
                    "/api/music/mv/admin/{id}/audit", true),
            new CategorySpec("SINGER", "/api/music/singers/admin/page",
                    Map.of("status", "0"), HttpMethod.POST,
                    "/api/music/singers/admin/{id}/audit", true)
    );

    private final AutoReviewProperties properties;
    private final AiModelConfigService modelConfigService;
    private final AiReviewService aiReviewService;
    private final AiReviewTaskMapper taskMapper;
    private final ReviewLogService reviewLogService;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "ai-auto-review");
        thread.setDaemon(true);
        return thread;
    });

    private volatile LocalDateTime lastRunAt;
    private volatile Map<String, Object> lastSummary = Map.of("state", "NEVER_RUN");

    @Scheduled(fixedDelayString = "${kumao.ai.auto-review.interval-ms:60000}", initialDelayString = "15000")
    public void scheduledRun() {
        runOnce();
    }

    /** 手动触发，异步执行，接口立即返回。 */
    public Map<String, Object> trigger() {
        if (!properties.isEnabled()) {
            throw new BizException("AI 自动审核未开启");
        }
        if (running.get()) {
            return Map.of("queued", false, "running", true, "message", "自动审核正在执行");
        }
        executor.submit(this::runOnce);
        return Map.of("queued", true, "running", false, "message", "已提交自动审核任务");
    }

    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", properties.isEnabled());
        result.put("modelConfigured", modelConfigService.resolve(AiModelConfigService.TYPE_REVIEW) != null);
        result.put("running", running.get());
        result.put("intervalMs", properties.getIntervalMs());
        result.put("passRiskThreshold", properties.getPassRiskThreshold());
        result.put("autoApproveCategories", properties.getAutoApproveCategories());
        result.put("autoApproved", count("AUTO_APPROVED"));
        result.put("pendingAdmin", count("PENDING_ADMIN"));
        result.put("approvalFailed", count("APPROVAL_FAILED"));
        result.put("lastRunAt", lastRunAt);
        result.put("lastSummary", lastSummary);
        return result;
    }

    public PageResult<AiReviewTask> tasks(PageQuery query, String status, String category) {
        Page<AiReviewTask> page = taskMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<AiReviewTask>()
                        .eq(StringUtils.hasText(status), AiReviewTask::getStatus, status)
                        .eq(StringUtils.hasText(category), AiReviewTask::getCategory, category)
                        .orderByDesc(AiReviewTask::getCreateTime));
        return PageResult.of(page);
    }

    /** 管理员确认后把任务标记为已处理，避免同一待审项目被反复自动分析。 */
    public int resolve(String category, String itemId) {
        AiReviewTask task = find(category, itemId);
        if (task == null) return 0;
        task.setStatus("RESOLVED");
        task.setApprovalError(null);
        return taskMapper.updateById(task);
    }

    public Map<String, Object> summary() {
        return lastSummary;
    }

    public void runOnce() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        long start = System.currentTimeMillis();
        int processed = 0;
        int autoApproved = 0;
        int pendingAdmin = 0;
        int failed = 0;
        String state = "DONE";
        String message = null;
        try {
            if (!properties.isEnabled()) {
                state = "DISABLED";
                return;
            }
            if (modelConfigService.resolve(AiModelConfigService.TYPE_REVIEW) == null) {
                state = "MODEL_NOT_CONFIGURED";
                message = "审核模型未配置，跳过自动审核";
                return;
            }
            String token = jwtUtils.createToken(0L, "ai-auto-review", "ADMIN", "ADMIN");
            for (CategorySpec spec : CATEGORIES) {
                if (processed >= properties.getMaxItemsPerRun()) break;
                List<Map<String, Object>> items;
                try {
                    items = fetchPending(spec, token);
                } catch (Exception e) {
                    log.warn("自动审核拉取待审列表失败 category={}: {}", spec.type, e.getMessage());
                    continue;
                }
                for (Map<String, Object> raw : items) {
                    if (processed >= properties.getMaxItemsPerRun()) break;
                    String itemId = text(raw.get("id"));
                    if (!StringUtils.hasText(itemId)) continue;
                    if (spec.type.equals("AD_PLAN") && !"PAID".equals(text(raw.get("paymentStatus")))) continue;
                    AiReviewTask existing = find(spec.type, itemId);
                    if (existing != null) {
                        if ("PENDING_ADMIN".equals(existing.getStatus())
                                || "AUTO_APPROVED".equals(existing.getStatus())
                                || "RESOLVED".equals(existing.getStatus())) continue;
                        if ("APPROVAL_FAILED".equals(existing.getStatus())
                                && valueOrDefault(existing.getAttempts(), 0) >= 3) continue;
                    }
                    processed++;
                    try {
                        AiReviewRequest reviewRequest = toRequest(spec, raw);
                        AiReviewResult ai = aiReviewService.review(reviewRequest);
                        boolean autoApprove = isAutoApprove(spec.type)
                                && (!isIdentityCategory(spec.type) || identityComplete(raw))
                                && "AI_MODEL".equals(ai.getSource())
                                && "PASS".equals(ai.getDecision())
                                && ai.getRiskScore() < properties.getPassRiskThreshold();
                        if (autoApprove) {
                            try {
                                approve(spec, itemId, ai, token);
                                saveTask(existing, spec, raw, reviewRequest, ai, "AUTO_APPROVED", null);
                                reviewLogService.recordAuto(spec.type(), itemId, reviewRequest.getTitle(), ai);
                                autoApproved++;
                            } catch (Exception e) {
                                saveTask(existing, spec, raw, reviewRequest, ai, "APPROVAL_FAILED", e.getMessage());
                                failed++;
                                log.warn("自动审核通过落业务状态失败 category={} id={}: {}",
                                        spec.type, itemId, e.getMessage());
                            }
                        } else {
                            saveTask(existing, spec, raw, reviewRequest, ai, "PENDING_ADMIN", null);
                            pendingAdmin++;
                        }
                    } catch (Exception e) {
                        failed++;
                        log.warn("自动审核处理失败 category={} id={}: {}", spec.type, itemId, e.getMessage());
                    }
                    sleepQuietly(180L);
                }
            }
        } catch (Exception e) {
            state = "ERROR";
            message = e.getMessage();
            log.error("AI 自动审核执行失败", e);
        } finally {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("state", state);
            summary.put("processed", processed);
            summary.put("autoApproved", autoApproved);
            summary.put("pendingAdmin", pendingAdmin);
            summary.put("failed", failed);
            summary.put("elapsedMs", System.currentTimeMillis() - start);
            if (message != null) summary.put("message", message);
            lastSummary = summary;
            lastRunAt = LocalDateTime.now();
            running.set(false);
            log.info("AI 自动审核完成: {}", summary);
        }
    }

    private List<Map<String, Object>> fetchPending(CategorySpec spec, String token) throws Exception {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getGatewayUrl())
                .path(spec.listPath())
                .queryParam("pageNum", 1)
                .queryParam("pageSize", Math.max(1, properties.getPageSize()));
        spec.listQuery().forEach(builder::queryParam);
        ResponseEntity<String> response = restTemplate().exchange(
                builder.build().toUriString(), HttpMethod.GET, new HttpEntity<>(headers(token)), String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        if (root.path("code").asInt(500) != 200) {
            throw new BizException("待审列表返回失败：" + root.path("message").asText(""));
        }
        JsonNode data = root.path("data");
        JsonNode records = data.isArray() ? data : data.path("records");
        List<Map<String, Object>> result = new ArrayList<>();
        if (records.isArray()) {
            for (JsonNode node : records) {
                result.add(objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {
                }));
            }
        }
        return result;
    }

    private void approve(CategorySpec spec, String itemId, AiReviewResult ai, String token) {
        // 评论/弹幕没有“通过”状态动作，AI 通过即保持可见，仅记录审计日志。
        if (spec.approveMethod() == null) return;
        String reason = "AI 自动审核通过（"
                + (StringUtils.hasText(ai.getModel()) ? ai.getModel() : "AI")
                + "，风险 " + ai.getRiskScore() + "%）";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getGatewayUrl())
                .path(spec.approvePath().replace("{id}", itemId));
        if ("SONG".equals(spec.type())) {
            builder.queryParam("status", 1);
        } else {
            builder.queryParam("approved", true);
            if (spec.sendReason()) builder.queryParam("reason", reason);
        }
        try {
            ResponseEntity<String> response = restTemplate().exchange(
                    builder.build().toUriString(), spec.approveMethod(),
                    new HttpEntity<>(headers(token)), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.path("code").asInt(500) != 200) {
                throw new BizException(root.path("message").asText("业务审核接口返回失败"));
            }
        } catch (HttpStatusCodeException e) {
            throw new BizException("业务审核接口 HTTP " + e.getStatusCode().value()
                    + "：" + truncate(e.getResponseBodyAsString(), 200));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("业务审核接口调用失败：" + e.getMessage());
        }
    }

    private AiReviewRequest toRequest(CategorySpec spec, Map<String, Object> raw) {
        String title;
        String content;
        Map<String, Object> metadata = new LinkedHashMap<>();
        switch (spec.type()) {
            case "ANCHOR_APPLY" -> {
                title = firstText(raw.get("realName"), "主播申请 #" + text(raw.get("userId")));
                content = firstText(raw.get("intro"), "");
                metadata.put("userId", raw.get("userId"));
                metadata.put("liveCategory", raw.get("liveCategory"));
                metadata.put("status", raw.get("status"));
                putIdentityFlags(metadata, raw);
            }
            case "MUSICIAN" -> {
                title = firstText(raw.get("stageName"), firstText(raw.get("realName"), "音乐人"));
                content = firstText(raw.get("intro"), firstText(raw.get("representative"), ""));
                metadata.put("style", raw.get("style"));
                metadata.put("status", raw.get("status"));
                putIdentityFlags(metadata, raw);
            }
            case "ADVERTISER" -> {
                title = firstText(raw.get("companyName"), "广告制作人");
                content = firstText(raw.get("intro"), firstText(raw.get("representative"), ""));
                metadata.put("adType", raw.get("adType"));
                metadata.put("status", raw.get("status"));
                putIdentityFlags(metadata, raw);
            }
            case "AD_MATERIAL" -> {
                title = firstText(raw.get("name"), "广告素材");
                content = firstText(raw.get("content"), firstText(raw.get("coverUrl"), text(raw.get("fileUrl"))));
                metadata.put("type", raw.get("type"));
                metadata.put("coverUrl", raw.get("coverUrl"));
                metadata.put("mediaUrl", raw.get("fileUrl"));
                metadata.put("status", raw.get("status"));
            }
            case "AD_PLAN" -> {
                title = firstText(raw.get("materialName"), "投放计划");
                content = "版位：" + text(raw.get("slot")) + "，天数：" + text(raw.get("days"))
                        + "，金额：" + text(raw.get("amount")) + "，支付状态：" + text(raw.get("paymentStatus"));
                metadata.put("slot", raw.get("slot"));
                metadata.put("paymentStatus", raw.get("paymentStatus"));
                metadata.put("status", raw.get("status"));
            }
            case "SONG" -> {
                title = firstText(raw.get("name"), "歌曲");
                content = firstText(raw.get("singerName"), "") + " " + firstText(raw.get("style"), "");
                metadata.put("audioUrl", raw.get("audioUrl"));
                metadata.put("coverUrl", raw.get("coverUrl"));
                metadata.put("status", raw.get("status"));
            }
            case "MV" -> {
                title = firstText(raw.get("name"), "MV");
                content = firstText(raw.get("description"), "");
                metadata.put("mediaUrl", raw.get("videoUrl"));
                metadata.put("coverUrl", raw.get("coverUrl"));
                metadata.put("auditStatus", raw.get("auditStatus"));
            }
            case "SINGER" -> {
                title = firstText(raw.get("name"), "歌手认证");
                content = firstText(raw.get("intro"), "");
                metadata.put("region", raw.get("region"));
                metadata.put("status", raw.get("status"));
            }
            default -> {
                title = text(raw.get("title"));
                content = text(raw.get("content"));
            }
        }
        AiReviewRequest request = new AiReviewRequest();
        request.setCategory(spec.type());
        request.setItemId(text(raw.get("id")));
        request.setTitle(title);
        request.setContent(content);
        request.setMetadata(metadata);
        return request;
    }

    private void saveTask(AiReviewTask existing, CategorySpec spec, Map<String, Object> raw,
                          AiReviewRequest reviewRequest, AiReviewResult ai, String status, String error) {
        AiReviewTask task = existing == null ? new AiReviewTask() : existing;
        task.setCategory(spec.type());
        task.setItemId(text(raw.get("id")));
        task.setItemTitle(limit(reviewRequest.getTitle(), 300));
        task.setItemContent(limit(reviewRequest.getContent(), 1000));
        task.setDecision(ai.getDecision());
        task.setRiskScore(ai.getRiskScore());
        task.setReasons(limit(ai.getReasons(), 2000));
        task.setSignals(writeSignals(ai.getSignals()));
        task.setSource(ai.getSource());
        task.setProvider(ai.getProvider());
        task.setModel(ai.getModel());
        task.setStatus(status);
        task.setApprovalError(limit(error, 500));
        task.setAttempts(valueOrDefault(task.getAttempts(), 0) + 1);
        if (existing == null) {
            taskMapper.insert(task);
        } else {
            taskMapper.updateById(task);
        }
    }

    private void putIdentityFlags(Map<String, Object> metadata, Map<String, Object> raw) {
        metadata.put("hasIdCardNo", StringUtils.hasText(text(raw.get("idCard"))));
        metadata.put("hasIdCardFront", StringUtils.hasText(text(raw.get("idCardFrontUrl"))));
        metadata.put("hasIdCardBack", StringUtils.hasText(text(raw.get("idCardBackUrl"))));
    }

    private boolean isIdentityCategory(String category) {
        return "ANCHOR_APPLY".equals(category) || "MUSICIAN".equals(category) || "ADVERTISER".equals(category);
    }

    private boolean identityComplete(Map<String, Object> raw) {
        return StringUtils.hasText(text(raw.get("idCard")))
                && StringUtils.hasText(text(raw.get("idCardFrontUrl")))
                && StringUtils.hasText(text(raw.get("idCardBackUrl")));
    }

    private AiReviewTask find(String category, String itemId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<AiReviewTask>()
                .eq(AiReviewTask::getCategory, category)
                .eq(AiReviewTask::getItemId, itemId));
    }

    private long count(String status) {
        return taskMapper.selectCount(new LambdaQueryWrapper<AiReviewTask>()
                .eq(AiReviewTask::getStatus, status));
    }

    private boolean isAutoApprove(String category) {
        return properties.getAutoApproveCategories().stream()
                .anyMatch(item -> item != null && category.equalsIgnoreCase(item.trim()));
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return new RestTemplate(factory);
    }

    private String writeSignals(List<String> signals) {
        try {
            return objectMapper.writeValueAsString(signals == null ? List.of() : signals);
        } catch (Exception e) {
            return "[]";
        }
    }

    private int valueOrDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String text(Object value) {
        if (value == null) return "";
        if (value instanceof Number number) return String.valueOf(number.longValue());
        return String.valueOf(value);
    }

    private String firstText(Object value, String fallback) {
        String text = text(value);
        return StringUtils.hasText(text) ? text : fallback;
    }

    private String limit(String value, int length) {
        if (value == null) return null;
        return value.length() <= length ? value : value.substring(0, length);
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

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private record CategorySpec(String type, String listPath, Map<String, String> listQuery,
                                HttpMethod approveMethod, String approvePath, boolean sendReason) {
    }
}
