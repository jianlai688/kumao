package com.kumao.music.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("album")
public class Album extends BaseEntity {

    private String name;
    private Long singerId;
    private String singerName;
    private String coverUrl;
    private String intro;
    private LocalDate releaseDate;
    private Integer status;
    private Long musicianId;
    private Integer deleted;
}
