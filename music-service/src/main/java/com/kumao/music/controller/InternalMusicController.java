package com.kumao.music.controller;

import com.kumao.api.dto.SongDTO;
import com.kumao.common.result.R;
import com.kumao.music.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 微服务内部接口：供 playlist / comment / musician / search 调用
 */
@RestController
@RequestMapping("/internal/music")
@RequiredArgsConstructor
public class InternalMusicController {

    private final SongService songService;

    @GetMapping("/songs/{id}")
    public R<SongDTO> getSong(@PathVariable Long id) {
        return R.ok(songService.dto(id));
    }

    @GetMapping("/songs/batch")
    public R<List<SongDTO>> batch(@RequestParam List<Long> ids) {
        return R.ok(songService.batch(ids));
    }

    @PostMapping("/songs")
    public R<Long> create(@RequestBody SongDTO song) {
        com.kumao.music.dto.SongRequest req = new com.kumao.music.dto.SongRequest();
        org.springframework.beans.BeanUtils.copyProperties(song, req, "releaseDate");
        if (org.springframework.util.StringUtils.hasText(song.getReleaseDate())) {
            try {
                req.setReleaseDate(java.time.LocalDate.parse(song.getReleaseDate()));
            } catch (Exception ignored) {
                // 忽略非法日期
            }
        }
        return R.ok(songService.create(req));
    }

    @DeleteMapping("/songs/{id}")
    public R<Void> delete(@PathVariable Long id) {
        songService.delete(id);
        return R.ok();
    }

    @PostMapping("/songs/{id}/play-count")
    public R<Void> playCount(@PathVariable Long id, @RequestParam(defaultValue = "1") int delta) {
        songService.addPlayCount(id, delta);
        return R.ok();
    }

    @PostMapping("/songs/{id}/comment-count")
    public R<Void> commentCount(@PathVariable Long id, @RequestParam(defaultValue = "1") int delta) {
        songService.addCommentCount(id, delta);
        return R.ok();
    }

    @PostMapping("/songs/{id}/like-count")
    public R<Void> likeCount(@PathVariable Long id, @RequestParam(defaultValue = "1") int delta) {
        songService.addLikeCount(id, delta);
        return R.ok();
    }
}
