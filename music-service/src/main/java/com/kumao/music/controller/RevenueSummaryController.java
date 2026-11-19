package com.kumao.music.controller;

import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营后台「收益与结算」汇总：按业务线统计平台收入。
 *
 * <p>各业务数据分散在会员、直播、广告、音乐人、单曲购买等模块，这里统一汇总成一份口径，
 * 供数据看板一处展示。金额统一折算为元：单曲/音乐人收入按分 ÷100，直播酷币按 10 酷币 = 1 元。</p>
 */
@RestController
@RequestMapping("/admin/revenue")
@RequiredArgsConstructor
public class RevenueSummaryController {

    private static final BigDecimal COIN_PER_YUAN = BigDecimal.TEN;

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        UserContext.requireAdmin();

        BigDecimal vip = money("select coalesce(sum(amount), 0) from vip_order where status = 'PAID' and deleted = 0");
        BigDecimal song = money("select coalesce(sum(amount), 0) / 100 from song_purchase where status = 'PAID'");
        BigDecimal live = money("select coalesce(sum(platform_amount), 0) from live_settlement where status = 'SETTLED'")
                .divide(COIN_PER_YUAN, 2, RoundingMode.HALF_UP);
        BigDecimal ad = money("select coalesce(sum(amount), 0) from ad_plan where payment_status = 'PAID' and deleted = 0");
        BigDecimal musician = money("select coalesce(sum(amount), 0) / 100 from musician_income where status = 'SUCCESS'");

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(item("VIP", "会员收入", vip, "vip_order"));
        items.add(item("SONG", "单曲购买", song, "song_purchase"));
        items.add(item("LIVE", "直播礼物（平台分成）", live, "live_settlement"));
        items.add(item("AD", "广告投放", ad, "ad_plan"));
        items.add(cost("MUSICIAN", "音乐人分成", musician, "musician_income"));

        BigDecimal total = items.stream()
                .map(i -> "COST".equals(i.get("type"))
                        ? ((BigDecimal) i.get("amount")).negate()
                        : (BigDecimal) i.get("amount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total.setScale(2, RoundingMode.HALF_UP));
        result.put("items", items);
        result.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return R.ok(result);
    }

    private Map<String, Object> item(String key, String label, BigDecimal amount, String source) {
        return row(key, label, amount, source, "INCOME");
    }

    private Map<String, Object> cost(String key, String label, BigDecimal amount, String source) {
        return row(key, label, amount, source, "COST");
    }

    private Map<String, Object> row(String key, String label, BigDecimal amount, String source, String type) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", key);
        map.put("label", label);
        map.put("amount", amount.setScale(2, RoundingMode.HALF_UP));
        map.put("unit", "元");
        map.put("source", source);
        map.put("type", type);
        return map;
    }

    private BigDecimal money(String sql) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }
}

