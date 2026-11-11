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
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final SongCommentMapper commentMapper;
    private final MusicClient musicClient;
    private final SearchClient searchClient;
    private final ReviewClient reviewClient;
    private final UserClient userClient;

    @Value("${kumao.moderation.mute-minutes:60}")
    private int muteMinutes;
    @Value("${kumao.moderation.reject-risk-score:35}")
    private int rejectRiskScore;

    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, String nickname, String avatar, CommentRequest req) {
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
        moderateComment(userId, req.getSongId(), req.getContent());
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
        comment.setStatus(0);
        commentMapper.insert(comment);
        try {
            musicClient.commentCount(req.getSongId(), 1);
        } catch (Exception e) {
            log.warn("更新歌曲评论数失败: {}", e.getMessage());
        }
        return comment.getId();
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
            // 用户服务短暂不可用时不阻断正常评论；违规内容仍由 AI 审核拦截。
            log.warn("禁言状态查询失败，暂按未禁言处理: {}", e.getMessage());
        }
    }

    private void moderateComment(Long userId, Long songId, String content) {
        ModerationRequestDTO request = new ModerationRequestDTO();
        request.setCategory("COMMENT");
        request.setItemId(String.valueOf(songId));
        request.setContent(content);
        ModerationResultDTO result;
        try {
            R<ModerationResultDTO> response = reviewClient.moderate(request);
            result = response == null ? null : response.getData();
        } catch (Exception e) {
            // AI 审核服务不可用时放行，避免互动功能整体不可用；降级规则在 review-service 内部执行。
            log.warn("评论 AI 审核不可用，暂按放行处理: {}", e.getMessage());
            return;
        }
        if (result == null) return;
        boolean violation = "REJECT".equalsIgnoreCase(result.getDecision())
                || result.getRiskScore() >= rejectRiskScore;
        if (!violation) return;
        muteUser(userId, result.getReasons());
        throw new BizException(403, "评论内容违规，你已被禁言");
    }

    private void muteUser(Long userId, String reason) {
        try {
            userClient.mute(userId, muteMinutes);
            log.info("评论违规，已禁言用户 userId={} minutes={} reason={}", userId, muteMinutes, reason);
        } catch (Exception e) {
            log.warn("评论违规禁言失败 userId={}: {}", userId, e.getMessage());
        }
    }

    public PageResult<SongComment> pageBySong(Long songId, PageQuery query) {
        Page<SongComment> page = commentMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<SongComment>()
                        .eq(SongComment::getSongId, songId)
                        .eq(SongComment::getStatus, 0)
                        .eq(SongComment::getParentId, 0L)
                        .orderByDesc(SongComment::getCreateTime));
        return PageResult.of(page);
    }

    public PageResult<SongComment> replies(Long parentId, PageQuery query) {
        Page<SongComment> page = commentMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<SongComment>()
                        .eq(SongComment::getParentId, parentId)
                        .eq(SongComment::getStatus, 0)
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

    public void updateStatus(Long commentId, Integer status) {
        SongComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException("评论不存在");
        }
        comment.setStatus(status);
        commentMapper.updateById(comment);
    }

    public void adminDelete(Long commentId) {
        commentMapper.deleteById(commentId);
    }

    public long countBySong(Long songId) {
        return commentMapper.selectCount(new LambdaQueryWrapper<SongComment>()
                .eq(SongComment::getSongId, songId).eq(SongComment::getStatus, 0));
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
