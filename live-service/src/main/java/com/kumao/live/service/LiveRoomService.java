package com.kumao.live.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.api.client.SearchClient;
import com.kumao.api.client.VipClient;
import com.kumao.api.dto.SearchDocDTO;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.live.dto.RoomRequest;
import com.kumao.live.dto.RoomVO;
import com.kumao.live.entity.LiveRoom;
import com.kumao.live.mapper.LiveRoomMapper;
import com.kumao.live.websocket.LiveWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveRoomService {

    private final LiveRoomMapper roomMapper;
    private final PushAuthService pushAuthService;
    private final OnlineCountService onlineCountService;
    private final VipClient vipClient;
    private final SearchClient searchClient;
    private final LiveWebSocketHandler webSocketHandler;
    private final LiveSettlementService settlementService;
    private final AutoPushService autoPushService;

    public PageResult<RoomVO> square(PageQuery query, String category, String scene) {
        Page<LiveRoom> page = roomMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<LiveRoom>()
                        .eq(LiveRoom::getStatus, "LIVING")
                        .eq(LiveRoom::getBanned, 0)
                        .eq(StringUtils.hasText(category), LiveRoom::getCategory, category)
                        .eq(StringUtils.hasText(scene), LiveRoom::getScene, scene)
                        .and(StringUtils.hasText(query.getKeyword()), w -> w
                                .like(LiveRoom::getTitle, query.getKeyword())
                                .or().like(LiveRoom::getAnchorName, query.getKeyword()))
                        .orderByDesc(LiveRoom::getOnlineCount));
        List<RoomVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    public RoomVO detail(Long id, Long userId) {
        LiveRoom room = require(id);
        RoomVO vo = toVO(room);
        if (userId != null) {
            try {
                var r = vipClient.status(userId);
                if (r != null && r.getData() != null && r.getData().isVip()) {
                    vo.setVip(true);
                    vo.setDanmakuColor(r.getData().getDanmakuColor());
                    vo.setEntranceEffect(r.getData().getEntranceEffect());
                }
            } catch (Exception e) {
                log.debug("VIP 状态查询失败: {}", e.getMessage());
            }
        }
        return vo;
    }

    public LiveRoom require(Long id) {
        LiveRoom room = roomMapper.selectById(id);
        if (room == null) {
            throw new BizException("直播间不存在");
        }
        return room;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(Long anchorId, String anchorName, RoomRequest req) {
        Long existing = roomMapper.selectCount(new LambdaQueryWrapper<LiveRoom>()
                .eq(LiveRoom::getAnchorId, anchorId)
                .in(LiveRoom::getStatus, "NOT_STARTED", "LIVING")
                .eq(LiveRoom::getBanned, 0));
        if (existing != null && existing > 0) {
            throw new BizException("您已有未结束的直播间，请先关闭后再创建");
        }
        LiveRoom room = new LiveRoom();
        room.setTitle(req.getTitle());
        room.setIntro(req.getIntro());
        room.setCoverUrl(req.getCoverUrl());
        room.setCategory(req.getCategory());
        room.setScene(normalizeScene(req.getScene()));
        room.setLocation(normalizeLocation(room.getScene(), req.getLocation()));
        room.setAnchorId(anchorId);
        room.setAnchorName(anchorName);
        room.setStatus("NOT_STARTED");
        room.setOnlineCount(0L);
        room.setPeakOnline(0L);
        room.setLikeCount(0L);
        room.setGiftIncome(0L);
        room.setBanned(0);
        roomMapper.insert(room);
        room.setPushUrl(pushAuthService.buildPushUrl(room));
        room.setPullUrl(pushAuthService.buildPullUrl(room));
        roomMapper.updateById(room);
        return room.getId();
    }

    /**
     * 开播前设置直播歌单：仅主播本人可改，最多 50 首，按顺序播放。
     */
    public void updatePlaylist(Long roomId, Long anchorId, String songIds) {
        LiveRoom room = requireOwned(roomId, anchorId);
        room.setPlaylistIds(normalizeSongIds(songIds));
        roomMapper.updateById(room);
    }

    private String normalizeSongIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> s.matches("\\d{1,19}"))
                .distinct()
                .limit(50)
                .collect(java.util.stream.Collectors.joining(","));
    }

    public void update(Long roomId, Long anchorId, RoomRequest req) {
        LiveRoom room = requireOwned(roomId, anchorId);
        room.setTitle(req.getTitle());
        room.setIntro(req.getIntro());
        room.setCoverUrl(req.getCoverUrl());
        room.setCategory(req.getCategory());
        room.setScene(normalizeScene(req.getScene()));
        room.setLocation(normalizeLocation(room.getScene(), req.getLocation()));
        roomMapper.updateById(room);
        if (!"OUTDOOR".equals(room.getScene())) {
            // updateById 会忽略 null 字段，切回室内时显式清空户外地点
            roomMapper.update(null, new LambdaUpdateWrapper<LiveRoom>()
                    .eq(LiveRoom::getId, room.getId())
                    .set(LiveRoom::getLocation, null));
            room.setLocation(null);
        }
        syncSearch(room);
    }

    public LiveRoom start(Long roomId, Long anchorId) {
        return start(roomId, anchorId, false);
    }

    /**
     * 开播：状态置为 LIVING，生成推流地址；没有现成推流时自动启动演示推流。
     * skipAutoPush=true 用于前端已经控制 OBS 开始推流的场景，避免重复推同一路流。
     */
    public LiveRoom start(Long roomId, Long anchorId, boolean skipAutoPush) {
        LiveRoom room = requireOwned(roomId, anchorId);
        if ("BANNED".equals(room.getStatus())) {
            throw new BizException("直播间已被封禁，无法开播");
        }
        if ("LIVING".equals(room.getStatus())) {
            throw new BizException("直播间已在直播中");
        }
        room.setPushUrl(pushAuthService.buildPushUrl(room));
        room.setPullUrl(pushAuthService.buildPullUrl(room));
        // 主播可能已经用 OBS/手机推流，只有 SRS 上没有流时才自动推流
        if (!skipAutoPush) {
            autoPushService.startIfNeeded(room);
        }
        room.setStatus("LIVING");
        room.setLiveStartedAt(LocalDateTime.now());
        // 每场直播重新累计真实互动数据
        room.setOnlineCount(0L);
        room.setPeakOnline(0L);
        room.setLikeCount(0L);
        room.setGiftIncome(0L);
        roomMapper.updateById(room);
        onlineCountService.clear(roomId);
        // 重新开播时清空上一场回放地址（updateById 会忽略 null 字段，需显式 set）
        roomMapper.update(null, new LambdaUpdateWrapper<LiveRoom>()
                .eq(LiveRoom::getId, room.getId())
                .set(LiveRoom::getReplayUrl, null));
        room.setReplayUrl(null);
        syncSearch(room);
        return room;
    }

    /**
     * 一键下播 / 关闭：生成回放地址（SRS 录制文件由回调写入 MinIO）
     */
    @Transactional(rollbackFor = Exception.class)
    public LiveRoom stop(Long roomId, Long anchorId) {
        LiveRoom room = requireOwned(roomId, anchorId);
        autoPushService.stop(roomId);
        if ("ENDED".equals(room.getStatus())) {
            throw new BizException("直播间已结束");
        }
        long peak = onlineCountService.peak(roomId);
        if (room.getPeakOnline() == null || peak > room.getPeakOnline()) {
            room.setPeakOnline(peak);
        }
        room.setStatus("ENDED");
        room.setOnlineCount(0L);
        // SRS 录制回放：实际由 SRS on_dvr 回调写入 MinIO，这里生成预期地址
        room.setReplayUrl(String.format("%s/replay/%s.mp4", "http://127.0.0.1:9000/kumao", "room_" + roomId));
        roomMapper.updateById(room);
        // 下播结算：按本场礼物总额结算，平台抽成后剩余归主播
        settlementService.settle(room);
        onlineCountService.clear(roomId);
        syncSearch(room);
        return room;
    }

    /** 真实点赞：每次登录用户点击累加，并实时广播本场指标。 */
    public long like(Long roomId) {
        LiveRoom room = require(roomId);
        if (!"LIVING".equals(room.getStatus())) {
            throw new BizException("直播未开始，无法点赞");
        }
        roomMapper.update(null, new LambdaUpdateWrapper<LiveRoom>()
                .eq(LiveRoom::getId, roomId)
                .setSql("like_count = COALESCE(like_count, 0) + 1"));
        webSocketHandler.broadcastRoomMetrics(roomId);
        LiveRoom updated = roomMapper.selectById(roomId);
        return updated == null || updated.getLikeCount() == null ? 0 : updated.getLikeCount();
    }

    public void delete(Long roomId, Long anchorId, boolean admin) {
        LiveRoom room = require(roomId);
        if (!admin && !room.getAnchorId().equals(anchorId)) {
            throw new BizException(403, "只能操作自己的直播间");
        }
        if ("LIVING".equals(room.getStatus())) {
            throw new BizException("直播中不可删除，请先下播");
        }
        autoPushService.stop(roomId);
        roomMapper.deleteById(roomId);
        try {
            searchClient.delete("LIVE_ROOM", roomId);
        } catch (Exception e) {
            log.debug("删除直播间索引失败: {}", e.getMessage());
        }
    }

    public PageResult<LiveRoom> myRooms(Long anchorId, PageQuery query) {
        Page<LiveRoom> page = roomMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<LiveRoom>()
                        .eq(LiveRoom::getAnchorId, anchorId)
                        .orderByDesc(LiveRoom::getCreateTime));
        return PageResult.of(page);
    }

    // ---------------- 后台 ----------------

    public PageResult<LiveRoom> adminPage(PageQuery query, String status, Integer banned) {
        Page<LiveRoom> page = roomMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<LiveRoom>()
                        .eq(StringUtils.hasText(status), LiveRoom::getStatus, status)
                        .eq(banned != null, LiveRoom::getBanned, banned)
                        .like(StringUtils.hasText(query.getKeyword()), LiveRoom::getTitle, query.getKeyword())
                        .orderByDesc(LiveRoom::getCreateTime));
        return PageResult.of(page);
    }

    @Transactional(rollbackFor = Exception.class)
    public void ban(Long roomId, boolean banned, String reason) {
        LiveRoom room = require(roomId);
        if (banned) {
            autoPushService.stop(roomId);
        }
        room.setBanned(banned ? 1 : 0);
        room.setBanReason(banned ? reason : null);
        if (banned) {
            room.setStatus("BANNED");
            room.setOnlineCount(0L);
            onlineCountService.clear(roomId);
        } else if ("BANNED".equals(room.getStatus())) {
            room.setStatus("ENDED");
        }
        roomMapper.updateById(room);
        syncSearch(room);
    }

    public java.util.Map<String, Object> statistics() {
        long total = roomMapper.selectCount(null);
        long living = roomMapper.selectCount(new LambdaQueryWrapper<LiveRoom>().eq(LiveRoom::getStatus, "LIVING"));
        long banned = roomMapper.selectCount(new LambdaQueryWrapper<LiveRoom>().eq(LiveRoom::getBanned, 1));
        long totalOnline = roomMapper.selectList(new LambdaQueryWrapper<LiveRoom>().eq(LiveRoom::getStatus, "LIVING"))
                .stream().mapToLong(r -> onlineCountService.count(r.getId())).sum();
        return java.util.Map.of("total", total, "living", living, "banned", banned, "online", totalOnline);
    }

    private RoomVO toVO(LiveRoom room) {
        RoomVO vo = new RoomVO();
        BeanUtils.copyProperties(room, vo);
        long online = onlineCountService.count(room.getId());
        vo.setRealtimeOnline(online);
        vo.setOnlineCount(online);
        vo.setTotalViewers(onlineCountService.totalViewers(room.getId()));
        if ("LIVING".equals(room.getStatus())) {
            vo.setPeakOnline(onlineCountService.peak(room.getId()));
        }
        return vo;
    }

    private LiveRoom requireOwned(Long roomId, Long anchorId) {
        LiveRoom room = require(roomId);
        if (!room.getAnchorId().equals(anchorId)) {
            throw new BizException(403, "只能操作自己的直播间");
        }
        return room;
    }

    /**
     * 直播场景标准化：仅支持 INDOOR 室内 / OUTDOOR 户外
     */
    private String normalizeScene(String scene) {
        if (!StringUtils.hasText(scene)) {
            return "INDOOR";
        }
        String value = scene.trim().toUpperCase();
        if (!"INDOOR".equals(value) && !"OUTDOOR".equals(value)) {
            throw new BizException("直播场景仅支持 INDOOR（室内）或 OUTDOOR（户外）");
        }
        return value;
    }

    private String normalizeLocation(String scene, String location) {
        if (!"OUTDOOR".equals(scene)) {
            return null;
        }
        return StringUtils.hasText(location) ? location.trim() : null;
    }

    public void syncSearch(LiveRoom room) {
        try {
            SearchDocDTO doc = new SearchDocDTO();
            doc.setBizId(room.getId());
            doc.setType("LIVE_ROOM");
            doc.setTitle(room.getTitle());
            doc.setSubtitle(room.getAnchorName());
            doc.setCoverUrl(room.getCoverUrl());
            doc.setTags(room.getCategory() + ("OUTDOOR".equals(room.getScene()) ? ",户外" : ",室内"));
            doc.setDescription(room.getIntro() + (StringUtils.hasText(room.getLocation()) ? " · " + room.getLocation() : ""));
            doc.setHeat(onlineCountService.count(room.getId()));
            doc.setCreateTime(LocalDateTime.now().toString());
            if ("LIVING".equals(room.getStatus())) {
                searchClient.sync(doc);
            } else {
                searchClient.delete("LIVE_ROOM", room.getId());
            }
        } catch (Exception e) {
            log.debug("直播间索引同步失败: {}", e.getMessage());
        }
    }
}
