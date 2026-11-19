package com.kumao.live.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 读取本机 OBS WebSocket 配置，供主播控制台自动填充连接地址和密码。
 *
 * <p>优先使用环境变量/配置中心显式配置；否则读取 OBS 默认配置文件。
 * 该接口只返回给已登录的主播/管理员，不写日志。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObsConfigService {

    private final ObjectMapper objectMapper;

    @Value("${kumao.obs.websocket-url:ws://127.0.0.1:4455}")
    private String websocketUrl;
    @Value("${kumao.obs.password:}")
    private String configuredPassword;
    @Value("${kumao.obs.config-path:}")
    private String configuredConfigPath;

    public Map<String, Object> config() {
        String password = configuredPassword == null ? "" : configuredPassword.trim();
        String url = websocketUrl;
        if (!StringUtils.hasText(password)) {
            JsonNode file = readObsConfig();
            if (file != null) {
                String filePassword = file.path("server_password").asText("");
                if (StringUtils.hasText(filePassword)) {
                    password = filePassword;
                }
                int port = file.path("server_port").asInt(4455);
                if (port > 0 && (url == null || url.endsWith(":4455"))) {
                    url = "ws://127.0.0.1:" + port;
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("websocketUrl", url);
        result.put("password", password);
        result.put("available", StringUtils.hasText(password));
        return result;
    }

    private JsonNode readObsConfig() {
        Path path = resolveConfigPath();
        if (path == null || !Files.isReadable(path)) {
            return null;
        }
        try {
            return objectMapper.readTree(Files.readString(path));
        } catch (Exception e) {
            log.debug("读取 OBS WebSocket 配置失败: {}", e.getMessage());
            return null;
        }
    }

    private Path resolveConfigPath() {
        if (StringUtils.hasText(configuredConfigPath)) {
            return Path.of(configuredConfigPath);
        }
        String appData = System.getenv("APPDATA");
        if (StringUtils.hasText(appData)) {
            Path windowsPath = Path.of(appData, "obs-studio", "plugin_config", "obs-websocket", "config.json");
            if (Files.isReadable(windowsPath)) {
                return windowsPath;
            }
        }
        String home = System.getProperty("user.home");
        if (StringUtils.hasText(home)) {
            Path linuxPath = Path.of(home, ".config", "obs-studio", "plugin_config", "obs-websocket", "config.json");
            if (Files.isReadable(linuxPath)) {
                return linuxPath;
            }
        }
        return null;
    }
}
