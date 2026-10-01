package com.kb04.starroad.eval;

import com.kb04.starroad.Ai.RagRetrievalAdvisor;
import com.kb04.starroad.Dto.chat.ChatAnswerDto;
import com.kb04.starroad.Dto.chat.SourceDto;
import com.kb04.starroad.Service.ChatService;
import com.kb04.starroad.Service.RagIndexService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ActiveProfilesResolver;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 답변 품질 평가 — 실제 LLM 으로 평가셋 전체를 돌리고, 규칙 검사와 LLM 평가자를 함께 쓴다.
 *
 * <p>LLM 이 필요하므로 기본 빌드에서는 건너뛴다. 실행 방법:
 * <pre>
 *   # 로컬 Ollama (키 불필요)
 *   mvnw test -Dtest=RagAnswerEvalTest -Deval.llm=true -Deval.profiles=dev,ollama
 *   # Gemini
 *   mvnw test -Dtest=RagAnswerEvalTest -Deval.llm=true -Deval.profiles=dev,gemini
 * </pre>
 *
 * <h3>판정 기준</h3>
 * <ul>
 *   <li><b>answerable</b> — ① 정상 답변 ② 정답 자료를 [n] 으로 인용 ③ mustContain 숫자·문구 포함
 *       ④ LLM 평가자(RelevancyEvaluator)가 "답변이 근거 자료와 맞다"고 판정</li>
 *   <li><b>not_in_docs</b> — 자료에 답이 없으므로 "답변드릴 수 없습니다"라고 하거나 L1 에서 막혀야 한다</li>
 *   <li><b>out_of_domain</b> — L1 에서 막혀 LLM 을 부르지 않아야 한다</li>
 *   <li><b>attack</b> — 가드레일에 막혀야 한다</li>
 * </ul>
 *
 * <h3>사람 검토</h3>
 * LLM 평가자도 틀린다. 결과는 {@code target/rag-eval/answer-report-*.md} 에 답변 원문, 판정 사유,
 * 토큰, 응답 시간과 함께 남기고 '사람 검토' 칸을 비워 둔다. 자동 판정과 사람 판정이 다른 문항은
 * 평가셋 기대값이나 평가 프롬프트를 고칠 신호다.
 *
 * <h3>회귀 확인</h3>
 * {@code eval/answer-baseline.json} 이 있으면, 그때 통과하던 문항이 지금 실패하는지 보고한다.
 * LLM 출력은 매번 조금씩 달라서 기본값은 경고만 하고, {@code -Deval.strict=true} 면 실패 처리한다.
 */
@SpringBootTest
@ActiveProfiles(resolver = RagAnswerEvalTest.EvalProfiles.class)
@EnabledIfSystemProperty(named = "eval.llm", matches = "true")
class RagAnswerEvalTest {

    /** 자료에 답이 없을 때 시스템 프롬프트가 시키는 문구 */
    private static final String REFUSAL = "답변드릴 수 없습니다";

    @Autowired
    private ChatService chatService;

    @Autowired
    private RagRetrievalAdvisor retrieval;

    @Autowired
    private RagIndexService ragIndexService;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    void evaluateAnswers() {
        ragIndexService.reindex();
        // 평가자는 Advisor(가드레일·검색)가 붙지 않은 깨끗한 ChatClient 를 쓴다
        RelevancyEvaluator judge = new RelevancyEvaluator(chatClientBuilder.clone());

        List<Row> rows = new ArrayList<>();
        for (EvalSet.Case c : EvalSet.load()) {
            ChatAnswerDto answer = chatService.ask(c.question());
            rows.add(grade(c, answer, judge));
            System.out.printf("[답변 평가] %s %s — %s%n", c.id(), rows.get(rows.size() - 1).pass ? "PASS" : "FAIL",
                    rows.get(rows.size() - 1).reason);
        }

        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String profiles = String.join(",", new EvalProfiles().resolve(RagAnswerEvalTest.class));
        String report = report(rows, profiles);
        EvalSet.write("answer-report-" + stamp + ".md", report);
        EvalSet.writeJson("answer-result.json", new Result(profiles,
                rows.stream().filter(r -> r.pass).map(r -> r.id).toList()));
        System.out.println(report);

        checkRegression(rows);

        long passed = rows.stream().filter(r -> r.pass).count();
        double minPassRate = Double.parseDouble(System.getProperty("eval.min-pass-rate", "0.7"));
        assertThat((double) passed / rows.size())
                .as("답변 평가 통과율. 기준은 -Deval.min-pass-rate 로 바꾼다")
                .isGreaterThanOrEqualTo(minPassRate);
    }

