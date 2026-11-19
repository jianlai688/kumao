package com.kumao.music.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.music.entity.HomeSetting;
import com.kumao.music.entity.Song;
import com.kumao.music.mapper.HomeSettingMapper;
import com.kumao.music.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 首页展示设置：客户端读取，管理后台修改。
 *
 * <p>轮播内容支持两种模式：HOT 取热歌榜前 N 首；MANUAL 按后台选定的歌曲顺序展示。</p>
 */
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeSettingController {

    private static final long SETTING_ID = 1L;
    private static final int MAX_HERO_SONGS = 10;

    private final HomeSettingMapper homeSettingMapper;
    private final SongMapper songMapper;

    /**
     * 读取首页设置（公开接口，未配置时返回默认值，保证客户端永远拿得到可用配置）
     */
    @GetMapping("/settings")
    public R<HomeSetting> settings() {
        HomeSetting setting = homeSettingMapper.selectById(SETTING_ID);
        HomeSetting resolved = setting == null ? defaults() : setting;
        resolved.setSongs(resolveSongs(resolved));
        return R.ok(resolved);
    }

    /**
     * 保存首页设置（仅管理员）
     */
    @PutMapping("/admin/settings")
    public R<HomeSetting> save(@RequestBody HomeSetting body) {
        UserContext.requireAdmin();
        HomeSetting setting = new HomeSetting();
        setting.setId(SETTING_ID);
        setting.setHeroAutoplay(body.getHeroAutoplay() == null || body.getHeroAutoplay() != 0 ? 1 : 0);
        setting.setHeroInterval(clamp(body.getHeroInterval(), 2, 15, 5));
        setting.setHeroCount(clamp(body.getHeroCount(), 1, MAX_HERO_SONGS, 3));
        setting.setHeroMode(normalizeMode(body.getHeroMode(), body.getHeroSongIds()));
        setting.setHeroSongIds(normalizeIds(body.getHeroSongIds()));

        if (homeSettingMapper.selectById(SETTING_ID) == null) {
            homeSettingMapper.insert(setting);
        } else {
            homeSettingMapper.updateById(setting);
        }
        HomeSetting saved = homeSettingMapper.selectById(SETTING_ID);
        saved.setSongs(resolveSongs(saved));
        return R.ok(saved);
    }

    /**
     * 解析轮播歌曲：手动模式按 ID 顺序取；否则取热歌榜前 N 首（已上架、按播放量倒序）。
     */
    private List<Song> resolveSongs(HomeSetting setting) {
        List<Long> ids = parseIds(setting.getHeroSongIds());
        if ("MANUAL".equalsIgnoreCase(setting.getHeroMode()) && !ids.isEmpty()) {
            List<Song> songs = songMapper.selectBatchIds(ids);
            Map<Long, Song> byId = new LinkedHashMap<>();
            songs.forEach(song -> byId.put(song.getId(), song));
            List<Song> ordered = new ArrayList<>();
            for (Long id : ids) {
                Song song = byId.get(id);
                if (song != null) {
                    ordered.add(song);
                }
            }
            if (!ordered.isEmpty()) {
                return ordered;
            }
        }
        int count = clamp(setting.getHeroCount(), 1, MAX_HERO_SONGS, 3);
        return songMapper.selectList(new LambdaQueryWrapper<Song>()
                .eq(Song::getStatus, 1)
                .orderByDesc(Song::getPlayCount)
                .last("limit " + count));
    }

    private List<Long> parseIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> s.matches("\\d{1,19}"))
                .map(Long::valueOf)
                .distinct()
                .limit(MAX_HERO_SONGS)
                .forEach(ids::add);
        return ids;
    }

    private String normalizeIds(String raw) {
        List<Long> ids = parseIds(raw);
        return ids.isEmpty() ? "" : String.join(",", ids.stream().map(String::valueOf).toList());
    }

    private String normalizeMode(String mode, String rawIds) {
        if ("MANUAL".equalsIgnoreCase(mode) && !parseIds(rawIds).isEmpty()) {
            return "MANUAL";
        }
        return "HOT";
    }

    private HomeSetting defaults() {
        HomeSetting setting = new HomeSetting();
        setting.setId(SETTING_ID);
        setting.setHeroAutoplay(1);
        setting.setHeroInterval(5);
        setting.setHeroCount(3);
        setting.setHeroMode("HOT");
        setting.setHeroSongIds("");
        return setting;
    }

    private int clamp(Integer value, int min, int max, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }
}
