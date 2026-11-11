package com.kumao.music.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.api.dto.SongDTO;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.music.dto.SongRequest;
import com.kumao.music.entity.Song;
import com.kumao.music.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SongService {

    private final SongMapper songMapper;

    public PageResult<Song> page(PageQuery query, String region, String language, String style,
                                 Long singerId, Integer status, Long musicianId) {
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<Song>()
                .eq(StringUtils.hasText(region), Song::getRegion, region)
                .eq(StringUtils.hasText(language), Song::getLanguage, language)
                .eq(StringUtils.hasText(style), Song::getStyle, style)
                .eq(singerId != null, Song::getSingerId, singerId)
                .eq(musicianId != null, Song::getMusicianId, musicianId)
                .eq(status != null, Song::getStatus, status)
                .like(StringUtils.hasText(query.getKeyword()), Song::getName, query.getKeyword())
                .orderByDesc(Song::getCreateTime);
        Page<Song> page = songMapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }

    public Song detail(Long id) {
        Song song = songMapper.selectById(id);
        if (song == null) {
            throw new BizException("歌曲不存在");
        }
        return song;
    }

    public SongDTO dto(Long id) {
        return toDTO(detail(id));
    }

    public List<SongDTO> batch(List<Long> ids) {
        List<SongDTO> list = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            return list;
        }
        for (Song song : songMapper.selectBatchIds(ids)) {
            list.add(toDTO(song));
        }
        return list;
    }

    private SongDTO toDTO(Song song) {
        SongDTO dto = new SongDTO();
        BeanUtils.copyProperties(song, dto);
        // LocalDate -> String 类型不一致，BeanUtils 不会自动转换，这里手动赋值
        dto.setReleaseDate(song.getReleaseDate() == null ? null : song.getReleaseDate().toString());
        return dto;
    }

    public Long create(SongRequest req) {
        Song song = new Song();
        apply(song, req);
        if (song.getStatus() == null) {
            song.setStatus(1);
        }
        song.setPlayCount(0L);
        song.setLikeCount(0L);
        song.setCommentCount(0L);
        songMapper.insert(song);
        return song.getId();
    }

    public void update(Long id, SongRequest req) {
        Song song = detail(id);
        apply(song, req);
        songMapper.updateById(song);
    }

    public void updateStatus(Long id, Integer status) {
        Song song = detail(id);
        song.setStatus(status);
        songMapper.updateById(song);
    }

    public void delete(Long id) {
        songMapper.deleteById(id);
    }

    public void addPlayCount(Long id, int delta) {
        songMapper.addPlayCount(id, delta);
    }

    public void addLikeCount(Long id, int delta) {
        songMapper.addLikeCount(id, delta);
    }

    public void addCommentCount(Long id, int delta) {
        songMapper.addCommentCount(id, delta);
    }

    private void apply(Song song, SongRequest req) {
        song.setName(req.getName());
        song.setSingerId(req.getSingerId());
        song.setSingerName(req.getSingerName());
        song.setAlbumId(req.getAlbumId());
        song.setAlbumName(req.getAlbumName());
        song.setCoverUrl(req.getCoverUrl());
        song.setAudioUrl(req.getAudioUrl());
        song.setLyricUrl(req.getLyricUrl());
        song.setDuration(req.getDuration());
        song.setLanguage(req.getLanguage());
        song.setStyle(req.getStyle());
        song.setRegion(req.getRegion());
        song.setVipOnly(Boolean.TRUE.equals(req.getVipOnly()));
        song.setStatus(req.getStatus());
        song.setReleaseDate(req.getReleaseDate());
        song.setMusicianId(req.getMusicianId());
    }
}
