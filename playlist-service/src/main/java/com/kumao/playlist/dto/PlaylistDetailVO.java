package com.kumao.playlist.dto;

import com.kumao.playlist.entity.Playlist;
import com.kumao.playlist.entity.PlaylistSong;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlaylistDetailVO extends Playlist {

    private List<PlaylistSong> songs;
    private Boolean collected;
}
