package com.kumao.music.service;

import com.kumao.music.dto.LyricLine;
import com.kumao.music.entity.Song;
import com.kumao.music.util.LrcParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${kumao.music.static-base-url:http://127.0.0.1:8080}")
    private String staticBaseUrl;

    public List<LyricLine> lines(Long songId) {
        return LrcParser.parse(raw(songId));
    }

    public String raw(Long songId) {
        Song song = songService.detail(songId);
        String url = song.getLyricUrl();
        if (!StringUtils.hasText(url)) {
            return "";
        }
        // 新版上传的歌词是文件地址（/static/...），需要先取回文件内容
        if (url.startsWith("/static/")) {
            String base = staticBaseUrl == null ? "" : staticBaseUrl.replaceAll("/+$", "");
            return fetch(base + url, url);
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return fetch(url, url);
        }
        return url;
    }

    private String fetch(String url, String label) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            log.warn("歌词读取失败 {}: {}", label, e.getMessage());
            return "";
        }
    }
}
