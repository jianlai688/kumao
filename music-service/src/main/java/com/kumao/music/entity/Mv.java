package com.kumao.music.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mv")
public class Mv extends BaseEntity {

    /** 关联歌曲 ID，用于歌曲播放页切换 MV */
    private Long songId;
    private String name;
    private Long singerId;
    private String singerName;
    private Long musicianId;
    private Long uploaderUserId;
    private String coverUrl;
    private String videoUrl;
    private Integer duration;
    /** SD / HD / UHD */
    private String quality;
    /** 是否 VIP 专属高清 */
    private Boolean vipOnly;
    private Integer status;
    /** PENDING / APPROVED / REJECTED */
    private String auditStatus;
    private String rejectReason;
    private String auditTime;
    private Long playCount;
    private String description;
    private Integer deleted;
}
