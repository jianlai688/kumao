package com.kumao.vip.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vip_member")
public class VipMember extends BaseEntity {

    private Long userId;
    private String level;
    private LocalDateTime startTime;
    private LocalDateTime expireTime;
    /** ACTIVE / EXPIRED */
    private String status;
    private Integer totalDays;
    private String danmakuColor;
    private String entranceEffect;
    private Integer deleted;
}
