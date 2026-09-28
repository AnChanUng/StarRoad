package com.kb04.starroad.eval;

import com.kb04.starroad.Ai.RagRetrievalAdvisor;
import com.kb04.starroad.Service.RagIndexService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 검색(L1) 품질 평가. LLM 없이 임베딩 모델만으로 돈다.
 *
 * <p>평가셋의 질문을 실제 색인(dev 샘플 데이터)에 던져 다음을 잰다.
 * <ul>
 *   <li><b>hit@k</b> — 답이 있는 질문에서 정답 자료가 검색 결과 안에 들어왔는가</li>
 *   <li><b>top1</b> — 정답 자료가 1순위인가</li>
 *   <li><b>MRR</b> — 정답 자료 순위의 역수 평균</li>
 *   <li><b>거부율</b> — 무관한 질문을 L1 이 막았는가</li>
 * </ul>
 *
 * <p><b>두 가지를 한다.</b>
 * <ol>
 *   <li>{@link #sweepTopKAndThreshold()} — top-k 와 유사도 임계값을 하나씩 바꿔 가며 표로 비교한다.
 *       설정을 바꾸기 전에 이 표를 보고 근거를 남긴다.</li>
 *   <li>{@link #currentConfig_noRegression()} — 현재 설정으로 평가하고, 기준선
 *       ({@code eval/retrieval-baseline.json})에서 맞히던 질문을 이제 틀리지 않는지 확인한다.
 *       전체 점수가 올라도 개별 질문이 깨지면 실패한다.</li>
 * </ol>
 *
 * <p>결과는 {@code target/rag-eval/} 에 남는다. 개선을 확정하려면
 * {@code target/rag-eval/retrieval-result.json} 을 {@code src/test/resources/eval/retrieval-baseline.json}
 * 으로 복사해 기준선을 올린다.
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RagRetrievalEvalTest {

    private static final int[] TOP_KS = {1, 2, 4, 6};
    private static final double[] THRESHOLDS = {0.80, 0.81, 0.82, 0.83, 0.84, 0.85, 0.86, 0.87};

    @Autowired
    private RagRetrievalAdvisor retrieval;

    @Autowired
    private RagIndexService ragIndexService;

    private List<EvalSet.Case> cases;

    /** 질문별로 임계값 없이 넉넉히(최대 k) 검색해 둔 결과. 설정 조합은 이 결과를 잘라서 계산한다. */
    private Map<String, List<Document>> rawResults;

    @BeforeAll
    void searchOnce() {
        // 테스트 컨텍스트에서는 ApplicationReadyEvent 색인이 끝났는지 보장하려고 한 번 더 만든다.
        assertThat(ragIndexService.reindex()).as("색인할 샘플 데이터가 있어야 한다").isPositive();

        cases = EvalSet.load();
        int maxK = TOP_KS[TOP_KS.length - 1];
        rawResults = new LinkedHashMap<>();
        for (EvalSet.Case c : cases) {
            if (EvalSet.ANSWERABLE.equals(c.category()) || EvalSet.OUT_OF_DOMAIN.equals(c.category())
                    || EvalSet.NOT_IN_DOCS.equals(c.category())) {
                rawResults.put(c.id(), retrieval.search(c.question(), maxK, 0.0));
            }
        }
    }

    // ------------------------------------------------------------------ 1. 설정 비교표

    @Test
    void sweepTopKAndThreshold() {
        StringBuilder md = new StringBuilder();
        md.append("# 검색 설정 비교 (top-k × 유사도 임계값)\n\n");
        md.append("답이 있는 질문 ").append(EvalSet.byCategory(cases, EvalSet.ANSWERABLE).size())
                .append("개, 무관한 질문 ").append(EvalSet.byCategory(cases, EvalSet.OUT_OF_DOMAIN).size())
                .append("개 기준. `*` 는 현재 설정.\n\n");
        md.append("| top-k | 임계값 | hit@k | top1 | MRR | 무관 질문 거부율 | 자료 밖 질문 L1 통과 |\n");
        md.append("|---|---|---|---|---|---|---|\n");

        for (int k : TOP_KS) {
            for (double t : THRESHOLDS) {
                Metrics m = evaluate(k, t);
                boolean current = k == retrieval.getTopK()
                        && Math.abs(t - retrieval.getSimilarityThreshold()) < 1e-9;
                md.append(String.format(Locale.ROOT, "| %d | %.2f%s | %s | %s | %.3f | %s | %d/%d |%n",
                        k, t, current ? " *" : "",
                        pct(m.hits, m.answerable), pct(m.top1, m.answerable), m.mrr,
                        pct(m.rejected, m.outOfDomain), m.notInDocsPassed, m.notInDocs));
            }
        }

        md.append("\n## 질문별 1순위 자료와 점수\n\n");
        md.append("| id | 분류 | 질문 | 1순위 자료 | 점수 | 정답 순위 |\n|---|---|---|---|---|---|\n");
        for (EvalSet.Case c : cases) {
            List<Document> docs = rawResults.get(c.id());
            if (docs == null) {
                continue;
            }
            Document top = docs.isEmpty() ? null : docs.get(0);
            md.append(String.format(Locale.ROOT, "| %s | %s | %s | %s | %.4f | %s |%n",
                    c.id(), c.category(), c.question(),
                    top == null ? "-" : name(top), top == null ? 0.0 : score(top),
                    c.expectedSource() == null ? "-" : rankLabel(docs, c.expectedSource())));
        }

        EvalSet.write("retrieval-sweep.md", md.toString());
        System.out.println("\n" + md);
    }

    // ------------------------------------------------------------------ 2. 현재 설정 + 회귀 확인

    @Test
    void currentConfig_noRegression() {
        int k = retrieval.getTopK();
        double t = retrieval.getSimilarityThreshold();

        List<CaseResult> results = new ArrayList<>();
        for (EvalSet.Case c : cases) {
            if (EvalSet.ANSWERABLE.equals(c.category())) {
                int rank = rankOf(cut(rawResults.get(c.id()), k, t), c.expectedSource());
                results.add(new CaseResult(c.id(), c.category(), rank > 0, rank));
            } else if (EvalSet.OUT_OF_DOMAIN.equals(c.category())) {
                boolean rejected = cut(rawResults.get(c.id()), k, t).isEmpty();
                results.add(new CaseResult(c.id(), c.category(), rejected, 0));
            }
        }

        Metrics m = evaluate(k, t);
        List<String> passed = results.stream().filter(CaseResult::pass).map(CaseResult::id).toList();
        List<String> failed = results.stream().filter(r -> !r.pass()).map(CaseResult::id).toList();

        Baseline current = new Baseline(k, t, m.hitRate(), m.rejectRate(), m.mrr,
                round4(m.minRelevantScore), round4(m.maxIrrelevantScore), passed);
        EvalSet.writeJson("retrieval-result.json", current);

        System.out.printf(Locale.ROOT, "%n[검색 평가] top-k=%d, 임계값=%.2f → hit@k %s, top1 %s, MRR %.3f, 무관 질문 거부 %s%n",
                k, t, pct(m.hits, m.answerable), pct(m.top1, m.answerable), m.mrr, pct(m.rejected, m.outOfDomain));
        System.out.printf(Locale.ROOT, "[검색 평가] 임계값 여유: 무관 질문 최고점 %.4f 보다 %+.4f, 정답 자료 최저점 %.4f 보다 %+.4f%n",
                m.maxIrrelevantScore, t - m.maxIrrelevantScore, m.minRelevantScore, t - m.minRelevantScore);
        System.out.println("[검색 평가] 실패한 질문: " + (failed.isEmpty() ? "없음" : failed));

        Baseline baseline = EvalSet.readBaseline("retrieval-baseline.json", Baseline.class);
        if (baseline == null) {
            System.out.println("[검색 평가] 기준선이 없다. target/rag-eval/retrieval-result.json 을 "
                    + "src/test/resources/eval/retrieval-baseline.json 으로 복사해 기준선으로 삼을 것.");
            return;
        }

        List<String> regressions = baseline.passed().stream()
                .filter(id -> !passed.contains(id))
                .collect(Collectors.toList());
        System.out.printf(Locale.ROOT, "[검색 평가] 기준선(top-k=%d, 임계값=%.2f) 대비 hit %.3f→%.3f, 거부율 %.3f→%.3f, "
                        + "무관 질문과의 여유 %.4f→%.4f%n",
                baseline.topK(), baseline.threshold(), baseline.hitRate(), m.hitRate(),
                baseline.rejectRate(), m.rejectRate(),
                baseline.threshold() - baseline.maxIrrelevantScore(), t - m.maxIrrelevantScore);

        assertThat(regressions)
                .as("기준선에서 맞히던 질문이 이제 틀린다. 전체 점수가 올랐더라도 확인이 필요하다")
                .isEmpty();
    }

    // ------------------------------------------------------------------ 계산

    private Metrics evaluate(int k, double threshold) {
        Metrics m = new Metrics();
        for (EvalSet.Case c : cases) {
            List<Document> raw = rawResults.get(c.id());
            if (raw == null) {
                continue;
            }
            // 임계값과 무관하게 점수 분포를 기록한다 — 임계값을 얼마나 여유 있게 잡았는지 보려는 것이다
            if (EvalSet.ANSWERABLE.equals(c.category())) {
                raw.stream().filter(d -> c.expectedSource().equals(name(d))).findFirst()
                        .ifPresent(d -> m.minRelevantScore = Math.min(m.minRelevantScore, score(d)));
            } else if (EvalSet.OUT_OF_DOMAIN.equals(c.category()) && !raw.isEmpty()) {
                m.maxIrrelevantScore = Math.max(m.maxIrrelevantScore, score(raw.get(0)));
            }
            List<Document> docs = cut(raw, k, threshold);
            switch (c.category()) {
                case EvalSet.ANSWERABLE -> {
                    m.answerable++;
                    int rank = rankOf(docs, c.expectedSource());
                    if (rank > 0) {
                        m.hits++;
                        m.reciprocalSum += 1.0 / rank;
                    }
                    if (rank == 1) {
                        m.top1++;
                    }
                }
                case EvalSet.OUT_OF_DOMAIN -> {
                    m.outOfDomain++;
                    if (docs.isEmpty()) {
                        m.rejected++;
                    }
                }
                case EvalSet.NOT_IN_DOCS -> {
                    m.notInDocs++;
                    if (!docs.isEmpty()) {
                        m.notInDocsPassed++;
                    }
                }
                default -> {
                }
            }
        }
        m.mrr = m.answerable == 0 ? 0 : m.reciprocalSum / m.answerable;
        return m;
    }

    /** 운영과 같은 조건(상위 k개 + 임계값 이상)으로 자른다. SimpleVectorStore 결과는 점수 내림차순이다. */
    private static List<Document> cut(List<Document> raw, int k, double threshold) {
        return raw.stream().filter(d -> score(d) >= threshold).limit(k).toList();
    }

    private static int rankOf(List<Document> docs, String expectedName) {
        for (int i = 0; i < docs.size(); i++) {
            if (expectedName.equals(name(docs.get(i)))) {
                return i + 1;
            }
        }
        return 0;
    }

    private static String rankLabel(List<Document> docs, String expectedName) {
        int rank = rankOf(docs, expectedName);
        return rank == 0 ? "없음" : rank + "위 (" + String.format(Locale.ROOT, "%.4f", score(docs.get(rank - 1))) + ")";
    }

    private static String name(Document doc) {
        return String.valueOf(doc.getMetadata().get(RagIndexService.META_NAME));
    }

    private static double score(Document doc) {
        return doc.getScore() == null ? 0.0 : doc.getScore();
    }

    private static String pct(int n, int total) {
        return total == 0 ? "-" : String.format(Locale.ROOT, "%d/%d (%.0f%%)", n, total, 100.0 * n / total);
    }

    private static final class Metrics {
        int answerable;
        int hits;
        int top1;
        double reciprocalSum;
        double mrr;
        int outOfDomain;
        int rejected;
        int notInDocs;
        int notInDocsPassed;
        double minRelevantScore = 1.0;
        double maxIrrelevantScore = 0.0;

        double hitRate() {
            return answerable == 0 ? 0 : (double) hits / answerable;
        }

        double rejectRate() {
            return outOfDomain == 0 ? 0 : (double) rejected / outOfDomain;
        }
    }

    record CaseResult(String id, String category, boolean pass, int rank) {
    }

    /** 기준선 파일 형식. retrieval-result.json 과 같다. */
    public record Baseline(int topK, double threshold, double hitRate, double rejectRate,
                           double mrr, double minRelevantScore, double maxIrrelevantScore,
                           List<String> passed) {
    }

    private static double round4(double v) {
        return Math.round(v * 10000) / 10000.0;
    }
}
