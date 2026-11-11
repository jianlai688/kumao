package com.kumao.comment.controller;

import com.kumao.comment.dto.CommentRequest;
import com.kumao.comment.entity.SongComment;
import com.kumao.comment.service.CommentService;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.LoginUser;
import com.kumao.common.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/song/{songId}")
    public R<PageResult<SongComment>> list(@PathVariable Long songId, PageQuery query) {
        return R.ok(commentService.pageBySong(songId, query));
    }

    @GetMapping("/{id}/replies")
    public R<PageResult<SongComment>> replies(@PathVariable Long id, PageQuery query) {
        return R.ok(commentService.replies(id, query));
    }

    @PostMapping
    public R<Long> create(@RequestBody @Valid CommentRequest req) {
        LoginUser user = UserContext.get();
        return R.ok(commentService.create(user.getUserId(), user.getUsername(), null, req));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        commentService.deleteOwn(id, UserContext.requireUserId());
        return R.ok();
    }

    @GetMapping("/song/{songId}/count")
    public R<Long> count(@PathVariable Long songId) {
        return R.ok(commentService.countBySong(songId));
    }

    // ---------------- 后台 ----------------

    @GetMapping("/admin/page")
    public R<PageResult<SongComment>> adminPage(PageQuery query,
                                                @RequestParam(required = false) Long songId,
                                                @RequestParam(required = false) Integer status) {
        UserContext.requireAdmin();
        return R.ok(commentService.adminPage(query, songId, status));
    }

    @PutMapping("/admin/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        UserContext.requireAdmin();
        commentService.updateStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/admin/{id}")
    public R<Void> adminDelete(@PathVariable Long id) {
        UserContext.requireAdmin();
        commentService.adminDelete(id);
        return R.ok();
    }
}
