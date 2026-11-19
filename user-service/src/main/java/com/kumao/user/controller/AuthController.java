package com.kumao.user.controller;

import com.kumao.common.constant.CommonConstants;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.common.security.JwtUtils;
import com.kumao.user.dto.LoginRequest;
import com.kumao.user.dto.PasswordResetRequest;
import com.kumao.user.dto.RegisterRequest;
import com.kumao.user.dto.TokenVO;
import com.kumao.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/sms-code")
    public R<Map<String, String>> sendSms(@RequestParam @NotBlank String phone) {
        String code = authService.sendSmsCode(phone);
        return R.ok(code == null ? Map.of("sent", "true") : Map.of("sent", "true", "mockCode", code));
    }

    @PostMapping("/register")
    public R<Long> register(@RequestBody @Valid RegisterRequest req) {
        return R.ok(authService.register(req));
    }

    @PostMapping("/login")
    public R<TokenVO> login(@RequestBody @Valid LoginRequest req, HttpServletRequest request) {
        return R.ok(authService.login(req, request));
    }

    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = CommonConstants.HEADER_DEVICE, required = false) String device,
                          HttpServletRequest request) {
        String token = JwtUtils.resolveToken(request.getHeader(CommonConstants.TOKEN_HEADER));
        authService.logout(UserContext.getUserId(), device, token);
        return R.ok();
    }

    @PostMapping("/kick-others")
    public R<Integer> kickOthers(@RequestHeader(value = CommonConstants.HEADER_DEVICE, required = false) String device) {
        return R.ok(authService.kickOthers(UserContext.requireUserId(), device));
    }

    /** 注销账号（需登录）：清除登录态并脱敏后逻辑删除 */
    @PostMapping("/cancel")
    public R<Void> cancel(HttpServletRequest request) {
        String token = JwtUtils.resolveToken(request.getHeader(CommonConstants.TOKEN_HEADER));
        authService.cancelAccount(UserContext.requireUserId(), token);
        return R.ok();
    }

    @PostMapping("/password/reset")
    public R<Void> resetPassword(@RequestBody @Valid PasswordResetRequest req) {
        authService.resetPassword(req.getPhone(), req.getSmsCode(), req.getNewPassword());
        return R.ok();
    }

    @PostMapping("/password/change")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        authService.changePassword(UserContext.requireUserId(), body.get("oldPassword"), body.get("newPassword"));
        return R.ok();
    }
}
