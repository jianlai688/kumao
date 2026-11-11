package com.kumao.api.client;

import com.kumao.api.dto.VipStatusDTO;
import com.kumao.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "vip-service", contextId = "vipClient")
public interface VipClient {

    @GetMapping("/internal/vip/status")
    R<VipStatusDTO> status(@RequestParam("userId") Long userId);

    /**
     * @param type SONG / MV_HD / DANMAKU / ENTRANCE
     */
    @GetMapping("/internal/vip/permission")
    R<Boolean> hasPermission(@RequestParam("userId") Long userId, @RequestParam("type") String type);
}
