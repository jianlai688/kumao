package com.kumao.comment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.api.client.MusicClient;
import com.kumao.api.dto.SongDTO;
import com.kumao.comment.entity.SongLike;
import com.kumao.comment.mapper.SongLikeMapper;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private final SongLikeMapper likeMapper;
    private final MusicClient musicClient;

    @Transactional(rollbackFor = Exception.class)
    public void like(Long userId, Long songId) {
        Long exist = likeMapper.selectCount(new LambdaQueryWrapper<SongLike>()
                .eq(SongLike::getUserId, userId).eq(SongLike::getSongId, songId));
        if (exist != null && exist > 0) {
            throw new BizException("已添加红心");
        }
        SongDTO song;
        try {
            R<SongDTO> r = musicClient.getSong(songId);
            song = r == null ? null : r.getData();
        } catch (Exception e) {
            log.warn("音乐服务不可用: {}", e.getMessage());
            throw new BizException("音乐服务暂不可用，请稍后再试");
        }
        if (song == null) {
            throw new BizException("歌曲不存在");
        }
        SongLike like = new SongLike();
        like.setUserId(userId);
        like.setSongId(songId);
        like.setSongName(song.getName());
        like.setSingerName(song.getSingerName());
        like.setCoverUrl(song.getCoverUrl());
        like.setAudioUrl(song.getAudioUrl());
        like.setDuration(song.getDuration());
        likeMapper.insert(like);
        try {
            musicClient.likeCount(songId, 1);
        } catch (Exception e) {
            log.warn("更新歌曲红心数失败: {}", e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void unlike(Long userId, Long songId) {
        int removed = likeMapper.delete(new LambdaQueryWrapper<SongLike>()
                .eq(SongLike::getUserId, userId).eq(SongLike::getSongId, songId));
        if (removed > 0) {
            try {
                musicClient.likeCount(songId, -1);
            } catch (Exception e) {
                log.warn("更新歌曲红心数失败: {}", e.getMessage());
            }
        }
    }

    public PageResult<SongLike> myLikes(Long userId, PageQuery query) {
        Page<SongLike> page = likeMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<SongLike>().eq(SongLike::getUserId, userId)
                        .orderByDesc(SongLike::getCreateTime));
        return PageResult.of(page);
    }

    public boolean isLiked(Long userId, Long songId) {
        return likeMapper.selectCount(new LambdaQueryWrapper<SongLike>()
                .eq(SongLike::getUserId, userId).eq(SongLike::getSongId, songId)) > 0;
    }

    public long countBySong(Long songId) {
        return likeMapper.selectCount(new LambdaQueryWrapper<SongLike>().eq(SongLike::getSongId, songId));
    }
}
