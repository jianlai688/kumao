package com.kumao.review.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumao.review.dto.AiReviewRequest;
import com.kumao.review.dto.AiReviewResult;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiReviewServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final LocalReviewEngine local = new LocalReviewEngine();

    @Test
    void shouldRetryServerErrorsThenParseModelJson() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            int call = calls.incrementAndGet();
            byte[] body = call < 3 ? "{}".getBytes(StandardCharsets.UTF_8) : successBody();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(call < 3 ? 500 : 200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            AiReviewService service = service(server);
            AiReviewResult result = service.review(request());

            assertEquals("AI_MODEL", result.getSource());
            assertEquals("deepseek", result.getProvider());
            assertEquals("deepseek-chat", result.getModel());
            assertEquals("REVIEW", result.getDecision());
            assertEquals(55, result.getRiskScore());
            assertEquals(3, calls.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldFallbackOnAuthenticationFailureWithoutRetrying() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            byte[] body = "{\"error\":\"invalid key\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            AiReviewResult result = service(server).review(request());

            assertEquals("LOCAL_RULE", result.getSource());
            assertTrue(result.getFallbackReason().contains("HTTP 401"));
            assertEquals(1, calls.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldUseLocalRuleWhenModelNotConfigured() {
        AiReviewService service = new AiReviewService(
                new FakeModelConfigService(null, "审核模型未启用，请前往「模型配置」启用"), local, mapper);

        AiReviewResult result = service.review(request());

        assertEquals("LOCAL_RULE", result.getSource());
        assertTrue(result.getFallbackReason().contains("未启用"));
    }

    private AiReviewService service(HttpServer server) {
        AiModelConfigService.ResolvedModel resolved = new AiModelConfigService.ResolvedModel(
                AiModelConfigService.TYPE_REVIEW, "deepseek", "deepseek-chat",
                "http://127.0.0.1:" + server.getAddress().getPort(), "test-key",
                0.1, 2, 2, "DATABASE");
        return new AiReviewService(new FakeModelConfigService(resolved, null), local, mapper);
    }

    private static final class FakeModelConfigService extends AiModelConfigService {

        private final ResolvedModel resolved;
        private final String reason;

        private FakeModelConfigService(ResolvedModel resolved, String reason) {
            super(null, new com.kumao.review.config.DeepSeekProperties(),
                    new SecretCipher("unit-test-secret"), new ObjectMapper());
            this.resolved = resolved;
            this.reason = reason;
        }

        @Override
        public ResolvedModel resolve(String type) {
            return resolved;
        }

        @Override
        public String unavailableReason(String type) {
            return reason;
        }
    }

    private byte[] successBody() throws java.io.IOException {
        String content = "{\"decision\":\"REVIEW\",\"riskScore\":55,\"reasons\":\"需要人工复核版权信息\",\"signals\":[\"版权信息不足\"]}";
        Map<String, Object> response = Map.of("choices", List.of(
                Map.of("message", Map.of("content", content))));
        return mapper.writeValueAsBytes(response);
    }

    private AiReviewRequest request() {
        AiReviewRequest request = new AiReviewRequest();
        request.setCategory("MV");
        request.setItemId("1");
        request.setTitle("测试 MV");
        request.setContent("原创 MV，待审核");
        return request;
    }
}
