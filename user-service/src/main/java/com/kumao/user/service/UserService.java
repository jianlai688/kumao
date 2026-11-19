package com.kumao.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.user.dto.AnchorApplyRequest;
import com.kumao.user.dto.ProfileUpdateRequest;
import com.kumao.user.dto.UserVO;
import com.kumao.user.entity.AnchorApply;
import com.kumao.user.entity.SysUser;
import com.kumao.user.entity.UserLoginLog;
import com.kumao.user.mapper.AnchorApplyMapper;
import com.kumao.user.mapper.SysUserMapper;
import com.kumao.user.mapper.UserLoginLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final UserLoginLogMapper loginLogMapper;
    private final AnchorApplyMapper anchorApplyMapper;

    public UserVO profile(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return UserVO.from(user);
    }

    public void updateProfile(Long userId, ProfileUpdateRequest req) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (StringUtils.hasText(req.getNickname())) {
            user.setNickname(req.getNickname());
        }
        if (req.getAvatar() != null) {
            user.setAvatar(req.getAvatar());
        }
        if (req.getIntro() != null) {
            user.setIntro(req.getIntro());
        }
        if (req.getGender() != null) {
            int g = req.getGender();
            user.setGender(g == 1 || g == 2 ? g : 0);
        }
        if (req.getBirthday() != null) {
            if (req.getBirthday().isBlank()) {
                user.setBirthday(null);
            } else {
                try {
                    user.setBirthday(java.time.LocalDate.parse(req.getBirthday()));
                } catch (Exception e) {
                    throw new BizException("生日格式不正确，应为 yyyy-MM-dd");
                }
            }
        }
        if (req.getRegion() != null) {
            user.setRegion(req.getRegion().isBlank() ? null : req.getRegion().trim());
        }
        if (req.getEmail() != null) {
            String email = req.getEmail().trim();
            if (!email.isEmpty() && !email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
                throw new BizException("邮箱格式不正确");
            }
            user.setEmail(email.isEmpty() ? null : email);
        }
        userMapper.updateById(user);
    }

    public PageResult<UserVO> pageUsers(PageQuery query, Integer status, String role) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(status != null, SysUser::getStatus, status)
                .like(StringUtils.hasText(role), SysUser::getRoles, role)
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(SysUser::getUsername, query.getKeyword())
                        .or().like(SysUser::getNickname, query.getKeyword())
                        .or().like(SysUser::getPhone, query.getKeyword()))
                .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> page = userMapper.selectPage(query.toPage(), wrapper);
        List<UserVO> records = page.getRecords().stream().map(UserVO::from).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    public void updateStatus(Long userId, Integer status) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public boolean isMuted(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return user != null && user.getMutedUntil() != null
                && user.getMutedUntil().isAfter(LocalDateTime.now());
    }

    public void mute(Long userId, int minutes) {
        SysUser user = requireUser(userId);
        int duration = Math.max(1, Math.min(minutes, 60 * 24 * 30));
        LocalDateTime until = LocalDateTime.now().plusMinutes(duration);
        user.setMutedUntil(until);
        userMapper.updateById(user);
    }

    public PageResult<UserLoginLog> pageLoginLogs(PageQuery query, Long userId) {
        Page<UserLoginLog> page = loginLogMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<UserLoginLog>()
                        .eq(userId != null, UserLoginLog::getUserId, userId)
                        .orderByDesc(UserLoginLog::getCreateTime));
        return PageResult.of(page);
    }

    public void grantRole(Long userId, String role) {
        SysUser user = requireUser(userId);
        List<String> roles = splitRoles(user.getRoles());
        if (!roles.contains(role.toUpperCase())) {
            roles.add(role.toUpperCase());
            user.setRoles(String.join(",", roles));
            userMapper.updateById(user);
        }
    }

    public void revokeRole(Long userId, String role) {
        SysUser user = requireUser(userId);
        List<String> roles = splitRoles(user.getRoles());
        roles.remove(role.toUpperCase());
        if (roles.isEmpty()) {
            roles.add("USER");
        }
        user.setRoles(String.join(",", roles));
        userMapper.updateById(user);
    }

    private SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    private List<String> splitRoles(String roles) {
        return roles == null || roles.isBlank()
                ? new java.util.ArrayList<>(List.of("USER"))
                : new java.util.ArrayList<>(Arrays.asList(roles.split(",")));
    }

    // ---------------- 主播申请 ----------------

    @Transactional(rollbackFor = Exception.class)
