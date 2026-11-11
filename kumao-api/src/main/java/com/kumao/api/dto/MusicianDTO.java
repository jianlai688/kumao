package com.kumao.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 音乐人基础资料，供 music-service 校验歌手认证资格。
 */
@Data
public class MusicianDTO implements Serializable {

    private Long id;
    private Long userId;
    private String stageName;
    private String avatar;
    private String intro;
    private String style;
    /** PENDING / APPROVED / REJECTED / REVOKED */
    private String status;
}
