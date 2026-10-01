package com.kb04.starroad.Ai;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 입력·출력 가드레일. 검사 코드를 이 Advisor 한곳에 모은다.
 *
 * <pre>
 *  질문 → [GuardrailAdvisor 입력 검사] → [RagRetrievalAdvisor L1 검색] → LLM
 *                                                                      ↓
 *  답변 ← [GuardrailAdvisor 출력 검사] ←───────────────────────────────┘
 * </pre>
 *
 * <p>가장 바깥에 두어야 한다. 그래야 금지 문구가 들어간 질문은 벡터 검색조차 하지 않고,
 * 출력 검사는 모든 Advisor 를 거친 최종 답변에 적용된다.
 *
 * <p>차단하면 LLM 을 부르지 않거나(입력) 답변을 안내 문구로 바꾸고(출력),
 * 응답 컨텍스트에 {@link #VIOLATION} 을 남긴다. ChatService 는 이 값으로 차단 여부를 안다.
 *
 * <p>차단 횟수는 {@code starroad.guardrail.blocked} 지표로 Actuator 에 쌓인다.
 */
@Slf4j
@Component
public class GuardrailAdvisor implements CallAdvisor {

    /** 응답 컨텍스트 키. 값은 {@link GuardrailViolation} */
    public static final String VIOLATION = "starroad.guardrail.violation";

    public static final String METRIC_BLOCKED = "starroad.guardrail.blocked";

    private final GuardrailRules rules;
    private final MeterRegistry meterRegistry;

    public GuardrailAdvisor(GuardrailRules rules, MeterRegistry meterRegistry) {
        this.rules = rules;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // ---------- 입력 검사: 걸리면 검색도 LLM 호출도 하지 않는다 ----------
        Optional<GuardrailViolation> inputViolation = rules.checkInput(userText(request));
        if (inputViolation.isPresent()) {
            GuardrailViolation v = inputViolation.get();
            record(v);
            return AdvisorResponses.shortCircuit(v.userMessage(), request.context(), VIOLATION, v);
        }

        ChatClientResponse response = chain.nextCall(request);

        // ---------- 출력 검사: 걸리면 답변을 안내 문구로 바꾼다 ----------
        Optional<GuardrailViolation> outputViolation = rules.checkOutput(AdvisorResponses.text(response));
        if (outputViolation.isPresent()) {
            GuardrailViolation v = outputViolation.get();
            record(v);
            return AdvisorResponses.replaceText(response, v.userMessage(), VIOLATION, v);
        }
        return response;
    }

    private void record(GuardrailViolation v) {
        log.warn("[Guardrail] 차단 stage={} rule={} detail={}", v.stage(), v.rule(), v.detail());
        Counter.builder(METRIC_BLOCKED)
                .description("가드레일에 걸려 차단된 챗봇 요청 수")
                .tag("stage", v.stage())
                .tag("rule", v.rule())
                .register(meterRegistry)
                .increment();
    }

    private static String userText(ChatClientRequest request) {
        UserMessage userMessage = request.prompt().getUserMessage();
        return userMessage == null ? null : userMessage.getText();
    }

    @Override
    public String getName() {
        return "GuardrailAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1000;
    }
}
