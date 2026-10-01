package com.kb04.starroad.Service;

import com.kb04.starroad.Ai.ClearableVectorStore;
import com.kb04.starroad.Ai.TextChunker;
import com.kb04.starroad.Entity.Policy;
import com.kb04.starroad.Entity.Product;
import com.kb04.starroad.Repository.PolicyRepository;
import com.kb04.starroad.Repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DB 의 정책·상품 데이터를 벡터 색인으로 만드는 서비스.
 *
 * <p>RAG 의 'R'(검색)이 성립하려면 먼저 각 row 를 임베딩해 두어야 한다.
 * 색인은 애플리케이션 기동 시 1회 수행하고 파일로 저장해, 재기동 때는
 * 다시 임베딩하지 않고 불러온다.
 *
 * <p>색인되는 문서는 두 종류다.
 * <ul>
 *   <li><b>요약 문서</b> — 정책·상품 DB row 1건이 문서 1건. 200~400자라 나누지 않는다.</li>
 *   <li><b>공고문 청크</b> — 정책 공고문 원문({@code starroad.rag.notice-location})을
 *       {@link TextChunker} 로 나눈 조각. 조각마다 정책명·지역·분류를 머리말로 붙여
 *       "어느 정책의 조건인지"를 잃지 않게 한다.</li>
 * </ul>
 * 청크 크기와 겹침은 {@code RagChunkingEvalTest} 로 비교해서 정했다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagIndexService {

    /**
     * multilingual-e5 계열 모델은 학습 시 문서에 "passage: ", 질의에 "query: " 접두사를
     * 붙였다. 동일하게 맞춰야 검색 품질이 나온다.
     */
    public static final String PASSAGE_PREFIX = "passage: ";

    public static final String META_TYPE = "type";
    public static final String META_NO = "no";
    public static final String META_NAME = "name";
    public static final String META_LINK = "link";
    public static final String META_FACT = "fact";

    public static final String TYPE_POLICY = "policy";
    public static final String TYPE_PRODUCT = "product";

    /** 공고문 파일 첫 줄의 정책명 표기. 이 이름으로 DB 의 정책 row 와 연결한다. */
    private static final String NOTICE_NAME_KEY = "정책명:";
    /** 공고문 파일에서 머리말과 본문을 가르는 줄 */
    private static final String NOTICE_BODY_SEPARATOR = "---";

    private final ClearableVectorStore vectorStore;
    private final PolicyRepository policyRepository;
    private final ProductRepository productRepository;
    private final ResourceLoader resourceLoader;

    @Value("${starroad.rag.index-file:./rag-index.json}")
    private String indexFilePath;

    /**
     * true 면 저장된 색인을 무시하고 매 기동마다 DB 에서 새로 만든다.
     * 데이터가 매번 바뀌는 인메모리 DB(dev 프로필)에서 켠다.
     */
    @Value("${starroad.rag.always-reindex:false}")
    private boolean alwaysReindex;

    /**
     * 정책 공고문 원문 위치(예: {@code classpath:rag/notices/*.txt}, {@code file:/data/notices/*.txt}).
     * 비워 두면 공고문 없이 요약 문서만 색인한다.
     */
    @Value("${starroad.rag.notice-location:}")
    private String noticeLocation;

    /** 공고문 청크 한 개의 최대 글자 수. 0 이면 나누지 않는다. */
    @Value("${starroad.rag.chunk-size:200}")
    private int chunkSize;

    /** 이웃한 청크가 겹치는 최대 글자 수 */
    @Value("${starroad.rag.chunk-overlap:50}")
    private int chunkOverlap;

    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        File indexFile = new File(indexFilePath);
        try {
            if (indexFile.exists() && !alwaysReindex) {
                vectorStore.load(indexFile);
                log.info("[RAG] 기존 색인을 불러왔다: {}", indexFile.getAbsolutePath());
            } else {
                reindex();
            }
        } catch (Exception e) {
            // DB 가 떠 있지 않거나 임베딩 모델 다운로드에 실패해도 앱 자체는 기동시킨다.
            // 이 경우 챗봇만 "자료 없음"으로 동작한다.
            log.warn("[RAG] 색인 초기화 실패 — 챗봇이 답변할 자료가 없는 상태다. 원인: {}", e.toString());
        }
    }

    /**
     * DB 를 다시 읽어 색인을 통째로 만든다. 정책·상품 데이터나 공고문, 청크 설정을 바꾼 뒤 호출한다.
     *
     * @return 색인된 문서 수
     */
    public int reindex() {
        List<Document> documents = buildDocuments(chunkSize, chunkOverlap, true);

        if (documents.isEmpty()) {
            log.warn("[RAG] 색인할 정책·상품 데이터가 DB 에 없다.");
            return 0;
        }

        // 청크 설정이 바뀌면 청크 개수가 달라진다. 비우지 않으면 예전 조각이 남아 검색된다.
        vectorStore.clear();
        vectorStore.add(documents);

        File indexFile = new File(indexFilePath);
        vectorStore.save(indexFile);
        log.info("[RAG] 문서 {}건 색인 완료(청크 {}자, 겹침 {}자) → {}",
                documents.size(), chunkSize, chunkOverlap, indexFile.getAbsolutePath());

        return documents.size();
    }

    /**
     * 색인할 문서를 만든다. 저장소에 넣지는 않는다.
     *
     * <p>공개한 이유는 {@code RagChunkingEvalTest} 가 청크 설정을 바꿔 가며 운영과 똑같은
     * 방식으로 만든 문서를 비교하기 위해서다.
     *
     * @param chunkSize    공고문 청크 최대 글자 수. 0 이면 공고문 한 건을 통째로 한 문서로 만든다.
     * @param chunkOverlap 청크 겹침 글자 수
     * @param chunkHeader  청크마다 정책명·지역·분류 머리말을 붙일지. 운영은 항상 true.
     */
    public List<Document> buildDocuments(int chunkSize, int chunkOverlap, boolean chunkHeader) {
        List<Document> documents = new ArrayList<>();
        List<Policy> policies = policyRepository.findAll();

        for (Policy policy : policies) {
            documents.add(toDocument(policy));
        }
        for (Product product : productRepository.findAll()) {
            documents.add(toDocument(product));
        }
        documents.addAll(noticeDocuments(policies, chunkSize, chunkOverlap, chunkHeader));
        return documents;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    // ------------------------------------------------------------------ 공고문 원문

    private List<Document> noticeDocuments(List<Policy> policies, int chunkSize, int chunkOverlap,
                                           boolean chunkHeader) {
        List<Document> documents = new ArrayList<>();
        for (Notice notice : loadNotices()) {
            Policy policy = policies.stream()
                    .filter(p -> p.getName().equals(notice.policyName()))
                    .findFirst().orElse(null);
            if (policy == null) {
                // 출처 카드에 실을 DB 원본값(L4)이 없는 자료는 넣지 않는다.
                log.warn("[RAG] 공고문 '{}' 과 이름이 같은 정책이 DB 에 없어 색인하지 않는다.", notice.policyName());
                continue;
            }
            List<String> chunks = TextChunker.split(notice.body(), chunkSize, chunkOverlap);
            for (int i = 0; i < chunks.size(); i++) {
                documents.add(toDocument(policy, chunks.get(i), i + 1, chunks.size(), chunkHeader));
            }
        }
        return documents;
    }

    private List<Notice> loadNotices() {
        if (!StringUtils.hasText(noticeLocation)) {
            return List.of();
        }
        List<Notice> notices = new ArrayList<>();
        try {
            Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                    .getResources(noticeLocation);
            // 파일 순서가 바뀌어도 청크 id 와 평가 결과가 같도록 이름순으로 고정한다
            Arrays.sort(resources, Comparator.comparing(r -> String.valueOf(r.getFilename())));
            for (Resource resource : resources) {
                Notice notice = parseNotice(resource.getContentAsString(StandardCharsets.UTF_8));
                if (notice == null) {
                    log.warn("[RAG] 공고문 형식이 맞지 않아 건너뛴다: {}", resource.getFilename());
                    continue;
                }
                notices.add(notice);
            }
        } catch (IOException e) {
            log.warn("[RAG] 공고문을 읽지 못했다({}). 요약 문서만 색인한다. 원인: {}", noticeLocation, e.toString());
        }
        return notices;
    }

    /** 첫 줄 "정책명: …", 구분선 "---", 그 아래가 본문이다. */
    static Notice parseNotice(String content) {
        int separator = content.indexOf(NOTICE_BODY_SEPARATOR);
        if (separator < 0) {
            return null;
        }
        String head = content.substring(0, separator).strip();
        String body = content.substring(separator + NOTICE_BODY_SEPARATOR.length()).strip();
        if (!head.startsWith(NOTICE_NAME_KEY) || body.isEmpty()) {
            return null;
        }
        return new Notice(head.substring(NOTICE_NAME_KEY.length()).strip(), body);
    }

    record Notice(String policyName, String body) {
    }

    private Document toDocument(Policy policy, String chunk, int index, int total, boolean chunkHeader) {
        // 머리말이 없으면 "소득 기준은 중위소득 60% 이하" 같은 조각이 어느 정책 것인지 알 수 없다.
        String text = chunkHeader
                ? String.format("정책명: %s%n지역: %s%n분류: %s%n공고문: %s",
                        policy.getName(), policy.getLocation(), policy.getTag(), chunk)
                : chunk;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(META_TYPE, TYPE_POLICY);
        metadata.put(META_NO, policy.getNo());
        metadata.put(META_NAME, policy.getName());
        metadata.put(META_LINK, policy.getLink());
        metadata.put(META_FACT, policyFact(policy) + String.format(" · 공고문 %d/%d", index, total));

        return Document.builder()
                .id(TYPE_POLICY + "-" + policy.getNo() + "-notice-" + index)
                .text(PASSAGE_PREFIX + text)
                .metadata(metadata)
                .build();
    }

    // ------------------------------------------------------------------ DB row 요약 문서

    private static String endDateOf(Policy policy) {
        return policy.getEndDate() == null
                ? "상시"
                : new SimpleDateFormat("yyyy-MM-dd").format(policy.getEndDate());
    }

    private static String policyFact(Policy policy) {
        return String.format("지역: %s · 분류: %s · 마감: %s",
                policy.getLocation(), policy.getTag(), endDateOf(policy));
    }

    private Document toDocument(Policy policy) {
        String endDate = endDateOf(policy);
        String fact = policyFact(policy);

        // 임베딩 대상 텍스트. 정책명·지역·태그를 함께 넣어야 "서울 청년 월세" 같은
        // 복합 질의가 본문에만 의존하지 않고 걸린다.
        String text = String.format(
                "정책명: %s%n지역: %s%n분류: %s%n마감일: %s%n내용: %s",
                policy.getName(), policy.getLocation(), policy.getTag(), endDate, policy.getExplain());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(META_TYPE, TYPE_POLICY);
        metadata.put(META_NO, policy.getNo());
        metadata.put(META_NAME, policy.getName());
        metadata.put(META_LINK, policy.getLink());
        metadata.put(META_FACT, fact);

        return Document.builder()
                .id(TYPE_POLICY + "-" + policy.getNo())
                .text(PASSAGE_PREFIX + text)
                .metadata(metadata)
                .build();
    }

    private Document toDocument(Product product) {
        String kind = (product.getType() != null && product.getType() == 'S') ? "적금" : "예금";

        StringBuilder fact = new StringBuilder();
        fact.append(String.format("%s · 최고 연 %.2f%%", kind, product.getMaxRate()));
        fact.append(String.format(" · %d~%d개월", product.getMinPeriod(), product.getMaxPeriod()));
        fact.append(String.format(" · 최소 %,d원", product.getMinPrice()));
        if (product.getMaxPrice() != null) {
            fact.append(String.format(" · 최대 %,d원", product.getMaxPrice()));
        }

        String text = String.format(
                "상품명: %s%n종류: %s%n분류: %s%n최고금리: 연 %.2f%%%n가입기간: %d~%d개월%n"
                        + "가입금액: 최소 %,d원%n내용: %s",
                product.getName(), kind, product.getAttribute(), product.getMaxRate(),
                product.getMinPeriod(), product.getMaxPeriod(), product.getMinPrice(),
                product.getExplain());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(META_TYPE, TYPE_PRODUCT);
        metadata.put(META_NO, product.getNo());
        metadata.put(META_NAME, product.getName());
        metadata.put(META_LINK, product.getLink());
        metadata.put(META_FACT, fact.toString());

        return Document.builder()
                .id(TYPE_PRODUCT + "-" + product.getNo())
                .text(PASSAGE_PREFIX + text)
                .metadata(metadata)
                .build();
    }
}
