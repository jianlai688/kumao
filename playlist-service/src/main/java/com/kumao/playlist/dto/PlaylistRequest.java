package com.kumao.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlaylistRequest {

    @NotBlank(message = "歌单名称不能为空")
    private String name;
    private String coverUrl;
    private String description;
    /** 0 私有 1 公开 */
    private Integer isPublic;
    /** 兼容 App 端旧字段名，等价于 isPublic */
    private Integer publicFlag;
}
