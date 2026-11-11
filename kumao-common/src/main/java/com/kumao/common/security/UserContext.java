package com.kumao.common.security;

import com.kumao.common.exception.BizException;

/**
 * 当前登录用户 ThreadLocal 持有器
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static Long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        return userId;
    }

    public static String getRole() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getRole();
    }

    public static boolean isAdmin() {
        LoginUser user = HOLDER.get();
        return user != null && user.isAdmin();
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new BizException(403, "无权限访问");
        }
    }

    public static void clear() {
        HOLDER.remove();
    }
}
