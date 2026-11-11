package com.kumao.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kumao.music.entity.Song;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SongMapper extends BaseMapper<Song> {

    @Update("update song set play_count = play_count + #{delta} where id = #{id}")
    int addPlayCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("update song set like_count = like_count + #{delta} where id = #{id}")
    int addLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("update song set comment_count = comment_count + #{delta} where id = #{id}")
    int addCommentCount(@Param("id") Long id, @Param("delta") int delta);
}
