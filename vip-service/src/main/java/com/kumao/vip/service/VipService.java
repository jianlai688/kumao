package com.kumao.vip.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.api.client.AdClient;
import com.kumao.api.client.LiveClient;
import com.kumao.api.client.UserClient;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.R;
import com.kumao.common.result.PageResult;
import com.kumao.vip.dto.VipStatusVO;
import com.kumao.vip.entity.VipMember;
import com.kumao.vip.entity.VipOrder;
import com.kumao.vip.entity.VipPackage;
import com.kumao.vip.mapper.VipMemberMapper;
import com.kumao.vip.mapper.VipOrderMapper;
import com.kumao.vip.mapper.VipPackageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class VipService {

    private static final String AD_REWARD_PAY_TYPE = "AD_REWARD";
    private static final int AD_REWARD_DAYS = 1;

    private final VipPackageMapper packageMapper;
    private final VipOrderMapper orderMapper;
    private final VipMemberMapper memberMapper;
    private final UserClient userClient;
    private final LiveClient liveClient;
    private final AdClient adClient;

    public List<VipPackage> packages() {
        return packageMapper.selectList(new LambdaQueryWrapper<VipPackage>()
                .eq(VipPackage::getStatus, 1).orderByAsc(VipPackage::getSort));
    }

    public VipStatusVO status(Long userId) {
        VipStatusVO vo = new VipStatusVO();
        vo.setUserId(userId);
        if (userId == null) {
            return vo;
        }
        VipMember member = memberMapper.selectOne(new LambdaQueryWrapper<VipMember>()
                .eq(VipMember::getUserId, userId));
        if (member == null) {
            return vo;
        }
        boolean active = member.getExpireTime() != null && member.getExpireTime().isAfter(LocalDateTime.now());
        vo.setVip(active);
        vo.setLevel(active ? member.getLevel() : "EXPIRED");
        vo.setExpireTime(member.getExpireTime());
        vo.setDanmakuColor(member.getDanmakuColor());
        vo.setEntranceEffect(member.getEntranceEffect());
        if (active) {
            vo.setRemainDays((int) java.time.Duration.between(LocalDateTime.now(), member.getExpireTime()).toDays());
        }
        vo.setAdRewardClaimedToday(hasAdRewardToday(userId));
        return vo;
    }

    /**
     * 激励广告会员奖励：生成 0 元 AD_REWARD 订单，复用会员发放链路。
     * 同一账号每天只能领取一次，奖励天数与现有会员有效期叠加。
     */
    @Transactional(rollbackFor = Exception.class)
    public synchronized Map<String, Object> claimAdReward(Long userId, Long planId, Integer watchedSeconds) {
        if (planId == null || planId <= 0) {
            throw new BizException("广告信息无效，请重新加载广告");
        }
        if (watchedSeconds == null || watchedSeconds < 15) {
            throw new BizException("请完整观看 15 秒广告后再领取");
        }
        if (hasAdRewardToday(userId)) {
            throw new BizException("今日广告会员已领取，明天再来");
        }

        LocalDateTime now = LocalDateTime.now();
        VipOrder order = new VipOrder();
        order.setOrderNo("ADR" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000)));
        order.setUserId(userId);
        order.setPackageId(0L);
        order.setPackageName("广告畅听卡 · 1 天");
        order.setDays(AD_REWARD_DAYS);
        order.setAmount(BigDecimal.ZERO);
        order.setStatus("PAID");
        order.setPayType(AD_REWARD_PAY_TYPE);
        order.setTransactionId("AD_PLAN_" + planId);
        order.setPayTime(now);
        order.setNotifyTime(now);
        order.setRemark("激励广告观看奖励，planId=" + planId + "，watchedSeconds=" + watchedSeconds);
        orderMapper.insert(order);

        fulfillPaidOrder(order);

        Map<String, Object> result = new HashMap<>();
        result.put("days", AD_REWARD_DAYS);
        result.put("planId", planId);
        result.put("expireTime", order.getExpireTime());
        result.put("status", status(userId));
        return result;
    }

    private boolean hasAdRewardToday(Long userId) {
        LocalDateTime dayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        Long claimed = orderMapper.selectCount(new LambdaQueryWrapper<VipOrder>()
                .eq(VipOrder::getUserId, userId)
                .eq(VipOrder::getPayType, AD_REWARD_PAY_TYPE)
                .ge(VipOrder::getCreateTime, dayStart)
                .lt(VipOrder::getCreateTime, dayStart.plusDays(1)));
        return claimed != null && claimed > 0;
    }

    /**
     * 支付成功后发放会员权益（由 PaymentService 在支付事务中调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void fulfillPaidOrder(VipOrder order) {
        if (order == null || order.getUserId() == null) {
            return;
        }
        if (order.getRemark() != null && order.getRemark().startsWith("AD_PLAN:")) {
            long planId;
            try {
                planId = Long.parseLong(order.getRemark().substring("AD_PLAN:".length()));
            } catch (NumberFormatException e) {
                throw new BizException("广告投放订单数据无效");
            }
            R<Void> result = adClient.markPaid(planId, order.getOrderNo());
            if (result == null || result.getCode() != 200) {
                throw new BizException(result == null ? "广告投放状态同步失败" : result.getMessage());
            }
            return;
        }

        if (order.getRemark() != null && order.getRemark().startsWith("COIN_RECHARGE:")) {
            int coinAmount;
            try {
                coinAmount = Integer.parseInt(order.getRemark().substring("COIN_RECHARGE:".length()));
            } catch (NumberFormatException e) {
                throw new BizException("酷猫币充值订单数据无效");
            }
            R<Integer> credit = liveClient.creditCoins(order.getUserId(), coinAmount, order.getOrderNo());
            if (credit == null || credit.getCode() != 200) {
                throw new BizException(credit == null ? "酷猫币入账失败" : credit.getMessage());
            }
            return;
        }

        int days = order.getDays() == null ? 0 : order.getDays();
        VipMember member = memberMapper.selectOne(new LambdaQueryWrapper<VipMember>()
                .eq(VipMember::getUserId, order.getUserId()));
        LocalDateTime base = LocalDateTime.now();
        if (member != null && member.getExpireTime() != null && member.getExpireTime().isAfter(base)) {
            base = member.getExpireTime();
        }
        LocalDateTime expireTime = base.plusDays(days);
        order.setExpireTime(expireTime);
        orderMapper.updateById(order);

        if (member == null) {
            member = new VipMember();
            member.setUserId(order.getUserId());
            member.setStartTime(LocalDateTime.now());
            member.setTotalDays(days);
            member.setLevel("VIP");
            member.setStatus("ACTIVE");
            member.setDanmakuColor("#FFD700");
            member.setEntranceEffect("gold-glow");
            member.setExpireTime(expireTime);
            memberMapper.insert(member);
        } else {
            member.setExpireTime(expireTime);
            member.setTotalDays((member.getTotalDays() == null ? 0 : member.getTotalDays()) + days);
            member.setStatus("ACTIVE");
            member.setLevel("VIP");
            if (member.getDanmakuColor() == null) {
                member.setDanmakuColor("#FFD700");
            }
            if (member.getEntranceEffect() == null) {
                member.setEntranceEffect("gold-glow");
            }
            memberMapper.updateById(member);
        }
        // 同步用户角色 VIP
        try {
            userClient.grantRole(order.getUserId(), "VIP");
        } catch (Exception e) {
            log.warn("同步 VIP 角色失败: {}", e.getMessage());
        }
    }

    /**
     * 付费权限校验
     *
     * @param type SONG / MV_HD / DANMAKU / ENTRANCE
     */
    public boolean hasPermission(Long userId, String type) {
        if ("DANMAKU".equalsIgnoreCase(type) || "ENTRANCE".equalsIgnoreCase(type)) {
            return status(userId).isVip();
        }
        return status(userId).isVip();
    }

    public PageResult<VipOrder> myOrders(Long userId, PageQuery query) {
        Page<VipOrder> page = orderMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<VipOrder>().eq(VipOrder::getUserId, userId)
                        .and(w -> w.isNull(VipOrder::getRemark)
                                .or().notLikeRight(VipOrder::getRemark, "COIN_RECHARGE:")
                                .or().notLikeRight(VipOrder::getRemark, "AD_PLAN:"))
                        .orderByDesc(VipOrder::getCreateTime));
        return PageResult.of(page);
    }

    // ---------------- 后台 ----------------

    public PageResult<VipMember> adminMembers(PageQuery query, String status) {
        Page<VipMember> page = memberMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<VipMember>()
                        .eq(StringUtils.hasText(status), VipMember::getStatus, status)
                        .orderByDesc(VipMember::getCreateTime));
        return PageResult.of(page);
    }

    public PageResult<VipOrder> adminOrders(PageQuery query, Long userId) {
        Page<VipOrder> page = orderMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<VipOrder>()
                        .eq(userId != null, VipOrder::getUserId, userId)
                        .orderByDesc(VipOrder::getCreateTime));
        return PageResult.of(page);
    }

    public Map<String, Object> statistics() {
        List<VipMember> all = memberMapper.selectList(null);
        long active = all.stream().filter(m -> m.getExpireTime() != null
                && m.getExpireTime().isAfter(LocalDateTime.now())).count();
        java.math.BigDecimal revenue = orderMapper.selectList(new LambdaQueryWrapper<VipOrder>()
                        .eq(VipOrder::getStatus, "PAID")).stream()
                .map(VipOrder::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        return Map.of("totalMember", all.size(), "activeMember", active, "revenue", revenue);
    }

    public Long createPackage(VipPackage pkg) {
        if (pkg.getStatus() == null) {
            pkg.setStatus(1);
        }
        packageMapper.insert(pkg);
        return pkg.getId();
    }

    public void updatePackage(Long id, VipPackage pkg) {
        pkg.setId(id);
        packageMapper.updateById(pkg);
    }

    public void deletePackage(Long id) {
        packageMapper.deleteById(id);
    }
}
