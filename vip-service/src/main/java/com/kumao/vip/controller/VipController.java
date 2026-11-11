package com.kumao.vip.controller;

import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.vip.dto.AdRewardRequest;
import com.kumao.vip.dto.OpenVipRequest;
import com.kumao.vip.dto.VipStatusVO;
import com.kumao.vip.entity.VipMember;
import com.kumao.vip.entity.VipOrder;
import com.kumao.vip.entity.VipPackage;
import com.kumao.vip.pay.service.PaymentService;
import com.kumao.vip.service.VipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vip")
@RequiredArgsConstructor
public class VipController {

    private final VipService vipService;
    private final PaymentService paymentService;

    @GetMapping("/packages")
    public R<List<VipPackage>> packages() {
        return R.ok(vipService.packages());
    }

    @GetMapping("/status")
    public R<VipStatusVO> status() {
        return R.ok(vipService.status(UserContext.getUserId()));
    }

    /** 完成激励广告后领取 1 天会员，每账号每日限领一次 */
    @PostMapping("/reward/ad")
    public R<Map<String, Object>> claimAdReward(@RequestBody @Valid AdRewardRequest req) {
        return R.ok(vipService.claimAdReward(
                UserContext.requireUserId(), req.getPlanId(), req.getWatchedSeconds()));
    }

    /**
     * 兼容旧版下单接口：MOCK 渠道直接支付成功，真实渠道返回支付参数。
     * 新前端请使用 /vip/pay/create。
     */
    @PostMapping("/open")
    public R<Map<String, Object>> open(@RequestBody @Valid OpenVipRequest req) {
        return R.ok(paymentService.openLegacy(UserContext.requireUserId(), req.getPackageId(), req.getPayType()));
    }

    @GetMapping("/orders")
    public R<PageResult<VipOrder>> orders(PageQuery query) {
        return R.ok(vipService.myOrders(UserContext.requireUserId(), query));
    }

    @GetMapping("/permission")
    public R<Boolean> permission(@RequestParam String type) {
        return R.ok(vipService.hasPermission(UserContext.requireUserId(), type));
    }

    // ---------------- 后台 ----------------

    @GetMapping("/admin/members")
    public R<PageResult<VipMember>> members(PageQuery query,
                                            @RequestParam(required = false) String status) {
        UserContext.requireAdmin();
        return R.ok(vipService.adminMembers(query, status));
    }

    @GetMapping("/admin/orders")
    public R<PageResult<VipOrder>> adminOrders(PageQuery query,
                                               @RequestParam(required = false) Long userId) {
        UserContext.requireAdmin();
        return R.ok(vipService.adminOrders(query, userId));
    }

    @GetMapping("/admin/statistics")
    public R<Map<String, Object>> statistics() {
        UserContext.requireAdmin();
        return R.ok(vipService.statistics());
    }

    @PostMapping("/admin/packages")
    public R<Long> createPackage(@RequestBody VipPackage pkg) {
        UserContext.requireAdmin();
        return R.ok(vipService.createPackage(pkg));
    }

    @PutMapping("/admin/packages/{id}")
    public R<Void> updatePackage(@PathVariable Long id, @RequestBody VipPackage pkg) {
        UserContext.requireAdmin();
        vipService.updatePackage(id, pkg);
        return R.ok();
    }

    @DeleteMapping("/admin/packages/{id}")
    public R<Void> deletePackage(@PathVariable Long id) {
        UserContext.requireAdmin();
        vipService.deletePackage(id);
        return R.ok();
    }
}
