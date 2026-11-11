package com.kumao.review.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AiReviewRequest {

    /** ANCHOR_APPLY / MUSICIAN / ADVERTISER / AD_MATERIAL / AD_PLAN / SONG / MV / COMMENT / LIVE_ROOM / DANMAKU / SINGER */
    @NotBlank(message = "审核类型不能为空")
    private String category;

    private String itemId;
    private String title;
    private String content;
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
