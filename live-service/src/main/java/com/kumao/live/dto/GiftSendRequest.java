package com.kumao.live.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GiftSendRequest {

    @NotNull(message = "直播间ID不能为空")
    private Long roomId;
    @NotNull(message = "礼物ID不能为空")
    private Long giftId;
    private Integer count = 1;
}
