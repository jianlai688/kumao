package com.kumao.live.controller;

import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.live.dto.GiftSendRequest;
import com.kumao.live.entity.LiveGift;
import com.kumao.live.entity.LiveGiftRecord;
import com.kumao.live.service.GiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/live/gifts")
@RequiredArgsConstructor
public class GiftController {

    private final GiftService giftService;

    @GetMapping
    public R<List<LiveGift>> gifts() {
        return R.ok(giftService.gifts());
    }

    @GetMapping("/balance")
    public R<Integer> balance() {
        return R.ok(giftService.balance(UserContext.requireUserId()));
    }

    @PostMapping("/send")
    public R<LiveGiftRecord> send(@RequestBody @Valid GiftSendRequest req) {
        var user = UserContext.get();
        return R.ok(giftService.send(user.getUserId(), user.getUsername(), req));
    }

    /** 主播查看收到礼物清单 */
    @GetMapping("/received")
    public R<PageResult<LiveGiftRecord>> received(PageQuery query) {
        return R.ok(giftService.myReceived(UserContext.requireUserId(), query));
    }

    @GetMapping("/received/summary")
    public R<Map<String, Object>> summary() {
        return R.ok(giftService.giftSummary(UserContext.requireUserId()));
    }

    // ---------------- 后台礼物配置 ----------------

    @GetMapping("/admin/page")
    public R<PageResult<LiveGift>> adminPage(PageQuery query) {
        UserContext.requireAdmin();
        return R.ok(giftService.adminGifts(query));
    }

    @PostMapping("/admin")
    public R<LiveGift> create(@RequestBody LiveGift gift) {
        UserContext.requireAdmin();
        return R.ok(giftService.createGift(gift));
    }

    @PutMapping("/admin/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody LiveGift gift) {
        UserContext.requireAdmin();
        giftService.updateGift(id, gift);
        return R.ok();
    }

    @DeleteMapping("/admin/{id}")
    public R<Void> delete(@PathVariable Long id) {
        UserContext.requireAdmin();
        giftService.deleteGift(id);
        return R.ok();
    }
}
