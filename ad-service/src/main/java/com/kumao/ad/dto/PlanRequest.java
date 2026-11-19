package com.kumao.ad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlanRequest {

    @NotNull(message = "素材ID不能为空")
    private Long materialId;
    /** SPLASH / HOME_BANNER / LIVE / PLAYER / VIP_REWARD */
    @NotBlank(message = "广告位不能为空")
    private String slot;
    @NotNull(message = "投放开始时间不能为空")
    private LocalDateTime startTime;
    @NotNull(message = "投放结束时间不能为空")
    private LocalDateTime endTime;
    private String targetAudience = "ALL";
}
