package com.kumao.common.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录用户上下文（由网关解析 Token 后经请求头透传）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private Long userId;
    private String username;
    private String role;
    private String device;

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * role 支持逗号分隔的多角色，例如 USER,MUSICIAN
     */
    public boolean hasRole(String... roles) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String[] owned = role.split(",");
        for (String o : owned) {
            for (String r : roles) {
                if (r.equalsIgnoreCase(o.trim())) {
                    return true;
                }
            }
        }
        return false;
    }
}
