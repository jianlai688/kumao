package com.kumao.comment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.api.client.MusicClient;
import com.kumao.api.client.ReviewClient;
import com.kumao.api.client.SearchClient;
import com.kumao.api.client.UserClient;
import com.kumao.api.dto.ModerationRequestDTO;
import com.kumao.api.dto.ModerationResultDTO;
import com.kumao.api.dto.SearchDocDTO;
import com.kumao.api.dto.SongDTO;
import com.kumao.comment.dto.CommentRequest;
import com.kumao.comment.entity.SongComment;
import com.kumao.comment.mapper.SongCommentMapper;
import com.kumao.common.constant.ModerationStatus;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final SongCommentMapper commentMapper;
    private final MusicClient musicClient;
    private final SearchClient searchClient;
    private final ReviewClient reviewClient;
    private final UserClient userClient;

    /** AI 风险分达到该值即不自动通过，转管理员复核。 */
    @Value("${kumao.moderation.reject-risk-score:35}")
    private int rejectRiskScore = 35;

    public CommentCreateOutcome create(Long userId, String nickname, String avatar, CommentRequest req) {
        // 校验歌曲存在
        try {
            R<SongDTO> song = musicClient.getSong(req.getSongId());
            if (song == null || song.getData() == null) {
                throw new BizException("歌曲不存在");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("音乐服务不可用，降级放行评论校验: {}", e.getMessage());
        }
        ensureNotMuted(userId);
        ModerationResultDTO moderation = moderateComment(req.getSongId(), req.getContent());
        boolean approved = shouldAutoApprove(moderation);
        SongComment comment = new SongComment();
        comment.setSongId(req.getSongId());
        comment.setUserId(userId);
        comment.setNickname(nickname);
        comment.setAvatar(avatar);
        comment.setContent(req.getContent());
        comment.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        comment.setReplyUserId(req.getReplyUserId());
        comment.setReplyNickname(req.getReplyNickname());
        comment.setLikeCount(0);
        comment.setStatus(approved ? ModerationStatus.APPROVED : ModerationStatus.PENDING);
        applyModeration(comment, moderation);
        commentMapper.insert(comment);
        if (approved) {
            adjustCommentCount(req.getSongId(), 1);
        }
        String message = approved
                ? "评论已通过 AI 审核并发布"
                : "评论已提交管理员复核，通过后展示";
        return new CommentCreateOutcome(comment.getId(), approved, message);
    }

    private void ensureNotMuted(Long userId) {
        try {
            R<Boolean> result = userClient.isMuted(userId);
            if (result != null && Boolean.TRUE.equals(result.getData())) {
                throw new BizException(403, "你已被禁言，暂时无法评论");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // 用户服务短暂不可用时不阻断正常评论；内容是否公开仍由 AI 审核决定。
            log.warn("禁言状态查询失败，暂按未禁言处理: {}", e.getMessage());
        }
    }

    private ModerationResultDTO moderateComment(Long songId, String content) {
        ModerationRequestDTO request = new ModerationRequestDTO();
        request.setCategory("COMMENT");
        request.setItemId(String.valueOf(songId));
        request.setContent(content);
        try {
            R<ModerationResultDTO> response = reviewClient.moderate(request);
            ModerationResultDTO result = response == null ? null : response.getData();
            if (result != null) {
                return result;
            }
            log.warn("评论 AI 审核返回空结果，转管理员复核 songId={}", songId);
        } catch (Exception e) {
            // 审核不可用时不能自动放行，保留内容并交给管理员复核。
            log.warn("评论 AI 审核不可用，转管理员复核 songId={}: {}", songId, e.getMessage());
        }
        return unavailableResult();
    }

    private boolean shouldAutoApprove(ModerationResultDTO result) {
        return result != null
                && "PASS".equalsIgnoreCase(result.getDecision())
                && result.getRiskScore() < rejectRiskScore;
    }

    private void applyModeration(SongComment comment, ModerationResultDTO result) {
        if (result == null) return;
        comment.setAiDecision(limit(result.getDecision(), 20));
        comment.setAiRiskScore(result.getRiskScore());
        comment.setAiSource(limit(result.getSource(), 30));
        comment.setAiReasons(limit(result.getReasons(), 1000));
    }

    private ModerationResultDTO unavailableResult() {
        ModerationResultDTO result = new ModerationResultDTO();
        result.setDecision("REVIEW");
        result.setRiskScore(50);
        result.setSource("UNAVAILABLE");
        result.setReasons("AI 审核服务暂不可用，已转管理员复核");
        return result;
    }

    private String limit(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void adjustCommentCount(Long songId, int delta) {
        try {
            musicClient.commentCount(songId, delta);
        } catch (Exception e) {
            log.warn("更新歌曲评论数失败 songId={} delta={}: {}", songId, delta, e.getMessage());
        }
    }

    public PageResult<SongComment> pageBySong(Long songId, PageQuery query) {
        Page<SongComment> page = commentMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<SongComment>()
                        .eq(SongComment::getSongId, songId)
                        .eq(SongComment::getStatus, ModerationStatus.APPROVED)
                        .eq(SongComment::getParentId, 0L)
                        .orderByDesc(SongComment::getCreateTime));
        return PageResult.of(page);
    }

    public PageResult<SongComment> replies(Long parentId, PageQuery query) {
        Page<SongComment> page = commentMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<SongComment>()
                        .eq(SongComment::getParentId, parentId)
                        .eq(SongComment::getStatus, ModerationStatus.APPROVED)
                        .orderByAsc(SongComment::getCreateTime));
        return PageResult.of(page);
    }

    public void deleteOwn(Long commentId, Long userId) {
        SongComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BizException(403, "只能删除自己的评论");
        }
        commentMapper.deleteById(commentId);
        if (ModerationStatus.isApproved(comment.getStatus())) {
            adjustCommentCount(comment.getSongId(), -1);
        }
    }

    // ---------------- 后台评论管理 ----------------

    public PageResult<SongComment> adminPage(PageQuery query, Long songId, Integer status) {
        Page<SongComment> page = commentMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<SongComment>()
                        .eq(songId != null, SongComment::getSongId, songId)
                        .eq(status != null, SongComment::getStatus, status)
                        .orderByDesc(SongComment::getCreateTime));
        return PageResult.of(page);
    }

    /**
     * 定时复检「已通过但还没有 AI 记录」的评论：AI 判定 REJECT 的直接封禁（status=1）。
     */
    @Scheduled(fixedDelayString = "${kumao.comment.post-review.interval-ms:180000}", initialDelayString = "45000")
    public void scheduledPostReview() {
        try {
            Map<String, Object> summary = rescanApproved(30);
            if (((Number) summary.get("scanned")).intValue() > 0) {
                log.info("[评论AI复检] {}", summary);
            }
        } catch (Exception e) {
            log.warn("[评论AI复检] 执行失败: {}", e.getMessage());
        }
    }

    /**
     * 人工/定时触发：复检已通过且无 AI 记录的评论。
     */
    public Map<String, Object> rescanApproved(int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        List<SongComment> list = commentMapper.selectList(new LambdaQueryWrapper<SongComment>()
                .eq(SongComment::getStatus, ModerationStatus.APPROVED)
                .isNull(SongComment::getAiDecision)
                .orderByDesc(SongComment::getCreateTime)
                .last("limit " + size));
        int blocked = 0;
        int passed = 0;
        int review = 0;
        for (SongComment comment : list) {
            ModerationResultDTO result = moderateComment(comment.getSongId(), comment.getContent());
            applyModeration(comment, result);
            String decision = result == null ? "" : result.getDecision();
            if ("REJECT".equalsIgnoreCase(decision)) {
                comment.setStatus(ModerationStatus.REJECTED);
                commentMapper.updateById(comment);
                adjustCommentCount(comment.getSongId(), -1);
                blocked++;
            } else {
                commentMapper.updateById(comment);
                if ("PASS".equalsIgnoreCase(decision)) {
                    passed++;
                } else {
                    review++;
                }
            }
        }
        return Map.of("scanned", list.size(), "blocked", blocked, "passed", passed, "review", review);
    }

    public void updateStatus(Long commentId, Integer status) {
        if (!ModerationStatus.isValid(status)) {
            throw new BizException("无效的评论审核状态");
        }
        SongComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException("评论不存在");
        }
        boolean wasApproved = ModerationStatus.isApproved(comment.getStatus());
        boolean willApprove = ModerationStatus.isApproved(status);
        comment.setStatus(status);
        commentMapper.updateById(comment);
        if (wasApproved != willApprove) {
            adjustCommentCount(comment.getSongId(), willApprove ? 1 : -1);
        }
    }

    public void adminDelete(Long commentId) {
        SongComment comment = commentMapper.selectById(commentId);
        if (comment == null) return;
        commentMapper.deleteById(commentId);
        if (ModerationStatus.isApproved(comment.getStatus())) {
            adjustCommentCount(comment.getSongId(), -1);
        }
    }

    public long countBySong(Long songId) {
        return commentMapper.selectCount(new LambdaQueryWrapper<SongComment>()
                .eq(SongComment::getSongId, songId).eq(SongComment::getStatus, ModerationStatus.APPROVED));
    }

    public record CommentCreateOutcome(Long id, boolean approved, String message) {
    }

    /** 供 search-service 同步评论热度（可选） */
    public void syncSearchDoc(SongComment comment) {
        try {
            SearchDocDTO doc = new SearchDocDTO();
            doc.setBizId(comment.getSongId());
            doc.setType("SONG");
            doc.setHeat(comment.getLikeCount() == null ? 0L : comment.getLikeCount().longValue());
            searchClient.sync(doc);
        } catch (Exception e) {
            log.debug("评论热度同步跳过: {}", e.getMessage());
        }
    }
}
