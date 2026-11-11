package com.kumao.musician.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("musician_album")
public class MusicianAlbum extends BaseEntity {

    private Long musicianId;
    private Long userId;
    private String name;
    private String coverUrl;
    private String intro;
    private LocalDate releaseDate;
    private Integer songCount;
    private Integer status;
    private Integer deleted;
}
