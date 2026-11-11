package com.kumao.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserDTO implements Serializable {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String roles;
    private Integer status;
}
