package com.kumao.music.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumao.music.dto.RankItemVO;
import com.kumao.music.entity.Song;
import com.kumao.music.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 榜单服务：热门榜 / 新歌榜 / 飙升榜，结果缓存 Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankService {

    private static final String CACHE_KEY = "kumao:rank:";

    private final SongMapper songMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kumao.rank.cache-seconds:300}")
    private long cacheSeconds;

    public List<RankItemVO> hot(int limit) {
        return cached("hot", limit, () -> {
            List<Song> songs = songMapper.selectList(new LambdaQueryWrapper<Song>()
                    .eq(Song::getStatus, 1)
                    .orderByDesc(Song::getPlayCount)
                    .last("limit " + safeLimit(limit)));
            return convert(songs);
        });
    }

    public List<RankItemVO> newSongs(int limit) {
        return cached("new", limit, () -> {
            List<Song> songs = songMapper.selectList(new LambdaQueryWrapper<Song>()
                    .eq(Song::getStatus, 1)
                    .orderByDesc(Song::getReleaseDate)
                    .orderByDesc(Song::getCreateTime)
                    .last("limit " + safeLimit(limit)));
            return convert(songs);
        });
    }

    public List<RankItemVO> rising(int limit) {
        return cached("rising", limit, () -> {
            // 模拟飙升榜：近 7 天新增播放量高的歌曲（无埋点数据时按点赞率排序）
            List<Song> songs = songMapper.selectList(new LambdaQueryWrapper<Song>()
                    .eq(Song::getStatus, 1)
                    .ge(Song::getReleaseDate, LocalDate.now().minusDays(30))
                    .orderByDesc(Song::getLikeCount)
                    .last("limit " + safeLimit(limit)));
            if (songs.isEmpty()) {
                songs = songMapper.selectList(new LambdaQueryWrapper<Song>()
                        .eq(Song::getStatus, 1).orderByDesc(Song::getLikeCount).last("limit " + safeLimit(limit)));
            }
            return convert(songs);
        });
    }

    private int safeLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }

    private List<RankItemVO> cached(String type, int limit, java.util.function.Supplier<List<RankItemVO>> loader) {
        String key = CACHE_KEY + type + ":" + safeLimit(limit);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<List<RankItemVO>>() {
                });
            }
        } catch (Exception e) {
            log.warn("读取榜单缓存失败: {}", e.getMessage());
        }
        List<RankItemVO> items = loader.get();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(items), Duration.ofSeconds(cacheSeconds));
        } catch (Exception e) {
            log.warn("写入榜单缓存失败: {}", e.getMessage());
        }
        return items;
    }

    private List<RankItemVO> convert(List<Song> songs) {
        List<RankItemVO> list = new ArrayList<>();
        int index = 1;
        for (Song song : songs) {
            RankItemVO vo = new RankItemVO();
            vo.setRank(index++);
            vo.setSongId(song.getId());
            vo.setName(song.getName());
            vo.setSingerName(song.getSingerName());
            vo.setCoverUrl(song.getCoverUrl());
            vo.setAudioUrl(song.getAudioUrl());
            vo.setDuration(song.getDuration());
            vo.setPlayCount(song.getPlayCount());
            vo.setVipOnly(Boolean.TRUE.equals(song.getVipOnly()));
            vo.setTrend(index % 3 == 0 ? "up" : index % 3 == 1 ? "flat" : "down");
            list.add(vo);
        }
        return list;
    }

    /** 数据变更后清空榜单缓存 */
    public void clearCache() {
        java.util.Set<String> keys = redisTemplate.keys(CACHE_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
