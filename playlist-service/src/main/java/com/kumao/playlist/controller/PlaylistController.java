package com.kumao.playlist.controller;

import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.playlist.dto.PlaylistDetailVO;
import com.kumao.playlist.dto.PlaylistRequest;
import com.kumao.playlist.entity.Playlist;
import com.kumao.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    // ---------------- 公开查询（网关白名单） ----------------

    @GetMapping("/public")
    public R<PageResult<Playlist>> publicPage(PageQuery query,
                                              @RequestParam(required = false) String sort) {
        return R.ok(playlistService.publicPage(query, sort));
    }

    @GetMapping("/public/{id}")
    public R<PlaylistDetailVO> publicDetail(@PathVariable Long id) {
        return R.ok(playlistService.detail(id, UserContext.getUserId()));
    }

    // ---------------- 我的歌单 ----------------

    @GetMapping("/mine")
    public R<PageResult<Playlist>> mine(PageQuery query) {
        return R.ok(playlistService.myPlaylists(UserContext.requireUserId(), query));
    }

    @GetMapping("/collected")
    public R<PageResult<Playlist>> collected(PageQuery query) {
        return R.ok(playlistService.myCollected(UserContext.requireUserId(), query));
    }

    @GetMapping("/{id}")
    public R<PlaylistDetailVO> detail(@PathVariable Long id) {
        return R.ok(playlistService.detail(id, UserContext.getUserId()));
    }

    @PostMapping
    public R<Long> create(@RequestBody @Valid PlaylistRequest req) {
        Long userId = UserContext.requireUserId();
        String creatorName = com.kumao.common.security.UserContext.get().getUsername();
        return R.ok(playlistService.create(userId, creatorName, req));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid PlaylistRequest req) {
        playlistService.update(id, UserContext.requireUserId(), req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        playlistService.delete(id, UserContext.requireUserId());
        return R.ok();
    }

    @PostMapping("/{id}/songs/{songId}")
    public R<Void> addSong(@PathVariable Long id, @PathVariable Long songId) {
        playlistService.addSong(id, UserContext.requireUserId(), songId);
        return R.ok();
    }

    @DeleteMapping("/{id}/songs/{songId}")
    public R<Void> removeSong(@PathVariable Long id, @PathVariable Long songId) {
        playlistService.removeSong(id, UserContext.requireUserId(), songId);
        return R.ok();
    }

    @PostMapping("/{id}/collect")
    public R<Void> collect(@PathVariable Long id) {
        playlistService.collect(id, UserContext.requireUserId());
        return R.ok();
    }

    @DeleteMapping("/{id}/collect")
    public R<Void> cancelCollect(@PathVariable Long id) {
        playlistService.cancelCollect(id, UserContext.requireUserId());
        return R.ok();
    }

    // ---------------- 后台 ----------------

    @GetMapping("/admin/page")
    public R<PageResult<Playlist>> adminPage(PageQuery query,
                                             @RequestParam(required = false) Integer isPublic) {
        UserContext.requireAdmin();
        return R.ok(playlistService.adminPage(query, isPublic));
    }

    @DeleteMapping("/admin/{id}")
    public R<Void> adminDelete(@PathVariable Long id) {
        UserContext.requireAdmin();
        playlistService.adminDelete(id);
        return R.ok();
    }
}
