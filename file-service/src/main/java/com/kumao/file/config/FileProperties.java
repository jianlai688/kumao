package com.kumao.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Configuration
@ConfigurationProperties(prefix = "kumao.upload")
public class FileProperties {

    private String imageExt = "jpg,jpeg,png,gif,webp,bmp";
    private String audioExt = "mp3,wav,flac,m4a,aac";
    private String videoExt = "mp4,flv,mov,mkv,m3u8,ts";
    private String lyricExt = "lrc,txt";
    private long imageMaxSize = 10 * 1024 * 1024L;
    private long audioMaxSize = 100 * 1024 * 1024L;
    private long videoMaxSize = 500 * 1024 * 1024L;
    private long lyricMaxSize = 1024 * 1024L;

    public Set<String> imageExtensions() {
        return split(imageExt);
    }

    public Set<String> audioExtensions() {
        return split(audioExt);
    }

    public Set<String> videoExtensions() {
        return split(videoExt);
    }

    public Set<String> lyricExtensions() {
        return split(lyricExt);
    }

    private Set<String> split(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).map(String::toLowerCase).collect(Collectors.toSet());
    }
}
