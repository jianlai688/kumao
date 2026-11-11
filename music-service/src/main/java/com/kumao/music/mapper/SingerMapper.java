package com.kumao.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kumao.music.entity.Singer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SingerMapper extends BaseMapper<Singer> {
}
