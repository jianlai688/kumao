package com.kumao.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String phone;
    private String nickname;
    private String avatar;
    private String intro;
    /** 0 未知 1 男 2 女 */
    private Integer gender;
    private java.time.LocalDate birthday;
    private String region;
    private String email;
    /** USER,ANCHOR,MUSICIAN,VIP,ADVERTISER,ADMIN 逗号分隔 */
    private String roles;
    /** 0 正常 1 禁用 */
    private Integer status;
    private String thirdType;
    private String thirdOpenId;
    /** 登录策略 SHARED / EXCLUSIVE */
    private String loginPolicy;
    private Integer deleted;
    private LocalDateTime lastLoginTime;
    /** 违规禁言截止时间，null 或早于当前时间表示未禁言 */
    private LocalDateTime mutedUntil;
}

