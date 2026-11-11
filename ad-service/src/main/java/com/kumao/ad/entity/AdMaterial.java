package com.kumao.ad.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ad_material")
public class AdMaterial extends BaseEntity {

    private Long advertiserId;
    private Long userId;
    private String name;
    /** AUDIO / VIDEO / IMAGE_TEXT */
    private String type;
    private String coverUrl;
    private String fileUrl;
    private String content;
    /** PENDING / APPROVED / REJECTED / OFFLINE */
    private String status;
    private String rejectReason;
    private Long exposureCount;
    private Long clickCount;
    private Integer deleted;
}
