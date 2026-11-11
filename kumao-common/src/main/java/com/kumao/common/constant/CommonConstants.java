package com.kumao.common.constant;

/**
 * 全局常量
 */
public interface CommonConstants {

    /** JWT 请求头 */
    String TOKEN_HEADER = "Authorization";
    String TOKEN_PREFIX = "Bearer ";

    /** 网关向下游透传的请求头 */
    String HEADER_USER_ID = "X-User-Id";
    String HEADER_USERNAME = "X-Username";
    String HEADER_ROLE = "X-Role";
    String HEADER_DEVICE = "X-Device";

    /** 角色 */
    String ROLE_USER = "USER";
    String ROLE_ANCHOR = "ANCHOR";
    String ROLE_MUSICIAN = "MUSICIAN";
    String ROLE_VIP = "VIP";
    String ROLE_ADVERTISER = "ADVERTISER";
    String ROLE_ADMIN = "ADMIN";

    /** 客户端类型 */
    String DEVICE_PC = "PC";
    String DEVICE_APP = "APP";
    String DEVICE_DESKTOP = "DESKTOP";
    String DEVICE_ADMIN = "ADMIN";

    /** 登录策略 */
    String LOGIN_POLICY_SHARED = "SHARED";
    String LOGIN_POLICY_EXCLUSIVE = "EXCLUSIVE";

    int CODE_UNAUTHORIZED = 401;
    int CODE_FORBIDDEN = 403;
}
