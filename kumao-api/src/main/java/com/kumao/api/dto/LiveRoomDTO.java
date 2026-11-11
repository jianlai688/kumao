package com.kumao.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class LiveRoomDTO implements Serializable {
    private Long id;
    private String title;
    private Long anchorId;
    private String anchorName;
    private String coverUrl;
    private String category;
    private String status;
    private Long onlineCount;
    private String pullUrl;
}
