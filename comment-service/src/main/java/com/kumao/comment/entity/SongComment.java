package com.kumao.comment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("song_comment")
public class SongComment extends BaseEntity {

    private Long songId;
    private Long userId;
    private String nickname;
    private String avatar;
    private String content;
    private Long parentId;
    private Long replyUserId;
    private String replyNickname;
    private Integer likeCount;
    /** 0 正常 1 已屏蔽 */
    private Integer status;
    private Integer deleted;
}
