package com.kumao.music.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("banner")
public class Banner extends BaseEntity {

    private String title;
    private String imageUrl;
    private String linkUrl;
    private Integer sort;
    private Integer status;
    private Integer deleted;
}
