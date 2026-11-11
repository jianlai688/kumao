package com.kumao.live.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DanmakuMessage implements Serializable {

    private Long roomId;
    private Long userId;
    private String nickname;
    private String content;
    private String color;
    private Boolean vip;
    private String type = "TEXT";
    private Long giftId;
    private Integer giftCount;
    private Long onlineCount;
    private Long totalViewers;
    private Long peakOnline;
    private Long likeCount;
    private Long giftIncome;
    private String roomStatus;
    private String pullUrl;
    private Long timestamp;

    public static DanmakuMessage system(Long roomId, String content) {
        DanmakuMessage msg = new DanmakuMessage();
        msg.setRoomId(roomId);
        msg.setContent(content);
        msg.setType("SYSTEM");
        msg.setNickname("系统");
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }
}
