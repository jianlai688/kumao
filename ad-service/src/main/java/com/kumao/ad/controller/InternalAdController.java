package com.kumao.ad.controller;

import com.kumao.ad.service.AdService;
import com.kumao.api.dto.AdPlanPaymentDTO;
import com.kumao.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微服务内部接口：供 vip-service 支付编排查询广告计划并确认到账。
 */
@RestController
@RequestMapping("/internal/ad")
@RequiredArgsConstructor
public class InternalAdController {

    private final AdService adService;

    @GetMapping("/plans/{id}/payment-info")
    public R<AdPlanPaymentDTO> paymentInfo(@PathVariable Long id) {
        return R.ok(adService.paymentInfo(id));
    }

    @PostMapping("/plans/{id}/paid")
    public R<Void> markPaid(@PathVariable Long id, @RequestParam String orderNo) {
        adService.markPlanPaid(id, orderNo);
        return R.ok();
    }
}
