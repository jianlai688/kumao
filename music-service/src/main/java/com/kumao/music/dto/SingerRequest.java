package com.kumao.music.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SingerRequest {

    @NotBlank(message = "歌手名不能为空")
    private String name;
    private String avatar;
    private String gender;
    private String region;
    private String intro;
    private Integer status;
}
