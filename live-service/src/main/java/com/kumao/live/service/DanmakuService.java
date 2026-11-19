package com.kumao.live.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.api.dto.ModerationResultDTO;
import com.kumao.common.constant.ModerationStatus;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.live.dto.DanmakuMessage;
import com.kumao.live.dto.DanmakuSendResult;
import com.kumao.live.entity.LiveDanmaku;
import com.kumao.live.mapper.LiveDanmakuMapper;
import com.kumao.live.websocket.LiveWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DanmakuService {

    private final LiveDanmakuMapper danmakuMapper;
    private final LiveContentModerationService moderationService;
    private final RabbitTemplate rabbitTemplate;
    private final LiveWebSocketHandler webSocketHandler;

    @Value("${kumao.mq.danmaku-exchange:kumao.live.exchange}")
    private String exchange;
    @Value("${kumao.mq.danmaku-routing-key:live.danmaku}")
    private String routingKey;
    /** AI 风险分达到该值即不自动通过，转管理员复核。 */
    @Value("${kumao.moderation.reject-risk-score:35}")
    private int rejectRiskScore = 35;

    /**
     * 文本弹幕先审核：PASS 且低风险立即广播并异步落库；其余仅落库，交管理员复核。
     * 礼物、系统等非文本消息保持原有广播与持久化行为。
     */
    public DanmakuSendResult broadcastAndPersist(DanmakuMessage message) {
        if (message.getTimestamp() == null) {
            message.setTimestamp(System.currentTimeMillis());
        }
        if (message.getRoomId() == null) {
            throw new BizException("直播间ID不能为空");
        }

        ModerationResultDTO moderation = null;
        if ("TEXT".equals(message.getType())) {
            if (message.getContent() == null || message.getContent().isBlank()) {
                throw new BizException("弹幕内容不能为空");
            }
            if (message.getContent().length() > 100) {
                throw new BizException("弹幕内容不能超过100字");
            }
            moderation = moderationService.review(
                    message.getUserId(), message.getRoomId(), message.getContent());
            if (!shouldAutoApprove(moderation)) {
                persist(message, ModerationStatus.PENDING, moderation);
                log.info("弹幕转管理员复核 roomId={} userId={} decision={} risk={}",
                        message.getRoomId(), message.getUserId(),
                        moderation == null ? null : moderation.getDecision(),
                        moderation == null ? null : moderation.getRiskScore());
                return sendResult(ModerationStatus.PENDING, false,
                        "弹幕已提交管理员复核，通过后展示", moderation);
            }
        }

        webSocketHandler.broadcast(message);
        if (!"SYSTEM".equals(message.getType())) {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, message);
            } catch (Exception e) {
                log.warn("弹幕投递 MQ 失败，降级同步落库: {}", e.getMessage());
                persist(message, ModerationStatus.APPROVED, moderation);
            }
        }
        return sendResult(ModerationStatus.APPROVED, true, "弹幕已发送", moderation);
    }

    public void persist(DanmakuMessage message) {
        persist(message, ModerationStatus.APPROVED, null);
    }

    public void persist(DanmakuMessage message, Integer status, ModerationResultDTO moderation) {
        LiveDanmaku danmaku = new LiveDanmaku();
        danmaku.setRoomId(message.getRoomId());
        danmaku.setUserId(message.getUserId());
        danmaku.setNickname(message.getNickname());
        danmaku.setContent(message.getContent());
        danmaku.setColor(message.getColor());
        danmaku.setVip(Boolean.TRUE.equals(message.getVip()));
        danmaku.setType(message.getType());
        danmaku.setStatus(status);
        if (moderation != null) {
            danmaku.setAiDecision(limit(moderation.getDecision(), 20));
            danmaku.setAiRiskScore(moderation.getRiskScore());
            danmaku.setAiSource(limit(moderation.getSource(), 30));
            danmaku.setAiReasons(limit(moderation.getReasons(), 1000));
        }
        danmakuMapper.insert(danmaku);
    }

    /**
     * 定时复检「已通过但还没有 AI 记录」的弹幕：AI 判定 REJECT 的直接封禁（status=1）。
     */
    @Scheduled(fixedDelayString = "${kumao.live.post-review.interval-ms:180000}", initialDelayString = "60000")
    public void scheduledPostReview() {
        try {
            Map<String, Object> summary = rescanApproved(30);
            if (((Number) summary.get("scanned")).intValue() > 0) {
                log.info("[弹幕AI复检] {}", summary);
            }
        } catch (Exception e) {
            log.warn("[弹幕AI复检] 执行失败: {}", e.getMessage());
        }
    }

    public Map<String, Object> rescanApproved(int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        List<LiveDanmaku> list = danmakuMapper.selectList(new LambdaQueryWrapper<LiveDanmaku>()
                .eq(LiveDanmaku::getStatus, ModerationStatus.APPROVED)
                .eq(LiveDanmaku::getType, "TEXT")
                .isNull(LiveDanmaku::getAiDecision)
                .orderByDesc(LiveDanmaku::getCreateTime)
                .last("limit " + size));
        int blocked = 0;
        int passed = 0;
        int review = 0;
        for (LiveDanmaku danmaku : list) {
            ModerationResultDTO result = moderationService.review(
                    danmaku.getUserId(), danmaku.getRoomId(), danmaku.getContent());
            if (result != null) {
                danmaku.setAiDecision(limit(result.getDecision(), 20));
                danmaku.setAiRiskScore(result.getRiskScore());
                danmaku.setAiSource(limit(result.getSource(), 30));
                danmaku.setAiReasons(limit(result.getReasons(), 1000));
            }
            String decision = result == null ? "" : result.getDecision();
            if ("REJECT".equalsIgnoreCase(decision)) {
                danmaku.setStatus(ModerationStatus.REJECTED);
                blocked++;
            } else if ("PASS".equalsIgnoreCase(decision)) {
                passed++;
            } else {
                review++;
            }
            danmakuMapper.updateById(danmaku);
        }
        return Map.of("scanned", list.size(), "blocked", blocked, "passed", passed, "review", review);
    }

    public PageResult<LiveDanmaku> history(Long roomId, PageQuery query) {
        Page<LiveDanmaku> page = danmakuMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<LiveDanmaku>()
                        .eq(LiveDanmaku::getRoomId, roomId)
                        .eq(LiveDanmaku::getStatus, ModerationStatus.APPROVED)
                        .orderByDesc(LiveDanmaku::getCreateTime));
        return PageResult.of(page);
    }

    public PageResult<LiveDanmaku> adminPage(PageQuery query, Long roomId, Integer status) {
        Page<LiveDanmaku> page = danmakuMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<LiveDanmaku>()
                        .eq(roomId != null, LiveDanmaku::getRoomId, roomId)
                        .eq(status != null, LiveDanmaku::getStatus, status)
                        .orderByDesc(LiveDanmaku::getCreateTime));
        return PageResult.of(page);
    }

    public void updateStatus(Long id, Integer status) {
        if (!ModerationStatus.isValid(status)) {
            throw new BizException("无效的弹幕审核状态");
        }
        LiveDanmaku danmaku = danmakuMapper.selectById(id);
        if (danmaku == null) {
            throw new BizException("弹幕不存在");
        }
        danmaku.setStatus(status);
        danmakuMapper.updateById(danmaku);
    }

    public void adminDelete(Long id) {
        danmakuMapper.deleteById(id);
    }

    private boolean shouldAutoApprove(ModerationResultDTO result) {
        return result != null
                && "PASS".equalsIgnoreCase(result.getDecision())
                && result.getRiskScore() < rejectRiskScore;
    }

    private DanmakuSendResult sendResult(Integer status, boolean published, String message,
                                         ModerationResultDTO moderation) {
        return new DanmakuSendResult(
                status,
                published,
                message,
                moderation == null ? null : moderation.getDecision(),
                moderation == null ? null : moderation.getRiskScore());
    }

    private String limit(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}

