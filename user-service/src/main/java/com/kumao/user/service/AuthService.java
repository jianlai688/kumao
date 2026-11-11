package com.kumao.user.service;

import com.kumao.common.constant.CommonConstants;
import com.kumao.common.exception.BizException;
import com.kumao.common.security.JwtUtils;
import com.kumao.user.dto.LoginRequest;
import com.kumao.user.dto.RegisterRequest;
import com.kumao.user.dto.TokenVO;
import com.kumao.user.entity.SysUser;
import com.kumao.user.entity.UserLoginLog;
import com.kumao.user.mapper.SysUserMapper;
import com.kumao.user.mapper.UserLoginLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 认证与账号服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String SMS_CODE_KEY = "kumao:sms:code:";
    private static final String SMS_SEND_KEY = "kumao:sms:send:";
    private static final String LOGIN_TOKEN_KEY = "kumao:login:token:";
    private static final String BLACKLIST_KEY = "kumao:login:blacklist:";

    private final SysUserMapper userMapper;
    private final UserLoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    @Value("${kumao.sms.mock:true}")
    private boolean smsMock;
    @Value("${kumao.sms.mock-code:123456}")
    private String smsMockCode;

    /**
     * 发送短信验证码（演示环境固定 123456，同时写入 Redis）
     */
    public String sendSmsCode(String phone) {
        if (!StringUtils.hasText(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException("手机号格式不正确");
        }
        String limitKey = SMS_SEND_KEY + phone;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(limitKey, "1", Duration.ofSeconds(60));
        if (Boolean.FALSE.equals(first)) {
            throw new BizException("验证码发送过于频繁，请稍后再试");
        }
        String code = smsMock ? smsMockCode : String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
        redisTemplate.opsForValue().set(SMS_CODE_KEY + phone, code, Duration.ofMinutes(5));
        if (smsMock) {
            log.info("[模拟短信] {} 验证码: {}", phone, code);
        }
        return smsMock ? code : null;
    }

    /**
     * 校验验证码；mock 模式未发码时也允许固定码，方便联调
     */
    public void verifySmsCode(String phone, String code) {
        if (!StringUtils.hasText(code)) {
            throw new BizException("请输入短信验证码");
        }
        String cached = redisTemplate.opsForValue().get(SMS_CODE_KEY + phone);
        String expected = cached != null ? cached : (smsMock ? smsMockCode : null);
        if (expected == null || !expected.equals(code)) {
            throw new BizException("短信验证码错误或已过期");
        }
        redisTemplate.delete(SMS_CODE_KEY + phone);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest req) {
        String type = req.getType().toUpperCase();
        SysUser user = new SysUser();
        user.setStatus(0);
        user.setRoles(CommonConstants.ROLE_USER);
        user.setLoginPolicy(CommonConstants.LOGIN_POLICY_SHARED);
        user.setNickname("酷猫用户" + ThreadLocalRandom.current().nextInt(100000, 999999));

        if ("PHONE".equals(type)) {
            if (!StringUtils.hasText(req.getPhone())) {
                throw new BizException("请输入手机号");
            }
            verifySmsCode(req.getPhone(), req.getSmsCode());
            if (userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPhone, req.getPhone())) > 0) {
                throw new BizException("该手机号已注册");
            }
            user.setPhone(req.getPhone());
            String username = StringUtils.hasText(req.getUsername()) ? req.getUsername() : "km_" + req.getPhone();
            if (userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, username)) > 0) {
                throw new BizException("用户名已被占用");
            }
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(StringUtils.hasText(req.getPassword()) ? req.getPassword() : "123456"));
        } else if ("PASSWORD".equals(type)) {
            if (!StringUtils.hasText(req.getUsername()) || !StringUtils.hasText(req.getPassword())) {
                throw new BizException("用户名和密码不能为空");
            }
            if (userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, req.getUsername())) > 0) {
                throw new BizException("用户名已被占用");
            }
            user.setUsername(req.getUsername());
            user.setPassword(passwordEncoder.encode(req.getPassword()));
            user.setPhone(req.getPhone());
        } else {
            throw new BizException("不支持的注册方式: " + type);
        }
        userMapper.insert(user);
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public TokenVO login(LoginRequest req, HttpServletRequest httpRequest) {
        String type = req.getType().toUpperCase();
        String device = normalizeDevice(req.getDevice());
        SysUser user = null;
        switch (type) {
            case "PASSWORD" -> {
                user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, req.getUsername()));
                if (user == null || !passwordMatches(req.getPassword(), user.getPassword())) {
                    throw new BizException("用户名或密码错误");
                }
            }
            case "SMS" -> {
                verifySmsCode(req.getPhone(), req.getSmsCode());
                user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getPhone, req.getPhone()));
                if (user == null) {
                    // 未注册手机号自动注册（验证码已在上方校验通过，此处不再重复校验）
                    user = new SysUser();
                    user.setUsername("km_" + req.getPhone());
                    user.setPhone(req.getPhone());
                    user.setNickname("酷猫用户" + req.getPhone().substring(7));
                    user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
                    user.setRoles(CommonConstants.ROLE_USER);
                    user.setStatus(0);
                    user.setLoginPolicy(CommonConstants.LOGIN_POLICY_SHARED);
                    userMapper.insert(user);
                }
            }
            case "OAUTH" -> user = oauthLogin(req);
            case "ONE_CLICK" -> user = oneClickLogin(req);
            default -> throw new BizException("不支持的登录方式: " + type);
        }
        if (user == null) {
            throw new BizException("登录失败，用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BizException("账号已被禁用，请联系管理员");
        }
        return issueToken(user, device, type, httpRequest);
    }

    private SysUser oauthLogin(LoginRequest req) {
        String thirdType = StringUtils.hasText(req.getThirdType()) ? req.getThirdType().toUpperCase() : "WECHAT";
        String rawOpenId = StringUtils.hasText(req.getThirdOpenId())
                ? req.getThirdOpenId()
                : "mock-" + thirdType + "-" + Math.abs(
                        (req.getUsername() == null ? thirdType : req.getUsername()).hashCode());
        String openId = rawOpenId;
        SysUser user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getThirdType, thirdType).eq(SysUser::getThirdOpenId, openId));
        if (user != null) {
            return user;
        }
        user = new SysUser();
        user.setUsername(thirdType.toLowerCase() + "_" + Math.abs(openId.hashCode()));
        user.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname() : "第三方用户");
        user.setAvatar(req.getAvatar());
        user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setRoles(CommonConstants.ROLE_USER);
        user.setStatus(0);
        user.setLoginPolicy(CommonConstants.LOGIN_POLICY_SHARED);
        user.setThirdType(thirdType);
        user.setThirdOpenId(openId);
        userMapper.insert(user);
        return user;
    }

    private SysUser oneClickLogin(LoginRequest req) {
        String token = req.getCarrierToken();
        if (!StringUtils.hasText(token)) {
            throw new BizException("运营商登录凭证不能为空");
        }
        // 模拟运营商返回手机号：真实接入时调用运营商 SDK 换取
        String phone = StringUtils.hasText(req.getPhone()) ? req.getPhone()
                : "138" + String.format("%08d", Math.abs(token.hashCode()) % 100000000);
        SysUser user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone));
        if (user != null) {
            return user;
        }
        user = new SysUser();
        user.setUsername("km_" + phone);
        user.setPhone(phone);
        user.setNickname("酷猫用户" + phone.substring(7));
        user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setRoles(CommonConstants.ROLE_USER);
        user.setStatus(0);
        user.setLoginPolicy(CommonConstants.LOGIN_POLICY_SHARED);
        userMapper.insert(user);
        return user;
    }

    private TokenVO issueToken(SysUser user, String device, String loginType, HttpServletRequest request) {
        String token = jwtUtils.createToken(user.getId(), user.getUsername(), user.getRoles(), device);
        long expire = jwtUtils.getExpireSeconds();
        String tokenKey = LOGIN_TOKEN_KEY + user.getId() + ":" + device;
        redisTemplate.opsForValue().set(tokenKey, token, Duration.ofSeconds(expire));

        // 多端登录策略
        String policy = user.getLoginPolicy() == null ? CommonConstants.LOGIN_POLICY_SHARED : user.getLoginPolicy();
        if (CommonConstants.LOGIN_POLICY_EXCLUSIVE.equalsIgnoreCase(policy)) {
            // 互斥：清掉其它设备的登录态
            java.util.Set<String> keys = redisTemplate.keys(LOGIN_TOKEN_KEY + user.getId() + ":*");
            if (keys != null) {
                for (String key : keys) {
                    if (!key.endsWith(":" + device)) {
                        redisTemplate.delete(key);
                    }
                }
            }
        }

        UserLoginLog loginLog = new UserLoginLog();
        loginLog.setUserId(user.getId());
        loginLog.setDevice(device);
        loginLog.setIp(clientIp(request));
        loginLog.setUserAgent(request.getHeader("User-Agent"));
        loginLog.setLoginType(loginType);
        loginLog.setTokenId(token.substring(Math.max(0, token.length() - 16)));
        loginLogMapper.insert(loginLog);

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        return new TokenVO(token, expire, user.getId(), user.getUsername(), user.getNickname(),
                user.getAvatar(), user.getRoles(), device);
    }

    public void logout(Long userId, String device, String token) {
        if (userId == null) {
            return;
        }
        String targetDevice = StringUtils.hasText(device) ? device : CommonConstants.DEVICE_PC;
        redisTemplate.delete(LOGIN_TOKEN_KEY + userId + ":" + targetDevice);
        if (StringUtils.hasText(token)) {
            redisTemplate.opsForValue().set(BLACKLIST_KEY + token, "1", Duration.ofDays(7));
        }
    }

    /**
     * 踢掉其它端（单点下线）
     */
    public int kickOthers(Long userId, String currentDevice) {
        java.util.Set<String> keys = redisTemplate.keys(LOGIN_TOKEN_KEY + userId + ":*");
        int count = 0;
        if (keys != null) {
            for (String key : keys) {
                if (currentDevice == null || !key.endsWith(":" + currentDevice)) {
                    redisTemplate.delete(key);
                    count++;
                }
            }
        }
        return count;
    }

    public void resetPassword(String phone, String smsCode, String newPassword) {
        verifySmsCode(phone, smsCode);
        SysUser user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone));
        if (user == null) {
            throw new BizException("该手机号未注册");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new BizException("新密码长度不能少于6位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        clearLogin(user.getId());
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (!passwordMatches(oldPassword, user.getPassword())) {
            throw new BizException("原密码错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException("新密码长度不能少于6位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        clearLogin(userId);
    }

    private void clearLogin(Long userId) {
        java.util.Set<String> keys = redisTemplate.keys(LOGIN_TOKEN_KEY + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public String normalizeDevice(String device) {
        if (!StringUtils.hasText(device)) {
            return CommonConstants.DEVICE_PC;
        }
        String d = device.toUpperCase();
        return switch (d) {
            case "PC", "WEB", "H5" -> CommonConstants.DEVICE_PC;
            case "APP", "ANDROID", "IOS" -> CommonConstants.DEVICE_APP;
            case "DESKTOP", "ELECTRON", "CLIENT" -> CommonConstants.DEVICE_DESKTOP;
            case "ADMIN", "BACKEND" -> CommonConstants.DEVICE_ADMIN;
            default -> throw new BizException("不支持的设备类型: " + device);
        };
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 兼容 {noop} 明文密码（初始化脚本使用）与 BCrypt 密文
     */
    private boolean passwordMatches(String raw, String stored) {
        if (!StringUtils.hasText(raw) || !StringUtils.hasText(stored)) {
            return false;
        }
        if (stored.startsWith("{noop}")) {
            return java.security.MessageDigest.isEqual(
                    raw.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    stored.substring(6).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return passwordEncoder.matches(raw, stored);
    }
}
