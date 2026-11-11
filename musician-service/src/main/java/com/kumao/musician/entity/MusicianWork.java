package com.kumao.musician.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 音乐人作品（原创歌曲）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("musician_work")
public class MusicianWork extends BaseEntity {

    private Long musicianId;
    private Long userId;
    private String name;
    private Long albumId;
    private String albumName;
    private String coverUrl;
    private String audioUrl;
    private String lyricUrl;
    private Integer duration;
    private String language;
    private String style;
    private Boolean vipOnly;
    private LocalDate releaseDate;
    /** PENDING 待审核 / APPROVED 已通过 / REJECTED 未通过 / OFFLINE 已下架 */
    private String status;
    private String rejectReason;
    private Long songId;
    private Long playCount;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    private Long collectCount;
    private Integer deleted;
}