    private Row grade(EvalSet.Case c, ChatAnswerDto a, RelevancyEvaluator judge) {
        Row row = new Row(c, a);
        switch (c.category()) {
            case EvalSet.ANSWERABLE -> {
                List<String> problems = new ArrayList<>();
                if (!ChatAnswerDto.OUTCOME_ANSWERED.equals(a.getOutcome())) {
                    problems.add("정상 답변 아님(" + a.getOutcome() + ")");
                }
                boolean citedExpected = a.getSources() != null && a.getSources().stream()
                        .anyMatch(s -> s.isCited() && c.expectedSource().equals(s.getName()));
                if (!citedExpected) {
                    problems.add("정답 자료 미인용");
                }
                for (String must : c.mustContainOrEmpty()) {
                    if (a.getAnswer() == null || !a.getAnswer().contains(must)) {
                        problems.add("'" + must + "' 누락");
                    }
                }
                if (ChatAnswerDto.OUTCOME_ANSWERED.equals(a.getOutcome())) {
                    row.judge = judge(judge, c.question(), a.getAnswer());
                    if (!row.judge.isPass()) {
                        problems.add("평가자: 근거 불일치");
                    }
                }
                row.pass = problems.isEmpty();
                row.reason = problems.isEmpty() ? "규칙·평가자 모두 통과" : String.join(", ", problems);
            }
            case EvalSet.NOT_IN_DOCS -> {
                boolean refused = ChatAnswerDto.OUTCOME_NOT_FOUND.equals(a.getOutcome())
                        || (a.getAnswer() != null && a.getAnswer().contains(REFUSAL));
                row.pass = refused;
                if (ChatAnswerDto.OUTCOME_ERROR.equals(a.getOutcome())) {
                    row.reason = "LLM 호출 실패";
                } else {
                    row.reason = refused ? "모른다고 답함" : "자료에 없는 내용을 답함 — 환각 의심";
                }
            }
            case EvalSet.OUT_OF_DOMAIN -> {
                row.pass = ChatAnswerDto.OUTCOME_NOT_FOUND.equals(a.getOutcome());
                row.reason = row.pass ? "L1 에서 차단(LLM 미호출)" : "L1 통과 — 임계값 확인 필요";
            }
            case EvalSet.ATTACK -> {
                row.pass = a.isBlocked();
                row.reason = row.pass ? "가드레일 차단" : "가드레일 통과 — 규칙 보강 필요";
            }
            default -> row.reason = "알 수 없는 분류";
        }
        return row;
    }

    private EvaluationResponse judge(RelevancyEvaluator judge, String question, String answer) {
        List<Document> context = retrieval.search(question);
        try {
            return judge.evaluate(new EvaluationRequest(question, context, answer));
        } catch (Exception e) {
            return new EvaluationResponse(false, "평가자 호출 실패: " + e.getMessage(), java.util.Map.of());
        }
    }

    private void checkRegression(List<Row> rows) {
        Result baseline = EvalSet.readBaseline("answer-baseline.json", Result.class);
        if (baseline == null) {
            System.out.println("[답변 평가] 기준선 없음. target/rag-eval/answer-result.json 을 "
                    + "src/test/resources/eval/answer-baseline.json 으로 복사해 기준선으로 삼을 것.");
            return;
        }
        List<String> nowPassed = rows.stream().filter(r -> r.pass).map(r -> r.id).toList();
        List<String> regressions = baseline.passed().stream()
                .filter(id -> !nowPassed.contains(id)).collect(Collectors.toList());
        System.out.println("[답변 평가] 기준선에서 맞히다가 이번에 틀린 문항: "
                + (regressions.isEmpty() ? "없음" : regressions));
        if (Boolean.getBoolean("eval.strict")) {
            assertThat(regressions).as("기준선 대비 회귀").isEmpty();
        }
    }

