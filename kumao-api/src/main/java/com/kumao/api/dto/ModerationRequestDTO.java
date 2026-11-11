package com.kumao.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 评论 / 弹幕等实时内容的 AI 审核请求。
 */
@Data
public class ModerationRequestDTO implements Serializable {

    /** COMMENT / DANMAKU / LIVE_ROOM */
    private String category;
    private String itemId;
    private String title;
    private String content;
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
