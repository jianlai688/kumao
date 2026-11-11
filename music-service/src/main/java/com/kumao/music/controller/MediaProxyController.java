package com.kumao.music.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * 音频代理：第三方音源大多没有 CORS 头，App 端要做“真实下载/离线缓存”必须走同源代理。
 * 仅允许 http/https，且限制在音源白名单域名内，避免被当成任意 URL 代理。
 */
@Slf4j
@RestController
@RequestMapping("/proxy")
public class MediaProxyController {

    private static final List<String> ALLOWED_HOST_SUFFIXES = List.of(
            "soundhelix.com",
            "kuwo.cn",
            "kugou.com",
            "qq.com",
            "music.163.com",
            "126.net",
            "bilibili.com",
            "picsum.photos",
            "dicebear.com",
            "aliyuncs.com",
            "myqcloud.com"
    );

    private static final int CONNECT_TIMEOUT = 8000;
    private static final int READ_TIMEOUT = 30000;

    @GetMapping("/media")
    public void media(@RequestParam("url") String url, HttpServletResponse response) {
        if (url == null || url.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        URL target;
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            String lowerHost = host.toLowerCase();
            boolean allowed = ALLOWED_HOST_SUFFIXES.stream().anyMatch(lowerHost::endsWith);
            if (!allowed) {
                log.warn("拒绝代理非白名单域名: {}", host);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            target = uri.toURL();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) target.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "KuMao-Proxy/1.0");
            int status = conn.getResponseCode();
            if (status >= 400) {
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                return;
            }
            String contentType = conn.getContentType();
            response.setContentType(contentType == null ? "application/octet-stream" : contentType);
            long length = conn.getContentLengthLong();
            if (length > 0) {
                response.setContentLengthLong(length);
            }
            response.setHeader("Cache-Control", "public, max-age=86400");
            response.setHeader("Access-Control-Allow-Origin", "*");

            try (InputStream in = conn.getInputStream(); OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
        } catch (Exception e) {
            log.warn("音频代理失败 url={}: {}", url, e.getMessage());
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
