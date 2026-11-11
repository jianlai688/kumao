package com.kumao.musician.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收益明细
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("musician_income")
public class MusicianIncome extends BaseEntity {

    private Long musicianId;
    private Long userId;
    /** PLAY 播放收益 / GIFT 礼物收益 / WITHDRAW 提现 */
    private String type;
    private Integer amount;
    private String description;
    private String status;
    private Long bizId;
}
