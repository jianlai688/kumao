package com.kumao.review.service;

import com.kumao.review.dto.AiReviewRequest;
import com.kumao.review.dto.AiReviewResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalReviewEngineTest {

    private final LocalReviewEngine engine = new LocalReviewEngine();

    @Test
    void shouldRejectHighRiskCommentWithExplainableSignals() {
        AiReviewRequest request = request("COMMENT", "评论 #1",
                "加微信刷单，保证返利，点击 http://spam.example.com");
        AiReviewResult result = engine.review(request, "test");

        assertEquals("REJECT", result.getDecision());
        assertTrue(result.getRiskScore() >= 70);
        assertTrue(result.getSignals().stream().anyMatch(s -> s.contains("刷单") || s.contains("站外导流")));
        assertEquals("LOCAL_RULE", result.getSource());
    }

    @Test
    void shouldPassCleanApplicationButKeepManualReviewReminder() {
        AiReviewRequest request = request("MUSICIAN", "独立音乐人申请",
                "独立民谣创作者，发布过两张原创专辑，希望加入平台。");
        request.setMetadata(Map.of(
                "hasIdCardNo", true,
                "hasIdCardFront", true,
                "hasIdCardBack", true));
        AiReviewResult result = engine.review(request, "test");

        assertEquals("PASS", result.getDecision());
        assertTrue(result.getReasons().contains("人工确认"));
    }

    @Test
    void shouldRequireBothSidesOfIdCardForCertification() {
        AiReviewRequest request = request("MUSICIAN", "独立音乐人申请",
                "独立民谣创作者，发布过两张原创专辑，希望加入平台。");
        request.setMetadata(Map.of(
                "hasIdCardNo", true,
                "hasIdCardFront", true,
                "hasIdCardBack", false));
        AiReviewResult result = engine.review(request, "test");

        assertEquals("REVIEW", result.getDecision());
        assertTrue(result.getSignals().stream().anyMatch(s -> s.contains("身份证") || s.contains("证件")));
    }

    @Test
    void shouldFlagMissingMediaForSong() {
        AiReviewRequest request = request("SONG", "未命名歌曲", "原创歌曲，暂无音频");
        request.setMetadata(Map.of());
        AiReviewResult result = engine.review(request, "test");

        assertTrue(result.getRiskScore() >= 35);
        assertTrue(result.getSignals().stream().anyMatch(s -> s.contains("音频") || s.contains("封面")));
    }

    @Test
    void shouldMaskIdCardAndPhoneBeforeExternalModel() {
        String masked = LocalReviewEngine.maskSensitive("身份证 110101199001011234 手机 13800138000");
        assertTrue(masked.contains("******************"));
        assertTrue(masked.contains("***********"));
        assertTrue(!masked.contains("110101199001011234"));
        assertTrue(!masked.contains("13800138000"));
    }

    private AiReviewRequest request(String category, String title, String content) {
        AiReviewRequest request = new AiReviewRequest();
        request.setCategory(category);
        request.setItemId("1");
        request.setTitle(title);
        request.setContent(content);
        return request;
    }
}
