package com.kumao.musician.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kumao.api.client.LiveClient;
import com.kumao.common.exception.BizException;
import com.kumao.musician.entity.Musician;
import com.kumao.musician.entity.MusicianIncome;
import com.kumao.musician.entity.MusicianWork;
import com.kumao.musician.mapper.MusicianIncomeMapper;
import com.kumao.musician.mapper.MusicianMapper;
import com.kumao.musician.mapper.MusicianWorkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收益结算（模拟）：播放收益 + 礼物收益
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MusicianIncomeService {

    private final MusicianMapper musicianMapper;
    private final MusicianWorkMapper workMapper;
    private final MusicianIncomeMapper incomeMapper;
    private final LiveClient liveClient;

    @Value("${kumao.income.play-rate:10}")
    private int playRate;
    @Value("${kumao.income.gift-rate:70}")
    private int giftRate;

    public Map<String, Object> summary(Long userId) {
        Musician musician = requireMusician(userId);
        List<MusicianIncome> incomes = incomeMapper.selectList(new LambdaQueryWrapper<MusicianIncome>()
                .eq(MusicianIncome::getMusicianId, musician.getId()));
        long income = incomes.stream().filter(i -> !"WITHDRAW".equals(i.getType()))
                .mapToLong(i -> i.getAmount() == null ? 0 : i.getAmount()).sum();
        long withdraw = incomes.stream().filter(i -> "WITHDRAW".equals(i.getType()))
                .mapToLong(i -> Math.abs(i.getAmount() == null ? 0 : i.getAmount())).sum();
        long giftIncome = 0;
        try {
            var r = liveClient.giftIncome(userId);
            if (r != null && r.getData() != null) {
                giftIncome = r.getData();
            }
        } catch (Exception e) {
            log.debug("查询直播礼物收益失败: {}", e.getMessage());
        }
        Map<String, Object> map = new HashMap<>();
        map.put("totalIncome", income);
        map.put("withdrawn", withdraw);
        map.put("balance", income - withdraw);
        map.put("liveGiftIncome", giftIncome);
        map.put("musicianId", musician.getId());
        return map;
    }

    /**
     * 结算：按播放量折算 + 拉取直播礼物收益
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> settle(Long userId) {
        Musician musician = requireMusician(userId);
        List<MusicianWork> works = workMapper.selectList(new LambdaQueryWrapper<MusicianWork>()
                .eq(MusicianWork::getMusicianId, musician.getId()));
        long playTotal = works.stream().mapToLong(w -> w.getPlayCount() == null ? 0 : w.getPlayCount()).sum();

        long alreadySettled = incomeMapper.selectList(new LambdaQueryWrapper<MusicianIncome>()
                        .eq(MusicianIncome::getMusicianId, musician.getId())
                        .eq(MusicianIncome::getType, "PLAY"))
                .stream().mapToLong(i -> i.getAmount() == null ? 0 : i.getAmount()).sum();
        long should = (playTotal / 1000) * playRate;
        long delta = Math.max(0, should - alreadySettled);
        if (delta > 0) {
            MusicianIncome income = new MusicianIncome();
            income.setMusicianId(musician.getId());
            income.setUserId(userId);
            income.setType("PLAY");
            income.setAmount((int) delta);
            income.setDescription("播放收益结算：总播放 " + playTotal);
            income.setStatus("SUCCESS");
            incomeMapper.insert(income);
        }

        long giftIncome = 0;
        try {
            var r = liveClient.giftIncome(userId);
            if (r != null && r.getData() != null) {
                giftIncome = r.getData();
            }
        } catch (Exception e) {
            log.debug("查询礼物收益失败: {}", e.getMessage());
            throw new BizException("直播服务暂不可用，礼物收益稍后结算");
        }
        long giftAmount = giftIncome * giftRate / 100;
        long giftSettled = incomeMapper.selectList(new LambdaQueryWrapper<MusicianIncome>()
                        .eq(MusicianIncome::getMusicianId, musician.getId())
                        .eq(MusicianIncome::getType, "GIFT"))
                .stream().mapToLong(i -> i.getAmount() == null ? 0 : i.getAmount()).sum();
        long giftDelta = Math.max(0, giftAmount - giftSettled);
        if (giftDelta > 0) {
            MusicianIncome income = new MusicianIncome();
            income.setMusicianId(musician.getId());
            income.setUserId(userId);
            income.setType("GIFT");
            income.setAmount((int) giftDelta);
            income.setDescription("直播礼物收益结算");
            income.setStatus("SUCCESS");
            incomeMapper.insert(income);
        }
        return Map.of("playIncome", delta, "giftIncome", giftDelta, "playTotal", playTotal);
    }

    private Musician requireMusician(Long userId) {
        Musician musician = musicianMapper.selectOne(new LambdaQueryWrapper<Musician>()
                .eq(Musician::getUserId, userId));
        if (musician == null || !"APPROVED".equals(musician.getStatus())) {
            throw new BizException(403, "仅认证通过的音乐人可访问收益中心");
        }
        return musician;
    }
}
