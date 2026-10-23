package com.kb04.starroad.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.List;

/** Server-only bridge to the official policy corpus; IDs are never JPA policy numbers. */
@Service
public class PolicyAiClient {
    private final RestTemplate http;
    private final String baseUrl;
    private final String token;

    public PolicyAiClient(@Value("${policy-ai.base-url:http://127.0.0.1:8001}") String baseUrl,
                          @Value("${policy-ai.token:}") String token,
                          @Value("${policy-ai.read-timeout-seconds:120}") int timeout) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.token = token;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(timeout));
        this.http = new RestTemplate(factory);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PolicyOption(String id, String name, String region, String source_url) {}
    public record Message(String role, String content) {}
    public record Question(String policy_id, String question, List<Message> history, String mode) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(Integer number, String title, String url, Integer page) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Answer(String text, List<Source> sources, boolean uncited, long elapsed_ms, String status) {}

    public List<PolicyOption> policies() {
        return call("/policies", HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    }

    public Answer ask(Question question) {
        return call("/chat/ask", HttpMethod.POST, question, new ParameterizedTypeReference<>() {});
    }

    private <T> T call(String path, HttpMethod method, Object body, ParameterizedTypeReference<T> type) {
        if (token.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "정책 AI 연결 설정이 필요합니다.");
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            T result = http.exchange(baseUrl + path, method, new HttpEntity<>(body, headers), type).getBody();
            if (result == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 응답이 비어 있습니다.");
            return result;
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            if (status == 404) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "정책 목록을 새로 불러와 주세요.");
            if (status == 429) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "다른 상담을 처리 중입니다. 잠시 후 다시 시도해 주세요.");
            if (status == 400 || status == 422) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문을 확인하고 주민등록번호·전화번호 등 개인정보를 제거해 주세요.");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "정책 AI를 이용할 수 없습니다. 잠시 후 다시 시도해 주세요.");
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI 연결에 실패했거나 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "정책 AI 응답을 읽을 수 없습니다.");
        }
    }
}
