package com.kb04.starroad.eval;

import com.kb04.starroad.Ai.ClearableVectorStore;
import com.kb04.starroad.Ai.RagRetrievalAdvisor;
import com.kb04.starroad.Service.RagIndexService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공고문 청크 설정(Chunk Size × Chunk Overlap) 비교. LLM 없이 임베딩 모델만으로 돈다.
 *
 * <p>설정마다 색인을 새로 만들고 평가셋을 던져 아래를 잰다. 대상은 답이 공고문 원문에만 있는
 * {@code notice_detail} 질문이다.
 * <ul>
 *   <li><b>근거 유사도</b> — 질문과, 근거 문구가 실제로 들어 있는 자료 사이의 코사인 유사도 (평균·최저).
 *       이 값이 임계값보다 낮으면 근거가 있어도 L1 에서 막힌다.</li>
 *   <li><b>hit@k</b> — 임계값을 넘긴 상위 k개 안에 정답 정책의 자료가 있는가</li>
 *   <li><b>근거 포함</b> — 그 자료 한 개 안에 답의 근거 문구(evidence)가 실제로 들어 있는가.
 *       정책은 맞혔는데 엉뚱한 절을 가져오면 LLM 은 답을 못 하거나 지어낸다. 유사도만 보면 이걸 놓친다.</li>
 *   <li><b>무관 질문 최고점 · 거부율</b> — 청크가 늘면 무관한 질문이 우연히 걸릴 자리도 늘어난다</li>
 *   <li><b>주입 글자 수</b> — LLM 프롬프트에 들어가는 자료의 평균 길이(비용)</li>
 * </ul>
 *
 * <p>결과는 {@code target/rag-eval/chunking-sweep.md} 에 남는다.
 * {@code starroad.rag.chunk-size}, {@code starroad.rag.chunk-overlap} 을 바꾸기 전에 이 표를 본다.
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RagChunkingEvalTest {

    /** 0 은 나누지 않음(공고문 한 건 = 문서 한 건) */
    private static final int[] CHUNK_SIZES = {0, 150, 200, 300, 400, 600, 800};
    private static final int[] OVERLAPS = {0, 50, 100};
    private static final double[] THRESHOLDS = {0.80, 0.82, 0.84, 0.85, 0.86, 0.88, 0.90};

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private RagIndexService ragIndexService;

    @Autowired
    private RagRetrievalAdvisor retrieval;

    private List<EvalSet.Case> cases;

    /** 같은 설정을 두 번 임베딩하지 않으려고 결과를 모아 둔다 */
    private final Map<String, Run> runs = new LinkedHashMap<>();

    @BeforeAll
    void load() {
        cases = EvalSet.load();
        assertThat(EvalSet.byCategory(cases, EvalSet.NOTICE_DETAIL)).as("공고문 질문이 평가셋에 있어야 한다").isNotEmpty();
    }

    // ------------------------------------------------------------------ 1. 설정 비교표

    @Test
    void sweepChunkSizeAndOverlap() {
        int k = retrieval.getTopK();
        double t = retrieval.getSimilarityThreshold();

        StringBuilder md = new StringBuilder();
        md.append("# 공고문 청크 설정 비교 (Chunk Size × Chunk Overlap)\n\n");
        md.append(String.format(Locale.ROOT, "공고문 질문 %d개, 요약 문서 질문 %d개, 무관한 질문 %d개. top-k=%d, 임계값=%.2f. `*` 는 현재 설정.%n%n",
                EvalSet.byCategory(cases, EvalSet.NOTICE_DETAIL).size(),
                EvalSet.byCategory(cases, EvalSet.ANSWERABLE).size(),
                EvalSet.byCategory(cases, EvalSet.OUT_OF_DOMAIN).size(), k, t));

        md.append("## 1. 청크 크기 × 겹침 (머리말 있음)\n\n");
        md.append(HEADER_ROW);
        for (int size : CHUNK_SIZES) {
            for (int overlap : OVERLAPS) {
                if (!valid(size, overlap)) {
                    continue;
                }
                md.append(row(run(size, overlap, true), k, t));
            }
        }

        md.append("\n## 2. 머리말(정책명·지역·분류)을 청크마다 붙이는 효과 (겹침 ")
                .append(ragIndexService.getChunkOverlap()).append("자)\n\n");
        md.append(HEADER_ROW);
        for (int size : CHUNK_SIZES) {
            if (!valid(size, size == 0 ? 0 : ragIndexService.getChunkOverlap())) {
                continue;
            }
            int overlap = size == 0 ? 0 : ragIndexService.getChunkOverlap();
            md.append(row(run(size, overlap, true), k, t));
            md.append(row(run(size, overlap, false), k, t));
        }

        md.append("\n## 3. 임계값을 올리면 정상 질문이 막히는가 — 나누기 전과 현재 설정\n\n");
        md.append("공고문 질문의 근거 포함 수 / 무관 질문 거부 수.\n\n");
        Run whole = run(0, 0, true);
        Run current = run(ragIndexService.getChunkSize(), ragIndexService.getChunkOverlap(), true);
        md.append("| 임계값 | 나누지 않음: 근거 포함 | 나누지 않음: 무관 거부 | 현재 설정: 근거 포함 | 현재 설정: 무관 거부 |\n");
        md.append("|---|---|---|---|---|\n");
        for (double threshold : THRESHOLDS) {
            Metrics w = whole.metrics(k, threshold);
            Metrics c = current.metrics(k, threshold);
            md.append(String.format(Locale.ROOT, "| %.2f%s | %d/%d | %d/%d | %d/%d | %d/%d |%n",
                    threshold, Math.abs(threshold - t) < 1e-9 ? " *" : "",
                    w.evidenceHits, w.detail, w.rejected, w.outOfDomain,
                    c.evidenceHits, c.detail, c.rejected, c.outOfDomain));
        }

        md.append("\n## 4. 질문별 근거 유사도 — 나누기 전과 현재 설정\n\n");
        md.append("| id | 질문 | 나누지 않음 | 현재 설정 | 차이 | 현재 설정 근거 포함 |\n|---|---|---|---|---|---|\n");
        for (EvalSet.Case c : EvalSet.byCategory(cases, EvalSet.NOTICE_DETAIL)) {
            double before = whole.bestEvidenceScore(c);
            double after = current.bestEvidenceScore(c);
            md.append(String.format(Locale.ROOT, "| %s | %s | %.4f | %.4f | %+.4f | %s |%n",
                    c.id(), c.question(), before, after, after - before,
                    current.hasEvidence(c, k, t) ? "O" : "X"));
        }

        EvalSet.write("chunking-sweep.md", md.toString());
        System.out.println("\n" + md);
    }

    // ------------------------------------------------------------------ 2. 현재 설정 확인

    /** 청킹이 나누기 전보다 근거를 못 찾으면 설정이 잘못된 것이다. */
    @Test
    void currentChunkConfig_findsEvidenceBetterThanWholeDocument() {
        int k = retrieval.getTopK();
        double t = retrieval.getSimilarityThreshold();

        Metrics whole = run(0, 0, true).metrics(k, t);
        Metrics current = run(ragIndexService.getChunkSize(), ragIndexService.getChunkOverlap(), true).metrics(k, t);

        System.out.printf(Locale.ROOT, "%n[청크 평가] 청크 %d자·겹침 %d자 → 공고문 질문 근거 포함 %d/%d (나누기 전 %d/%d), "
                        + "근거 유사도 평균 %.4f (나누기 전 %.4f), 무관 질문 거부 %d/%d%n",
                ragIndexService.getChunkSize(), ragIndexService.getChunkOverlap(),
                current.evidenceHits, current.detail, whole.evidenceHits, whole.detail,
                current.avgScore, whole.avgScore, current.rejected, current.outOfDomain);

        assertThat(current.evidenceHits)
                .as("현재 청크 설정이 공고문을 나누지 않았을 때보다 근거를 덜 찾는다")
                .isGreaterThanOrEqualTo(whole.evidenceHits);
        assertThat(current.summaryHits)
                .as("공고문 청크를 넣은 뒤에도 요약 문서 질문은 그대로 찾아야 한다")
                .isEqualTo(current.summary);
        assertThat(current.rejected)
                .as("청크가 늘어도 무관한 질문은 L1 에서 막혀야 한다")
                .isEqualTo(current.outOfDomain);
    }

    // ------------------------------------------------------------------ 실행

    private static boolean valid(int chunkSize, int overlap) {
        return chunkSize == 0 ? overlap == 0 : overlap < chunkSize;
    }

    private Run run(int chunkSize, int overlap, boolean header) {
        String key = chunkSize + "/" + overlap + "/" + header;
        return runs.computeIfAbsent(key, ignored -> {
            List<Document> documents = ragIndexService.buildDocuments(chunkSize, overlap, header);
            ClearableVectorStore store = new ClearableVectorStore(embeddingModel);
            store.add(documents);

            Map<String, List<Document>> results = new LinkedHashMap<>();
            for (EvalSet.Case c : cases) {
                if (c.answerable() || EvalSet.OUT_OF_DOMAIN.equals(c.category())) {
                    // 임계값 없이 전부 받아 둔다. 설정 조합은 이 결과를 잘라서 계산한다.
                    results.put(c.id(), store.similaritySearch(SearchRequest.builder()
                            .query(RagRetrievalAdvisor.QUERY_PREFIX + c.question())
                            .topK(documents.size())
                            .similarityThreshold(0.0)
                            .build()));
                }
            }
            return new Run(chunkSize, overlap, header, documents.size(), results);
        });
    }

    private static final String HEADER_ROW =
            "| 청크 크기 | 겹침 | 머리말 | 문서 수 | 근거 유사도 평균 | 근거 유사도 최저 | hit@k | 근거 포함 | 요약 질문 hit@k "
                    + "| 무관 질문 최고점 | 무관 질문 거부 | 주입 글자 수 |\n|---|---|---|---|---|---|---|---|---|---|---|---|\n";

    private String row(Run run, int k, double t) {
        Metrics m = run.metrics(k, t);
        boolean current = run.header && run.chunkSize == ragIndexService.getChunkSize()
                && run.overlap == ragIndexService.getChunkOverlap();
        return String.format(Locale.ROOT, "| %s%s | %d | %s | %d | %.4f | %.4f | %d/%d | %d/%d | %d/%d | %.4f | %d/%d | %.0f |%n",
                run.chunkSize == 0 ? "나누지 않음" : String.valueOf(run.chunkSize), current ? " *" : "",
                run.overlap, run.header ? "O" : "X", run.documentCount,
                m.avgScore, m.minScore, m.hits, m.detail, m.evidenceHits, m.detail,
                m.summaryHits, m.summary, m.maxIrrelevantScore, m.rejected, m.outOfDomain, m.avgInjectedChars);
    }

    /** 설정 한 가지로 색인하고 검색해 둔 결과 */
    private final class Run {
        final int chunkSize;
        final int overlap;
        final boolean header;
        final int documentCount;
        final Map<String, List<Document>> results;

        Run(int chunkSize, int overlap, boolean header, int documentCount, Map<String, List<Document>> results) {
            this.chunkSize = chunkSize;
            this.overlap = overlap;
            this.header = header;
            this.documentCount = documentCount;
            this.results = results;
        }

        /** 운영과 같은 조건(임계값 이상 + 상위 k개)으로 자른다 */
        List<Document> cut(EvalSet.Case c, int k, double threshold) {
            return results.get(c.id()).stream().filter(d -> score(d) >= threshold).limit(k).toList();
        }

        /**
         * 근거 문구가 실제로 들어 있는 자료 중 질문과 가장 가까운 것의 유사도.
         * 같은 정책의 요약 문서는 정책명만 맞아도 점수가 높게 나오므로 세지 않는다.
         */
        double bestEvidenceScore(EvalSet.Case c) {
            return results.get(c.id()).stream()
                    .filter(d -> c.expectedSource().equals(name(d)))
                    .filter(d -> c.evidenceOrEmpty().stream().allMatch(e -> d.getText().contains(e)))
                    .mapToDouble(RagChunkingEvalTest::score).max().orElse(0.0);
        }

        boolean hasEvidence(EvalSet.Case c, int k, double threshold) {
            return cut(c, k, threshold).stream()
                    .filter(d -> c.expectedSource().equals(name(d)))
                    .anyMatch(d -> c.evidenceOrEmpty().stream().allMatch(e -> d.getText().contains(e)));
        }

        Metrics metrics(int k, double threshold) {
            Metrics m = new Metrics();
            List<Double> scores = new ArrayList<>();
            long injected = 0;
            for (EvalSet.Case c : cases) {
                if (!results.containsKey(c.id())) {
                    continue;
                }
                List<Document> docs = cut(c, k, threshold);
                boolean hit = docs.stream().anyMatch(d -> c.expectedSource() != null && c.expectedSource().equals(name(d)));
                switch (c.category()) {
                    case EvalSet.NOTICE_DETAIL -> {
                        m.detail++;
                        scores.add(bestEvidenceScore(c));
                        if (hit) {
                            m.hits++;
                        }
                        if (hasEvidence(c, k, threshold)) {
                            m.evidenceHits++;
                        }
                        injected += docs.stream().mapToInt(d -> d.getText().length()).sum();
                    }
                    case EvalSet.ANSWERABLE -> {
                        m.summary++;
                        if (hit) {
                            m.summaryHits++;
                        }
                    }
                    case EvalSet.OUT_OF_DOMAIN -> {
                        m.outOfDomain++;
                        if (docs.isEmpty()) {
                            m.rejected++;
                        }
                        List<Document> raw = results.get(c.id());
                        if (!raw.isEmpty()) {
                            m.maxIrrelevantScore = Math.max(m.maxIrrelevantScore, score(raw.get(0)));
                        }
                    }
                    default -> {
                    }
                }
            }
            m.avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            m.minScore = scores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            m.avgInjectedChars = m.detail == 0 ? 0 : (double) injected / m.detail;
            return m;
        }
    }

    private static final class Metrics {
        int detail;
        int hits;
        int evidenceHits;
        int summary;
        int summaryHits;
        int outOfDomain;
        int rejected;
        double avgScore;
        double minScore;
        double maxIrrelevantScore;
        double avgInjectedChars;
    }

    private static String name(Document doc) {
        return String.valueOf(doc.getMetadata().get(RagIndexService.META_NAME));
    }

    private static double score(Document doc) {
        return doc.getScore() == null ? 0.0 : doc.getScore();
    }
}
