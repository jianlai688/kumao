package com.kumao.playlist.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.api.client.MusicClient;
import com.kumao.api.dto.SongDTO;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.playlist.dto.PlaylistDetailVO;
import com.kumao.playlist.dto.PlaylistRequest;
import com.kumao.playlist.entity.Playlist;
import com.kumao.playlist.entity.PlaylistCollect;
import com.kumao.playlist.entity.PlaylistSong;
import com.kumao.playlist.mapper.PlaylistCollectMapper;
import com.kumao.playlist.mapper.PlaylistMapper;
import com.kumao.playlist.mapper.PlaylistSongMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistMapper playlistMapper;
    private final PlaylistSongMapper playlistSongMapper;
    private final PlaylistCollectMapper playlistCollectMapper;
    private final MusicClient musicClient;

    public PageResult<Playlist> publicPage(PageQuery query, String sort) {
        LambdaQueryWrapper<Playlist> wrapper = new LambdaQueryWrapper<Playlist>()
                .eq(Playlist::getIsPublic, 1)
                .eq(Playlist::getStatus, 1)
                .like(StringUtils.hasText(query.getKeyword()), Playlist::getName, query.getKeyword());
        if ("hot".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(Playlist::getCollectCount);
        } else {
            wrapper.orderByDesc(Playlist::getCreateTime);
        }
        Page<Playlist> page = playlistMapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }

    public PageResult<Playlist> myPlaylists(Long userId, PageQuery query) {
        Page<Playlist> page = playlistMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<Playlist>()
                        .eq(Playlist::getUserId, userId)
                        .orderByDesc(Playlist::getCreateTime));
        return PageResult.of(page);
    }

    public PageResult<Playlist> myCollected(Long userId, PageQuery query) {
        List<Long> ids = playlistCollectMapper.selectList(new LambdaQueryWrapper<PlaylistCollect>()
                        .eq(PlaylistCollect::getUserId, userId))
                .stream().map(PlaylistCollect::getPlaylistId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return PageResult.empty(query.getPageNum(), query.getPageSize());
        }
        Page<Playlist> page = playlistMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<Playlist>().in(Playlist::getId, ids)
                        .orderByDesc(Playlist::getCreateTime));
        return PageResult.of(page);
    }

    public PlaylistDetailVO detail(Long id, Long userId) {
        Playlist playlist = requirePlaylist(id);
        if (playlist.getIsPublic() != null && playlist.getIsPublic() == 0
                && (userId == null || !userId.equals(playlist.getUserId()))) {
            throw new BizException(403, "私有歌单无权访问");
        }
        PlaylistDetailVO vo = new PlaylistDetailVO();
        BeanUtils.copyProperties(playlist, vo);
        vo.setSongs(playlistSongMapper.selectList(new LambdaQueryWrapper<PlaylistSong>()
                .eq(PlaylistSong::getPlaylistId, id).orderByAsc(PlaylistSong::getSort)));
        if (userId != null) {
            vo.setCollected(playlistCollectMapper.selectCount(new LambdaQueryWrapper<PlaylistCollect>()
                    .eq(PlaylistCollect::getPlaylistId, id).eq(PlaylistCollect::getUserId, userId)) > 0);
        }
        return vo;
    }

    public Long create(Long userId, String creatorName, PlaylistRequest req) {
        Playlist playlist = new Playlist();
        playlist.setName(req.getName());
        playlist.setCoverUrl(req.getCoverUrl());
        playlist.setDescription(req.getDescription());
        playlist.setIsPublic(req.getIsPublic() == null ? 1 : req.getIsPublic());
        playlist.setUserId(userId);
        playlist.setCreatorName(creatorName);
        playlist.setSongCount(0);
        playlist.setPlayCount(0L);
        playlist.setCollectCount(0L);
        playlist.setStatus(1);
        playlistMapper.insert(playlist);
        return playlist.getId();
    }

    public void update(Long id, Long userId, PlaylistRequest req) {
        Playlist playlist = requireOwned(id, userId);
        playlist.setName(req.getName());
        playlist.setCoverUrl(req.getCoverUrl());
        playlist.setDescription(req.getDescription());
        if (req.getIsPublic() != null) {
            playlist.setIsPublic(req.getIsPublic());
        }
        playlistMapper.updateById(playlist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        requireOwned(id, userId);
        playlistMapper.deleteById(id);
        playlistSongMapper.delete(new LambdaQueryWrapper<PlaylistSong>().eq(PlaylistSong::getPlaylistId, id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void addSong(Long playlistId, Long userId, Long songId) {
        Playlist playlist = requireOwned(playlistId, userId);
        Long exist = playlistSongMapper.selectCount(new LambdaQueryWrapper<PlaylistSong>()
                .eq(PlaylistSong::getPlaylistId, playlistId).eq(PlaylistSong::getSongId, songId));
        if (exist != null && exist > 0) {
            throw new BizException("歌曲已在歌单中");
        }
        SongDTO song;
        try {
            R<SongDTO> r = musicClient.getSong(songId);
            song = r == null ? null : r.getData();
        } catch (Exception e) {
            log.warn("调用音乐服务失败: {}", e.getMessage());
            throw new BizException("音乐服务暂不可用，请稍后重试");
        }
        if (song == null) {
            throw new BizException("歌曲不存在");
        }
        PlaylistSong ps = new PlaylistSong();
        ps.setPlaylistId(playlistId);
        ps.setSongId(songId);
        ps.setSongName(song.getName());
        ps.setSingerName(song.getSingerName());
        ps.setCoverUrl(song.getCoverUrl());
        ps.setAudioUrl(song.getAudioUrl());
        ps.setDuration(song.getDuration());
        ps.setSort((int) (playlistSongMapper.selectCount(new LambdaQueryWrapper<PlaylistSong>()
                .eq(PlaylistSong::getPlaylistId, playlistId)) + 1));
        playlistSongMapper.insert(ps);

        playlist.setSongCount((playlist.getSongCount() == null ? 0 : playlist.getSongCount()) + 1);
        playlistMapper.updateById(playlist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeSong(Long playlistId, Long userId, Long songId) {
        Playlist playlist = requireOwned(playlistId, userId);
        int removed = playlistSongMapper.delete(new LambdaQueryWrapper<PlaylistSong>()
                .eq(PlaylistSong::getPlaylistId, playlistId).eq(PlaylistSong::getSongId, songId));
        if (removed > 0) {
            playlist.setSongCount(Math.max(0, (playlist.getSongCount() == null ? 0 : playlist.getSongCount()) - 1));
            playlistMapper.updateById(playlist);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void collect(Long playlistId, Long userId) {
        Playlist playlist = requirePlaylist(playlistId);
        if (playlist.getIsPublic() != null && playlist.getIsPublic() == 0) {
            throw new BizException("私有歌单不可收藏");
        }
        if (userId.equals(playlist.getUserId())) {
            throw new BizException("不能收藏自己的歌单");
        }
        Long exist = playlistCollectMapper.selectCount(new LambdaQueryWrapper<PlaylistCollect>()
                .eq(PlaylistCollect::getPlaylistId, playlistId).eq(PlaylistCollect::getUserId, userId));
        if (exist != null && exist > 0) {
            throw new BizException("已收藏该歌单");
        }
        PlaylistCollect collect = new PlaylistCollect();
        collect.setPlaylistId(playlistId);
        collect.setUserId(userId);
        playlistCollectMapper.insert(collect);
        playlist.setCollectCount((playlist.getCollectCount() == null ? 0 : playlist.getCollectCount()) + 1);
        playlistMapper.updateById(playlist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelCollect(Long playlistId, Long userId) {
        int removed = playlistCollectMapper.delete(new LambdaQueryWrapper<PlaylistCollect>()
                .eq(PlaylistCollect::getPlaylistId, playlistId).eq(PlaylistCollect::getUserId, userId));
        if (removed > 0) {
            Playlist playlist = requirePlaylist(playlistId);
            playlist.setCollectCount(Math.max(0, (playlist.getCollectCount() == null ? 0 : playlist.getCollectCount()) - 1));
            playlistMapper.updateById(playlist);
        }
    }

    /** 后台歌单管理：可查看全部歌单 */
    public PageResult<Playlist> adminPage(PageQuery query, Integer isPublic) {
        Page<Playlist> page = playlistMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<Playlist>()
                        .eq(isPublic != null, Playlist::getIsPublic, isPublic)
                        .like(StringUtils.hasText(query.getKeyword()), Playlist::getName, query.getKeyword())
                        .orderByDesc(Playlist::getCreateTime));
        return PageResult.of(page);
    }

    public void adminDelete(Long id) {
        playlistMapper.deleteById(id);
    }

    /** 供 comment 服务统计/查询 */
    public List<Playlist> listByIds(List<Long> ids) {
        return ids == null || ids.isEmpty() ? new ArrayList<>() : playlistMapper.selectBatchIds(ids);
    }

    private Playlist requirePlaylist(Long id) {
        Playlist playlist = playlistMapper.selectById(id);
        if (playlist == null) {
            throw new BizException("歌单不存在");
        }
        return playlist;
    }

    private Playlist requireOwned(Long id, Long userId) {
        Playlist playlist = requirePlaylist(id);
        if (!playlist.getUserId().equals(userId)) {
            throw new BizException(403, "只能操作自己的歌单");
        }
        return playlist;
    }
}
