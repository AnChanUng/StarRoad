package com.kb04.starroad.Controller;

import com.kb04.starroad.Service.PolicyAiClient;
import com.kb04.starroad.Service.PolicyAiClient.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Controller
@RequestMapping("/starroad/policy/ai")
public class PolicyAiController {
    private final PolicyAiClient client;
    private static final String CSRF = "policyAiCsrf";
    private static final String OWNER = "policyAiOwner";

    public PolicyAiController(PolicyAiClient client) { this.client = client; }

    @GetMapping
    public String page(HttpSession session, Model model) {
        synchronized (session) {
            if (session.getAttribute(CSRF) == null) session.setAttribute(CSRF, UUID.randomUUID().toString());
            if (session.getAttribute(OWNER) == null) session.setAttribute(OWNER, UUID.randomUUID().toString());
            model.addAttribute("policyAiCsrf", session.getAttribute(CSRF));
        }
        return "policy/consultation";
    }

    @GetMapping("/policies") @ResponseBody
    public List<PolicyOption> policies() { return client.policies(); }

    @PostMapping("/ask") @ResponseBody
    public Answer ask(@RequestBody Question input,
                      @RequestHeader(value = "X-Policy-CSRF", required = false) String csrf,
                      HttpSession session) {
        csrf(csrf, session);
        return client.ask(validate(input), owner(session));
    }

    @PostMapping("/jobs") @ResponseBody
    public ResponseEntity<JobStatus> submit(@RequestBody JobRequest input,
            @RequestHeader(value = "X-Policy-CSRF", required = false) String csrf, HttpSession session) {
        csrf(csrf, session);
        if (input.request_id() == null || !input.request_id().matches("[A-Za-z0-9_-]{16,64}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 번호를 확인해 주세요.");
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
                .body(client.submit(new JobRequest(input.request_id(), validate(input.request())), owner(session)));
    }

    @GetMapping("/jobs/{id}") @ResponseBody
    public ResponseEntity<JobStatus> job(@PathVariable String id, HttpSession session) {
        if (!id.matches("[a-f0-9]{32}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상담 작업 번호를 확인해 주세요.");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(client.job(id, owner(session)));
    }

    private String owner(HttpSession session) {
        Object value = session.getAttribute(OWNER);
        if (value == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "상담 화면을 다시 열어 주세요.");
        return value.toString();
    }

    private void csrf(String csrf, HttpSession session) {
        if (csrf == null || !csrf.equals(session.getAttribute(CSRF)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "화면을 새로고침한 후 다시 시도해 주세요.");
    }

    private Question validate(Question input) {
        if (input == null || input.policy_id() == null || !input.policy_id().matches("[A-Za-z0-9_-]{1,100}")
                || invalid(input.question(), 1000)
                || !Set.of("chat", "checklist").contains(input.mode() == null ? "" : input.mode()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "정책, 질문(1~1,000자), 상담 모드를 확인해 주세요.");
        List<Message> history = input.history() == null ? List.of() : input.history();
        if (history.size() > 8 || history.stream().anyMatch(m -> m == null
                || !("user".equals(m.role()) || "assistant".equals(m.role())) || invalid(m.content(), 3000)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대화 기록 형식이 올바르지 않습니다.");
        return new Question(input.policy_id(), input.question().trim(), history, input.mode());
    }

    private boolean invalid(String value, int max) { return value == null || value.isBlank() || value.length() > max; }

    @ExceptionHandler(ResponseStatusException.class) @ResponseBody
    public ResponseEntity<Map<String, String>> failure(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", Objects.requireNonNullElse(error.getReason(), "요청을 처리할 수 없습니다.")));
    }
}
