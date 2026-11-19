package com.kumao.ad.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdvertiserApplyRequest {

    @NotBlank(message = "艺名/主体名称不能为空")
    private String companyName;
    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    @NotBlank(message = "身份证号不能为空")
    private String idCard;
    @NotBlank(message = "身份证正面照片不能为空")
    private String idCardFrontUrl;
    @NotBlank(message = "身份证反面照片不能为空")
    private String idCardBackUrl;
    @NotBlank(message = "广告类型不能为空")
    private String adType;
    private String intro;
    private String avatar;
    private String representative;
}
