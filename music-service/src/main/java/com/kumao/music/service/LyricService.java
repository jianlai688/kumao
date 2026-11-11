package com.kumao.music.service;

import com.kumao.music.dto.LyricLine;
import com.kumao.music.entity.Song;
import com.kumao.music.util.LrcParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * 歌词服务：读取 LRC 文件并解析为带时间轴的行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LyricService {

    private final SongService songService;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public List<LyricLine> lines(Long songId) {
        return LrcParser.parse(raw(songId));
    }

    public String raw(Long songId) {
        Song song = songService.detail(songId);
        String url = song.getLyricUrl();
        if (!StringUtils.hasText(url)) {
            return "";
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build();
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            } catch (Exception e) {
                log.warn("远程歌词读取失败 {}: {}", url, e.getMessage());
                return "";
            }
        }
        return url;
    }
}
