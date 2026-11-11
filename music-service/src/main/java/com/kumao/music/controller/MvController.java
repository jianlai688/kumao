package com.kumao.music.controller;

import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.music.dto.MvPublishRequest;
import com.kumao.music.dto.MvRequest;
import com.kumao.music.entity.Mv;
import com.kumao.music.service.MvService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mv")
@RequiredArgsConstructor
public class MvController {

    private final MvService mvService;

    @GetMapping
    public R<PageResult<Mv>> page(PageQuery query,
                                  @RequestParam(required = false) Long singerId,
                                  @RequestParam(required = false) Integer status) {
        return R.ok(mvService.page(query, singerId, status));
    }

    @GetMapping("/mine")
    public R<PageResult<Mv>> mine(PageQuery query) {
        return R.ok(mvService.myPage(UserContext.requireUserId(), query));
    }

    @PostMapping("/publish")
    public R<Long> publish(@RequestBody @Valid MvPublishRequest req) {
        return R.ok(mvService.publish(UserContext.requireUserId(), req));
    }

    /** 编辑自己的 MV（音乐人 / 歌手） */
    @PutMapping("/mine/{id}")
    public R<Void> updateMine(@PathVariable Long id, @RequestBody @Valid MvPublishRequest req) {
        mvService.updateOwn(UserContext.requireUserId(), id, req);
        return R.ok();
    }

    /** 上架 / 下架自己的 MV */
    @PutMapping("/mine/{id}/status")
    public R<Void> updateMineStatus(@PathVariable Long id, @RequestParam Integer status) {
        mvService.updateOwnStatus(UserContext.requireUserId(), id, status);
        return R.ok();
    }

    /** 删除自己的 MV */
    @DeleteMapping("/mine/{id}")
    public R<Void> deleteMine(@PathVariable Long id) {
        mvService.deleteOwn(UserContext.requireUserId(), id);
        return R.ok();
    }
    @GetMapping("/{id}")
    public R<Mv> detail(@PathVariable Long id) {
        return R.ok(mvService.detail(id));
    }

    /** 播放接口：VIP 高清鉴权 */
    @GetMapping("/{id}/play")
    public R<Mv> play(@PathVariable Long id) {
        return R.ok(mvService.play(id, UserContext.getUserId()));
    }

    @GetMapping("/admin/page")
    public R<PageResult<Mv>> adminPage(PageQuery query,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) String auditStatus) {
        UserContext.requireAdmin();
        return R.ok(mvService.adminPage(query, status, auditStatus));
    }

    @PostMapping("/admin/{id}/audit")
    public R<Void> audit(@PathVariable Long id,
                         @RequestParam boolean approved,
                         @RequestParam(required = false) String reason) {
        UserContext.requireAdmin();
        mvService.audit(id, approved, reason);
        return R.ok();
    }

    @PostMapping("/admin")
    public R<Long> create(@RequestBody @Valid MvRequest req) {
        UserContext.requireAdmin();
        return R.ok(mvService.create(req));
    }

    @PutMapping("/admin/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid MvRequest req) {
        UserContext.requireAdmin();
        mvService.update(id, req);
        return R.ok();
    }

    @PutMapping("/admin/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        UserContext.requireAdmin();
        mvService.updateStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/admin/{id}")
    public R<Void> delete(@PathVariable Long id) {
        UserContext.requireAdmin();
        mvService.delete(id);
        return R.ok();
    }
}

