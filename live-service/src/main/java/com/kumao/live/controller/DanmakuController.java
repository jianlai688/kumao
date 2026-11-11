package com.kumao.live.controller;

import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.live.dto.DanmakuMessage;
import com.kumao.live.entity.LiveDanmaku;
import com.kumao.live.service.DanmakuService;
import com.kumao.live.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/live/danmaku")
@RequiredArgsConstructor
public class DanmakuController {

    private final DanmakuService danmakuService;
    private final SensitiveWordService sensitiveWordService;

    @GetMapping("/history/{roomId}")
    public R<PageResult<LiveDanmaku>> history(@PathVariable Long roomId, PageQuery query) {
        return R.ok(danmakuService.history(roomId, query));
    }

    /** HTTP 发送弹幕（WebSocket 的降级通道） */
    @PostMapping("/send")
    public R<Void> send(@RequestBody DanmakuMessage message) {
        var user = UserContext.get();
        message.setUserId(user.getUserId());
        message.setNickname(user.getUsername());
        if (message.getColor() == null) {
            message.setColor("#FFFFFF");
        }
        danmakuService.broadcastAndPersist(message);
        return R.ok();
    }

    /** 敏感词预检，供前端即时提示 */
    @GetMapping("/check")
    public R<Boolean> check(@RequestParam String content) {
        return R.ok(sensitiveWordService.contains(content));
    }

    // ---------------- 后台 ----------------

    @GetMapping("/admin/page")
    public R<PageResult<LiveDanmaku>> adminPage(PageQuery query,
                                                @RequestParam(required = false) Long roomId) {
        UserContext.requireAdmin();
        return R.ok(danmakuService.adminPage(query, roomId));
    }

    @DeleteMapping("/admin/{id}")
    public R<Void> adminDelete(@PathVariable Long id) {
        UserContext.requireAdmin();
        danmakuService.adminDelete(id);
        return R.ok();
    }
}
