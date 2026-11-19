package com.kumao.live.service;

import com.kumao.api.client.ReviewClient;
import com.kumao.api.client.UserClient;
import com.kumao.api.dto.ModerationRequestDTO;
import com.kumao.api.dto.ModerationResultDTO;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 直播弹幕实时审核：先检查禁言状态，再由 review-service 做 AI 内容审核。
 *
 * <p>审核结果只决定弹幕是否自动通过；PASS 且低风险才广播，其余结果保留并转管理员复核，
 * 不再自动禁言发送者。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveContentModerationService {

    private final SensitiveWordService sensitiveWordService;
    private final ReviewClient reviewClient;
    private final UserClient userClient;

    public ModerationResultDTO review(Long userId, Long roomId, String content) {
        ensureNotMuted(userId);
        if (sensitiveWordService.contains(content)) {
            return localResult("REJECT", 100, "弹幕命中敏感词，已转管理员复核",
                    List.of("敏感词命中"));
        }

        ModerationRequestDTO request = new ModerationRequestDTO();
        request.setCategory("DANMAKU");
        request.setItemId(String.valueOf(roomId));
        request.setContent(content);
        request.getMetadata().put("roomId", roomId);
        try {
            R<ModerationResultDTO> response = reviewClient.moderate(request);
            ModerationResultDTO result = response == null ? null : response.getData();
            if (result != null) {
                return result;
            }
            log.warn("弹幕 AI 审核返回空结果，转管理员复核 roomId={}", roomId);
        } catch (Exception e) {
            // 审核不可用时不能自动放行，保留弹幕并交给管理员复核。
            log.warn("弹幕 AI 审核不可用，转管理员复核 roomId={}: {}", roomId, e.getMessage());
        }
        return localResult("REVIEW", 50, "AI 审核服务暂不可用，已转管理员复核",
                List.of("审核服务不可用"));
    }

    private ModerationResultDTO localResult(String decision, int riskScore, String reason,
                                            List<String> signals) {
        ModerationResultDTO result = new ModerationResultDTO();
        result.setDecision(decision);
        result.setRiskScore(riskScore);
        result.setSource("LOCAL_RULE");
        result.setReasons(reason);
        result.setSignals(signals);
        return result;
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
}
