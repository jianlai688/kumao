package com.kumao.comment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("play_record")
public class PlayRecord extends BaseEntity {

    private Long userId;
    private Long songId;
    private String songName;
    private String singerName;
    private String coverUrl;
    private String device;
    private Integer duration;
    private Integer playedSeconds;
}
