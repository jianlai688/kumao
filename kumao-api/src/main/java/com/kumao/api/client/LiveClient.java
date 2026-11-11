package com.kumao.api.client;

import com.kumao.api.dto.LiveRoomDTO;
import com.kumao.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "live-service", contextId = "liveClient")
public interface LiveClient {

    @GetMapping("/internal/live/gift-income")
    R<Long> giftIncome(@RequestParam("anchorId") Long anchorId);

    @GetMapping("/internal/live/rooms/{id}")
    R<LiveRoomDTO> room(@PathVariable("id") Long id);

    @PostMapping("/internal/live/coins/credit")
    R<Integer> creditCoins(@RequestParam("userId") Long userId,
                           @RequestParam("amount") int amount,
                           @RequestParam("orderNo") String orderNo);
}
