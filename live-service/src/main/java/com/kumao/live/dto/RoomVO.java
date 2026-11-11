package com.kumao.live.dto;

import com.kumao.live.entity.LiveRoom;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoomVO extends LiveRoom {

    /** 实时在线人数（Redis，仅登录用户且去重） */
    private Long realtimeOnline;
    /** 本场累计观看人数（Redis，仅登录用户且去重） */
    private Long totalViewers;
    private Boolean vip;
    private String danmakuColor;
    private String entranceEffect;
}
