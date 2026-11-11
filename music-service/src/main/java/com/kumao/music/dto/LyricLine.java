package com.kumao.music.dto;

import lombok.Data;

import java.util.List;

/**
 * LRC 歌词行
 */
@Data
public class LyricLine {

    private long time;
    private String text;

    public LyricLine() {
    }

    public LyricLine(long time, String text) {
        this.time = time;
        this.text = text;
    }

    public static LyricLine of(long time, String text) {
        return new LyricLine(time, text);
    }

    public List<LyricLine> single() {
        return List.of(this);
    }
}
