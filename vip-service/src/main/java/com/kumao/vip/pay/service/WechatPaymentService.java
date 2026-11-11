package com.kumao.vip.pay.service;

import com.kumao.common.exception.BizException;
import com.kumao.vip.entity.VipOrder;
import com.kumao.vip.pay.config.PaymentProperties;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 微信支付 APIv3 Native（扫码支付）。
 * <p>
 * 微信支付没有公开沙箱，需使用真实商户号；未配置时由 PaymentService 降级为本地模拟支付。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPaymentService {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    private final PaymentProperties properties;

    private volatile RSAAutoCertificateConfig config;
    private volatile NativePayService nativePayService;
    private volatile NotificationParser notificationParser;

    public boolean enabled() {
        PaymentProperties.Wechat cfg = properties.getWechat();
        boolean hasKey = hasText(cfg.getPrivateKeyPath()) || hasText(cfg.getPrivateKey());
        return cfg.isEnabled()
                && hasText(cfg.getAppId())
                && hasText(cfg.getMchId())
                && hasText(cfg.getMerchantSerialNumber())
                && hasText(cfg.getApiV3Key())
                && hasKey;
    }

    /**
     * Native 下单，返回 code_url，前端展示为二维码
     */
    public String prepay(VipOrder order) {
        PrepayRequest request = new PrepayRequest();
        request.setAppid(properties.getWechat().getAppId());
        request.setMchid(properties.getWechat().getMchId());
        request.setDescription(order.getRemark() != null && order.getRemark().startsWith("COIN_RECHARGE:")
                ? "酷猫币充值-" + order.getPackageName()
                : order.getRemark() != null && order.getRemark().startsWith("AD_PLAN:")
                ? order.getPackageName()
                : "酷猫音乐VIP-" + order.getPackageName());
        request.setOutTradeNo(order.getOrderNo());
        request.setNotifyUrl(properties.wechatNotifyUrl());
        if (order.getPayExpireTime() != null) {
            request.setTimeExpire(order.getPayExpireTime().atZone(CHINA_ZONE)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        Amount amount = new Amount();
        amount.setTotal(order.getAmount().movePointRight(2).intValueExact());
        amount.setCurrency("CNY");
        request.setAmount(amount);
        try {
            String codeUrl = service().prepay(request).getCodeUrl();
            if (!hasText(codeUrl)) {
                throw new BizException("微信支付下单失败：未返回 code_url");
            }
            return codeUrl;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信支付下单异常 orderNo={}", order.getOrderNo(), e);
            throw new BizException("微信支付下单失败：" + e.getMessage());
        }
    }

    public QueryResult query(String orderNo) {
        if (!enabled()) {
            return QueryResult.notPaid("WECHAT_DISABLED");
        }
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(properties.getWechat().getMchId());
        request.setOutTradeNo(orderNo);
        try {
            Transaction transaction = service().queryOrderByOutTradeNo(request);
            boolean paid = Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState());
            return new QueryResult(paid, transaction.getTransactionId(),
                    transaction.getTradeState() == null ? null : transaction.getTradeState().name());
        } catch (Exception e) {
            log.warn("微信支付查单失败 orderNo={}: {}", orderNo, e.getMessage());
            return QueryResult.notPaid(e.getMessage());
        }
    }

    /**
     * 回调验签 + 解密，返回微信支付订单对象
     */
    public Transaction parseNotify(HttpHeaders headers, String body) {
        String serial = headers.getFirst("Wechatpay-Serial");
        String nonce = headers.getFirst("Wechatpay-Nonce");
        String signature = headers.getFirst("Wechatpay-Signature");
        String timestamp = headers.getFirst("Wechatpay-Timestamp");
        if (!hasText(serial) || !hasText(nonce) || !hasText(signature) || !hasText(timestamp)) {
            throw new BizException("微信支付回调缺少签名头");
        }
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(serial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(body)
                .build();
        try {
            return parser().parse(requestParam, Transaction.class);
        } catch (Exception e) {
            log.warn("微信支付回调验签/解密失败: {}", e.getMessage());
            throw new BizException("微信支付回调验签失败");
        }
    }

    private NativePayService service() {
        NativePayService local = nativePayService;
        if (local == null) {
            synchronized (this) {
                local = nativePayService;
                if (local == null) {
                    local = new NativePayService.Builder().config(config()).build();
                    nativePayService = local;
                }
            }
        }
        return local;
    }

    private NotificationParser parser() {
        NotificationParser local = notificationParser;
        if (local == null) {
            synchronized (this) {
                local = notificationParser;
                if (local == null) {
                    local = new NotificationParser(config());
                    notificationParser = local;
                }
            }
        }
        return local;
    }

    private RSAAutoCertificateConfig config() {
        RSAAutoCertificateConfig local = config;
        if (local == null) {
            synchronized (this) {
                local = config;
                if (local == null) {
                    PaymentProperties.Wechat cfg = properties.getWechat();
                    RSAAutoCertificateConfig.Builder builder = new RSAAutoCertificateConfig.Builder()
                            .merchantId(cfg.getMchId())
                            .merchantSerialNumber(cfg.getMerchantSerialNumber())
                            .apiV3Key(cfg.getApiV3Key());
                    if (hasText(cfg.getPrivateKeyPath())) {
                        builder.privateKeyFromPath(cfg.getPrivateKeyPath());
                    } else {
                        builder.privateKey(cfg.getPrivateKey());
                    }
                    local = builder.build();
                    config = local;
                }
            }
        }
        return local;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record QueryResult(boolean paid, String transactionId, String state) {
        public static QueryResult notPaid(String state) {
            return new QueryResult(false, null, state);
        }
    }
}
