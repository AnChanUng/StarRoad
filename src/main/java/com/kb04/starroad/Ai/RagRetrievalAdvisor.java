package com.kb04.starroad.Ai;

import com.kb04.starroad.Service.RagIndexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 환각 차단 L1(검색 게이트) + L2(컨텍스트 주입).
 *
 * <p>질문으로 벡터 검색을 해서 유사도 기준을 넘는 자료가 없으면 LLM 을 부르지 않고 끝낸다.
 * 자료가 있으면 사용자 메시지를 {@code <자료>…</자료> 질문: …} 형태로 바꿔 LLM 에 넘긴다.
 *
 * <p>검색된 문서는 응답 컨텍스트의 {@link #DOCUMENTS} 에 담는다. ChatService 는 이것으로
 * 인용 번호를 검증하고(L3) DB 원본 수치를 출처 카드에 싣는다(L4).
 *
 * <p>{@link VectorStore} 인터페이스에만 의존하므로 SimpleVectorStore 를 PGVector 등으로
 * 바꿔도 이 클래스는 그대로다.
 */
@Slf4j
@Component
public class RagRetrievalAdvisor implements CallAdvisor {

    /** 응답 컨텍스트 키. 값은 {@code List<Document>} (없으면 빈 리스트) */
    public static final String DOCUMENTS = "starroad.rag.documents";

    /** multilingual-e5 는 질의에 "query: " 접두사를 붙여야 검색 품질이 나온다. */
    public static final String QUERY_PREFIX = "query: ";

    public static final String NOT_FOUND_MESSAGE =
            "제가 가진 청년정책·예적금 상품 자료로는 답변드릴 수 없습니다. "
                    + "정책명이나 상품명을 좀 더 구체적으로 알려주시면 다시 찾아보겠습니다.";

    private final VectorStore vectorStore;

    /** 검색해 올 문서 수 */
    @Value("${starroad.rag.top-k:4}")
    private int topK;

    /**
     * 코사인 유사도 하한. 이 값 미만이면 '관련 자료 없음'으로 처리한다.
     *
     * <p>multilingual-e5 는 무관한 문장끼리도 0.78 안팎이 나오는 특성이 있어
     * 임계값을 낮게 잡으면 게이트가 그대로 무력해진다.
     * {@code RagRetrievalCalibrationTest}·{@code RagRetrievalEvalTest} 실측으로 정했다.
     */
    @Value("${starroad.rag.similarity-threshold:0.85}")
    private double similarityThreshold;

    public RagRetrievalAdvisor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String question = request.prompt().getUserMessage().getText();

        List<Document> found = search(question);
        if (found.isEmpty()) {
            log.debug("[RAG] 임계값 {} 이상 자료 없음 — LLM 호출 생략. 질문: {}", similarityThreshold, question);
            return AdvisorResponses.shortCircuit(NOT_FOUND_MESSAGE, request.context(), DOCUMENTS, List.of());
        }

        ChatClientRequest augmented = request.mutate()
                .prompt(request.prompt().augmentUserMessage(
                        user -> user.mutate().text(buildUserMessage(question, found)).build()))
                .context(DOCUMENTS, found)
                .build();

        ChatClientResponse response = chain.nextCall(augmented);

        Map<String, Object> context = new HashMap<>(response.context());
        context.put(DOCUMENTS, found);
        return response.mutate().context(context).build();
    }

    /** L1 검색. 평가 테스트에서도 운영과 같은 조건으로 검색하려고 공개한다. */
    public List<Document> search(String question) {
        return search(question, topK, similarityThreshold);
    }

    public List<Document> search(String question, int k, double threshold) {
        List<Document> found = vectorStore.similaritySearch(SearchRequest.builder()
                .query(QUERY_PREFIX + question)
                .topK(k)
                .similarityThreshold(threshold)
                .build());
        return found == null ? List.of() : found;
    }

    static String buildUserMessage(String question, List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        sb.append("<자료>\n");
        for (int i = 0; i < documents.size(); i++) {
            sb.append("[").append(i + 1).append("] ");
            sb.append(stripPassagePrefix(documents.get(i).getText())).append("\n\n");
        }
        sb.append("</자료>\n\n");
        sb.append("질문: ").append(question);
        return sb.toString();
    }

    private static String stripPassagePrefix(String text) {
        if (text == null) {
            return "";
        }
        return text.startsWith(RagIndexService.PASSAGE_PREFIX)
                ? text.substring(RagIndexService.PASSAGE_PREFIX.length())
                : text;
    }

    public int getTopK() {
        return topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    @Override
    public String getName() {
        return "RagRetrievalAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2000;
    }
}
