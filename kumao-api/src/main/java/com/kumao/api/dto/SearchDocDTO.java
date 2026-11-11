package com.kumao.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * ES 索引文档：歌曲、歌手、歌单、直播间、音乐人五类对象统一结构
 */
@Data
public class SearchDocDTO implements Serializable {

    /** 业务主键 */
    private Long bizId;
    /** SONG / SINGER / PLAYLIST / LIVE_ROOM / MUSICIAN */
    private String type;
    private String title;
    private String subtitle;
    private String coverUrl;
    private String tags;
    private String description;
    private Long heat;
    private String createTime;
}