/**
     * 主播修改个人信息（取最近一条已通过的主播入驻记录）
     */
    public void updateAnchorProfile(Long userId, com.kumao.user.dto.AnchorProfileRequest req) {
        AnchorApply apply = anchorApplyMapper.selectOne(new LambdaQueryWrapper<AnchorApply>()
                .eq(AnchorApply::getUserId, userId)
                .eq(AnchorApply::getStatus, "APPROVED")
                .orderByDesc(AnchorApply::getCreateTime)
                .last("limit 1"));
        if (apply == null) {
            throw new BizException("尚未通过主播认证，无法修改主播信息");
        }
        if (req.getAnchorName() != null) {
            apply.setAnchorName(req.getAnchorName().isBlank() ? null : req.getAnchorName().trim());
        }
        if (req.getAvatar() != null) {
            apply.setAvatar(req.getAvatar());
        }
        if (req.getLiveCategory() != null) {
            apply.setLiveCategory(req.getLiveCategory());
        }
        if (req.getIntro() != null) {
            apply.setIntro(req.getIntro());
        }
        if (req.getCoverUrl() != null) {
            apply.setCoverUrl(req.getCoverUrl());
        }
        if (req.getPayType() != null) {
            String payType = req.getPayType().trim().toUpperCase();
            if (!"ALIPAY".equals(payType) && !"WECHAT".equals(payType)) {
                throw new BizException("收款方式仅支持支付宝或微信");
            }
            apply.setPayType(payType);
        }
        if (req.getPayAccount() != null) {
            String payAccount = req.getPayAccount().trim();
            if (payAccount.length() > 100) {
                throw new BizException("收款账号不能超过 100 个字符");
            }
            apply.setPayAccount(payAccount.isEmpty() ? null : payAccount);
        }
        if (req.getPayName() != null) {
            String payName = req.getPayName().trim();
            if (payName.length() > 32) {
                throw new BizException("收款人姓名不能超过 32 个字符");
            }
            apply.setPayName(payName.isEmpty() ? null : payName);
        }
        if (StringUtils.hasText(apply.getPayType()) && !StringUtils.hasText(apply.getPayAccount())) {
            throw new BizException("请填写收款账号");
        }
        anchorApplyMapper.updateById(apply);
    }

    public Long submitAnchorApply(Long userId, AnchorApplyRequest req) {
        Long pending = anchorApplyMapper.selectCount(new LambdaQueryWrapper<AnchorApply>()
                .eq(AnchorApply::getUserId, userId).eq(AnchorApply::getStatus, "PENDING"));
        if (pending != null && pending > 0) {
            throw new BizException("已有待审核的主播申请，请勿重复提交");
        }
        SysUser user = requireUser(userId);
        if (user.getRoles() != null && user.getRoles().contains("ANCHOR")) {
            throw new BizException("您已是主播，无需重复申请");
        }
        AnchorApply apply = new AnchorApply();
        apply.setUserId(userId);
        apply.setRealName(req.getRealName());
        apply.setIdCard(req.getIdCard());
        apply.setIdCardFrontUrl(req.getIdCardFrontUrl());
        apply.setIdCardBackUrl(req.getIdCardBackUrl());
        apply.setLiveCategory(req.getLiveCategory());
        apply.setIntro(req.getIntro());
        apply.setCoverUrl(req.getCoverUrl());
        apply.setStatus("PENDING");
        anchorApplyMapper.insert(apply);
        return apply.getId();
    }

    public PageResult<AnchorApply> pageAnchorApplies(PageQuery query, String status) {
        Page<AnchorApply> page = anchorApplyMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<AnchorApply>()
                        .eq(StringUtils.hasText(status), AnchorApply::getStatus, status)
                        .orderByDesc(AnchorApply::getCreateTime));
        return PageResult.of(page);
    }

    public List<AnchorApply> myAnchorApplies(Long userId) {
        return anchorApplyMapper.selectList(new LambdaQueryWrapper<AnchorApply>()
                .eq(AnchorApply::getUserId, userId).orderByDesc(AnchorApply::getCreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditAnchorApply(Long applyId, boolean approved, String reason, Long adminId) {
        AnchorApply apply = anchorApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException("申请不存在");
        }
        if (!"PENDING".equals(apply.getStatus())) {
            throw new BizException("该申请已处理");
        }
        if (approved && (!StringUtils.hasText(apply.getIdCard())
                || !StringUtils.hasText(apply.getIdCardFrontUrl())
                || !StringUtils.hasText(apply.getIdCardBackUrl()))) {
            throw new BizException("请先核验身份证号及正反面照片，再通过审核");
        }
        apply.setStatus(approved ? "APPROVED" : "REJECTED");
        apply.setRejectReason(approved ? null : reason);
        apply.setAuditBy(adminId);
        apply.setAuditTime(LocalDateTime.now().toString());
        anchorApplyMapper.updateById(apply);
        if (approved) {
            grantRole(apply.getUserId(), "ANCHOR");
        }
    }

    /** 后台看板：用户角色分布 */
    public Map<String, Object> statistics() {
        List<SysUser> all = userMapper.selectList(null);
        long anchor = all.stream().filter(u -> u.getRoles() != null && u.getRoles().contains("ANCHOR")).count();
        long musician = all.stream().filter(u -> u.getRoles() != null && u.getRoles().contains("MUSICIAN")).count();
        long vip = all.stream().filter(u -> u.getRoles() != null && u.getRoles().contains("VIP")).count();
        long disabled = all.stream().filter(u -> u.getStatus() != null && u.getStatus() == 1).count();
        return Map.of("total", all.size(), "anchor", anchor, "musician", musician,
                "vip", vip, "disabled", disabled, "todayLogin", loginLogMapper.selectCount(
                        new LambdaQueryWrapper<UserLoginLog>().ge(UserLoginLog::getCreateTime, LocalDateTime.now().toLocalDate().atStartOfDay())));
    }
}


