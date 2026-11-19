package com.kumao.comment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("song_comment")
public class SongComment extends BaseEntity {

    /** 雪花 songId 超出 JS 安全整数范围，必须序列化为字符串 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long songId;
    private Long userId;
    private String nickname;
    private String avatar;
    private String content;
    private Long parentId;
    private Long replyUserId;
    private String replyNickname;
    private Integer likeCount;
    /** 0 已通过/正常 1 已驳回/屏蔽 2 待管理员复核 */
    private Integer status;
    /** PASS / REVIEW / REJECT */
    private String aiDecision;
    private Integer aiRiskScore;
    /** AI_MODEL / LOCAL_RULE / UNAVAILABLE */
    private String aiSource;
    private String aiReasons;
    private Integer deleted;
}
