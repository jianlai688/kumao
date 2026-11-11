package com.kumao.user.dto;

import com.kumao.user.entity.SysUser;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserVO implements Serializable {

    private Long id;
    private String username;
    private String phone;
    private String nickname;
    private String avatar;
    private String intro;
    private String roles;
    private Integer status;
    private String lastLoginTime;
    private Integer gender;
    private String birthday;
    private String region;
    private String email;
    private String createTime;

    public static UserVO from(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone() == null ? null : user.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setIntro(user.getIntro());
        vo.setRoles(user.getRoles());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime() == null ? null : user.getLastLoginTime().toString().replace('T', ' '));
        vo.setGender(user.getGender() == null ? 0 : user.getGender());
        vo.setBirthday(user.getBirthday() == null ? null : user.getBirthday().toString());
        vo.setRegion(user.getRegion());
        vo.setEmail(user.getEmail());
        vo.setCreateTime(user.getCreateTime() == null ? null : user.getCreateTime().toString().replace('T', ' '));
        return vo;
    }
}

