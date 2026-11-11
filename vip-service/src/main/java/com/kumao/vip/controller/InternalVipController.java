package com.kumao.vip.controller;

import com.kumao.api.dto.VipStatusDTO;
import com.kumao.common.result.R;
import com.kumao.vip.dto.VipStatusVO;
import com.kumao.vip.service.VipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微服务内部接口：music-service、live-service 校验权限
 */
@RestController
@RequestMapping("/internal/vip")
@RequiredArgsConstructor
public class InternalVipController {

    private final VipService vipService;

    @GetMapping("/status")
    public R<VipStatusDTO> status(@RequestParam Long userId) {
        VipStatusVO vo = vipService.status(userId);
        VipStatusDTO dto = new VipStatusDTO();
        dto.setUserId(userId);
        dto.setVip(vo.isVip());
        dto.setLevel(vo.getLevel());
        dto.setExpireTime(vo.getExpireTime() == null ? null : vo.getExpireTime().toString());
        dto.setDanmakuColor(vo.getDanmakuColor());
        dto.setEntranceEffect(vo.getEntranceEffect());
        return R.ok(dto);
    }

    @GetMapping("/permission")
    public R<Boolean> permission(@RequestParam Long userId, @RequestParam String type) {
        return R.ok(vipService.hasPermission(userId, type));
    }
}
