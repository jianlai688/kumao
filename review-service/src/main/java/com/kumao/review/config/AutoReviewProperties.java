package com.kumao.review.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "kumao.ai.auto-review")
public class AutoReviewProperties {

    /** 是否开启定时自动审核 */
    private boolean enabled = true;
    /** 调度间隔（毫秒） */
    private long intervalMs = 60_000;
    /** 内部调用网关地址 */
    private String gatewayUrl = "http://127.0.0.1:8080";
    /** 每次每类最多拉取条数 */
    private int pageSize = 30;
    /** 自动通过的最高风险分（AI 必须返回 PASS 且风险分低于该值） */
    private int passRiskThreshold = 35;
    /** 允许 AI 自动通过并落业务状态的分类；评论/弹幕在发送时同步审核，不进入定时扫描 */
    private List<String> autoApproveCategories = new ArrayList<>(List.of(
            "ANCHOR_APPLY", "MUSICIAN", "ADVERTISER", "AD_MATERIAL", "AD_PLAN",
            "SONG", "MV", "SINGER"));
    /** 单轮最多处理条数，避免一次性打满模型配额 */
    private int maxItemsPerRun = 60;
}
