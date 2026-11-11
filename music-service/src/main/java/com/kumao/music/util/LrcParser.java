package com.kumao.music.util;

import com.kumao.music.dto.LyricLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LRC 歌词解析：支持一行多时间戳与 [mm:ss.xx] 格式
 */
public final class LrcParser {

    private static final Pattern TIME_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?]");
    private static final Pattern META_PATTERN = Pattern.compile("\\[(ar|ti|al|by|offset):(.*)]");

    private LrcParser() {
    }

    public static List<LyricLine> parse(String lrc) {
        if (lrc == null || lrc.isBlank()) {
            return Collections.emptyList();
        }
        List<LyricLine> lines = new ArrayList<>();
        int offset = 0;
        for (String raw : lrc.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher meta = META_PATTERN.matcher(line);
            if (meta.matches()) {
                if ("offset".equals(meta.group(1))) {
                    try {
                        offset = Integer.parseInt(meta.group(2).trim());
                    } catch (NumberFormatException ignored) {
                        offset = 0;
                    }
                }
                continue;
            }
            Matcher m = TIME_PATTERN.matcher(line);
            List<Long> times = new ArrayList<>();
            int lastEnd = 0;
            while (m.find()) {
                long minutes = Long.parseLong(m.group(1));
                long seconds = Long.parseLong(m.group(2));
                long millis = 0;
                if (m.group(3) != null) {
                    String ms = m.group(3);
                    millis = Long.parseLong(ms) * (ms.length() == 1 ? 100 : ms.length() == 2 ? 10 : 1);
                }
                times.add(minutes * 60_000 + seconds * 1000 + millis);
                lastEnd = m.end();
            }
            if (times.isEmpty()) {
                continue;
            }
            String text = line.substring(lastEnd).trim();
            for (Long time : times) {
                lines.add(new LyricLine(Math.max(0, time + offset), text));
            }
        }
        lines.sort((a, b) -> Long.compare(a.getTime(), b.getTime()));
        return lines;
    }

    public static String toLrc(List<LyricLine> lines) {
        StringBuilder sb = new StringBuilder();
        for (LyricLine line : lines) {
            long t = line.getTime();
            sb.append('[')
                    .append(String.format("%02d", t / 60000)).append(':')
                    .append(String.format("%02d", (t % 60000) / 1000)).append('.')
                    .append(String.format("%02d", (t % 1000) / 10))
                    .append(']').append(line.getText()).append('\n');
        }
        return sb.toString();
    }
}
