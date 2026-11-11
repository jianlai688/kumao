package com.kumao.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.common.security.LoginUser;
import com.kumao.common.security.UserContext;
import com.kumao.review.dto.ReviewLogRequest;
import com.kumao.review.entity.ReviewLog;
import com.kumao.review.mapper.ReviewLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReviewLogService {

    private static final Set<String> CATEGORIES = Set.of(
            "ANCHOR_APPLY", "MUSICIAN", "ADVERTISER", "AD_MATERIAL", "AD_PLAN",
            "SONG", "MV", "COMMENT", "LIVE_ROOM", "DANMAKU", "SINGER");

    private final ReviewLogMapper logMapper;

    public Long record(ReviewLogRequest request) {
        if (!CATEGORIES.contains(request.getCategory())) {
            throw new BizException("未知的审核类型");
        }
        LoginUser admin = UserContext.get();
        if (admin == null) throw new BizException(401, "请先登录");
        ReviewLog log = new ReviewLog();
        log.setCategory(request.getCategory());
        log.setItemId(limit(request.getItemId(), 64));
        log.setItemTitle(limit(request.getItemTitle(), 300));
        log.setDecision(request.getDecision());
        log.setAdminReason(limit(request.getAdminReason(), 1000));
        log.setAiDecision(limit(request.getAiDecision(), 20));
        log.setAiRiskScore(request.getAiRiskScore());
        log.setAiSource(limit(request.getAiSource(), 30));
        log.setAiReasons(limit(request.getAiReasons(), 2000));
        log.setAdminId(admin.getUserId());
        log.setAdminName(limit(admin.getUsername(), 100));
        logMapper.insert(log);
        return log.getId();
    }

    /** AI 自动通过时写入的审计日志，不依赖管理员登录态。 */
    public Long recordAuto(String category, String itemId, String itemTitle,
                           com.kumao.review.dto.AiReviewResult ai) {
        if (!CATEGORIES.contains(category)) {
            throw new BizException("未知的审核类型");
        }
        ReviewLog log = new ReviewLog();
        log.setCategory(category);
        log.setItemId(limit(itemId, 64));
        log.setItemTitle(limit(itemTitle, 300));
        log.setDecision("APPROVED");
        log.setAdminReason("AI 自动审核通过");
        log.setAiDecision(limit(ai.getDecision(), 20));
        log.setAiRiskScore(ai.getRiskScore());
        log.setAiSource(limit(ai.getSource(), 30));
        log.setAiReasons(limit(ai.getReasons(), 2000));
        log.setAdminId(0L);
        log.setAdminName("AI 自动审核");
        logMapper.insert(log);
        return log.getId();
    }

    public PageResult<ReviewLog> page(PageQuery query, String category, String decision) {
        Page<ReviewLog> page = logMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<ReviewLog>()
                        .eq(StringUtils.hasText(category), ReviewLog::getCategory, category)
                        .eq(StringUtils.hasText(decision), ReviewLog::getDecision, decision)
                        .like(StringUtils.hasText(query.getKeyword()), ReviewLog::getItemTitle, query.getKeyword())
                        .orderByDesc(ReviewLog::getCreateTime));
        return PageResult.of(page);
    }

    public Map<String, Object> statistics() {
        List<ReviewLog> logs = logMapper.selectList(new LambdaQueryWrapper<ReviewLog>()
                .orderByDesc(ReviewLog::getCreateTime));
        long approved = logs.stream().filter(l -> "APPROVED".equals(l.getDecision())).count();
        long rejected = logs.stream().filter(l -> "REJECTED".equals(l.getDecision())).count();
        LocalDate today = LocalDate.now();
        long todayCount = logs.stream()
                .filter(l -> l.getCreateTime() != null && l.getCreateTime().toLocalDate().equals(today))
                .count();

        Map<String, Map<String, Long>> byCategory = new LinkedHashMap<>();
        for (String category : CATEGORIES) {
            Map<String, Long> counts = new LinkedHashMap<>();
            counts.put("total", 0L);
            counts.put("approved", 0L);
            counts.put("rejected", 0L);
            byCategory.put(category, counts);
        }
        Map<String, Long> bySource = new LinkedHashMap<>();
        for (ReviewLog log : logs) {
            Map<String, Long> counts = byCategory.get(log.getCategory());
            if (counts != null) {
                counts.put("total", counts.get("total") + 1);
                if ("APPROVED".equals(log.getDecision())) counts.put("approved", counts.get("approved") + 1);
                if ("REJECTED".equals(log.getDecision())) counts.put("rejected", counts.get("rejected") + 1);
            }
            String source = StringUtils.hasText(log.getAiSource()) ? log.getAiSource() : "UNKNOWN";
            bySource.put(source, bySource.getOrDefault(source, 0L) + 1);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", logs.size());
        result.put("approved", approved);
        result.put("rejected", rejected);
        result.put("today", todayCount);
        result.put("byCategory", byCategory);
        result.put("bySource", bySource);
        result.put("recent", logs.stream().limit(8).toList());
        return result;
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
