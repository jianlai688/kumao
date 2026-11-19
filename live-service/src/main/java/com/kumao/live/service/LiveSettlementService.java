package com.kumao.live.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kumao.live.entity.LiveGiftRecord;
import com.kumao.live.entity.LiveRoom;
import com.kumao.live.entity.LiveSettlement;
import com.kumao.live.mapper.LiveGiftRecordMapper;
import com.kumao.live.mapper.LiveSettlementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 直播礼物结算：下播时按本场礼物总额结算，平台按比例抽成（默认 50%）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveSettlementService {

    private final LiveSettlementMapper settlementMapper;
    private final LiveGiftRecordMapper giftRecordMapper;

    /** 平台抽成百分比，默认 50（即主播拿一半） */
    @Value("${kumao.live.platform-rate:50}")
    private int platformRate = 50;

    /**
     * 下播结算：同一直播场次只生成一张结算单，重复调用直接返回已有记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public LiveSettlement settle(LiveRoom room) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = room.getLiveStartedAt();
        String sessionKey = start == null ? "ALL" : start.toString();
        LambdaQueryWrapper<LiveSettlement> existQuery = new LambdaQueryWrapper<LiveSettlement>()
                .eq(LiveSettlement::getRoomId, room.getId())
                .eq(LiveSettlement::getAnchorId, room.getAnchorId());
        if (start == null) {
            existQuery.isNull(LiveSettlement::getSessionStart);
        } else {
            existQuery.eq(LiveSettlement::getSessionStart, start);
        }
        LiveSettlement existed = settlementMapper.selectOne(existQuery.last("limit 1"));
        if (existed != null) {
            return existed;
        }

        List<LiveGiftRecord> records = giftRecordMapper.selectList(new LambdaQueryWrapper<LiveGiftRecord>()
                .eq(LiveGiftRecord::getRoomId, room.getId())
                .ge(start != null, LiveGiftRecord::getCreateTime, start));
        int total = records.stream().mapToInt(r -> r.getTotalPrice() == null ? 0 : r.getTotalPrice()).sum();
        int rate = Math.max(0, Math.min(100, platformRate));
        int platform = total * rate / 100;
        int anchor = total - platform;

        LiveSettlement settlement = new LiveSettlement();
        settlement.setRoomId(room.getId());
        settlement.setAnchorId(room.getAnchorId());
        settlement.setSessionStart(start);
        settlement.setSessionEnd(end);
        settlement.setGiftTotal(total);
        settlement.setPlatformRate(rate);
        settlement.setPlatformAmount(platform);
        settlement.setAnchorAmount(anchor);
        settlement.setStatus("SETTLED");
        settlementMapper.insert(settlement);
        log.info("[直播结算] room={} 礼物总额={} 平台抽成{}%={} 主播={} session={}",
                room.getId(), total, rate, platform, anchor, sessionKey);
        return settlement;
    }

    public List<LiveSettlement> mine(Long anchorId) {
        return settlementMapper.selectList(new LambdaQueryWrapper<LiveSettlement>()
                .eq(LiveSettlement::getAnchorId, anchorId)
                .orderByDesc(LiveSettlement::getCreateTime)
                .last("limit 50"));
    }

    public LiveSettlement byRoom(Long roomId) {
        return settlementMapper.selectOne(new LambdaQueryWrapper<LiveSettlement>()
                .eq(LiveSettlement::getRoomId, roomId)
                .orderByDesc(LiveSettlement::getCreateTime)
                .last("limit 1"));
    }
}
