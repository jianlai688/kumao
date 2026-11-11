package com.kumao.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlaylistRequest {

    @NotBlank(message = "歌单名称不能为空")
    private String name;
    private String coverUrl;
    private String description;
    private Integer isPublic = 1;
}
