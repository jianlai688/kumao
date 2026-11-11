package com.kumao.live.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.live.dto.DanmakuMessage;
import com.kumao.live.dto.GiftSendRequest;
import com.kumao.live.entity.LiveCoin;
import com.kumao.live.entity.LiveCoinRecharge;
import com.kumao.live.entity.LiveGift;
import com.kumao.live.entity.LiveGiftRecord;
import com.kumao.live.entity.LiveRoom;
import com.kumao.live.mapper.LiveCoinMapper;
import com.kumao.live.mapper.LiveCoinRechargeMapper;
import com.kumao.live.mapper.LiveGiftMapper;
import com.kumao.live.mapper.LiveGiftRecordMapper;
import com.kumao.live.mapper.LiveRoomMapper;
import com.kumao.live.websocket.LiveWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GiftService {

    private final LiveGiftMapper giftMapper;
    private final LiveGiftRecordMapper giftRecordMapper;
    private final LiveCoinMapper coinMapper;
    private final LiveCoinRechargeMapper coinRechargeMapper;
    private final LiveRoomMapper roomMapper;
    private final DanmakuService danmakuService;
    private final LiveWebSocketHandler webSocketHandler;

    public List<LiveGift> gifts() {
        return giftMapper.selectList(new LambdaQueryWrapper<LiveGift>()
                .eq(LiveGift::getStatus, 1).orderByAsc(LiveGift::getPrice));
    }

    public int balance(Long userId) {
        LiveCoin coin = getOrCreate(userId);
        return coin.getBalance();
    }

    @Transactional(rollbackFor = Exception.class)
    public LiveGiftRecord send(Long userId, String nickname, GiftSendRequest req) {
        LiveGift gift = giftMapper.selectById(req.getGiftId());
        if (gift == null || gift.getStatus() == null || gift.getStatus() != 1) {
            throw new BizException("礼物不存在或已下架");
        }
        LiveRoom room = roomMapper.selectById(req.getRoomId());
        if (room == null) {
            throw new BizException("直播间不存在");
        }
        if (!"LIVING".equals(room.getStatus())) {
            throw new BizException("直播间未开播，无法赠送礼物");
        }
        int count = req.getCount() == null || req.getCount() < 1 ? 1 : Math.min(req.getCount(), 99);
        int total = gift.getPrice() * count;

        LiveCoin coin = getOrCreate(userId);
        if (coin.getBalance() < total) {
            throw new BizException("酷猫币余额不足，请先充值");
        }
        coin.setBalance(coin.getBalance() - total);
        coinMapper.updateById(coin);

        LiveGiftRecord record = new LiveGiftRecord();
        record.setRoomId(room.getId());
        record.setGiftId(gift.getId());
        record.setGiftName(gift.getName());
        record.setGiftIcon(gift.getIcon());
        record.setPrice(gift.getPrice());
        record.setCount(count);
        record.setTotalPrice(total);
        record.setSenderId(userId);
        record.setSenderName(nickname);
        record.setAnchorId(room.getAnchorId());
        giftRecordMapper.insert(record);

        room.setGiftIncome((room.getGiftIncome() == null ? 0 : room.getGiftIncome()) + total);
        roomMapper.updateById(room);
        webSocketHandler.broadcastRoomMetrics(room.getId());

        // 广播礼物特效消息
        DanmakuMessage msg = new DanmakuMessage();
        msg.setRoomId(room.getId());
        msg.setUserId(userId);
        msg.setNickname(nickname);
        msg.setType("GIFT");
        msg.setGiftId(gift.getId());
        msg.setGiftCount(count);
        msg.setContent(String.format("%s 送出 %s x%d", nickname, gift.getName(), count));
        msg.setTimestamp(System.currentTimeMillis());
        danmakuService.broadcastAndPersist(msg);
        return record;
    }

    /**
     * 支付成功后的酷猫币入账。orderNo 唯一，支付回调重试时不会重复加币。
     */
    @Transactional(rollbackFor = Exception.class)
    public int creditCoins(Long userId, int amount, String orderNo) {
        if (userId == null || amount <= 0 || orderNo == null || orderNo.isBlank()) {
            throw new BizException("酷猫币入账参数无效");
        }
        LiveCoinRecharge existed = coinRechargeMapper.selectOne(new LambdaQueryWrapper<LiveCoinRecharge>()
                .eq(LiveCoinRecharge::getOrderNo, orderNo).last("limit 1"));
        if (existed != null) {
            return getOrCreate(userId).getBalance();
        }
        LiveCoin coin = getOrCreate(userId);
        coin.setBalance(coin.getBalance() + amount);
        coinMapper.updateById(coin);

        LiveCoinRecharge ledger = new LiveCoinRecharge();
        ledger.setUserId(userId);
        ledger.setOrderNo(orderNo);
        ledger.setAmount(amount);
        ledger.setBalanceAfter(coin.getBalance());
        coinRechargeMapper.insert(ledger);
        return coin.getBalance();
    }

    public PageResult<LiveGiftRecord> roomRecords(Long roomId, PageQuery query) {
        Page<LiveGiftRecord> page = giftRecordMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<LiveGiftRecord>()
                        .eq(roomId != null, LiveGiftRecord::getRoomId, roomId)
                        .orderByDesc(LiveGiftRecord::getCreateTime));
        return PageResult.of(page);
    }

    public PageResult<LiveGiftRecord> myReceived(Long anchorId, PageQuery query) {
        Page<LiveGiftRecord> page = giftRecordMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<LiveGiftRecord>()
                        .eq(LiveGiftRecord::getAnchorId, anchorId)
                        .orderByDesc(LiveGiftRecord::getCreateTime));
        return PageResult.of(page);
    }

    public long giftIncome(Long anchorId) {
        return giftRecordMapper.selectList(new LambdaQueryWrapper<LiveGiftRecord>()
                        .eq(LiveGiftRecord::getAnchorId, anchorId))
                .stream().mapToLong(r -> r.getTotalPrice() == null ? 0 : r.getTotalPrice()).sum();
    }

    /** 主播礼物清单汇总 */
    public Map<String, Object> giftSummary(Long anchorId) {
        List<LiveGiftRecord> records = giftRecordMapper.selectList(
                new LambdaQueryWrapper<LiveGiftRecord>().eq(LiveGiftRecord::getAnchorId, anchorId));
        Map<String, Long> byGift = records.stream().collect(Collectors.groupingBy(
                r -> r.getGiftName() == null ? "未知礼物" : r.getGiftName(),
                Collectors.summingLong(r -> r.getCount() == null ? 0 : r.getCount())));
        long total = records.stream().mapToLong(r -> r.getTotalPrice() == null ? 0 : r.getTotalPrice()).sum();
        return Map.of("totalIncome", total, "count", records.size(), "byGift", byGift);
    }

    // ---------------- 后台礼物配置 ----------------

    public LiveGift createGift(LiveGift gift) {
        if (gift.getStatus() == null) {
            gift.setStatus(1);
        }
        giftMapper.insert(gift);
        return gift;
    }

    public void updateGift(Long id, LiveGift gift) {
        gift.setId(id);
        giftMapper.updateById(gift);
    }

    public void deleteGift(Long id) {
        giftMapper.deleteById(id);
    }

    public PageResult<LiveGift> adminGifts(PageQuery query) {
        return PageResult.of(giftMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<LiveGift>().orderByAsc(LiveGift::getPrice)));
    }

    private LiveCoin getOrCreate(Long userId) {
        LiveCoin coin = coinMapper.selectOne(new LambdaQueryWrapper<LiveCoin>().eq(LiveCoin::getUserId, userId));
        if (coin == null) {
            coin = new LiveCoin();
            coin.setUserId(userId);
            coin.setBalance(1000);
            coinMapper.insert(coin);
        }
        return coin;
    }
}