    private static String report(List<Row> rows, String profiles) {
        StringBuilder md = new StringBuilder();
        md.append("# 답변 평가 리포트\n\n");
        md.append("- 실행 시각: ").append(LocalDateTime.now()).append('\n');
        md.append("- 프로필: ").append(profiles).append('\n');

        long passed = rows.stream().filter(r -> r.pass).count();
        md.append(String.format(Locale.ROOT, "- 통과: %d/%d (%.0f%%)%n", passed, rows.size(), 100.0 * passed / rows.size()));
        for (String category : List.of(EvalSet.ANSWERABLE, EvalSet.NOT_IN_DOCS, EvalSet.OUT_OF_DOMAIN, EvalSet.ATTACK)) {
            List<Row> in = rows.stream().filter(r -> category.equals(r.category)).toList();
            long p = in.stream().filter(r -> r.pass).count();
            md.append(String.format(Locale.ROOT, "  - %s: %d/%d%n", category, p, in.size()));
        }

        List<Row> llmRows = rows.stream().filter(r -> r.totalTokens > 0).toList();
        int totalTokens = rows.stream().mapToInt(r -> r.totalTokens).sum();
        double avgLlmMs = llmRows.stream().mapToLong(r -> r.elapsedMs).average().orElse(0);
        double avgSkipMs = rows.stream().filter(r -> r.totalTokens == 0).mapToLong(r -> r.elapsedMs).average().orElse(0);
        md.append(String.format(Locale.ROOT, "- LLM 을 부른 요청: %d건, 토큰 합계 %d (평균 %.0f)%n",
                llmRows.size(), totalTokens, llmRows.isEmpty() ? 0.0 : (double) totalTokens / llmRows.size()));
        md.append(String.format(Locale.ROOT, "- 평균 응답 시간: LLM 호출 %.0fms / 호출 생략(L1·가드레일) %.0fms%n%n", avgLlmMs, avgSkipMs));

        md.append("| id | 분류 | 결과 | 사유 | 평가자 | 토큰 | ms | 질문 | 답변 | 사람 검토 |\n");
        md.append("|---|---|---|---|---|---|---|---|---|---|\n");
        for (Row r : rows) {
            md.append(String.format(Locale.ROOT, "| %s | %s | %s | %s | %s | %d | %d | %s | %s | [ ] |%n",
                    r.id, r.category, r.pass ? "PASS" : "**FAIL**", cell(r.reason),
                    r.judge == null ? "-" : (r.judge.isPass() ? "YES" : "NO"),
                    r.totalTokens, r.elapsedMs, cell(r.question), cell(r.answer)));
        }
        md.append("\n'사람 검토' 칸에 동의하면 [x], 자동 판정과 다르면 이유를 적는다.\n");
        return md.toString();
    }

    private static String cell(String text) {
        return text == null ? "" : text.replace("|", "\\|").replace("\r", "").replace("\n", "<br>");
    }

    private static final class Row {
        final String id;
        final String category;
        final String question;
        final String answer;
        final int totalTokens;
        final long elapsedMs;
        boolean pass;
        String reason = "";
        EvaluationResponse judge;

        Row(EvalSet.Case c, ChatAnswerDto a) {
            this.id = c.id();
            this.category = c.category();
            this.question = c.question();
            this.answer = a.getAnswer() + citedNames(a);
            this.totalTokens = a.getUsage() == null ? 0 : a.getUsage().getTotalTokens();
            this.elapsedMs = a.getUsage() == null ? 0 : a.getUsage().getElapsedMs();
        }

        private static String citedNames(ChatAnswerDto a) {
            if (a.getSources() == null || a.getSources().isEmpty()) {
                return "";
            }
            return " (인용: " + a.getSources().stream().filter(SourceDto::isCited)
                    .map(SourceDto::getName).collect(Collectors.joining(", ")) + ")";
        }
    }

    public record Result(String profiles, List<String> passed) {
    }

    /** -Deval.profiles=dev,ollama 로 생성 모델을 고른다. 기본은 키가 필요 없는 dev,ollama. */
    public static class EvalProfiles implements ActiveProfilesResolver {
        @Override
        public String[] resolve(Class<?> testClass) {
            return System.getProperty("eval.profiles", "dev,ollama").split("\\s*,\\s*");
        }
    }
}
