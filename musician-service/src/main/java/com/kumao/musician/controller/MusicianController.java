package com.kumao.musician.controller;

import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.musician.dto.AlbumRequest;
import com.kumao.musician.dto.MusicianApplyRequest;
import com.kumao.musician.dto.WorkRequest;
import com.kumao.musician.entity.*;
import com.kumao.musician.service.MusicianService;
import com.kumao.musician.service.MusicianIncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/musician")
@RequiredArgsConstructor
public class MusicianController {

    private final MusicianService musicianService;
    private final MusicianIncomeService incomeService;

    // ---------------- 入驻 ----------------

    @PostMapping("/apply")
    public R<Long> apply(@RequestBody @Valid MusicianApplyRequest req) {
        return R.ok(musicianService.apply(UserContext.requireUserId(), req));
    }

    @GetMapping("/mine")
    public R<Musician> mine() {
        return R.ok(musicianService.mine(UserContext.requireUserId()));
    }

    // ---------------- 主页 ----------------

    @GetMapping("/page")
    public R<PageResult<Musician>> page(PageQuery query, @RequestParam(required = false) String style) {
        return R.ok(musicianService.pageMusicians(query, style));
    }

    @GetMapping("/{id}/homepage")
    public R<Map<String, Object>> homepage(@PathVariable Long id) {
        return R.ok(musicianService.homepage(id, UserContext.getUserId()));
    }

    @PostMapping("/{id}/follow")
    public R<Void> follow(@PathVariable Long id) {
        musicianService.follow(id, UserContext.requireUserId());
        return R.ok();
    }

    @DeleteMapping("/{id}/follow")
    public R<Void> unfollow(@PathVariable Long id) {
        musicianService.unfollow(id, UserContext.requireUserId());
        return R.ok();
    }

    // ---------------- 工作台：作品 ----------------

    @GetMapping("/works")
    public R<PageResult<MusicianWork>> works(PageQuery query,
                                             @RequestParam(required = false) String status) {
        return R.ok(musicianService.works(UserContext.requireUserId(), query, status));
    }

    @PostMapping("/works")
    public R<Long> createWork(@RequestBody @Valid WorkRequest req) {
        return R.ok(musicianService.createWork(UserContext.requireUserId(), req));
    }

    @PutMapping("/works/{id}")
    public R<Void> updateWork(@PathVariable Long id, @RequestBody @Valid WorkRequest req) {
        musicianService.updateWork(id, UserContext.requireUserId(), req);
        return R.ok();
    }

    @PostMapping("/works/{id}/offline")
    public R<Void> offline(@PathVariable Long id) {
        musicianService.offlineWork(id, UserContext.requireUserId());
        return R.ok();
    }

    @PostMapping("/works/{id}/online")
    public R<Void> online(@PathVariable Long id) {
        musicianService.onlineWork(id, UserContext.requireUserId());
        return R.ok();
    }

    @PostMapping("/albums/{id}/status")
    public R<Void> albumStatus(@PathVariable Long id, @RequestParam Integer status) {
        musicianService.updateAlbumStatus(id, UserContext.requireUserId(), status);
        return R.ok();
    }

    @DeleteMapping("/works/{id}")
    public R<Void> deleteWork(@PathVariable Long id) {
        musicianService.deleteWork(id, UserContext.requireUserId());
        return R.ok();
    }

    // ---------------- 工作台：专辑 ----------------

    @GetMapping("/albums")
    public R<List<MusicianAlbum>> albums() {
        return R.ok(musicianService.albums(UserContext.requireUserId()));
    }

    @PostMapping("/albums")
    public R<Long> createAlbum(@RequestBody @Valid AlbumRequest req) {
        return R.ok(musicianService.createAlbum(UserContext.requireUserId(), req));
    }

    @PutMapping("/albums/{id}")
    public R<Void> updateAlbum(@PathVariable Long id, @RequestBody @Valid AlbumRequest req) {
        musicianService.updateAlbum(id, UserContext.requireUserId(), req);
        return R.ok();
    }

    @DeleteMapping("/albums/{id}")
    public R<Void> deleteAlbum(@PathVariable Long id) {
        musicianService.deleteAlbum(id, UserContext.requireUserId());
        return R.ok();
    }

