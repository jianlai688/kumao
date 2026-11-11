package com.kumao.review.service;

import com.kumao.review.dto.AiReviewRequest;
import com.kumao.review.dto.AiReviewResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 可解释的本地规则兜底。
 *
 * <p>它只负责在审核模型未配置、超时、限流或返回无法解析时给出可复核的建议，
 * 不执行任何审核动作。规则故意保持保守：命中风险词或资料异常时提升复核优先级，
 * 不会因为单条低置信规则直接替管理员做最终决定。</p>
 */
@Component
public class LocalReviewEngine {

    private static final Map<String, Integer> RISK_WORDS = new LinkedHashMap<>();

    static {
        RISK_WORDS.put("诈骗", 35);
        RISK_WORDS.put("赌博", 30);
        RISK_WORDS.put("色情", 35);
        RISK_WORDS.put("毒品", 40);
        RISK_WORDS.put("枪支", 40);
        RISK_WORDS.put("代考", 30);
        RISK_WORDS.put("刷单", 25);
        RISK_WORDS.put("加微信", 20);
        RISK_WORDS.put("加v", 20);
        RISK_WORDS.put("政治谣言", 40);
        RISK_WORDS.put("裸聊", 40);
        RISK_WORDS.put("私聊", 12);
        RISK_WORDS.put("返利", 18);
        RISK_WORDS.put("贷款", 15);
        RISK_WORDS.put("暴力", 20);
        RISK_WORDS.put("辱骂", 15);
    }

    public AiReviewResult review(AiReviewRequest request, String fallbackReason) {
        long start = System.currentTimeMillis();
        String text = joinText(request);
        String lower = text.toLowerCase();
        List<String> signals = new ArrayList<>();
        LinkedHashSet<String> hits = new LinkedHashSet<>();

        int risk = 8;
        for (Map.Entry<String, Integer> entry : RISK_WORDS.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase())) {
                hits.add(entry.getKey());
                risk += entry.getValue();
            }
        }
        if (!hits.isEmpty()) {
            signals.add("命中风险词：" + String.join("、", hits));
        }

        int categoryRisk = categorySignals(request, lower, signals);
        risk += categoryRisk;

        String reasons;
        if (signals.isEmpty()) {
            reasons = "未命中本地风险词或资料完整性规则；仍需结合原件、账号历史和上下文人工确认。";
        } else {
            reasons = "本地规则建议复核：" + String.join("；", signals) + "。";
        }
        risk = Math.max(0, Math.min(99, risk));
        String decision = risk >= 70 ? "REJECT" : risk >= 35 ? "REVIEW" : "PASS";
        return AiReviewResult.local(decision, risk, reasons, signals, fallbackReason,
                System.currentTimeMillis() - start);
    }

    private int categorySignals(AiReviewRequest request, String lower, List<String> signals) {
        String category = request.getCategory() == null ? "" : request.getCategory();
        String content = request.getContent() == null ? "" : request.getContent();
        int extra = 0;

        if (("COMMENT".equals(category) || "DANMAKU".equals(category))
                && (lower.contains("http://") || lower.contains("https://")
                || lower.contains("www.") || lower.contains("加我"))) {
            extra += 15;
            signals.add("疑似站外导流或联系方式");
        }
        if (("COMMENT".equals(category) || "DANMAKU".equals(category))
                && lower.matches(".*1[3-9]\\d{9}.*")) {
            extra += 18;
            signals.add("疑似包含手机号");
        }
        if (("COMMENT".equals(category) || "DANMAKU".equals(category))
                && content.matches(".*(.)\\1{5,}.*")) {
            extra += 8;
            signals.add("存在重复刷屏字符");
        }
        if (("COMMENT".equals(category) || "DANMAKU".equals(category)) && content.length() > 100) {
            extra += 8;
            signals.add("文本超出常规长度");
        }

        if ("AD_MATERIAL".equals(category)) {
            if (lower.contains("保证") || lower.contains("最有效") || lower.contains("第一")
                    || lower.contains("绝对")) {
                extra += 12;
                signals.add("广告文案含绝对化用语");
            }
            if (lower.contains("治疗") || lower.contains("收益") || lower.contains("投资")) {
                extra += 15;
                signals.add("含医疗/金融效果承诺风险");
            }
            if (lower.contains("http://") || lower.contains("https://")) {
                extra += 10;
                signals.add("素材含外部链接");
            }
        }

        if ("AD_PLAN".equals(category)) {
            Object payment = request.getMetadata() == null ? null : request.getMetadata().get("paymentStatus");
            if (payment != null && !"PAID".equals(String.valueOf(payment))) {
                extra += 20;
                signals.add("投放计划尚未完成付费");
            }
        }

        if ("LIVE_ROOM".equals(category)) {
            Object banned = request.getMetadata() == null ? null : request.getMetadata().get("banned");
            if (banned != null && ("1".equals(String.valueOf(banned)) || "true".equalsIgnoreCase(String.valueOf(banned)))) {
                extra += 55;
                signals.add("直播间已被封禁，需重点复核");
            }
            if (!StringUtils.hasText(request.getContent())) {
                extra += 8;
                signals.add("直播间简介为空");
            }
        }

        if ("SONG".equals(category) || "MV".equals(category)) {
            Object media = request.getMetadata() == null ? null : request.getMetadata().get("mediaUrl");
            if (media == null || !StringUtils.hasText(String.valueOf(media))) {
                extra += 22;
                signals.add("缺少可播放的音频或视频地址");
            }
            Object cover = request.getMetadata() == null ? null : request.getMetadata().get("coverUrl");
            if (cover == null || !StringUtils.hasText(String.valueOf(cover))) {
                extra += 6;
                signals.add("缺少封面图");
            }
        }

        if ("ANCHOR_APPLY".equals(category) && !StringUtils.hasText(request.getContent())) {
            extra += 10;
            signals.add("主播申请资料描述为空");
        }
        if (("MUSICIAN".equals(category) || "SINGER".equals(category))
                && !StringUtils.hasText(request.getContent())) {
            extra += 8;
            signals.add("资质简介为空");
        }
        if ("ADVERTISER".equals(category) && !StringUtils.hasText(request.getContent())) {
            extra += 8;
            signals.add("广告制作人简介为空");
        }
        return extra;
    }

    private String joinText(AiReviewRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.getTitle() != null) sb.append(request.getTitle()).append(' ');
        if (request.getContent() != null) sb.append(request.getContent()).append(' ');
        if (request.getMetadata() != null) sb.append(request.getMetadata());
        return sb.toString().trim();
    }

    /** 对发给外部模型的文本做轻量脱敏，避免把完整证件号、手机号直接送出。 */
    public static String maskSensitive(String value) {
        if (value == null) return null;
        String masked = value.replaceAll("\\b\\d{17}[\\dXx]\\b", "******************");
        masked = masked.replaceAll("\\b1[3-9]\\d{9}\\b", "***********");
        return masked;
    }
}
