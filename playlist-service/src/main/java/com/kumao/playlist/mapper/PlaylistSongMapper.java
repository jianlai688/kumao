package com.kumao.playlist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kumao.playlist.entity.PlaylistSong;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlaylistSongMapper extends BaseMapper<PlaylistSong> {
}
