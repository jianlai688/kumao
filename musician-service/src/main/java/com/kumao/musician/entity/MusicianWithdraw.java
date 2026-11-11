package com.kumao.musician.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("musician_withdraw")
public class MusicianWithdraw extends BaseEntity {

    private Long musicianId;
    private Long userId;
    private Integer amount;
    private String account;
    /** ALIPAY / WECHAT */
    private String payType;
    /** PENDING / APPROVED / REJECTED */
    private String status;
    private String remark;
    private String auditTime;
}
