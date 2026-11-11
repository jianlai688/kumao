package com.kumao.playlist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kumao.playlist.entity.Playlist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlaylistMapper extends BaseMapper<Playlist> {
}
