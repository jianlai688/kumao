package com.kumao.music.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 榜单项：歌曲 + 排名 + 趋势
 */
@Data
public class RankItemVO implements Serializable {

    private Integer rank;
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
