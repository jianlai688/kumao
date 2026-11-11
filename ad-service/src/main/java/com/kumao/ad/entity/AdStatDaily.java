package com.kumao.ad.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ad_stat_daily")
public class AdStatDaily extends BaseEntity {

    private Long advertiserId;
    private Long materialId;
    private Long planId;
    private LocalDate statDate;
    private Long exposureCount;
    private Long clickCount;
    private String region;
    private Integer hourSlot;
}
