package com.kumao.music.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 首页展示设置（单行配置，id 固定 1）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("home_setting")
public class HomeSetting extends BaseEntity {

    /** 今日主打是否自动轮播：1 开 0 关 */
    private Integer heroAutoplay;

    /** 自动切换间隔（秒） */
    private Integer heroInterval;

    /** 参与轮播的歌曲条数（HOT 模式生效） */
    private Integer heroCount;

    /** HOT=热歌榜自动取前 N 首；MANUAL=手动选歌 */
    private String heroMode;

    /** 手动选歌的歌曲 ID，逗号分隔，按顺序展示 */
    private String heroSongIds;

    /** 解析后的轮播歌曲（不入库，仅接口返回） */
    @TableField(exist = false)
    private List<Song> songs;
}
