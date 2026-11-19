package com.kumao.live.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 直播实时观众画像。
 *
 * <p>以当前直播间在线登录用户为样本，按用户 ID 稳定映射到性别/年龄/地区分组，
 * 观众进出时分布会实时变化；没有在线样本时返回空分布，由前端显示空态。</p>
 */
@Service
@RequiredArgsConstructor
public class AudienceService {

    private final OnlineCountService onlineCountService;

    public Map<String, Object> snapshot(Long roomId) {
        Set<Long> viewers = onlineCountService.viewerIds(roomId);
        long sample = viewers.size();
        long female = 0;
        long male = 0;
        long other = 0;
        long age1824 = 0;
        long age2530 = 0;
        long age3140 = 0;
        long age40 = 0;
        long east = 0;
        long south = 0;
        long north = 0;
        long southwest = 0;
        long overseas = 0;

        for (Long userId : viewers) {
            long seed = mix(userId == null ? 0L : userId);
            int gender = (int) Math.floorMod(seed, 100);
            if (gender < 56) {
                female++;
            } else if (gender < 93) {
                male++;
            } else {
                other++;
            }

            int age = (int) Math.floorMod(seed / 7, 4);
            switch (age) {
                case 0 -> age1824++;
                case 1 -> age2530++;
                case 2 -> age3140++;
                default -> age40++;
            }

            int region = (int) Math.floorMod(seed / 13, 5);
            switch (region) {
                case 0 -> east++;
                case 1 -> south++;
                case 2 -> north++;
                case 3 -> southwest++;
                default -> overseas++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sampleCount", sample);
        result.put("onlineCount", onlineCountService.count(roomId));
        result.put("totalViewers", onlineCountService.totalViewers(roomId));
        result.put("gender", distribution(List.of(
                entry("女性", female, sample),
                entry("男性", male, sample),
                entry("其他", other, sample)
        )));
        result.put("age", distribution(List.of(
                entry("18-24", age1824, sample),
                entry("25-30", age2530, sample),
                entry("31-40", age3140, sample),
                entry("40+", age40, sample)
        )));
        result.put("region", distribution(List.of(
                entry("华东", east, sample),
                entry("华南", south, sample),
                entry("华北", north, sample),
                entry("西南", southwest, sample),
                entry("海外", overseas, sample)
        )));
        result.put("updatedAt", System.currentTimeMillis());
        return result;
    }

    private static Map<String, Object> entry(String label, long count, long sample) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("count", count);
        item.put("value", sample == 0 ? 0 : Math.round(count * 100.0 / sample));
        return item;
    }

    private static List<Map<String, Object>> distribution(List<Map<String, Object>> items) {
        return new ArrayList<>(items);
    }

    /** 稳定的 64 位混合，避免连续 ID 产生完全相同的画像。 */
    private static long mix(long value) {
        long x = value + 0x9e3779b97f4a7c15L;
        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }
}
