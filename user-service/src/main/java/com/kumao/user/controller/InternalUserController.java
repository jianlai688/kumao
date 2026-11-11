package com.kumao.user.controller;

import com.kumao.api.dto.UserDTO;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.R;
import com.kumao.user.entity.SysUser;
import com.kumao.user.mapper.SysUserMapper;
import com.kumao.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 微服务内部接口
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final SysUserMapper userMapper;
    private final UserService userService;

    @GetMapping("/{id}")
    public R<UserDTO> get(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setRoles(user.getRoles());
        dto.setStatus(user.getStatus());
        return R.ok(dto);
    }

    @PostMapping("/{id}/roles")
    public R<Void> grantRole(@PathVariable Long id, @RequestParam String role) {
        userService.grantRole(id, role);
        return R.ok();
    }

    @DeleteMapping("/{id}/roles")
    public R<Void> revokeRole(@PathVariable Long id, @RequestParam String role) {
        userService.revokeRole(id, role);
        return R.ok();
    }

    @GetMapping("/{id}/muted")
    public R<Boolean> muted(@PathVariable Long id) {
        return R.ok(userService.isMuted(id));
    }

    @PostMapping("/{id}/mute")
    public R<Void> mute(@PathVariable Long id,
                        @RequestParam(defaultValue = "60") int minutes) {
        userService.mute(id, minutes);
        return R.ok();
    }
}
