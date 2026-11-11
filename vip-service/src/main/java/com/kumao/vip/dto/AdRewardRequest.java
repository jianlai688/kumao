package com.kumao.vip.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 激励广告会员奖励请求。
 */
@Data
public class AdRewardRequest implements Serializable {

    @NotNull(message = "广告计划不能为空")
    private Long planId;

    @NotNull(message = "广告观看时长不能为空")
    @Min(value = 15, message = "请完整观看 15 秒广告后再领取")
    private Integer watchedSeconds;
}
