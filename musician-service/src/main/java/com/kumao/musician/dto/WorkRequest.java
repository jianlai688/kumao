package com.kumao.musician.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WorkRequest {

    @NotBlank(message = "歌曲名称不能为空")
    private String name;
    private Long albumId;
    private String albumName;
    private String coverUrl;
    @NotBlank(message = "音频地址不能为空")
    private String audioUrl;
    private String lyricUrl;
    private Integer duration;
    private String language;
    private String style;
    private Boolean vipOnly;
    /** 单曲价格（分），0 表示免费 */
    private Integer price;
    private LocalDate releaseDate;
}
