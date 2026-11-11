package com.kumao.musician.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AlbumRequest {

    @NotBlank(message = "专辑名称不能为空")
    private String name;
    private String coverUrl;
    private String intro;
    private LocalDate releaseDate;
}
