package com.kumao.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnchorApplyRequest {

    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    private String idCard;
    @NotBlank(message = "直播分类不能为空")
    private String liveCategory;
    private String intro;
    private String coverUrl;
}
