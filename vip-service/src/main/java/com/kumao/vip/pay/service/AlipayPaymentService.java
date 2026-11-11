package com.kumao.vip.pay.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.kumao.common.exception.BizException;
import com.kumao.vip.entity.VipOrder;
import com.kumao.vip.pay.config.PaymentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.Map;

/**
 * 支付宝电脑网站支付（沙箱 / 生产共用）。
 * <p>
 * 沙箱使用 https://openapi-sandbox.dl.alipaydev.com/gateway.do，
 * 配置沙箱 APPID、应用私钥、支付宝公钥即可。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayPaymentService {

    private final PaymentProperties properties;

    private volatile AlipayClient client;

    public boolean enabled() {
        PaymentProperties.Alipay cfg = properties.getAlipay();
        return cfg.isEnabled()
                && hasText(cfg.getAppId())
                && hasText(cfg.getPrivateKey())
                && hasText(cfg.getPublicKey())
                && hasText(cfg.getGatewayUrl());
    }

    /**
     * 生成电脑网站支付表单（自动提交的 HTML）
     */
    public String pageForm(VipOrder order) {
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(order.getOrderNo());
        model.setTotalAmount(order.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        model.setSubject(order.getRemark() != null && order.getRemark().startsWith("COIN_RECHARGE:")
                ? "酷猫币充值-" + order.getPackageName()
                : order.getRemark() != null && order.getRemark().startsWith("AD_PLAN:")
                ? order.getPackageName()
                : "酷猫音乐VIP-" + order.getPackageName());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        model.setTimeoutExpress(Math.max(1, properties.getOrderTimeoutMinutes()) + "m");

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(properties.alipayNotifyUrl());
        request.setReturnUrl(properties.getAlipay().getReturnUrl());
        request.setBizModel(model);

        try {
            AlipayTradePagePayResponse response = client().pageExecute(request);
            if (!response.isSuccess() || !hasText(response.getBody())) {
                throw new BizException("支付宝下单失败：" + safeMessage(response.getSubMsg(), response.getMsg()));
            }
            return response.getBody();
        } catch (AlipayApiException e) {
            log.error("支付宝下单异常 orderNo={}", order.getOrderNo(), e);
            throw new BizException("支付宝下单失败：" + e.getErrMsg());
        }
    }

    /**
     * 主动查单，用于回调无法到达本地时的兜底
     */
    public QueryResult query(String orderNo) {
        if (!enabled()) {
            return QueryResult.notPaid("ALIPAY_DISABLED");
        }
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(orderNo);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizModel(model);
        try {
            AlipayTradeQueryResponse response = client().execute(request);
            if (response.isSuccess()) {
                String tradeStatus = response.getTradeStatus();
                boolean paid = "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
                return new QueryResult(paid, response.getTradeNo(), tradeStatus);
            }
            return QueryResult.notPaid(response.getSubCode());
        } catch (AlipayApiException e) {
            log.warn("支付宝查单失败 orderNo={}: {}", orderNo, e.getErrMsg());
            return QueryResult.notPaid(e.getErrMsg());
        }
    }

    /**
     * 回调验签：支付宝公钥 + RSA2，并校验 APPID
     */
    public boolean verifyNotify(Map<String, String> params) {
        PaymentProperties.Alipay cfg = properties.getAlipay();
        if (!enabled()) {
            return false;
        }
        try {
            boolean signOk = AlipaySignature.rsaCheckV1(params, cfg.getPublicKey(), "UTF-8", "RSA2");
            if (!signOk) {
                log.warn("支付宝回调验签失败 orderNo={}", params.get("out_trade_no"));
                return false;
            }
            return cfg.getAppId().equals(params.get("app_id"));
        } catch (AlipayApiException e) {
            log.warn("支付宝回调验签异常: {}", e.getErrMsg());
            return false;
        }
    }

    private AlipayClient client() {
        AlipayClient local = client;
        if (local == null) {
            synchronized (this) {
                local = client;
                if (local == null) {
                    PaymentProperties.Alipay cfg = properties.getAlipay();
                    local = new DefaultAlipayClient(cfg.getGatewayUrl(), cfg.getAppId(), cfg.getPrivateKey(),
                            "json", "UTF-8", cfg.getPublicKey(), "RSA2");
                    client = local;
                }
            }
        }
        return local;
    }

    private static String safeMessage(String subMsg, String msg) {
        return hasText(subMsg) ? subMsg : msg;
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
