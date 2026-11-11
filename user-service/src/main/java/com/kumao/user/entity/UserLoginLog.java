package com.kumao.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_login_log")
public class UserLoginLog extends BaseEntity {

    private Long userId;
    private String device;
    private String ip;
    private String userAgent;
    private String loginType;
    private String tokenId;
}
