package com.kumao.vip.pay.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kumao.api.client.AdClient;
import com.kumao.api.dto.AdPlanPaymentDTO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.R;
import com.kumao.vip.entity.VipOrder;
import com.kumao.vip.entity.VipPackage;
import com.kumao.vip.mapper.VipOrderMapper;
import com.kumao.vip.mapper.VipPackageMapper;
import com.kumao.vip.pay.PayChannel;
import com.kumao.vip.pay.config.PaymentProperties;
import com.kumao.vip.pay.dto.PaymentChannelsVO;
import com.kumao.vip.pay.dto.PaymentCreateVO;
import com.kumao.vip.pay.dto.PaymentStatusVO;
import com.kumao.vip.pay.entity.PayNotifyLog;
import com.kumao.vip.pay.mapper.PayNotifyLogMapper;
import com.kumao.vip.pay.support.QrCodeUtils;
import com.kumao.vip.service.VipService;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付编排：下单、查单、回调、会员权益发放。
 * <p>
 * 状态机：UNPAID -> PAID / CLOSED，所有渠道回调最终都走 {@link #markPaid}，
 * 通过带状态条件的 UPDATE 保证同一订单只会发放一次权益。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final VipPackageMapper packageMapper;
    private final AdClient adClient;
    private final VipOrderMapper orderMapper;
    private final VipService vipService;
    private final PaymentProperties properties;
    private final AlipayPaymentService alipayPaymentService;
    private final WechatPaymentService wechatPaymentService;
    private final PayNotifyLogMapper notifyLogMapper;

    public PaymentChannelsVO channels() {
        return new PaymentChannelsVO(alipayPaymentService.enabled(),
                wechatPaymentService.enabled(), properties.isMockEnabled());
    }

    /** 酷猫币充值套餐 */
    public List<VipPackage> coinPackages() {
        return packageMapper.selectList(new LambdaQueryWrapper<VipPackage>()
                .eq(VipPackage::getStatus, 1)
                .eq(VipPackage::getType, "COIN")
                .orderByAsc(VipPackage::getSort));
    }

    /**
     * 创建支付订单
     */
    @Transactional(rollbackFor = Exception.class)
    public PaymentCreateVO createAdPlan(Long userId, Long planId, String payType) {
        if (planId == null) throw new BizException("投放计划不能为空");
        R<AdPlanPaymentDTO> infoResult = adClient.paymentInfo(planId);
        AdPlanPaymentDTO info = infoResult == null ? null : infoResult.getData();
        if (info == null) throw new BizException("投放计划不存在");
        if (!userId.equals(info.getUserId())) throw new BizException("不能支付他人的投放计划");
        if (!"UNPAID".equals(info.getPaymentStatus())) throw new BizException("该投放计划已完成支付");
        if (info.getAmount() == null || info.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("投放费用异常，请重新创建计划");
        }
        PayChannel requested = PayChannel.from(payType);
        PayChannel channel = resolveChannel(requested);
        VipOrder order = new VipOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPackageId(0L);
        order.setPackageName(info.getSubject());
        order.setDays(0);
        order.setAmount(info.getAmount());
        order.setStatus("UNPAID");
        order.setPayType(channel.name());
        order.setRemark("AD_PLAN:" + planId);
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(Math.max(1, properties.getOrderTimeoutMinutes())));
        orderMapper.insert(order);
        return buildPaymentResponse(order, channel);
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentCreateVO create(Long userId, Long packageId, String payType) {
        VipPackage pkg = packageMapper.selectById(packageId);
        if (pkg == null || !Integer.valueOf(1).equals(pkg.getStatus())) {
            throw new BizException("套餐不存在或已下架");
        }
        if ("COIN".equals(pkg.getType()) && (pkg.getCoinAmount() == null || pkg.getCoinAmount() <= 0)) {
            throw new BizException("酷猫币套餐配置异常");
        }
        PayChannel requested = PayChannel.from(payType);
        PayChannel channel = resolveChannel(requested);

        VipOrder order = new VipOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPackageId(pkg.getId());
        order.setPackageName(pkg.getName());
        order.setDays(pkg.getDays());
        order.setAmount(pkg.getPrice());
        order.setStatus("UNPAID");
        order.setPayType(channel.name());
        if ("COIN".equals(pkg.getType())) {
            order.setRemark("COIN_RECHARGE:" + pkg.getCoinAmount());
        }
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(Math.max(1, properties.getOrderTimeoutMinutes())));
        orderMapper.insert(order);

        return buildPaymentResponse(order, channel);
    }

    private PaymentCreateVO buildPaymentResponse(VipOrder order, PayChannel channel) {
        PaymentCreateVO.PaymentCreateVOBuilder builder = PaymentCreateVO.builder()
                .orderNo(order.getOrderNo())
                .payType(channel.name())
                .amount(order.getAmount())
                .days(order.getDays())
                .packageName(order.getPackageName())
                .mock(channel == PayChannel.MOCK)
                .payExpireTime(order.getPayExpireTime());
        if (channel == PayChannel.ALIPAY) {
            builder.payUrl("/api/vip/vip/pay/alipay/page/" + order.getOrderNo());
        } else if (channel == PayChannel.WECHAT) {
            String codeUrl = wechatPaymentService.prepay(order);
            builder.codeUrl(codeUrl).qrCode(QrCodeUtils.toDataUrl(codeUrl, 320));
        }
        return builder.build();
    }

    /**
     * 查询订单状态；未支付时主动向渠道查单，兼容本地收不到异步回调的场景
     */
    @Transactional(rollbackFor = Exception.class)
    public PaymentStatusVO status(Long userId, String orderNo) {
        VipOrder order = requireOrder(userId, orderNo);
        if ("UNPAID".equals(order.getStatus())) {
            refreshFromChannel(order);
            order = requireOrder(userId, orderNo);
        }
        return toStatusVO(order);
    }

    /**
     * 本地模拟支付（仅开发演示，且必须是 MOCK 渠道订单）
     */
    @Transactional(rollbackFor = Exception.class)
    public PaymentStatusVO mockSuccess(Long userId, String orderNo) {
        if (!properties.isMockEnabled()) {
            throw new BizException(403, "模拟支付未开启");
        }
        VipOrder order = requireOrder(userId, orderNo);
        if (!PayChannel.MOCK.name().equals(order.getPayType())) {
            throw new BizException("该订单不是模拟支付订单");
        }
        if (!"PAID".equals(order.getStatus())) {
            markPaid(orderNo, PayChannel.MOCK.name(), "MOCK" + System.currentTimeMillis(), order.getAmount());
        }
        return toStatusVO(requireOrder(userId, orderNo));
    }

    /**
     * 兼容旧版 /vip/open：MOCK 渠道立即完成支付，其它渠道返回支付参数
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> openLegacy(Long userId, Long packageId, String payType) {
        PaymentCreateVO payment = create(userId, packageId, payType);
        if (payment.isMock()) {
            markPaid(payment.getOrderNo(), PayChannel.MOCK.name(),
                    "MOCK" + System.currentTimeMillis(), payment.getAmount());
            VipOrder paid = requireOrder(userId, payment.getOrderNo());
            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", paid.getOrderNo());
            result.put("expireTime", paid.getExpireTime() == null ? null : paid.getExpireTime().toString());
            result.put("amount", paid.getAmount());
            result.put("status", paid.getStatus());
            return result;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", payment.getOrderNo());
        result.put("amount", payment.getAmount());
        result.put("status", "UNPAID");
        result.put("payType", payment.getPayType());
        result.put("payUrl", payment.getPayUrl());
        result.put("qrCode", payment.getQrCode());
        return result;
    }

    /**
     * 支付宝电脑网站支付表单（由浏览器直接打开该地址）
     */
    public String alipayPage(String orderNo) {
        VipOrder order = requireOrder(null, orderNo);
        if (!PayChannel.ALIPAY.name().equals(order.getPayType())) {
            throw new BizException("该订单不是支付宝订单");
        }
        if (!"UNPAID".equals(order.getStatus())) {
            throw new BizException("订单已支付或已关闭");
        }
        if (order.getPayExpireTime() != null && order.getPayExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException("订单已超时，请重新下单");
        }
        return alipayPaymentService.pageForm(order);
    }

    /**
     * 支付宝异步通知，返回 true 表示应答 success
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean handleAlipayNotify(Map<String, String> params) {
        String orderNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        boolean ack;
        String message;
        if (!alipayPaymentService.verifyNotify(params)) {
            ack = false;
            message = "验签失败";
        } else if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            // 非成功状态只需应答，避免支付宝重复通知
            ack = true;
            message = "忽略状态:" + tradeStatus;
        } else {
            BigDecimal amount = new BigDecimal(params.getOrDefault("total_amount", "0"));
            ack = markPaid(orderNo, PayChannel.ALIPAY.name(), tradeNo, amount);
            message = ack ? "OK" : "订单不存在或金额不一致";
        }
        saveNotifyLog(PayChannel.ALIPAY.name(), orderNo, tradeNo, ack, message, params.toString());
        return ack;
    }

    /**
     * 微信支付异步通知，异常会向微信返回 500 触发重试
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleWechatNotify(HttpHeaders headers, String body) {
        Transaction transaction = wechatPaymentService.parseNotify(headers, body);
        if (!properties.getWechat().getMchId().equals(transaction.getMchid())) {
            throw new BizException("微信支付商户号不匹配");
        }
        String orderNo = transaction.getOutTradeNo();
        String tradeNo = transaction.getTransactionId();
        boolean ack;
        String message;
        if (!Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState())) {
            ack = true;
            message = "忽略状态:" + transaction.getTradeState();
        } else {
            BigDecimal amount = BigDecimal.valueOf(transaction.getAmount() == null
                    ? 0 : transaction.getAmount().getTotal()).movePointLeft(2);
            ack = markPaid(orderNo, PayChannel.WECHAT.name(), tradeNo, amount);
            message = ack ? "OK" : "订单不存在或金额不一致";
        }
        saveNotifyLog(PayChannel.WECHAT.name(), orderNo, tradeNo, ack, message, body);
        if (!ack) {
            throw new BizException(message);
        }
    }

    /**
     * 幂等入账：条件更新成功才发放会员权益
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markPaid(String orderNo, String channel, String transactionId, BigDecimal paidAmount) {
        VipOrder order = orderMapper.selectOne(new LambdaQueryWrapper<VipOrder>()
                .eq(VipOrder::getOrderNo, orderNo).last("limit 1"));
        if (order == null) {
            log.warn("支付回调订单不存在 orderNo={} channel={}", orderNo, channel);
            return false;
        }
        if (!channel.equalsIgnoreCase(order.getPayType())) {
            log.warn("支付渠道与订单不一致 orderNo={} orderChannel={} notifyChannel={}",
                    orderNo, order.getPayType(), channel);
            return false;
        }
        if (paidAmount != null && order.getAmount() != null
                && order.getAmount().compareTo(paidAmount) != 0) {
            log.warn("支付金额不一致 orderNo={} orderAmount={} paidAmount={}",
                    orderNo, order.getAmount(), paidAmount);
            return false;
        }
        if ("PAID".equals(order.getStatus())) {
            return true;
        }
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<VipOrder>()
                .eq(VipOrder::getOrderNo, orderNo)
                .ne(VipOrder::getStatus, "PAID")
                .set(VipOrder::getStatus, "PAID")
                .set(VipOrder::getPayTime, LocalDateTime.now())
                .set(VipOrder::getNotifyTime, LocalDateTime.now())
                .set(VipOrder::getTransactionId, transactionId));
        if (updated == 0) {
            return true;
        }
        order.setStatus("PAID");
        order.setTransactionId(transactionId);
        vipService.fulfillPaidOrder(order);
        log.info("支付成功并开通会员 orderNo={} channel={} tx={}", orderNo, channel, transactionId);
        return true;
    }

    /**
     * 定期关闭超时未支付订单
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void closeExpiredOrders() {
        int closed = orderMapper.update(null, new LambdaUpdateWrapper<VipOrder>()
                .eq(VipOrder::getStatus, "UNPAID")
                .isNotNull(VipOrder::getPayExpireTime)
                .lt(VipOrder::getPayExpireTime, LocalDateTime.now())
                .set(VipOrder::getStatus, "CLOSED"));
        if (closed > 0) {
            log.info("已关闭超时未支付订单 {} 笔", closed);
        }
    }

    private void refreshFromChannel(VipOrder order) {
        try {
            if (PayChannel.ALIPAY.name().equals(order.getPayType()) && alipayPaymentService.enabled()) {
                AlipayPaymentService.QueryResult result = alipayPaymentService.query(order.getOrderNo());
                if (result.paid()) {
                    markPaid(order.getOrderNo(), PayChannel.ALIPAY.name(), result.transactionId(), order.getAmount());
                }
            } else if (PayChannel.WECHAT.name().equals(order.getPayType()) && wechatPaymentService.enabled()) {
                WechatPaymentService.QueryResult result = wechatPaymentService.query(order.getOrderNo());
                if (result.paid()) {
                    markPaid(order.getOrderNo(), PayChannel.WECHAT.name(), result.transactionId(), order.getAmount());
                }
            }
        } catch (Exception e) {
            log.warn("主动查单失败 orderNo={}: {}", order.getOrderNo(), e.getMessage());
        }
    }

    private PayChannel resolveChannel(PayChannel requested) {
        if (requested == PayChannel.MOCK) {
            if (!properties.isMockEnabled()) {
                throw new BizException("模拟支付已关闭");
            }
            return PayChannel.MOCK;
        }
        if (requested == PayChannel.ALIPAY && alipayPaymentService.enabled()) {
            return PayChannel.ALIPAY;
        }
        if (requested == PayChannel.WECHAT && wechatPaymentService.enabled()) {
            return PayChannel.WECHAT;
        }
        if (properties.isMockEnabled()) {
            log.warn("支付渠道 {} 未配置，降级为本地模拟支付", requested);
            return PayChannel.MOCK;
        }
        throw new BizException("支付渠道未配置：" + requested);
    }

    private VipOrder requireOrder(Long userId, String orderNo) {
        VipOrder order = orderMapper.selectOne(new LambdaQueryWrapper<VipOrder>()
                .eq(VipOrder::getOrderNo, orderNo).last("limit 1"));
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (userId != null && !userId.equals(order.getUserId())) {
            throw new BizException(403, "无权查看该订单");
        }
        return order;
    }

    private PaymentStatusVO toStatusVO(VipOrder order) {
        return PaymentStatusVO.builder()
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .payType(order.getPayType())
                .amount(order.getAmount())
                .packageName(order.getPackageName())
                .days(order.getDays())
                .transactionId(order.getTransactionId())
                .mock(PayChannel.MOCK.name().equals(order.getPayType()))
                .payTime(order.getPayTime())
                .payExpireTime(order.getPayExpireTime())
                .expireTime(order.getExpireTime())
                .build();
    }

    private String generateOrderNo() {
        return "VIP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
    }

    private void saveNotifyLog(String channel, String orderNo, String transactionId,
                               boolean success, String message, String rawBody) {
        try {
            PayNotifyLog logRow = new PayNotifyLog();
            logRow.setChannel(channel);
            logRow.setOrderNo(orderNo);
            logRow.setTransactionId(transactionId);
            logRow.setSuccess(success ? 1 : 0);
            logRow.setMessage(message != null && message.length() > 250 ? message.substring(0, 250) : message);
            logRow.setRawBody(rawBody);
            notifyLogMapper.insert(logRow);
        } catch (Exception e) {
            log.warn("保存支付回调日志失败 orderNo={}: {}", orderNo, e.getMessage());
        }
    }
}
