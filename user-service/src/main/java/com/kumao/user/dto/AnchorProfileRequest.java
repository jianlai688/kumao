package com.kumao.user.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 主播个人信息修改
 */
@Data
public class AnchorProfileRequest implements Serializable {

    private String anchorName;
    private String avatar;
    private String liveCategory;
    private String intro;
    private String coverUrl;
    private String payType;
    private String payAccount;
    private String payName;
}
