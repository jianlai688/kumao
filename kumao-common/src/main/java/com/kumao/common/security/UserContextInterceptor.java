package com.kumao.common.security;

import com.kumao.common.constant.CommonConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 微服务内部解析网关透传的用户身份
 */
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(CommonConstants.HEADER_USER_ID);
        if (userId != null && !userId.isBlank() && !"null".equals(userId)) {
            LoginUser user = new LoginUser();
            user.setUserId(Long.valueOf(userId));
            user.setUsername(request.getHeader(CommonConstants.HEADER_USERNAME));
            user.setRole(request.getHeader(CommonConstants.HEADER_ROLE));
            user.setDevice(request.getHeader(CommonConstants.HEADER_DEVICE));
            UserContext.set(user);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
