package com.kumao.music;

import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 极简本机静态文件服务：把 APK assets/www 以 http://127.0.0.1 提供出来。
 * 这样 WebView 中的 ES Module / fetch / WebSocket 都运行在正常的 http 源上。
 */
public class LocalAssetServer {

    private final AssetManager assets;
    private ServerSocket server;
    private Thread acceptThread;

    public LocalAssetServer(AssetManager assets) {
        this.assets = assets;
    }

    public int start() throws IOException {
        // 固定端口，保证 WebView 的 origin 不变，登录态/localStorage 才能在重启后保留。
        server = new ServerSocket(8099, 50, InetAddress.getByName("127.0.0.1"));
        int port = server.getLocalPort();
        acceptThread = new Thread(this::acceptLoop, "kumao-assets");
        acceptThread.setDaemon(true);
        acceptThread.start();
        return port;
    }

    private void acceptLoop() {
        while (server != null && !server.isClosed()) {
            try {
                Socket socket = server.accept();
                Thread worker = new Thread(() -> handle(socket), "kumao-assets-request");
                worker.setDaemon(true);
                worker.start();
            } catch (IOException ignored) {
                // server closed
            }
        }
    }

    private void handle(Socket socket) {
        try (Socket s = socket) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII));
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;
            String method = parts[0];
            String path = parts[1];
            int query = path.indexOf('?');
            if (query >= 0) path = path.substring(0, query);
            path = URLDecoder.decode(path, "UTF-8");
            if (path.equals("/") || path.isEmpty()) path = "/index.html";
            if (!path.startsWith("/")) path = "/" + path;

            String assetPath = "www" + path;
            byte[] body;
            try (InputStream in = assets.open(assetPath)) {
                body = readAll(in);
            } catch (IOException e) {
                body = "Not Found".getBytes(StandardCharsets.UTF_8);
                writeResponse(s.getOutputStream(), 404, "text/plain; charset=utf-8", body, method);
                return;
            }
            writeResponse(s.getOutputStream(), 200, mimeType(path), body, method);
        } catch (Exception ignored) {
            // Request failed; WebView will surface the network error.
        }
    }

    private byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private void writeResponse(OutputStream out, int status, String mime, byte[] body, String method)
            throws IOException {
        String reason = status == 200 ? "OK" : "Not Found";
        String headers = "HTTP/1.1 " + status + " " + reason + "\r\n"
                + "Content-Type: " + mime + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Cache-Control: no-cache\r\n"
                + "Access-Control-Allow-Origin: *\r\n"
                + "Connection: close\r\n\r\n";
        out.write(headers.getBytes(StandardCharsets.US_ASCII));
        if (!"HEAD".equalsIgnoreCase(method)) {
            out.write(body);
        }
        out.flush();
    }

    private String mimeType(String path) {
        String lower = path.toLowerCase(Locale.US);
        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "text/javascript; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".ttf")) return "font/ttf";
        if (lower.endsWith(".wasm")) return "application/wasm";
        return "application/octet-stream";
    }

    public void stop() {
        try {
            if (server != null) server.close();
        } catch (IOException ignored) {
        }
        server = null;
    }
}
