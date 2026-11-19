package com.kumao.music.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;

/**
 * 榜单项：歌曲 + 排名 + 趋势
 */
@Data
public class RankItemVO implements Serializable {

    private Integer rank;
    /** 雪花 songId 超出 JS 安全整数范围，必须序列化为字符串 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long songId;
    private String name;
    private String singerName;
    private String coverUrl;
    private String audioUrl;
    private Integer duration;
    private Long playCount;
    private String trend;
    private Boolean vipOnly;
}
