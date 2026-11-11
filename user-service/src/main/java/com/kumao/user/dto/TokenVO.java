package com.kumao.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO implements Serializable {

    private String token;
    private String tokenType = "Bearer";
    private long expireSeconds;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String roles;
    private String device;

    public TokenVO(String token, long expireSeconds, Long userId, String username,
                   String nickname, String avatar, String roles, String device) {
        this.token = token;
        this.expireSeconds = expireSeconds;
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.avatar = avatar;
        this.roles = roles;
        this.device = device;
    }
}
