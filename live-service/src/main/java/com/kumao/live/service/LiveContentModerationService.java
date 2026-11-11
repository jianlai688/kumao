package com.kumao.live.service;

import com.kumao.api.client.ReviewClient;
import com.kumao.api.client.UserClient;
import com.kumao.api.dto.ModerationRequestDTO;
import com.kumao.api.dto.ModerationResultDTO;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 直播弹幕实时审核：先检查禁言状态，再由 review-service 做 AI 内容审核。
 * 违规内容不广播、不持久化，并自动禁言发送者。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveContentModerationService {

    private final SensitiveWordService sensitiveWordService;
    private final ReviewClient reviewClient;
    private final UserClient userClient;

    @Value("${kumao.moderation.mute-minutes:60}")
    private int muteMinutes;
    @Value("${kumao.moderation.reject-risk-score:35}")
    private int rejectRiskScore;

    /** AI 审核服务不可用时，短时间内跳过远程调用，避免每条弹幕都被超时拖慢。 */
    private volatile long moderationSkipUntil = 0L;

    public void checkText(Long userId, Long roomId, String content) {
        ensureNotMuted(userId);
        if (sensitiveWordService.contains(content)) {
            muteUser(userId, "弹幕命中敏感词");
            throw new BizException(403, "弹幕包含敏感词，你已被禁言");
        }
        if (System.currentTimeMillis() < moderationSkipUntil) {
            return;
        }

        ModerationRequestDTO request = new ModerationRequestDTO();
        request.setCategory("DANMAKU");
        request.setItemId(String.valueOf(roomId));
        request.setContent(content);
        request.getMetadata().put("roomId", roomId);
        ModerationResultDTO result;
        try {
            R<ModerationResultDTO> response = reviewClient.moderate(request);
            result = response == null ? null : response.getData();
            moderationSkipUntil = 0L;
        } catch (Exception e) {
            // review-service 短暂不可用时放行，避免直播互动整体不可用。
            moderationSkipUntil = System.currentTimeMillis() + 30_000L;
            log.warn("弹幕 AI 审核不可用，暂按放行处理（30 秒内跳过远程审核）: {}", e.getMessage());
            return;
        }
        if (result == null) return;
        boolean violation = "REJECT".equalsIgnoreCase(result.getDecision())
                || result.getRiskScore() >= rejectRiskScore;
        if (!violation) return;
        muteUser(userId, result.getReasons());
        throw new BizException(403, "弹幕内容违规，你已被禁言");
    }

    private void ensureNotMuted(Long userId) {
        if (userId == null) return;
        try {
            R<Boolean> result = userClient.isMuted(userId);
            if (result != null && Boolean.TRUE.equals(result.getData())) {
                throw new BizException(403, "你已被禁言，暂时无法发送弹幕");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("禁言状态查询失败，暂按未禁言处理: {}", e.getMessage());
        }
    }

    private void muteUser(Long userId, String reason) {
        if (userId == null) return;
        try {
            userClient.mute(userId, muteMinutes);
            log.info("弹幕违规，已禁言用户 userId={} minutes={} reason={}", userId, muteMinutes, reason);
        } catch (Exception e) {
            log.warn("弹幕违规禁言失败 userId={}: {}", userId, e.getMessage());
        }
    }
}
