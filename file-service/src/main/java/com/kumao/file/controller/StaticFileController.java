package com.kumao.file.controller;

import com.kumao.file.config.MinioConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;

/**
 * 静态资源访问路由：网关 /static/** -> file-service -> MinIO
 * 支持图片、音频、歌词、MV 视频与直播回放，便于统一鉴权与防盗链扩展
 */
@Slf4j
@RestController
@RequestMapping("/static")
@RequiredArgsConstructor
public class StaticFileController {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @GetMapping("/{bucket}/**")
    public ResponseEntity<StreamingResponseBody> get(@PathVariable String bucket,
                                                     jakarta.servlet.http.HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = "/static/" + bucket + "/";
        String objectKey = uri.length() > prefix.length() ? uri.substring(prefix.length()) : "";
        if (objectKey.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket).object(objectKey).build());
            StreamingResponseBody body = outputStream -> {
                try (var in = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(bucket).object(objectKey).build())) {
                    in.transferTo(outputStream);
                } catch (Exception e) {
                    throw new IOException("读取 MinIO 对象失败: " + objectKey, e);
                }
            };
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, stat.contentType() == null
                            ? MediaType.APPLICATION_OCTET_STREAM_VALUE : stat.contentType())
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(stat.size()))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(body);
        } catch (Exception e) {
            log.debug("静态资源读取失败 {}: {}", objectKey, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /** 演示环境占位图 */
    @GetMapping("/placeholder/**")
    public ResponseEntity<byte[]> placeholder() {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"400\" height=\"400\">"
                + "<rect width=\"400\" height=\"400\" fill=\"#1d2129\"/>"
                + "<text x=\"200\" y=\"210\" font-size=\"28\" fill=\"#7a8494\" text-anchor=\"middle\">Kumao Music</text>"
                + "</svg>";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .body(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
