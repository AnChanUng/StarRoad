package com.kb04.starroad;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Anthropic 으로 실제 나가는 요청 본문을 검증하는 회귀 테스트.
 *
 * <p><b>왜 필요한가.</b> Spring AI 1.1.8 은 Anthropic 요청에 temperature 를 기본값 0.8 로
 * 항상 싣는다. 그런데 Claude Opus 5 계열은 temperature·top_p·top_k 가 제거된 모델이라
 * 이 값이 붙으면 400 을 돌려준다. {@link com.kb04.starroad.Config.RagConfig} 의
 * BeanPostProcessor 가 기본 옵션에서 이 셋을 지우는데, 그게 실제로 먹었는지는
 * 나가는 본문을 봐야만 알 수 있다.
 *
 * <p>API 키 없이 검증하려고 base-url 을 로컬 스텁 서버로 돌려 본문을 가로챈다.
 * 실제 API 는 인증(401)을 본문 검증보다 먼저 하기 때문에, 키가 없으면 이 문제를
 * 잡아낼 수 없다.
 */
@SpringBootTest
@ActiveProfiles("dev")
class AnthropicRequestOptionsTest {

    private static HttpServer server;
    private static final AtomicReference<String> CAPTURED_BODY = new AtomicReference<>();

    private static final String STUB_RESPONSE = """
            {"id":"msg_stub","type":"message","role":"assistant","model":"claude-opus-5",\
            "content":[{"type":"text","text":"ok"}],"stop_reason":"end_turn","stop_sequence":null,\
            "usage":{"input_tokens":1,"output_tokens":1}}""";

    @BeforeAll
    static void startStubServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (InputStream is = exchange.getRequestBody()) {
                CAPTURED_BODY.set(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
            byte[] body = STUB_RESPONSE.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
    }

    @AfterAll
    static void stopStubServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @DynamicPropertySource
    static void pointAnthropicAtStub(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.anthropic.base-url",
                () -> "http://127.0.0.1:" + server.getAddress().getPort());
        registry.add("spring.ai.anthropic.api-key", () -> "test-key-not-real");
    }

    @Autowired
    private ChatClient chatClient;

    @Test
    void sendsNoSamplingParameters() {
        // ChatClient 에는 L1 검색 게이트(RagRetrievalAdvisor)가 붙어 있다. 무관한 문장을 보내면
        // LLM 까지 가지 않으므로, dev 샘플 데이터에 답이 있는 질문을 보내 실제 요청 경로를 탄다.
        chatClient.prompt().user("KB청년희망적금 최고 금리가 몇 퍼센트야?").call().content();

        String body = CAPTURED_BODY.get();
        assertNotNull(body, "스텁 서버가 요청을 받지 못했다");
        System.out.println("[전송된 요청 본문] " + body);

        // Claude Opus 5 계열에서 400 을 유발하는 파라미터들
        assertFalse(body.contains("\"temperature\""),
                "temperature 가 실려 나간다. Opus 5 계열은 400 을 돌려준다. 본문: " + body);
        assertFalse(body.contains("\"top_p\""), "top_p 가 실려 나간다. 본문: " + body);
        assertFalse(body.contains("\"top_k\""), "top_k 가 실려 나간다. 본문: " + body);

        // 설정한 모델이 그대로 나가는지도 함께 확인한다
        assertTrue(body.contains("claude-opus-5"),
                "요청 모델이 claude-opus-5 가 아니다. 본문: " + body);
    }
}
