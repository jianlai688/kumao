package com.kumao.api.client;

import com.kumao.api.dto.SongDTO;
import com.kumao.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "music-service", contextId = "musicClient")
public interface MusicClient {

    @GetMapping("/internal/music/songs/{id}")
    R<SongDTO> getSong(@PathVariable("id") Long id);

    @GetMapping("/internal/music/songs/batch")
    R<List<SongDTO>> batchSongs(@RequestParam("ids") List<Long> ids);

    @PostMapping("/internal/music/songs")
    R<Long> createSong(@RequestBody SongDTO song);

    @DeleteMapping("/internal/music/songs/{id}")
    R<Void> deleteSong(@PathVariable("id") Long id);

    @PostMapping("/internal/music/songs/{id}/play-count")
    R<Void> addPlayCount(@PathVariable("id") Long id, @RequestParam("delta") int delta);

    @PostMapping("/internal/music/songs/{id}/like-count")
    R<Void> likeCount(@PathVariable("id") Long id, @RequestParam("delta") int delta);

    @PostMapping("/internal/music/songs/{id}/comment-count")
    R<Void> commentCount(@PathVariable("id") Long id, @RequestParam("delta") int delta);
}
