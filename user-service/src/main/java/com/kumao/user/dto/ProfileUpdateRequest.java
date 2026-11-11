package com.kumao.user.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {

    private String nickname;
    private String avatar;
    private String intro;
    /** 0 未知 1 男 2 女 */
    private Integer gender;
    /** 生日 yyyy-MM-dd */
    private String birthday;
    private String region;
    private String email;
    /** 兼容旧前端：设备类型（可不传） */
    private String device;
}

