package com.kumao.playlist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("playlist_collect")
public class PlaylistCollect extends BaseEntity {

    private Long playlistId;
    private Long userId;
}
