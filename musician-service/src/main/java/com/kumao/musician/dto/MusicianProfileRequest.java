package com.kumao.musician.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 音乐人个人信息修改
 */
@Data
public class MusicianProfileRequest implements Serializable {

    private String stageName;
    private String avatar;
    private String style;
    private String intro;
    private String representative;
}
