package com.kumao.ad.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaterialRequest {

    @NotBlank(message = "素材名称不能为空")
    private String name;
    /** AUDIO / VIDEO / IMAGE_TEXT */
    @NotBlank(message = "素材类型不能为空")
    private String type;
    private String coverUrl;
    private String fileUrl;
    private String content;
}
