package com.kumao.music.controller;

import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.music.dto.SingerApplyRequest;
import com.kumao.music.dto.SingerRequest;
import com.kumao.music.entity.Singer;
import com.kumao.music.entity.Song;
import com.kumao.music.service.SingerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/singers")
@RequiredArgsConstructor
public class SingerController {

    private final SingerService singerService;

    @GetMapping
    public R<PageResult<Singer>> page(PageQuery query, @RequestParam(required = false) String region) {
        return R.ok(singerService.page(query, region));
    }

    @GetMapping("/mine")
    public R<Singer> mine() {
        return R.ok(singerService.mine(UserContext.requireUserId()));
    }

    @PostMapping("/apply")
    public R<Long> apply(@RequestBody @Valid SingerApplyRequest req) {
        return R.ok(singerService.apply(UserContext.requireUserId(), req));
    }

    @GetMapping("/{id}")
    public R<Singer> detail(@PathVariable Long id) {
        return R.ok(singerService.detail(id));
    }

    @GetMapping("/{id}/songs")
    public R<List<Song>> songs(@PathVariable Long id) {
        return R.ok(singerService.songs(id));
    }

    @GetMapping("/admin/page")
    public R<PageResult<Singer>> adminPage(PageQuery query,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) String region) {
        UserContext.requireAdmin();
        return R.ok(singerService.adminPage(query, status, region));
    }

    @PostMapping("/admin/{id}/audit")
    public R<Void> audit(@PathVariable Long id,
                         @RequestParam boolean approved,
                         @RequestParam(required = false) String reason) {
        UserContext.requireAdmin();
        singerService.audit(id, approved, reason);
        return R.ok();
    }

    @PostMapping("/admin")
    public R<Long> create(@RequestBody @Valid SingerRequest req) {
        UserContext.requireAdmin();
        return R.ok(singerService.create(req));
    }

    @PutMapping("/admin/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid SingerRequest req) {
        UserContext.requireAdmin();
        singerService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/admin/{id}")
    public R<Void> delete(@PathVariable Long id) {
        UserContext.requireAdmin();
        singerService.delete(id);
        return R.ok();
    }
}
