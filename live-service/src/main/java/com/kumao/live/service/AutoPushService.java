package com.kumao.live.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumao.common.exception.BizException;
import com.kumao.live.entity.LiveRoom;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 主播点击「开始直播」时自动生成一路演示推流，避免控制台开播后没有画面。
 *
 * <p>演示推流由 ffmpeg 生成：一张带直播间标题的渐变画面 + 静音 AAC 音轨，
 * 推到 SRS 的 {@code live/room_{id}}。如果 SRS 上已经有主播用 OBS/手机推上来的流，
 * 则跳过自动推流，保留主播自己的画面。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoPushService {

    private static final String STREAM_PREFIX = "room_";

    private final ObjectMapper objectMapper;

    @Value("${kumao.live.autopush.enabled:true}")
    private boolean enabled;
    @Value("${kumao.live.autopush.ffmpeg:ffmpeg}")
    private String ffmpegPath;
    @Value("${kumao.live.autopush.width:1280}")
    private int width;
    @Value("${kumao.live.autopush.height:720}")
    private int height;
    @Value("${kumao.live.autopush.fps:25}")
    private int fps;
    @Value("${kumao.live.autopush.bitrate:1800k}")
    private String bitrate;

    @Value("${kumao.srs.rtmp-host:127.0.0.1}")
    private String rtmpHost;
    @Value("${kumao.srs.rtmp-port:1935}")
    private int rtmpPort;
    @Value("${kumao.srs.api-url:http://127.0.0.1:1985}")
    private String srsApiUrl;

    private final Map<Long, PushProcess> processes = new ConcurrentHashMap<>();

    /**
     * 开播时调用：若没有现成推流则启动自动推流。
     */
    public void startIfNeeded(LiveRoom room) {
        if (!enabled) {
            log.debug("自动推流已关闭，跳过 room={}", room.getId());
            return;
        }
        Long roomId = room.getId();
        if (processes.containsKey(roomId)) {
            return;
        }
        if (isStreamActive(roomId)) {
            log.info("room={} 已有推流，跳过自动推流", roomId);
            return;
        }
        Path frame = null;
        try {
            frame = renderFrame(room);
            List<String> command = buildCommand(room, frame);
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            Process process = builder.start();
            // 进程若立即退出（常见原因是 ffmpeg 不存在或参数不兼容），不要让房间“假开播”。
            if (!process.waitFor(700, TimeUnit.MILLISECONDS)) {
                processes.put(roomId, new PushProcess(process, frame));
                process.onExit().thenAccept(p -> onProcessExit(roomId, p));
                log.info("room={} 自动推流已启动 pid={}", roomId, process.pid());
            } else {
                deleteQuietly(frame);
                throw new BizException("自动推流启动失败，请检查服务器 ffmpeg 是否可用");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            deleteQuietly(frame);
            log.error("room={} 自动推流启动失败", roomId, e);
            throw new BizException("自动推流启动失败：" + e.getMessage());
        }
    }

    /**
     * 下播/封禁时停止自动推流。
     */
    public void stop(Long roomId) {
        PushProcess current = processes.remove(roomId);
        if (current == null) {
            return;
        }
        Process process = current.process();
        // Windows 上 ffmpeg 可能是 Python/包装脚本，真正编码进程是子进程，需要一起结束。
        List<ProcessHandle> children = process.descendants().toList();
        children.forEach(handle -> {
            try {
                handle.destroy();
            } catch (Exception ignored) {
                // 进程可能已自行退出
            }
        });
        process.destroy();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        } finally {
            children.forEach(handle -> {
                if (handle.isAlive()) {
                    handle.destroyForcibly();
                }
            });
            deleteQuietly(current.frame());
        }
        log.info("room={} 自动推流已停止", roomId);
    }

    @PreDestroy
    public void shutdown() {
        for (Long roomId : new ArrayList<>(processes.keySet())) {
            stop(roomId);
        }
    }

    private void onProcessExit(Long roomId, Process process) {
        PushProcess current = processes.get(roomId);
        if (current == null || current.process() != process) {
            return;
        }
        processes.remove(roomId, current);
        deleteQuietly(current.frame());
        log.warn("room={} 自动推流进程已退出 code={}", roomId, process.exitValue());
    }

    private boolean isStreamActive(Long roomId) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(1200);
            factory.setReadTimeout(1200);
            restTemplate.setRequestFactory(factory);
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            ResponseEntity<String> response = restTemplate.exchange(
                    srsApiUrl.replaceAll("/$", "") + "/api/v1/streams",
                    HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode streams = objectMapper.readTree(response.getBody()).path("streams");
            String expected = STREAM_PREFIX + roomId;
            if (streams.isArray()) {
                for (JsonNode stream : streams) {
                    if (!stream.path("publish").path("active").asBoolean(false)) {
                        continue;
                    }
                    if (expected.equals(stream.path("name").asText(""))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("查询 SRS 活跃流失败，按无流处理: {}", e.getMessage());
        }
        return false;
    }

    private List<String> buildCommand(LiveRoom room, Path frame) {
        String rtmp = String.format("rtmp://%s:%d/live/%s%d", rtmpHost, rtmpPort, STREAM_PREFIX, room.getId());
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-y");
        command.add("-loglevel");
        command.add("warning");
        command.add("-re");
        command.add("-loop");
        command.add("1");
        command.add("-framerate");
        command.add(String.valueOf(fps));
        command.add("-i");
        command.add(frame.toString());
        command.add("-f");
        command.add("lavfi");
        command.add("-i");
        command.add("anullsrc=channel_layout=stereo:sample_rate=44100");
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("veryfast");
        command.add("-tune");
        command.add("stillimage");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-r");
        command.add(String.valueOf(fps));
        command.add("-g");
        command.add(String.valueOf(fps * 2));
        command.add("-b:v");
        command.add(bitrate);
        command.add("-threads");
        command.add("4");
        command.add("-c:a");
        command.add("aac");
        command.add("-strict");
        command.add("-2");
        command.add("-b:a");
        command.add("128k");
        command.add("-ar");
        command.add("44100");
        command.add("-f");
        command.add("flv");
        command.add(rtmp);
        return command;
    }

    private Path renderFrame(LiveRoom room) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setPaint(new GradientPaint(0, 0, new Color(27, 12, 42), width, height, new Color(145, 37, 156)));
            g.fillRect(0, 0, width, height);
            g.setColor(new Color(255, 255, 255, 14));
            g.fillOval(-width / 4, height / 3, width, width);
            g.setColor(new Color(255, 255, 255, 10));
            g.fillOval(width / 2, -height / 3, width, width);
            g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(255, 255, 255, 32));
            g.drawOval(width - 300, -180, 520, 520);
            g.drawOval(width - 220, -100, 360, 360);

            String family = fontFamily();
            g.setFont(new Font(family, Font.BOLD, 21));
            g.setColor(new Color(255, 255, 255, 205));
            g.drawString("KUMAO MUSIC  ·  LIVE", 64, 72);

            // 中央“正在直播”标记
            g.setFont(new Font(family, Font.PLAIN, 24));
            g.setColor(new Color(255, 255, 255, 175));
            drawCentered(g, "正在直播", width / 2, height / 2 - 130);

            String title = StringUtils.hasText(room.getTitle()) ? room.getTitle().trim() : "酷猫直播间";
            Font titleFont = fitFont(g, title, width - 180, 72, family);
            g.setFont(titleFont);
            g.setColor(Color.WHITE);
            drawCentered(g, title, width / 2, height / 2 - 35);

            String anchor = StringUtils.hasText(room.getAnchorName()) ? room.getAnchorName() : "酷猫主播";
            g.setFont(new Font(family, Font.PLAIN, 30));
            g.setColor(new Color(255, 255, 255, 205));
            drawCentered(g, anchor, width / 2, height / 2 + 45);

            String detail = StringUtils.hasText(room.getCategory()) ? room.getCategory() : "音乐现场";
            if ("OUTDOOR".equals(room.getScene()) && StringUtils.hasText(room.getLocation())) {
                detail = detail + "  ·  " + room.getLocation();
            }
            g.setFont(new Font(family, Font.PLAIN, 20));
            g.setColor(new Color(255, 255, 255, 150));
            drawCentered(g, detail, width / 2, height / 2 + 92);

            // 底部装饰线与状态
            g.setColor(new Color(255, 255, 255, 45));
            g.fillRect(64, height - 112, width - 128, 1);
            g.setFont(new Font(family, Font.PLAIN, 18));
            g.setColor(new Color(255, 255, 255, 150));
            g.drawString("点击进入直播间  ·  KUMAO MUSIC", 64, height - 72);

            int barX = width - 236;
            int barBase = height - 64;
            g.setColor(new Color(255, 255, 255, 165));
            for (int i = 0; i < 5; i++) {
                int barHeight = 12 + (i % 3) * 9;
                g.fillRoundRect(barX + i * 14, barBase - barHeight, 6, barHeight, 3, 3);
            }
        } finally {
            g.dispose();
        }
        Path frame = Files.createTempFile("kumao-live-" + room.getId() + "-", ".png");
        ImageIO.write(image, "png", frame.toFile());
        return frame;
    }

    private String fontFamily() {
        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        return Arrays.stream(names)
                .filter(name -> name.toLowerCase().contains("yahei") || name.toLowerCase().contains("noto sans cjk"))
                .findFirst()
                .orElse(Font.SANS_SERIF);
    }

    private Font fitFont(Graphics2D g, String text, int maxWidth, int preferredSize, String family) {
        int size = preferredSize;
        while (size > 28) {
            Font font = new Font(family, Font.BOLD, size);
            FontMetrics metrics = g.getFontMetrics(font);
            if (metrics.stringWidth(text) <= maxWidth) {
                return font;
            }
            size -= 4;
        }
        return new Font(family, Font.BOLD, 28);
    }

    private void drawCentered(Graphics2D g, String text, int centerX, int baseline) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, centerX - textWidth / 2, baseline);
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.debug("删除自动推流临时画面失败: {}", e.getMessage());
        }
    }

    private record PushProcess(Process process, Path frame) {
    }
}
