package com.kumao.live.controller;

import com.kumao.common.result.R;
import com.kumao.live.entity.LiveGiftRecord;
import com.kumao.live.service.GiftService;
import com.kumao.live.service.LiveRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微服务内部接口：供 musician-service 对接礼物收益
 */
@RestController
@RequestMapping("/internal/live")
@RequiredArgsConstructor
public class InternalLiveController {

    private final GiftService giftService;
    private final LiveRoomService roomService;

    @GetMapping("/gift-income")
    public R<Long> giftIncome(@RequestParam Long anchorId) {
        return R.ok(giftService.giftIncome(anchorId));
    }

    @PostMapping("/coins/credit")
    public R<Integer> creditCoins(@RequestParam Long userId,
                                  @RequestParam int amount,
                                  @RequestParam String orderNo) {
        return R.ok(giftService.creditCoins(userId, amount, orderNo));
    }

    @GetMapping("/rooms/{id}")
    public R<com.kumao.api.dto.LiveRoomDTO> room(@PathVariable Long id) {
        com.kumao.live.entity.LiveRoom room = roomService.require(id);
        com.kumao.api.dto.LiveRoomDTO dto = new com.kumao.api.dto.LiveRoomDTO();
        dto.setId(room.getId());
        dto.setTitle(room.getTitle());
        dto.setAnchorId(room.getAnchorId());
        dto.setAnchorName(room.getAnchorName());
        dto.setCoverUrl(room.getCoverUrl());
        dto.setCategory(room.getCategory());
        dto.setStatus(room.getStatus());
        dto.setOnlineCount(room.getOnlineCount());
        dto.setPullUrl(room.getPullUrl());
        return R.ok(dto);
    }
}
