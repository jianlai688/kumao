package com.kumao.ad.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 广告主个人信息修改
 */
@Data
public class AdvertiserProfileRequest implements Serializable {

    private String companyName;
    private String realName;
    private String avatar;
    private String adType;
    private String intro;
    private String representative;
}
