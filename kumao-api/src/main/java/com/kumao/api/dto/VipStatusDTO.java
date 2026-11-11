package com.kumao.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class VipStatusDTO implements Serializable {
    private Long userId;
    private boolean vip;
    private String level;
    private String expireTime;
    private String danmakuColor;
    private String entranceEffect;
}
