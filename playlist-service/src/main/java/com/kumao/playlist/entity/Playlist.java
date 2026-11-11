package com.kumao.playlist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("playlist")
public class Playlist extends BaseEntity {

    private String name;
    private String coverUrl;
    private String description;
    private Long userId;
    private String creatorName;
    /** 0 私有 1 公开 */
    private Integer isPublic;
    private Integer songCount;
    private Long playCount;
    private Long collectCount;
    private Integer status;
    private Integer deleted;
}
