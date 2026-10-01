package com.kb04.starroad.Ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 챗봇 입력·출력 검사 규칙.
 *
 * <p>규칙 자체는 순수 로직이라 Spring 없이 단위 테스트한다({@code GuardrailRulesTest}).
 * 실제 요청 흐름에 끼워 넣는 일은 {@link GuardrailAdvisor} 가 맡는다.
 *
 * <h3>입력 검사</h3>
 * <ul>
 *   <li>{@code too_long} — 질문이 너무 길다. 긴 입력은 프롬프트 주입과 비용 폭증의 통로다.</li>
 *   <li>{@code banned_phrase} — 미리 정한 금지 문구(프롬프트 주입 시도)가 들어 있다.</li>
 *   <li>{@code pii_*} — 주민등록번호·휴대폰번호·카드번호. 외부 LLM API 로 개인정보를 보내지 않는다.</li>
 * </ul>
 *
 * <h3>출력 검사</h3>
 * <ul>
 *   <li>{@code pii_*} — 답변에 개인정보 패턴이 들어 있다.</li>
 *   <li>{@code prompt_leak} — 답변에 시스템 프롬프트의 특정 문구가 들어 있다(프롬프트 유출).</li>
 * </ul>
 *
 * <p><b>이 규칙이 잡지 못하는 것</b>은 docs/PORTFOLIO.md 의 '가드레일의 한계'에 정리했다.
 * 문자열·정규식 기반이라 말을 바꾼 우회(패러프레이즈), 다른 언어, 인코딩된 입력은 통과한다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "starroad.guardrail")
public class GuardrailRules {

    public static final String RULE_TOO_LONG = "too_long";
    public static final String RULE_BANNED_PHRASE = "banned_phrase";
    public static final String RULE_PROMPT_LEAK = "prompt_leak";

    /** 질문 최대 길이(문자 수) */
    private int maxQuestionLength = 500;

    /**
     * 입력 금지 문구. 비교 전에 공백을 모두 지우고 소문자로 바꾸므로
     * "이전 지시 무시", "이전지시무시", "IGNORE previous instructions" 가 모두 걸린다.
     */
    private List<String> bannedPhrases = List.of(
            // 지시 무력화 시도
            "이전 지시 무시", "이전 지시를 무시", "앞의 지시를 무시", "위의 지시를 무시",
            "모든 지시를 무시", "규칙을 무시", "규칙은 무시",
            // 시스템 프롬프트 탈취 시도
            "시스템 프롬프트", "프롬프트를 보여", "프롬프트를 출력", "너의 지침", "너의 설정",
            // 역할 탈취
            "탈옥", "개발자 모드", "제한 없는 모드",
            // 영문 변형
            "ignore previous instructions", "ignore all previous", "ignore the above",
            "system prompt", "jailbreak", "developer mode"
    );

    /**
     * 시스템 프롬프트에만 있는 문구. 답변에 이 문구가 나오면 프롬프트가 유출된 것으로 본다.
     * ChatService.SYSTEM_PROMPT 와 RagRetrievalAdvisor 의 자료 태그에서 골랐다.
     * 시스템 프롬프트를 고치면 여기도 함께 고친다.
     */
    private List<String> leakMarkers = List.of(
            "반드시 지켜야 할 규칙",
            "자료에 없는 사실은 절대 만들어내지 않는다",
            "문장마다 근거가 된 자료의 번호를",
            "<자료>", "</자료>"
    );

    /** 규칙 이름 → 패턴. 입력·출력 양쪽에 쓴다. 순서를 지키려고 Map.of 대신 명시적으로 나열한다. */
    private static final List<Map.Entry<String, Pattern>> PII_PATTERNS = List.of(
            // 900101-1234567 (외국인등록번호 5~8 포함)
            Map.entry("pii_resident_no", Pattern.compile("(?<!\\d)\\d{6}\\s*-\\s*[1-8]\\d{6}(?!\\d)")),
            // 010-1234-5678, 01012345678, 010 1234 5678
            Map.entry("pii_mobile", Pattern.compile("(?<!\\d)01[016789][-.\\s]?\\d{3,4}[-.\\s]?\\d{4}(?!\\d)")),
            // 1234-5678-1234-5678 (구분자가 있는 경우만 — 금액 숫자와 헷갈리지 않게)
            Map.entry("pii_card_no", Pattern.compile("(?<!\\d)\\d{4}[-\\s]\\d{4}[-\\s]\\d{4}[-\\s]\\d{4}(?!\\d)"))
    );

    public Optional<GuardrailViolation> checkInput(String question) {
        if (question == null) {
            return Optional.empty();
        }
        if (question.length() > maxQuestionLength) {
            return violation(GuardrailViolation.STAGE_INPUT, RULE_TOO_LONG,
                    "length=" + question.length() + " > " + maxQuestionLength);
        }

        String normalized = normalize(question);
        for (String phrase : bannedPhrases) {
            if (normalized.contains(normalize(phrase))) {
                return violation(GuardrailViolation.STAGE_INPUT, RULE_BANNED_PHRASE, phrase);
            }
        }

        return findPii(GuardrailViolation.STAGE_INPUT, question);
    }

    public Optional<GuardrailViolation> checkOutput(String answer) {
        if (answer == null) {
            return Optional.empty();
        }

        Optional<GuardrailViolation> pii = findPii(GuardrailViolation.STAGE_OUTPUT, answer);
        if (pii.isPresent()) {
            return pii;
        }

        String normalized = normalize(answer);
        for (String marker : leakMarkers) {
            if (normalized.contains(normalize(marker))) {
                return violation(GuardrailViolation.STAGE_OUTPUT, RULE_PROMPT_LEAK, marker);
            }
        }
        return Optional.empty();
    }

    private Optional<GuardrailViolation> findPii(String stage, String text) {
        for (Map.Entry<String, Pattern> entry : PII_PATTERNS) {
            Matcher matcher = entry.getValue().matcher(text);
            if (matcher.find()) {
                // 로그에 개인정보 원문을 남기지 않는다
                return violation(stage, entry.getKey(), "matched at index " + matcher.start());
            }
        }
        return Optional.empty();
    }

    /** 띄어쓰기·대소문자 변형으로 금지 문구를 피해 가지 못하게 한다. */
    static String normalize(String text) {
        return text.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static Optional<GuardrailViolation> violation(String stage, String rule, String detail) {
        return Optional.of(new GuardrailViolation(stage, rule, detail));
    }
}
