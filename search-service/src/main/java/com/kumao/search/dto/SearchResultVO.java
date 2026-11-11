package com.kumao.search.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResultVO implements Serializable {

    private long total;
    private String keyword;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item implements Serializable {
        /**
         * 雪花 ID 超出 JS Number 安全整数范围，必须按字符串返回，
         * 否则前端拿到的 bizId 会被四舍五入，跳转详情时查不到对应数据。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long bizId;
        private String type;
        /** 高亮标题（含 <em> 标签） */
        private String title;
        private String subtitle;
        private String coverUrl;
        private String tags;
        private String description;
        private Long heat;
    }
}
