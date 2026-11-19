package com.kumao.ad.controller;

import com.kumao.ad.dto.AdvertiserApplyRequest;
import com.kumao.ad.dto.MaterialRequest;
import com.kumao.ad.dto.PlanRequest;
import com.kumao.ad.entity.*;
import com.kumao.ad.service.AdService;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ad")
@RequiredArgsConstructor
public class AdController {

    private final AdService adService;

    // ---------------- 对外：广告展示 ----------------

    @GetMapping("/active")
    public R<List<Map<String, Object>>> active(@RequestParam(required = false) String slot) {
        return R.ok(adService.activeAds(slot));
    }

    /** 曝光/点击上报 */
    @PostMapping("/report/{planId}")
    public R<Void> report(@PathVariable Long planId,
                          @RequestParam String action,
                          @RequestParam(required = false) String region,
                          @RequestParam(required = false) Integer hour) {
        adService.report(planId, action, region, hour);
        return R.ok();
    }

    // ---------------- 入驻 ----------------

    @PostMapping("/apply")
    public R<Long> apply(@RequestBody @Valid AdvertiserApplyRequest req) {
        return R.ok(adService.apply(UserContext.requireUserId(), req));
    }

    @GetMapping("/mine")
    public R<Advertiser> mine() {
        return R.ok(adService.mine(UserContext.requireUserId()));
    }

    // ---------------- 工作台：素材 ----------------

    @PutMapping("/mine")
    public R<Void> updateMine(@RequestBody com.kumao.ad.dto.AdvertiserProfileRequest req) {
        adService.updateMyProfile(UserContext.requireUserId(), req);
        return R.ok();
    }

    @GetMapping("/materials")
    public R<PageResult<AdMaterial>> materials(PageQuery query,
                                               @RequestParam(required = false) String status) {
        return R.ok(adService.materials(UserContext.requireUserId(), query, status));
    }

    @PostMapping("/materials")
    public R<Long> createMaterial(@RequestBody @Valid MaterialRequest req) {
        return R.ok(adService.createMaterial(UserContext.requireUserId(), req));
    }

    @PutMapping("/materials/{id}")
    public R<Void> updateMaterial(@PathVariable Long id, @RequestBody @Valid MaterialRequest req) {
        adService.updateMaterial(id, UserContext.requireUserId(), req);
        return R.ok();
    }

    @PostMapping("/materials/{id}/offline")
    public R<Void> offlineMaterial(@PathVariable Long id) {
        adService.offlineMaterial(id, UserContext.requireUserId());
        return R.ok();
    }

    @DeleteMapping("/materials/{id}")
    public R<Void> deleteMaterial(@PathVariable Long id) {
        adService.deleteMaterial(id, UserContext.requireUserId());
        return R.ok();
    }

    // ---------------- 工作台：投放 ----------------

    @GetMapping("/pricing")
    public R<List<Map<String, Object>>> pricing() {
        return R.ok(adService.pricing());
    }

    @GetMapping("/admin/pricing")
    public R<List<Map<String, Object>>> adminPricing() {
        UserContext.requireAdmin();
        return R.ok(adService.pricing());
    }

    @PutMapping("/admin/pricing")
    public R<Void> updatePricing(@RequestBody List<AdSlotPrice> prices) {
        UserContext.requireAdmin();
        adService.updatePricing(prices);
        return R.ok();
    }

    @GetMapping("/plans")
    public R<PageResult<AdPlan>> plans(PageQuery query) {
        return R.ok(adService.plans(UserContext.requireUserId(), query));
    }

    @PostMapping("/plans")
    public R<AdPlan> createPlan(@RequestBody @Valid PlanRequest req) {
        return R.ok(adService.createPlan(UserContext.requireUserId(), req));
    }

    @PostMapping("/plans/{id}/{action}")
    public R<Void> planAction(@PathVariable Long id, @PathVariable String action) {
        adService.updatePlanStatus(id, UserContext.requireUserId(), action);
        return R.ok();
    }

    // ---------------- 工作台：数据 ----------------

    @GetMapping("/statistics")
    public R<Map<String, Object>> statistics() {
        return R.ok(adService.statistics(UserContext.requireUserId()));
    }

    @GetMapping("/statistics/rank")
    public R<List<AdMaterial>> rank() {
        return R.ok(adService.materialRank(UserContext.requireUserId()));
    }

    @GetMapping("/statistics/region")
    public R<Map<String, Object>> region() {
        return R.ok(adService.regionDistribution(UserContext.requireUserId()));
    }

    @GetMapping("/statistics/hourly")
    public R<Map<String, Object>> hourly() {
        return R.ok(adService.hourlyTrend(UserContext.requireUserId()));
    }

    // ---------------- 后台 ----------------

    @GetMapping("/admin/page")
    public R<PageResult<Advertiser>> adminPage(PageQuery query,
                                               @RequestParam(required = false) String status) {
        UserContext.requireAdmin();
        return R.ok(adService.adminPage(query, status));
    }

    @PostMapping("/admin/{id}/audit")
    public R<Void> audit(@PathVariable Long id,
                         @RequestParam boolean approved,
                         @RequestParam(required = false) String reason) {
        UserContext.requireAdmin();
        adService.audit(id, approved, reason, UserContext.getUserId());
        return R.ok();
    }

    @PostMapping("/admin/{id}/revoke")
    public R<Void> revoke(@PathVariable Long id, @RequestParam(required = false) String reason) {
        UserContext.requireAdmin();
        adService.revoke(id, reason);
        return R.ok();
    }

    @GetMapping("/admin/materials")
    public R<PageResult<AdMaterial>> adminMaterials(PageQuery query,
                                                    @RequestParam(required = false) String status) {
        UserContext.requireAdmin();
        return R.ok(adService.adminMaterials(query, status));
    }

    @PostMapping("/admin/materials/{id}/audit")
    public R<Void> auditMaterial(@PathVariable Long id,
                                 @RequestParam boolean approved,
                                 @RequestParam(required = false) String reason) {
        UserContext.requireAdmin();
        adService.auditMaterial(id, approved, reason);
        return R.ok();
    }

    @GetMapping("/admin/plans")
    public R<PageResult<AdPlan>> adminPlans(PageQuery query,
                                            @RequestParam(required = false) String status) {
        UserContext.requireAdmin();
        return R.ok(adService.adminPlans(query, status));
    }

    @PostMapping("/admin/plans/{id}/audit")
    public R<Void> auditPlan(@PathVariable Long id,
                             @RequestParam boolean approved,
                             @RequestParam(required = false) String reason) {
        UserContext.requireAdmin();
        adService.auditPlan(id, approved, reason);
        return R.ok();
    }

    /** 管理员对投放中的计划执行暂停 / 续投 / 终止 */
    @PostMapping("/admin/plans/{id}/action")
    public R<Void> adminPlanAction(@PathVariable Long id, @RequestParam String action) {
        UserContext.requireAdmin();
        adService.adminUpdatePlanStatus(id, action);
        return R.ok();
    }

    /** 广告投放收入（广告主为投流付费，平台侧只统计投放消耗，广告主不产生收益） */
    @GetMapping("/admin/revenue/summary")
    public R<Map<String, Object>> adminRevenueSummary() {
        UserContext.requireAdmin();
        return R.ok(adService.adminRevenueSummary());
    }
}
