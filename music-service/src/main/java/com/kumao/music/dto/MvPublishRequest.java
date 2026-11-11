package com.kumao.music.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MvPublishRequest {

    /** 关联歌曲 ID（可选） */
    private Long songId;
    @NotBlank(message = "MV 名称不能为空")
    @Size(max = 200, message = "MV 名称不能超过 200 字")
    private String name;
    private String coverUrl;
    @NotBlank(message = "请上传 MV 视频或填写视频地址")
    private String videoUrl;
    private Integer duration;
    private String quality;
    private Boolean vipOnly;
    @Size(max = 1000, message = "MV 描述不能超过 1000 字")
    private String description;
}
