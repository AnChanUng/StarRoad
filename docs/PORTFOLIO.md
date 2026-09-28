# StarRoad 정책 상담: Spring AI · ChromaDB · KLUE-BERT

정책명이나 상품명을 모르는 사용자도 자연어로 자료를 찾을 수 있게 한다. 정책·상품 한 건을 하나의 문서로 유지하므로, 다른 정책의 금리나 신청 조건을 같은 자료로 전달할 위험을 줄인다.

## 실행 경로

- 색인: DB → `RagIndexService` → 로컬 문장 인코더 → ChromaDB upsert. 문서 ID는 원본 PK에서 만든다. 동기화가 성공하면 이번 배치에 없는 이전 문서를 지운다. 한 컬렉션은 한 앱 인스턴스만 색인을 작성하는 전제다.
- 질문: 입력 가드레일 → 같은 인코더 → ChromaDB 코사인 검색 → 상위 4건 중 점수 0.40 이상인 자료만 전달. 자료가 없으면 LLM을 호출하지 않는다.
- 생성: Spring AI의 ChatClient가 별도 생성 모델을 호출한다. 출력 가드레일을 지난 답변에서 유효한 인용번호를 표시하고 색인 당시의 수치를 출처 카드에 첨부한다. 유효한 인용번호만으로 문장의 사실 일치까지 검증되지는 않는다.
- 한국어 모델: [KLUE-BERT](https://huggingface.co/klue/bert-base)를 문장 유사도에 맞게 학습한 [seongyeon1/korean-sbert-korsts](https://huggingface.co/seongyeon1/korean-sbert-korsts)의 고정 리비전을 사용한다. 128토큰까지 입력하고 패딩을 제외해 평균을 구한 뒤 정규화한 768차원 벡터를 반환한다. 임베딩 HTTP 형식은 OpenAI 호환이지만 기본 주소는 로컬 인코더이며 OpenAI에 임베딩을 요청하지 않는다.

## 실행 및 확인 (PowerShell)

1. 저장소 루트에서 `docker compose up -d --build`. 첫 실행은 모델 가중치를 내려받는다. `http://localhost:8100/docs`와 `http://localhost:8000/api/v2/heartbeat`가 응답할 때까지 기다린다.
2. 로컬 Ollama 실행 후 `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev,ollama"`. dev는 H2에 정책 6건·상품 4건을 준비한다. 애플리케이션을 시작하면 해당 DB를 ChromaDB와 동기화한다. 챗봇은 `/starroad/chat`, API는 `POST /starroad/chat/ask`이다.
3. 가드레일을 포함한 가벼운 검사: `.\mvnw.cmd test "-Dtest=ChatPipelineTest,GuardrailRulesTest"`. 검색 통합 평가: 두 서비스를 실행한 상태에서 `.\mvnw.cmd test "-Dtest=RagRetrievalEvalTest"`. Anthropic 클라이언트 설정이 키를 요구하는 테스트 환경에서는 테스트용 더미 키를 사용해도 된다. 검색 평가는 실제 생성 API를 호출하지 않는다.
4. LLM 답변 평가는 별도: `.\mvnw.cmd test "-Dtest=RagAnswerEvalTest" "-Deval.llm=true" "-Deval.profiles=dev,ollama"`. 샘플 요청과 Actuator 지표는 [Postman 컬렉션](postman/starroad-chat.postman_collection.json)을 참고한다.

개발·테스트·기본 환경은 서로 다른 컬렉션을 사용한다. Chroma 데이터는 Docker 볼륨에 저장된다. 인코더 가중치나 문서 전처리가 달라지면 새 컬렉션을 사용하고 임계값도 다시 측정한다. 서버 주소는 `CHROMA_HOST`, `CHROMA_PORT`, `KLUE_ENCODER_URL`, 검색 임계값은 `RAG_THRESHOLD`로 변경할 수 있다. 서비스는 기본적으로 호스트의 루프백에만 노출된다.

## 실제 검색 평가

2026-11-05에 실제 모델과 ChromaDB 1.3.7로 측정했다. 임계값 0.25에서 무관 질문 1개가 통과했고, 0.40에서는 아래 결과를 얻었다.

| 평가 | KLUE-BERT + ChromaDB | 9월 E5 기준선 |
|---|---:|---:|
| 정답이 상위 4개에 포함 | 19/19 | 19/19 |
| 정답이 1위 | 15/19 | 19/19 |
| 첫 정답 순위의 역수 평균(MRR) | 0.877 | 1.000 |
| 무관 질문 검색 거부 | 6/6 | 6/6 |

정답 문서의 최저점은 0.5481이고 무관 질문의 최고점은 0.2681이었다. 0.40은 두 값 사이에 있다. 주제는 맞지만 답이 없는 4문항은 모두 검색을 통과했다. 이런 질문의 답변 거부 여부는 별도 생성 평가에서 확인해야 한다. 이 수치는 개발 샘플 결과이며 실제 사용자 질문의 정확도를 뜻하지 않는다.

## 변경 이력

이 브랜치는 [2026-09-28 원본](https://github.com/AnChanUng/StarRoad/commit/57d2946)을 기준으로 재구성했다. 새 커밋의 author date는 기준 시점인 9월로 지정했고, 실제 구현·검증 및 커미터 날짜는 2026-11-05다. 9월 원본은 E5와 인메모리 저장소를 사용했다. 이전의 자세한 측정 기록은 [당시 포트폴리오](https://github.com/AnChanUng/StarRoad/blob/57d2946/docs/PORTFOLIO.md)에 남아 있다.
