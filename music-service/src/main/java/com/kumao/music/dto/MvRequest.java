package com.kumao.music.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MvRequest {

    /** 关联歌曲 ID（可选） */
    private Long songId;
    @NotBlank(message = "MV 名称不能为空")
    private String name;
    private Long singerId;
    private String singerName;
    private String coverUrl;
    @NotBlank(message = "视频地址不能为空")
    private String videoUrl;
    private Integer duration;
    private String quality;
    private Boolean vipOnly;
    private Integer status;
    private String description;
}
