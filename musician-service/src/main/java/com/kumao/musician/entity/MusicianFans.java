package com.kumao.musician.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("musician_fans")
public class MusicianFans extends BaseEntity {

    private Long musicianId;
    private Long userId;
    private String region;
}
