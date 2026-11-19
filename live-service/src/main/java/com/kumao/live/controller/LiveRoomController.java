package com.kumao.live.controller;

import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.LoginUser;
import com.kumao.common.security.UserContext;
import com.kumao.live.dto.RoomRequest;
import com.kumao.live.dto.RoomVO;
import com.kumao.live.entity.LiveRoom;
import com.kumao.live.entity.LiveSettlement;
import com.kumao.live.service.LiveRoomService;
import com.kumao.live.service.LiveSettlementService;
import com.kumao.live.service.ObsConfigService;
import com.kumao.live.service.PushAuthService;
import com.kumao.live.service.AudienceService;
import com.kumao.live.support.QrCodeUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/live/rooms")
@RequiredArgsConstructor
public class LiveRoomController {

    private final LiveRoomService roomService;
    private final PushAuthService pushAuthService;
    private final LiveSettlementService settlementService;
    private final ObsConfigService obsConfigService;
    private final AudienceService audienceService;

    /** 主播的结算单列表（最近 50 场） */
    @GetMapping("/settlements/mine")
    public R<List<LiveSettlement>> mySettlements() {
        return R.ok(settlementService.mine(UserContext.requireUserId()));
    }

    /** 指定直播间的最近一场结算单 */
    @GetMapping("/{id}/settlement")
    public R<LiveSettlement> roomSettlement(@PathVariable Long id) {
        return R.ok(settlementService.byRoom(id));
    }

    @GetMapping("/square")
    public R<PageResult<RoomVO>> square(PageQuery query,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(required = false) String scene) {
        return R.ok(roomService.square(query, category, scene));
    }

    @GetMapping("/categories")
    public R<List<String>> categories() {
        return R.ok(List.of("弹唱", "电台", "翻唱"));
    }

    /** 直播场景：室内 / 户外 */
    @GetMapping("/scenes")
    public R<List<Map<String, String>>> scenes() {
        return R.ok(List.of(
                Map.of("value", "INDOOR", "label", "室内直播"),
                Map.of("value", "OUTDOOR", "label", "户外直播")
        ));
    }

    @GetMapping("/{id}")
    public R<RoomVO> detail(@PathVariable Long id) {
        return R.ok(roomService.detail(id, UserContext.getUserId()));
    }

    @GetMapping("/{id}/online")
    public R<Long> online(@PathVariable Long id) {
        return R.ok(roomService.detail(id, UserContext.getUserId()).getRealtimeOnline());
    }

    // ---------------- 主播端 ----------------

    @PostMapping
    public R<Long> create(@RequestBody @Valid RoomRequest req) {
        LoginUser user = UserContext.get();
        if (!user.hasRole("ANCHOR", "ADMIN")) {
            throw new com.kumao.common.exception.BizException(403, "仅主播可创建直播间");
        }
        return R.ok(roomService.create(user.getUserId(), user.getUsername(), req));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid RoomRequest req) {
        roomService.update(id, UserContext.requireUserId(), req);
        return R.ok();
    }

    /** 获取推流地址（仅主播本人）；户外直播额外返回手机扫码推流二维码 */
    @GetMapping("/{id}/push-url")
    public R<Map<String, Object>> pushUrl(@PathVariable Long id) {
        LiveRoom room = roomService.require(id);
        if (!room.getAnchorId().equals(UserContext.requireUserId())) {
            throw new com.kumao.common.exception.BizException(403, "无权获取该直播间推流地址");
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("pushUrl", room.getPushUrl());
        result.put("pullUrl", room.getPullUrl());
        result.put("scene", room.getScene());
        result.put("location", room.getLocation());
        boolean outdoor = "OUTDOOR".equals(room.getScene());
        if (outdoor) {
            result.put("pushQr", QrCodeUtils.toDataUrl(room.getPushUrl(), 300));
            result.put("tip", "户外直播：在手机推流 App 中扫码或粘贴推流地址即可开播，建议使用 4G/5G 网络并开启摄像头与麦克风权限。");
        } else {
            result.put("tip", "使用 OBS 推流到以下地址即可开播（SRS）。");
        }
        return R.ok(result);
    }

    /** 实时观众画像：当前在线登录用户的性别/年龄/地区分布 */
    @GetMapping("/{id}/audience")
    public R<Map<String, Object>> audience(@PathVariable Long id) {
        UserContext.requireUserId();
        return R.ok(audienceService.snapshot(id));
    }

    /** 获取本机 OBS WebSocket 配置（仅主播/管理员），用于控制台自动填充 */
    @GetMapping("/obs-config")
    public R<java.util.Map<String, Object>> obsConfig() {
        UserContext.requireUserId();
        var user = UserContext.get();
        if (user == null || !user.hasRole("ANCHOR", "ADMIN")) {
            throw new com.kumao.common.exception.BizException(403, "仅主播可读取 OBS 配置");
        }
        return R.ok(obsConfigService.config());
    }

    /** 开播前设置直播歌单（主播本人） */
    @PutMapping("/{id}/playlist")
    public R<LiveRoom> updatePlaylist(@PathVariable Long id, @RequestBody Map<String, String> body) {
        roomService.updatePlaylist(id, UserContext.requireUserId(), body.get("songIds"));
        return R.ok(roomService.require(id));
    }

    @PostMapping("/{id}/start")
    public R<LiveRoom> start(@PathVariable Long id,
                             @RequestParam(defaultValue = "false") boolean skipAutoPush) {
        return R.ok(roomService.start(id, UserContext.requireUserId(), skipAutoPush));
    }

    /** 直播间点赞（仅登录用户） */
    @PostMapping("/{id}/like")
    public R<Long> like(@PathVariable Long id) {
        UserContext.requireUserId();
        return R.ok(roomService.like(id));
    }

    /** 一键下播 */
    @PostMapping("/{id}/stop")
    public R<LiveRoom> stop(@PathVariable Long id) {
        return R.ok(roomService.stop(id, UserContext.requireUserId()));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roomService.delete(id, UserContext.getUserId(), UserContext.isAdmin());
        return R.ok();
    }

    @GetMapping("/mine")
    public R<PageResult<LiveRoom>> mine(PageQuery query) {
        return R.ok(roomService.myRooms(UserContext.requireUserId(), query));
    }

    /** SRS 推流鉴权回调 */
    @GetMapping("/push-auth")
    public R<Boolean> pushAuth(@RequestParam String stream,
                               @RequestParam Long anchorId,
                               @RequestParam String token) {
        roomService.require(Long.parseLong(stream.replace("room_", "")));
        return R.ok(pushAuthService.verifyPush(stream, anchorId, token));
    }

    // ---------------- 后台 ----------------

    @GetMapping("/admin/page")
    public R<PageResult<LiveRoom>> adminPage(PageQuery query,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) Integer banned) {
        UserContext.requireAdmin();
        return R.ok(roomService.adminPage(query, status, banned));
    }

    @PostMapping("/admin/{id}/ban")
    public R<Void> ban(@PathVariable Long id,
                       @RequestParam boolean banned,
                       @RequestParam(required = false) String reason) {
        UserContext.requireAdmin();
        roomService.ban(id, banned, reason);
        return R.ok();
    }

    @GetMapping("/admin/statistics")
    public R<Map<String, Object>> statistics() {
        UserContext.requireAdmin();
        return R.ok(roomService.statistics());
    }
}
