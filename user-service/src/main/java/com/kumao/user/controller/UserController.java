package com.kumao.user.controller;

import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.user.dto.AnchorApplyRequest;
import com.kumao.user.dto.ProfileUpdateRequest;
import com.kumao.user.dto.UserVO;
import com.kumao.user.entity.AnchorApply;
import com.kumao.user.entity.UserLoginLog;
import com.kumao.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public R<UserVO> me() {
        return R.ok(userService.profile(UserContext.requireUserId()));
    }

    @PutMapping("/me")
    public R<Void> updateMe(@RequestBody @Valid ProfileUpdateRequest req) {
        userService.updateProfile(UserContext.requireUserId(), req);
        return R.ok();
    }

    @GetMapping("/me/login-logs")
    public R<PageResult<UserLoginLog>> myLoginLogs(PageQuery query) {
        return R.ok(userService.pageLoginLogs(query, UserContext.requireUserId()));
    }

    // ---------------- 主播申请 ----------------

    @PostMapping("/anchor/apply")
    public R<Long> applyAnchor(@RequestBody @Valid AnchorApplyRequest req) {
        return R.ok(userService.submitAnchorApply(UserContext.requireUserId(), req));
    }

    @GetMapping("/anchor/my")
    public R<List<AnchorApply>> myAnchorApply() {
        return R.ok(userService.myAnchorApplies(UserContext.requireUserId()));
    }

    // ---------------- 后台 ----------------

    @GetMapping("/admin/page")
    public R<PageResult<UserVO>> page(PageQuery query,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(required = false) String role) {
        UserContext.requireAdmin();
        return R.ok(userService.pageUsers(query, status, role));
    }

    @PutMapping("/admin/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        UserContext.requireAdmin();
        userService.updateStatus(id, status);
        return R.ok();
    }

    @GetMapping("/admin/login-logs")
    public R<PageResult<UserLoginLog>> loginLogs(PageQuery query, @RequestParam(required = false) Long userId) {
        UserContext.requireAdmin();
        return R.ok(userService.pageLoginLogs(query, userId));
    }

    @GetMapping("/admin/anchor-applies")
    public R<PageResult<AnchorApply>> anchorApplies(PageQuery query, @RequestParam(required = false) String status) {
        UserContext.requireAdmin();
        return R.ok(userService.pageAnchorApplies(query, status));
    }

    @PostMapping("/admin/anchor-applies/{id}/audit")
    public R<Void> audit(@PathVariable Long id,
                         @RequestParam boolean approved,
                         @RequestParam(required = false) String reason) {
        UserContext.requireAdmin();
        userService.auditAnchorApply(id, approved, reason, UserContext.getUserId());
        return R.ok();
    }

    @GetMapping("/admin/statistics")
    public R<Map<String, Object>> statistics() {
        UserContext.requireAdmin();
        return R.ok(userService.statistics());
    }

    @PutMapping("/admin/{id}/roles")
    public R<Void> grantRole(@PathVariable Long id, @RequestParam String role) {
        UserContext.requireAdmin();
        userService.grantRole(id, role);
        return R.ok();
    }
}
