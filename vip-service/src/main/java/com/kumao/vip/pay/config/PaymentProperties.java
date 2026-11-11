package com.kumao.vip.pay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付配置。
 * <p>
 * 支付宝默认指向沙箱网关，正式环境把 {@code kumao.pay.alipay.gateway-url}
 * 改为 https://openapi.alipay.com/gateway.do 即可。
 * <p>
 * 所有敏感信息均可通过环境变量注入，见 docs/支付接入说明.md。
 */
@Data
@Component
@ConfigurationProperties(prefix = "kumao.pay")
public class PaymentProperties {

    /** 渠道参数未配置时，是否允许降级为本地模拟支付（仅用于开发演示） */
    private boolean mockEnabled = true;

    /** 支付回调基础地址（必须是外网可访问的网关地址，本地联调可用内网穿透） */
    private String notifyBaseUrl = "http://127.0.0.1:8080";

    /** 订单支付超时时间（分钟） */
    private int orderTimeoutMinutes = 30;

    private Alipay alipay = new Alipay();

    private Wechat wechat = new Wechat();

    public String alipayNotifyUrl() {
        return trimSlash(notifyBaseUrl) + "/api/vip/vip/pay/alipay/notify";
    }

    public String wechatNotifyUrl() {
        String custom = wechat.getNotifyUrl();
        if (custom != null && !custom.isBlank()) {
            return custom.trim();
        }
        return trimSlash(notifyBaseUrl) + "/api/vip/vip/pay/wechat/notify";
    }

    private static String trimSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }

    @Data
    public static class Alipay {
        /** 是否启用真实支付宝支付（沙箱/生产共用一套代码） */
        private boolean enabled;
        /** 沙箱网关；生产改为 https://openapi.alipay.com/gateway.do */
        private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
        /** 沙箱应用 APPID */
        private String appId;
        /** 应用私钥（PKCS8，不带 BEGIN/END 头） */
        private String privateKey;
        /** 支付宝公钥（用于回调验签） */
        private String publicKey;
        /** 支付完成后浏览器回跳地址 */
        private String returnUrl = "http://127.0.0.1:5173/#/vip";
    }

    @Data
    public static class Wechat {
        /** 是否启用微信支付 APIv3（微信没有公开沙箱，需要真实商户号） */
        private boolean enabled;
        /** 公众号/小程序 APPID，Native 支付必填 */
        private String appId;
        /** 商户号 */
        private String mchId;
        /** 商户 API 证书序列号 */
        private String merchantSerialNumber;
        /** 商户 API 私钥内容（PEM），与 privateKeyPath 二选一 */
        private String privateKey;
        /** 商户 API 私钥文件路径（apiclient_key.pem） */
        private String privateKeyPath;
        /** APIv3 密钥（32 位） */
        private String apiV3Key;
        /** 可选：单独指定回调地址 */
        private String notifyUrl;
    }
}
