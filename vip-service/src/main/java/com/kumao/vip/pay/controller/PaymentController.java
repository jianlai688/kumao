package com.kumao.vip.pay.controller;

import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.vip.pay.dto.AdPaymentRequest;
import com.kumao.vip.pay.dto.PaymentChannelsVO;
import com.kumao.vip.pay.dto.PaymentCreateRequest;
import com.kumao.vip.pay.dto.PaymentCreateVO;
import com.kumao.vip.pay.dto.PaymentStatusVO;
import com.kumao.vip.pay.service.PaymentService;
import com.kumao.vip.entity.VipPackage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 支付接口：下单、查单、渠道能力、支付宝/微信回调。
 * 网关映射：/api/vip/vip/pay/** -> /vip/pay/**
 */
@Slf4j
@RestController
@RequestMapping("/vip/pay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** 当前可用的支付渠道 */
    @GetMapping("/channels")
    public R<PaymentChannelsVO> channels() {
        return R.ok(paymentService.channels());
    }

    /** 酷猫币充值套餐 */
    @GetMapping("/coin/packages")
    public R<List<VipPackage>> coinPackages() {
        return R.ok(paymentService.coinPackages());
    }

    /** 创建广告投放支付订单 */
    @PostMapping("/ad/create")
    public R<PaymentCreateVO> createAd(@RequestBody @Valid AdPaymentRequest request) {
        return R.ok(paymentService.createAdPlan(UserContext.requireUserId(),
                request.getPlanId(), request.getPayType()));
    }

    /** 创建支付订单（VIP 套餐与酷猫币套餐共用） */
    @PostMapping("/create")
    public R<PaymentCreateVO> create(@RequestBody @Valid PaymentCreateRequest request) {
        return R.ok(paymentService.create(UserContext.requireUserId(),
                request.getPackageId(), request.getPayType()));
    }

    /** 查询订单状态（未支付时会主动向渠道查单） */
    @GetMapping("/status/{orderNo}")
    public R<PaymentStatusVO> status(@PathVariable String orderNo) {
        return R.ok(paymentService.status(UserContext.requireUserId(), orderNo));
    }

    /** 本地模拟支付成功（仅 MOCK 订单） */
    @PostMapping("/mock/{orderNo}/success")
    public R<PaymentStatusVO> mockSuccess(@PathVariable String orderNo) {
        return R.ok(paymentService.mockSuccess(UserContext.requireUserId(), orderNo));
    }

    /** 支付宝电脑网站支付：返回自动提交表单，浏览器直接打开 */
    @GetMapping(value = "/alipay/page/{orderNo}", produces = MediaType.TEXT_HTML_VALUE)
    public String alipayPage(@PathVariable String orderNo) {
        return paymentService.alipayPage(orderNo);
    }

    /** 支付宝异步通知（表单参数） */
    @PostMapping(value = "/alipay/notify", produces = MediaType.TEXT_PLAIN_VALUE)
    public String alipayNotify(@RequestParam Map<String, String> params) {
        try {
            return paymentService.handleAlipayNotify(params) ? "success" : "failure";
        } catch (Exception e) {
            log.error("支付宝回调处理异常", e);
            return "failure";
        }
    }

    /** 微信支付 APIv3 异步通知（JSON + 签名头） */
    @PostMapping("/wechat/notify")
    public ResponseEntity<Map<String, String>> wechatNotify(
            @RequestHeader HttpHeaders headers, @RequestBody String body) {
        try {
            paymentService.handleWechatNotify(headers, body);
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (Exception e) {
            log.error("微信支付回调处理异常: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "FAIL", "message", e.getMessage() == null ? "处理失败" : e.getMessage()));
        }
    }
}
