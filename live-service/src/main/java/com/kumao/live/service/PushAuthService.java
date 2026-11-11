package com.kumao.live.service;

import com.kumao.live.entity.LiveRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * SRS 推流地址生成与鉴权，防止非法推流
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushAuthService {

    @Value("${kumao.srs.rtmp-host:127.0.0.1}")
    private String rtmpHost;
    @Value("${kumao.srs.rtmp-port:1935}")
    private int rtmpPort;
    @Value("${kumao.srs.push-host:}")
    private String pushHost;
    @Value("${kumao.srs.play-host:http://127.0.0.1:8088}")
    private String playHost;
    @Value("${kumao.srs.push-key:kumao-srs-push-key}")
    private String pushKey;

    public String buildPushUrl(LiveRoom room) {
        String stream = streamName(room);
        String token = sign(stream, String.valueOf(room.getAnchorId()));
        return String.format("rtmp://%s:%d/live/%s?token=%s", resolvePushHost(), rtmpPort, stream, token);
    }

    public String buildPullUrl(LiveRoom room) {
        String stream = streamName(room);
        return String.format("%s/live/%s.m3u8", playHost.replaceAll("/$", ""), stream);
    }

    public boolean verifyPush(String stream, Long anchorId, String token) {
        if (token == null) {
            return false;
        }
        String expected = sign(stream, String.valueOf(anchorId));
        return java.security.MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }

    public String streamName(LiveRoom room) {
        return "room_" + room.getId();
    }

    /**
     * 户外直播推流地址中的主机名：
     * 优先使用 SRS_PUSH_HOST；未配置且 rtmp-host 是回环地址时，自动取本机局域网 IP，
     * 保证手机在同一 Wi-Fi 下扫码即可推流。
     */
    private String resolvePushHost() {
        if (pushHost != null && !pushHost.isBlank()) {
            return pushHost.trim();
        }
        if (rtmpHost != null && !"127.0.0.1".equals(rtmpHost) && !"localhost".equalsIgnoreCase(rtmpHost)) {
            return rtmpHost;
        }
        // 通过默认路由探测本机对外 IP，避免选中 VMware / WSL / 热点等虚拟网卡
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 80);
            InetAddress local = socket.getLocalAddress();
            if (local instanceof Inet4Address && !local.isLoopbackAddress() && !local.isAnyLocalAddress()) {
                return local.getHostAddress();
            }
        } catch (Exception e) {
            log.debug("默认路由探测本机 IP 失败: {}", e.getMessage());
        }
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                var addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    var address = addresses.nextElement();
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("获取本机局域网 IP 失败: {}", e.getMessage());
        }
        return rtmpHost;
    }

    private String sign(String stream, String uid) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pushKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((stream + ":" + uid).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            log.error("推流签名失败", e);
            return "";
        }
    }
}
