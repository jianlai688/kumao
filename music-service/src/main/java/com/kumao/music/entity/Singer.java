package com.kumao.music.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("singer")
public class Singer extends BaseEntity {

    private String name;
    private String avatar;
    private String gender;
    /** 国内 / 国外 / 港台 / 欧美 / 日韩 */
    private String region;
    private String intro;
    /** 0 待审核 / 1 已认证 / 2 未通过 */
    private Integer status;
    private Long musicianId;
    private Long userId;
    private String rejectReason;
    private String auditTime;
    private Long fansCount;
    private Integer deleted;
}
