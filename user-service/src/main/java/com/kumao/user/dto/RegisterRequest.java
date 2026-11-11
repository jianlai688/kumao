package com.kumao.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    /** PHONE 手机号注册 / PASSWORD 账号密码注册 */
    @NotBlank(message = "注册方式不能为空")
    private String type;

    @Size(min = 4, max = 32, message = "用户名长度需在4-32位之间")
    private String username;

    @Size(min = 6, max = 32, message = "密码长度需在6-32位之间")
    private String password;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String smsCode;
}