    // ---------------- 工作台：数据 ----------------

    @GetMapping("/statistics")
    public R<Map<String, Object>> statistics(@RequestParam(defaultValue = "day") String period) {
        return R.ok(musicianService.statistics(UserContext.requireUserId(), period));
    }

    @GetMapping("/statistics/trend")
    public R<List<MusicianPlayStat>> trend(@RequestParam(defaultValue = "day") String period) {
        return R.ok(musicianService.trend(UserContext.requireUserId(), period));
    }

    @GetMapping("/statistics/rank")
    public R<List<MusicianWork>> rank() {
        return R.ok(musicianService.rankByPlay(UserContext.requireUserId()));
    }

    @GetMapping("/statistics/region")
    public R<List<Map<String, Object>>> fansRegions() {
        return R.ok(musicianService.fansRegions(UserContext.requireUserId()));
    }

    // ---------------- 工作台：收益 ----------------

    @GetMapping("/income/summary")
    public R<Map<String, Object>> incomeSummary() {
        return R.ok(incomeService.summary(UserContext.requireUserId()));
    }

    @GetMapping("/income")
    public R<PageResult<MusicianIncome>> incomes(PageQuery query) {
        return R.ok(musicianService.incomes(UserContext.requireUserId(), query));
    }

    @PostMapping("/income/settle")
    public R<Map<String, Object>> settle() {
        return R.ok(incomeService.settle(UserContext.requireUserId()));
    }

    @GetMapping("/withdraws")
    public R<PageResult<MusicianWithdraw>> withdraws(PageQuery query) {
        return R.ok(musicianService.withdraws(UserContext.requireUserId(), query));
    }

    @PostMapping("/withdraws")
    public R<Long> withdraw(@RequestParam int amount, @RequestParam String account, @RequestParam String payType) {
        return R.ok(musicianService.withdraw(UserContext.requireUserId(), amount, account, payType));
    }

    // ---------------- 后台 ----------------

    @GetMapping("/admin/page")
    public R<PageResult<Musician>> adminPage(PageQuery query,
                                             @RequestParam(required = false) String status) {
        UserContext.requireAdmin();
        return R.ok(musicianService.adminPage(query, status));
    }

    @PostMapping("/admin/{id}/audit")
    public R<Void> audit(@PathVariable Long id,
                         @RequestParam boolean approved,
                         @RequestParam(required = false) String reason) {
        UserContext.requireAdmin();
        musicianService.audit(id, approved, reason, UserContext.getUserId());
        return R.ok();
    }

    @PostMapping("/admin/{id}/revoke")
    public R<Void> revoke(@PathVariable Long id, @RequestParam(required = false) String reason) {
        UserContext.requireAdmin();
        musicianService.revoke(id, reason);
        return R.ok();
    }

    @GetMapping("/admin/works")
    public R<PageResult<MusicianWork>> adminWorks(PageQuery query,
                                                  @RequestParam(required = false) String status) {
        UserContext.requireAdmin();
        return R.ok(musicianService.adminWorks(query, status));
    }

    @PostMapping("/admin/works/{id}/audit")
    public R<Void> auditWork(@PathVariable Long id,
                             @RequestParam boolean approved,
                             @RequestParam(required = false) String reason) {
        UserContext.requireAdmin();
        musicianService.auditWork(id, approved, reason);
        return R.ok();
    }

    @GetMapping("/admin/income/summary")
    public R<Map<String, Object>> adminIncomeSummary() {
        UserContext.requireAdmin();
        return R.ok(musicianService.adminIncomeSummary());
    }

    @GetMapping("/admin/withdraws")
    public R<PageResult<MusicianWithdraw>> adminWithdraws(PageQuery query,
                                                          @RequestParam(required = false) String status) {
        UserContext.requireAdmin();
        return R.ok(musicianService.adminWithdraws(query, status));
    }

    @PostMapping("/admin/withdraws/{id}/audit")
    public R<Void> auditWithdraw(@PathVariable Long id,
                                 @RequestParam boolean approved,
                                 @RequestParam(required = false) String remark) {
        UserContext.requireAdmin();
        musicianService.auditWithdraw(id, approved, remark);
        return R.ok();
    }
}

