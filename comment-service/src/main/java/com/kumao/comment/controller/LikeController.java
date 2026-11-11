package com.kumao.comment.controller;

import com.kumao.comment.entity.SongLike;
import com.kumao.comment.service.LikeService;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{songId}")
    public R<Void> like(@PathVariable Long songId) {
        likeService.like(UserContext.requireUserId(), songId);
        return R.ok();
    }

    @DeleteMapping("/{songId}")
    public R<Void> unlike(@PathVariable Long songId) {
        likeService.unlike(UserContext.requireUserId(), songId);
        return R.ok();
    }

    @GetMapping("/mine")
    public R<PageResult<SongLike>> mine(PageQuery query) {
        return R.ok(likeService.myLikes(UserContext.requireUserId(), query));
    }

    @GetMapping("/{songId}/status")
    public R<Boolean> status(@PathVariable Long songId) {
        Long userId = UserContext.getUserId();
        return R.ok(userId != null && likeService.isLiked(userId, songId));
    }

    @GetMapping("/{songId}/count")
    public R<Long> count(@PathVariable Long songId) {
        return R.ok(likeService.countBySong(songId));
    }
}
