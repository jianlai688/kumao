package com.kumao.music.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SongRequest {

    @NotBlank(message = "歌曲名不能为空")
    private String name;
    private Long singerId;
    private String singerName;
    private Long albumId;
    private String albumName;
    private String coverUrl;
    private String audioUrl;
    private String lyricUrl;
    private Integer duration;
    private String language;
    private String style;
    private String region;
    private Boolean vipOnly;
    private Integer status;
    private LocalDate releaseDate;
    private Long musicianId;
}
