package com.kumao.vip.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vip_package")
public class VipPackage extends BaseEntity {

    private String name;
    /** MONTH / QUARTER / YEAR */
    private String type;
    private Integer days;
    /** COIN 类型套餐对应的酷猫币数量 */
    private Integer coinAmount;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String description;
    private Integer status;
    private Integer sort;
    private Integer deleted;
}
