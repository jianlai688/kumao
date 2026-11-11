package com.kumao.musician.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 作品日播放统计（趋势图）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("musician_play_stat")
public class MusicianPlayStat extends BaseEntity {

    private Long musicianId;
    private Long workId;
    private LocalDate statDate;
    private Long playCount;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
}
