package com.kumao.live.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("live_danmaku")
public class LiveDanmaku extends BaseEntity {

    private Long roomId;
    private Long userId;
    private String nickname;
    private String content;
    private String color;
    private Boolean vip;
    /** TEXT / GIFT / SYSTEM / ENTRANCE */
    private String type;
}
