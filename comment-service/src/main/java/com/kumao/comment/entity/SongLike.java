package com.kumao.comment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 我喜欢（红心歌曲）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("song_like")
public class SongLike extends BaseEntity {

    private Long userId;
    private Long songId;
    private String songName;
    private String singerName;
    private String coverUrl;
    private String audioUrl;
    private Integer duration;
}
