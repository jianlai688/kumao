package com.kumao.musician.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MusicianApplyRequest {

    @NotBlank(message = "艺名不能为空")
    private String stageName;
    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    @NotBlank(message = "身份证号不能为空")
    private String idCard;
    @NotBlank(message = "身份证正面照片不能为空")
    private String idCardFrontUrl;
    @NotBlank(message = "身份证反面照片不能为空")
    private String idCardBackUrl;
    @NotBlank(message = "音乐风格不能为空")
    private String style;
    private String intro;
    private String avatar;
    private String representative;
}
