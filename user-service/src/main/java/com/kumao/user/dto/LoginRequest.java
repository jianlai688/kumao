package com.kumao.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    /** PASSWORD / SMS / OAUTH / ONE_CLICK */
    @NotBlank(message = "登录方式不能为空")
    private String type;

    private String username;
    private String password;
    private String phone;
    private String smsCode;

    /** QQ / WECHAT */
    private String thirdType;
    private String thirdOpenId;
    private String nickname;
    private String avatar;

    /** 运营商一键登录 token（仅 APP） */
    private String carrierToken;

    /** PC / APP / DESKTOP / ADMIN */
    @NotBlank(message = "设备类型不能为空")
    private String device;
}
