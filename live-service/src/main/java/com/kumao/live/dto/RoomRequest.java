package com.kumao.live.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoomRequest {

    @NotBlank(message = "直播间标题不能为空")
    private String title;
    private String intro;
    private String coverUrl;
    /** 弹唱 / 电台 / 翻唱 */
    @NotBlank(message = "直播分类不能为空")
    private String category;
    /** 直播场景：INDOOR 室内 / OUTDOOR 户外，默认 INDOOR */
    private String scene = "INDOOR";
    /** 户外直播地点（户外场景建议填写） */
    @Size(max = 200, message = "直播地点不能超过 200 字")
    private String location;
}
