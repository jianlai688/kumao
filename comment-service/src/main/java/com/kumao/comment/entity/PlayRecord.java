package com.kumao.comment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("play_record")
public class PlayRecord extends BaseEntity {

    private Long userId;
    /** 雪花 songId 超出 JS 安全整数范围，必须序列化为字符串 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long songId;
    private String songName;
    private String singerName;
    private String coverUrl;
    private String device;
    private Integer duration;
    private Integer playedSeconds;
}
