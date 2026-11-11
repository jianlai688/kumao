package com.kumao.music.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SingerApplyRequest {

    @NotBlank(message = "请选择歌手地区")
    private String region;

    private String gender;

    @Size(max = 1000, message = "歌手简介不能超过 1000 字")
    private String intro;
}
