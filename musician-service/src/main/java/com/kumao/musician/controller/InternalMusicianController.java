package com.kumao.musician.controller;

import com.kumao.api.dto.MusicianDTO;
import com.kumao.common.result.R;
import com.kumao.musician.entity.Musician;
import com.kumao.musician.entity.MusicianWork;
import com.kumao.musician.mapper.MusicianMapper;
import com.kumao.musician.mapper.MusicianWorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微服务「内部」接口：运营侧/数据统计回写
 */
@RestController
@RequestMapping("/internal/musician")
@RequiredArgsConstructor
public class InternalMusicianController {

    private final MusicianWorkMapper workMapper;
    private final MusicianMapper musicianMapper;

    /** 供 music-service 校验歌手认证资格 */
    @GetMapping("/by-user/{userId}")
    public R<MusicianDTO> byUser(@PathVariable Long userId) {
        Musician musician = musicianMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Musician>()
                .eq(Musician::getUserId, userId).last("limit 1"));
        if (musician == null) {
            return R.ok(null);
        }
        MusicianDTO dto = new MusicianDTO();
        dto.setId(musician.getId());
        dto.setUserId(musician.getUserId());
        dto.setStageName(musician.getStageName());
        dto.setAvatar(musician.getAvatar());
        dto.setIntro(musician.getIntro());
        dto.setStyle(musician.getStyle());
        dto.setStatus(musician.getStatus());
        return R.ok(dto);
    }

    /**
     * 播放量/互动数据回写（comment-service、音乐播放埋点可调用）
     */
    @PostMapping("/works/{songId}/stats")
    public R<Void> addStat(@PathVariable Long songId,
                           @RequestParam(defaultValue = "0") long play,
                           @RequestParam(defaultValue = "0") long like,
                           @RequestParam(defaultValue = "0") long comment,
                           @RequestParam(defaultValue = "0") long share) {
        MusicianWork work = workMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MusicianWork>()
                .eq(MusicianWork::getSongId, songId).last("limit 1"));
        if (work == null) {
            return R.ok();
        }
        work.setPlayCount((work.getPlayCount() == null ? 0 : work.getPlayCount()) + play);
        work.setLikeCount((work.getLikeCount() == null ? 0 : work.getLikeCount()) + like);
        work.setCommentCount((work.getCommentCount() == null ? 0 : work.getCommentCount()) + comment);
        work.setShareCount((work.getShareCount() == null ? 0 : work.getShareCount()) + share);
        workMapper.updateById(work);
        return R.ok();
    }
}
