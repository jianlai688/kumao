package com.kumao.playlist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("playlist_song")
public class PlaylistSong extends BaseEntity {

    private Long playlistId;
    private Long songId;
    private String songName;
    private String singerName;
    private String coverUrl;
    private String audioUrl;
    private Integer duration;
    private Integer sort;
}
