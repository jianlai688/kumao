package com.kumao.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kumao.music.entity.Album;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlbumMapper extends BaseMapper<Album> {
}
