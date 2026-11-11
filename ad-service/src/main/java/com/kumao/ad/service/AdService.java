package com.kumao.ad.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.ad.dto.AdvertiserApplyRequest;
import com.kumao.ad.dto.MaterialRequest;
import com.kumao.ad.dto.PlanRequest;
import com.kumao.ad.entity.*;
import com.kumao.ad.mapper.*;
import com.kumao.api.dto.AdPlanPaymentDTO;
import com.kumao.api.client.UserClient;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdService {

    private final AdvertiserMapper advertiserMapper;
    private final AdMaterialMapper materialMapper;
    private final AdPlanMapper planMapper;
    private final AdStatDailyMapper statMapper;
    private final UserClient userClient;

    private static final Map<String, BigDecimal> SLOT_DAILY_PRICES = Map.of(
            "SPLASH", new BigDecimal("800.00"),
            "HOME_BANNER", new BigDecimal("520.00"),
            "LIVE", new BigDecimal("300.00"),
            "PLAYER", new BigDecimal("200.00"));

    private static final Map<String, String> SLOT_NAMES = Map.of(
            "SPLASH", "开屏广告",
            "HOME_BANNER", "首页 Banner",
            "LIVE", "直播页",
            "PLAYER", "播放页");

    // ---------------- 入驻 ----------------

    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long userId, AdvertiserApplyRequest req) {
        Advertiser exist = advertiserMapper.selectOne(new LambdaQueryWrapper<Advertiser>()
                .eq(Advertiser::getUserId, userId));
        if (exist != null && ("PENDING".equals(exist.getStatus()) || "APPROVED".equals(exist.getStatus()))) {
            throw new BizException("已提交过广告制作人认证，请勿重复提交");
        }
        Advertiser advertiser = exist == null ? new Advertiser() : exist;
        advertiser.setUserId(userId);
        advertiser.setCompanyName(req.getCompanyName());
        advertiser.setRealName(req.getRealName());
        advertiser.setAdType(req.getAdType());
        advertiser.setIntro(req.getIntro());
        advertiser.setAvatar(req.getAvatar());
        advertiser.setRepresentative(req.getRepresentative());
        advertiser.setStatus("PENDING");
        advertiser.setRejectReason(null);
        if (advertiser.getId() == null) {
            advertiserMapper.insert(advertiser);
        } else {
            advertiserMapper.updateById(advertiser);
        }
        return advertiser.getId();
    }

    public Advertiser mine(Long userId) {
        return advertiserMapper.selectOne(new LambdaQueryWrapper<Advertiser>().eq(Advertiser::getUserId, userId));
    }

    public Advertiser requireApproved(Long userId) {
        Advertiser advertiser = mine(userId);
        if (advertiser == null || !"APPROVED".equals(advertiser.getStatus())) {
            throw new BizException(403, "仅认证通过的广告制作人可访问工作台");
        }
        return advertiser;
    }

    public void audit(Long advertiserId, boolean approved, String reason, Long adminId) {
        Advertiser advertiser = advertiserMapper.selectById(advertiserId);
        if (advertiser == null) {
            throw new BizException("广告制作人申请不存在");
        }
        advertiser.setStatus(approved ? "APPROVED" : "REJECTED");
        advertiser.setRejectReason(approved ? null : reason);
        advertiser.setAuditBy(adminId);
        advertiser.setAuditTime(LocalDateTime.now().toString());
        advertiserMapper.updateById(advertiser);
        if (approved) {
            try {
                userClient.grantRole(advertiser.getUserId(), "ADVERTISER");
            } catch (Exception e) {
                log.warn("授予广告制作人角色失败: {}", e.getMessage());
            }
        }
    }

    /** 取消资格并下架全部素材 */
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long advertiserId, String reason) {
        Advertiser advertiser = advertiserMapper.selectById(advertiserId);
        if (advertiser == null) {
            throw new BizException("广告制作人不存在");
        }
        advertiser.setStatus("REVOKED");
        advertiser.setRejectReason(reason);
        advertiserMapper.updateById(advertiser);
        for (AdMaterial material : materialMapper.selectList(new LambdaQueryWrapper<AdMaterial>()
                .eq(AdMaterial::getAdvertiserId, advertiserId))) {
            material.setStatus("OFFLINE");
            materialMapper.updateById(material);
        }
        for (AdPlan plan : planMapper.selectList(new LambdaQueryWrapper<AdPlan>()
                .eq(AdPlan::getAdvertiserId, advertiserId))) {
            plan.setStatus("FINISHED");
            planMapper.updateById(plan);
        }
        try {
            userClient.revokeRole(advertiser.getUserId(), "ADVERTISER");
        } catch (Exception e) {
            log.warn("撤销广告制作人角色失败: {}", e.getMessage());
        }
    }

    public PageResult<Advertiser> adminPage(PageQuery query, String status) {
        Page<Advertiser> page = advertiserMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<Advertiser>()
                        .eq(StringUtils.hasText(status), Advertiser::getStatus, status)
                        .like(StringUtils.hasText(query.getKeyword()), Advertiser::getCompanyName, query.getKeyword())
                        .orderByDesc(Advertiser::getCreateTime));
        return PageResult.of(page);
    }

    // ---------------- 素材 ----------------

    public Long createMaterial(Long userId, MaterialRequest req) {
        Advertiser advertiser = requireApproved(userId);
        AdMaterial material = new AdMaterial();
        material.setAdvertiserId(advertiser.getId());
        material.setUserId(userId);
        material.setName(req.getName());
        material.setType(req.getType().toUpperCase());
        material.setCoverUrl(req.getCoverUrl());
        material.setFileUrl(req.getFileUrl());
        material.setContent(req.getContent());
        material.setStatus("PENDING");
        material.setExposureCount(0L);
        material.setClickCount(0L);
        materialMapper.insert(material);
        return material.getId();
    }

    public void updateMaterial(Long materialId, Long userId, MaterialRequest req) {
        AdMaterial material = requireOwnMaterial(materialId, userId);
        material.setName(req.getName());
        material.setType(req.getType().toUpperCase());
        material.setCoverUrl(req.getCoverUrl());
        material.setFileUrl(req.getFileUrl());
        material.setContent(req.getContent());
        if ("APPROVED".equals(material.getStatus())) {
            material.setStatus("PENDING");
        }
        materialMapper.updateById(material);
    }

    public void offlineMaterial(Long materialId, Long userId) {
        AdMaterial material = requireOwnMaterial(materialId, userId);
        material.setStatus("OFFLINE");
        materialMapper.updateById(material);
    }

    public void deleteMaterial(Long materialId, Long userId) {
        requireOwnMaterial(materialId, userId);
        materialMapper.deleteById(materialId);
    }

    public PageResult<AdMaterial> materials(Long userId, PageQuery query, String status) {
        Advertiser advertiser = requireApproved(userId);
        Page<AdMaterial> page = materialMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<AdMaterial>()
                        .eq(AdMaterial::getAdvertiserId, advertiser.getId())
                        .eq(StringUtils.hasText(status), AdMaterial::getStatus, status)
                        .orderByDesc(AdMaterial::getCreateTime));
        return PageResult.of(page);
    }

    public void auditMaterial(Long materialId, boolean approved, String reason) {
        AdMaterial material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BizException("素材不存在");
        }
        material.setStatus(approved ? "APPROVED" : "REJECTED");
        material.setRejectReason(approved ? null : reason);
        materialMapper.updateById(material);
    }

    public PageResult<AdMaterial> adminMaterials(PageQuery query, String status) {
        Page<AdMaterial> page = materialMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<AdMaterial>()
                        .eq(StringUtils.hasText(status), AdMaterial::getStatus, status)
                        .like(StringUtils.hasText(query.getKeyword()), AdMaterial::getName, query.getKeyword())
                        .orderByDesc(AdMaterial::getCreateTime));
        return PageResult.of(page);
    }

    // ---------------- 投放计划 ----------------

    public List<Map<String, Object>> pricing() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String slot : List.of("SPLASH", "HOME_BANNER", "LIVE", "PLAYER")) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("slot", slot);
            row.put("name", SLOT_NAMES.get(slot));
            row.put("dailyPrice", SLOT_DAILY_PRICES.get(slot));
            row.put("minDays", 1);
            result.add(row);
        }
        return result;
    }

    public AdPlan createPlan(Long userId, PlanRequest req) {
        Advertiser advertiser = requireApproved(userId);
        AdMaterial material = requireOwnMaterial(req.getMaterialId(), userId);
        if (!"APPROVED".equals(material.getStatus())) {
            throw new BizException("素材未审核通过，暂不可投放");
        }
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new BizException("投放结束时间必须晚于开始时间");
        }
        String slot = req.getSlot().toUpperCase();
        BigDecimal dailyPrice = SLOT_DAILY_PRICES.get(slot);
        if (dailyPrice == null) {
            throw new BizException("不支持的广告位");
        }
        int days = billableDays(req.getStartTime(), req.getEndTime());
        BigDecimal amount = dailyPrice.multiply(BigDecimal.valueOf(days));
        AdPlan plan = new AdPlan();
        plan.setAdvertiserId(advertiser.getId());
        plan.setUserId(userId);
        plan.setMaterialId(material.getId());
        plan.setMaterialName(material.getName());
        plan.setSlot(slot);
        plan.setStartTime(req.getStartTime());
        plan.setEndTime(req.getEndTime());
        plan.setTargetAudience(req.getTargetAudience());
        plan.setDays(days);
        plan.setDailyPrice(dailyPrice);
        plan.setAmount(amount);
        plan.setPaymentStatus("UNPAID");
        plan.setStatus("PENDING");
        plan.setExposureCount(0L);
        plan.setClickCount(0L);
        planMapper.insert(plan);
        return plan;
    }

    private int billableDays(LocalDateTime start, LocalDateTime end) {
        long minutes = Math.max(1, Duration.between(start, end).toMinutes());
        return Math.max(1, (int) Math.ceil(minutes / 1440.0));
    }

    public AdPlanPaymentDTO paymentInfo(Long planId) {
        AdPlan plan = planMapper.selectById(planId);
        if (plan == null) throw new BizException("投放计划不存在");
        AdPlanPaymentDTO dto = new AdPlanPaymentDTO();
        dto.setPlanId(plan.getId());
        dto.setUserId(plan.getUserId());
        dto.setAmount(plan.getAmount());
        dto.setSubject("广告投放-" + SLOT_NAMES.getOrDefault(plan.getSlot(), plan.getSlot()) + "-" + plan.getDays() + "天");
        dto.setPaymentStatus(plan.getPaymentStatus());
        dto.setStatus(plan.getStatus());
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markPlanPaid(Long planId, String orderNo) {
        AdPlan plan = planMapper.selectById(planId);
        if (plan == null) throw new BizException("投放计划不存在");
        if ("PAID".equals(plan.getPaymentStatus())) return;
        plan.setPaymentStatus("PAID");
        plan.setPaymentOrderNo(orderNo);
        plan.setPaidTime(LocalDateTime.now());
        planMapper.updateById(plan);
    }

    public void updatePlanStatus(Long planId, Long userId, String action) {
        AdPlan plan = requireOwnPlan(planId, userId);
        if (!"PAID".equals(plan.getPaymentStatus())) {
            throw new BizException("请先完成投放付费");
        }
        AdMaterial material = materialMapper.selectById(plan.getMaterialId());
        if (material == null || !"APPROVED".equals(material.getStatus())) {
            throw new BizException("广告素材未审核通过，暂不可投放");
        }
        switch (action.toUpperCase()) {
            case "PAUSE" -> plan.setStatus("PAUSED");
            case "RESUME" -> plan.setStatus("RUNNING");
            case "FINISH" -> plan.setStatus("FINISHED");
            default -> throw new BizException("不支持的操作: " + action);
        }
        planMapper.updateById(plan);
    }

    public void auditPlan(Long planId, boolean approved, String reason) {
        AdPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BizException("投放计划不存在");
        }
        if (approved && !"PAID".equals(plan.getPaymentStatus())) {
            throw new BizException("该投放计划尚未完成付费，暂不能通过审核");
        }
        if (approved) {
            AdMaterial material = materialMapper.selectById(plan.getMaterialId());
            if (material == null || !"APPROVED".equals(material.getStatus())) {
                throw new BizException("广告素材未审核通过，暂不能通过投放审核");
            }
        }
        plan.setStatus(approved ? "RUNNING" : "REJECTED");
        plan.setRejectReason(approved ? null : reason);
        planMapper.updateById(plan);
    }

    public PageResult<AdPlan> plans(Long userId, PageQuery query) {
        Advertiser advertiser = requireApproved(userId);
        Page<AdPlan> page = planMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<AdPlan>()
                        .eq(AdPlan::getAdvertiserId, advertiser.getId())
                        .orderByDesc(AdPlan::getCreateTime));
        return PageResult.of(page);
    }

    public PageResult<AdPlan> adminPlans(PageQuery query, String status) {
        Page<AdPlan> page = planMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<AdPlan>()
                        .eq(StringUtils.hasText(status), AdPlan::getStatus, status)
                        .orderByDesc(AdPlan::getCreateTime));
        return PageResult.of(page);
    }

    /**
     * 广告曝光 / 点击上报，写入日统计
     */
    @Transactional(rollbackFor = Exception.class)
    public void report(Long planId, String action, String region, Integer hour) {
        AdPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BizException("投放计划不存在");
        }
        boolean exposure = "EXPOSURE".equalsIgnoreCase(action);
        boolean click = "CLICK".equalsIgnoreCase(action);
        if (!exposure && !click) {
            throw new BizException("不支持的上报类型");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!"RUNNING".equals(plan.getStatus()) || !"PAID".equals(plan.getPaymentStatus())
                || plan.getStartTime() == null || plan.getEndTime() == null
                || now.isBefore(plan.getStartTime()) || now.isAfter(plan.getEndTime())) {
            throw new BizException("广告当前不在有效投放周期");
        }
        AdMaterial reportMaterial = materialMapper.selectById(plan.getMaterialId());
        if (reportMaterial == null || !"APPROVED".equals(reportMaterial.getStatus())) {
            throw new BizException("广告素材当前不可展示");
        }
        if (exposure) {
            plan.setExposureCount((plan.getExposureCount() == null ? 0 : plan.getExposureCount()) + 1);
            reportMaterial.setExposureCount((reportMaterial.getExposureCount() == null ? 0 : reportMaterial.getExposureCount()) + 1);
            materialMapper.updateById(reportMaterial);
        } else {
            plan.setClickCount((plan.getClickCount() == null ? 0 : plan.getClickCount()) + 1);
            reportMaterial.setClickCount((reportMaterial.getClickCount() == null ? 0 : reportMaterial.getClickCount()) + 1);
            materialMapper.updateById(reportMaterial);
        }
        planMapper.updateById(plan);

        AdStatDaily stat = new AdStatDaily();
        stat.setAdvertiserId(plan.getAdvertiserId());
        stat.setMaterialId(plan.getMaterialId());
        stat.setPlanId(plan.getId());
        stat.setStatDate(LocalDate.now());
        stat.setExposureCount(exposure ? 1L : 0L);
        stat.setClickCount(click ? 1L : 0L);
        stat.setRegion(StringUtils.hasText(region) ? region : "未知");
        stat.setHourSlot(hour == null ? LocalDateTime.now().getHour() : hour);
        statMapper.insert(stat);
    }

    /** 对外：按广告位获取正在投放的广告 */
    public List<Map<String, Object>> activeAds(String slot) {
        List<AdPlan> plans = planMapper.selectList(new LambdaQueryWrapper<AdPlan>()
                .eq(AdPlan::getStatus, "RUNNING")
                .eq(AdPlan::getPaymentStatus, "PAID")
                .eq(StringUtils.hasText(slot), AdPlan::getSlot, slot)
                .le(AdPlan::getStartTime, LocalDateTime.now())
                .ge(AdPlan::getEndTime, LocalDateTime.now()));
        return plans.stream().map(p -> {
            AdMaterial material = materialMapper.selectById(p.getMaterialId());
            if (material == null || !"APPROVED".equals(material.getStatus())) {
                return null;
            }
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("planId", p.getId());
            item.put("slot", p.getSlot() == null ? "" : p.getSlot());
            item.put("materialId", p.getMaterialId());
            item.put("name", p.getMaterialName() == null ? "" : p.getMaterialName());
            item.put("type", material == null || material.getType() == null ? "" : material.getType());
            item.put("coverUrl", material == null || material.getCoverUrl() == null ? "" : material.getCoverUrl());
            item.put("fileUrl", material == null || material.getFileUrl() == null ? "" : material.getFileUrl());
            item.put("content", material == null || material.getContent() == null ? "" : material.getContent());
            return item;
        }).filter(java.util.Objects::nonNull).toList();
    }

    // ---------------- 数据统计 ----------------

    public Map<String, Object> statistics(Long userId) {
        Advertiser advertiser = requireApproved(userId);
        List<AdMaterial> materials = materialMapper.selectList(new LambdaQueryWrapper<AdMaterial>()
                .eq(AdMaterial::getAdvertiserId, advertiser.getId()));
        long exposure = materials.stream().mapToLong(m -> m.getExposureCount() == null ? 0 : m.getExposureCount()).sum();
        long click = materials.stream().mapToLong(m -> m.getClickCount() == null ? 0 : m.getClickCount()).sum();
        double ctr = exposure == 0 ? 0 : (double) click / exposure * 100;
        return Map.of("exposure", exposure, "click", click,
                "ctr", String.format("%.2f%%", ctr), "materialCount", materials.size());
    }

    public List<AdMaterial> materialRank(Long userId) {
        Advertiser advertiser = requireApproved(userId);
        return materialMapper.selectList(new LambdaQueryWrapper<AdMaterial>()
                .eq(AdMaterial::getAdvertiserId, advertiser.getId())
                .orderByDesc(AdMaterial::getClickCount)
                .last("limit 10"));
    }

    public Map<String, Object> regionDistribution(Long userId) {
        Advertiser advertiser = requireApproved(userId);
        List<AdStatDaily> stats = statMapper.selectList(new LambdaQueryWrapper<AdStatDaily>()
                .eq(AdStatDaily::getAdvertiserId, advertiser.getId()));
        Map<String, Long> byRegion = new java.util.HashMap<>();
        for (AdStatDaily s : stats) {
            String region = s.getRegion() == null || s.getRegion().isBlank() ? "未知" : s.getRegion();
            long count = s.getExposureCount() == null ? 0L : s.getExposureCount();
            byRegion.merge(region, count, Long::sum);
        }
        return Map.of("byRegion", byRegion);
    }

    public Map<String, Object> hourlyTrend(Long userId) {
        Advertiser advertiser = requireApproved(userId);
        List<AdStatDaily> stats = statMapper.selectList(new LambdaQueryWrapper<AdStatDaily>()
                .eq(AdStatDaily::getAdvertiserId, advertiser.getId()));
        long[] hours = new long[24];
        for (AdStatDaily s : stats) {
            int h = s.getHourSlot() == null ? 0 : s.getHourSlot();
            hours[Math.max(0, Math.min(23, h))] += s.getExposureCount() == null ? 0 : s.getExposureCount();
        }
        return Map.of("hours", hours);
    }

    // ---------------- 后台：投放收入 ----------------

    /** 广告主只为投流付费，平台侧统计投放收入，广告主本身不产生收益、不存在提现 */
    public Map<String, Object> adminRevenueSummary() {
        List<AdPlan> paidPlans = planMapper.selectList(new LambdaQueryWrapper<AdPlan>()
                .eq(AdPlan::getPaymentStatus, "PAID"));
        BigDecimal revenue = paidPlans.stream()
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal runningBudget = paidPlans.stream()
                .filter(p -> "RUNNING".equals(p.getStatus()))
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long running = paidPlans.stream().filter(p -> "RUNNING".equals(p.getStatus())).count();
        return Map.of(
                "placementRevenue", revenue,
                "paidPlanCount", paidPlans.size(),
                "runningPlanCount", running,
                "runningBudget", runningBudget,
                "advertiserCount", advertiserMapper.selectCount(null));
    }
    // ---------------- 辅助 ----------------

    private AdMaterial requireOwnMaterial(Long materialId, Long userId) {
        AdMaterial material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BizException("素材不存在");
        }
        if (!material.getUserId().equals(userId)) {
            throw new BizException(403, "只能管理自己的素材");
        }
        return material;
    }

    private AdPlan requireOwnPlan(Long planId, Long userId) {
        AdPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BizException("投放计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new BizException(403, "只能管理自己的投放计划");
        }
        return plan;
    }
}
