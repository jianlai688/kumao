package com.kumao.musician.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kumao.musician.entity.Musician;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MusicianMapper extends BaseMapper<Musician> {
}
