package com.kumao.music.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("song")
public class Song extends BaseEntity {

    private String name;
    private Long singerId;
    private String singerName;
    private Long albumId;
    private String albumName;
    private String coverUrl;
    private String audioUrl;
    private String lyricUrl;
    private Integer duration;
    /** 国语 / 粤语 / 英语 / 日语 / 韩语 */
    private String language;
    /** 流行 / 摇滚 / 民谣 / 电子 / 说唱 / 古风 */
    private String style;
    /** 国内 / 国外 / 粤语 */
    private String region;
    private Boolean vipOnly;
    /** 0 待审核 1 已上架 2 已下架 */
    private Integer status;
    private Long playCount;
    private Long likeCount;
    private Long commentCount;
    private LocalDate releaseDate;
    private Long musicianId;
    private Integer deleted;
}
