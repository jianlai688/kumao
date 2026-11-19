package com.kumao.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 歌曲信息（跨服务传输）
 */
@Data
public class SongDTO implements Serializable {

    private Long id;
    private String name;
    private Long singerId;
    private String singerName;
    private String albumName;
    private String coverUrl;
    private String audioUrl;
    private String lyricUrl;
    private Integer duration;
    private String language;
    private String style;
    private String region;
    private Boolean vipOnly;
    /** 单曲价格（分），0 表示免费 */
    private Integer price;
    private Integer status;
    private Long playCount;
    private String releaseDate;
    private Long musicianId;
}
