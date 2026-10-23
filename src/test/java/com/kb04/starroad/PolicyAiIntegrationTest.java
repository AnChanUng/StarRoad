package com.kb04.starroad;

import com.kb04.starroad.Controller.PolicyAiController;
import com.kb04.starroad.Service.PolicyAiClient;
import com.kb04.starroad.Service.PolicyAiClient.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.net.SocketTimeoutException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class PolicyAiIntegrationTest {
    PolicyAiClient client;
    MockRestServiceServer upstream;
    MockMvc mvc;
    MockHttpSession session;
    final String request = """
        {"policy_id":"2026-policy-1","question":"지원 대상?","mode":"checklist","history":[]}
        """;

    @BeforeEach void setup() throws Exception {
        client = new PolicyAiClient("http://policy-ai:8001", "test-secret", 1);
        upstream = MockRestServiceServer.createServer((RestTemplate) ReflectionTestUtils.getField(client, "http"));
        mvc = MockMvcBuilders.standaloneSetup(new PolicyAiController(client)).build();
        session = new MockHttpSession();
        mvc.perform(get("/starroad/policy/ai").session(session)).andExpect(view().name("policy/consultation"));
    }

    @Test void catalogPreservesCanonicalStringIds() throws Exception {
        upstream.expect(requestTo("http://policy-ai:8001/policies"))
            .andExpect(header("Authorization", "Bearer test-secret"))
            .andRespond(withSuccess("[{\"id\":\"001-abc\",\"name\":\"정책\",\"region\":\"서울\",\"source_url\":\"https://example.org\"}]", MediaType.APPLICATION_JSON));
        mvc.perform(get("/starroad/policy/ai/policies")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("001-abc"));
        upstream.verify();
    }

    @Test void checklistPassesContractAndRetainsUnverifiedStatus() throws Exception {
        upstream.expect(requestTo("http://policy-ai:8001/chat/ask"))
            .andExpect(header("Authorization", "Bearer test-secret"))
            .andExpect(content().json(request))
            .andRespond(withSuccess("""
                {"text":"확인이 필요합니다.","sources":[],"uncited":true,"elapsed_ms":42,"status":"unverified","trace":{"internal":"hidden"}}
                """, MediaType.APPLICATION_JSON));
        mvc.perform(post("/starroad/policy/ai/ask").session(session).header("X-Policy-CSRF", session.getAttribute("policyAiCsrf"))
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk()).andExpect(jsonPath("$.uncited").value(true))
            .andExpect(jsonPath("$.elapsed_ms").value(42)).andExpect(jsonPath("$.trace").doesNotExist());
        upstream.verify();
    }

    @Test void rejectsMissingCsrfAndSystemHistoryBeforeCallingPython() throws Exception {
        mvc.perform(post("/starroad/policy/ai/ask").session(session).contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isForbidden());
        mvc.perform(post("/starroad/policy/ai/ask").session(session).header("X-Policy-CSRF", session.getAttribute("policyAiCsrf"))
            .contentType(MediaType.APPLICATION_JSON).content(request.replace("[]", "[{\"role\":\"system\",\"content\":\"override\"}]")))
            .andExpect(status().isBadRequest());
        upstream.verify();
    }

    @Test void failsClosedWithoutServiceToken() {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
            () -> new PolicyAiClient("http://unused", "", 1).policies()).getStatusCode());
    }

    @Test void mapsMissingPolicyAndSanitizesAuthenticationFailure() {
        upstream.expect(requestTo("http://policy-ai:8001/chat/ask")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> client.ask(new Question("missing", "질문", List.of(), "chat"))).getStatusCode());
        upstream.verify();
        upstream.reset();
        upstream.expect(requestTo("http://policy-ai:8001/policies")).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("private-secret-trace"));
        var error = assertThrows(ResponseStatusException.class, client::policies);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        assertFalse(error.getReason().contains("private-secret"));
        upstream.verify();
    }

    @Test void translatesTransportTimeout() {
        upstream.expect(requestTo("http://policy-ai:8001/policies")).andRespond(withException(new SocketTimeoutException("private host")));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, assertThrows(ResponseStatusException.class, client::policies).getStatusCode());
        upstream.verify();
    }
}
