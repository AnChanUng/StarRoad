# 벡터 저장소 선택: 인메모리 / Chroma

같은 RAG 검색·가드레일 코드에서 저장소를 선택한다. 한 실행에서 두 곳에 중복 저장하지 않으며, LLM 공급자(Claude/Gemini/Ollama) 선택과 독립적이다. 기본값은 기존 인메모리 방식이다.

## 인메모리로 실행

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev,ollama"
```

`SimpleVectorStore`를 사용하며 기존 `rag-index*.json` 저장·복원을 유지한다. 외부 Chroma 서버에 접속하지 않는다. `dev`는 Oracle 대신 H2를 선택하는 별개 옵션이다. H2 DB와 인메모리 벡터 저장소를 혼동하지 않는다.

## Chroma로 실행

```powershell
docker compose -f compose.chroma.yml up -d
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev,ollama,chroma"
```

이미 준비된 LLM 설정이 있다면 `ollama` 대신 해당 프로필을 사용하거나 생략한다. Chroma는 임베딩 모델이나 생성 모델을 대신 실행하지 않는다. 기존 JVM 임베딩 모델을 그대로 사용한다.

`compose.chroma.yml`은 Chroma 1.0.20을 로컬 8000 포트에 실행하고 named volume `/data`에 보관한다. DB를 삭제하려는 경우가 아니라면 `docker compose down -v`를 사용하지 않는다. 기본 포트는 로컬 컴퓨터에서만 접근할 수 있다. 공유·운영 서버의 인증·TLS 구성은 별도 범위다.

설정값:

| 설정 | 기본값 |
|---|---|
| `starroad.rag.store` / `RAG_VECTOR_STORE` | `memory` (`chroma`로 변경 가능) |
| `CHROMA_URL` | `http://localhost:8000` |
| `CHROMA_COLLECTION` | `starroad` |
| `CHROMA_TENANT` | `default_tenant` |
| `CHROMA_DATABASE` | `default_database` |

`chroma` 프로필은 store를 chroma로 지정한다. 프로필 없이 `RAG_VECTOR_STORE=chroma`로도 선택할 수 있으며 주소 등은 위 설정 키로 지정한다. Chroma가 연결되지 않으면 인메모리로 몰래 대체하지 않는다.

## 색인 동작

- 인메모리: 기존 파일이 있고 `always-reindex=false`이면 파일 복원. 그 외에는 원본 정책·상품 DB를 다시 읽는다.
- Chroma: 원본 DB 기준으로 기동할 때마다 다시 색인한다. JSON 파일은 읽거나 쓰지 않는다.
- 저장소를 전환할 때 기존 메모리 JSON을 Chroma로 직접 가져오지 않고, **같은 임베딩 모델로 원본 문서를 다시 임베딩**한다.
- 재색인에서는 `type=policy/product` 자료를 지운 후 현재 원본 자료를 추가해 삭제된 데이터가 검색에 남지 않게 한다. 다른 type은 보존한다. 비어 있는 원본도 반영한다.
- 재색인은 원자적 교체가 아니다. 적재 중 조회에 공백이 생길 수 있고 실패하면 재실행이 필요하다. 다중 인스턴스가 같은 collection에 동시에 재색인하지 않게 운영해야 한다. 서로 다른 개발 DB/임베딩 모델에는 별도 collection을 사용한다.
- 파일로 복원하는 인메모리 색인에 원본 변경을 반영하려면 `always-reindex=true` 또는 수동 재색인이 필요하다.
- 저장소별 점수 특성이 다를 수 있으므로 기존 임계값 0.85가 그대로 적합한지는 평가셋으로 재검증한다.

## 검증

```powershell
.\mvnw.cmd test "-Dtest=VectorStoreSelectionTest,RagIndexStoreTest,ChatPipelineTest,GuardrailRulesTest"
$env:CHROMA_TEST_URL='http://localhost:8000'
.\mvnw.cmd test "-Dtest=ChromaVectorStoreIntegrationTest"
```

통합 테스트는 임시 collection과 고정 임베딩을 사용해 실제 Chroma의 적재·검색·새 연결에서 복원·필터 삭제를 검증한다. 실제 금융 질문의 품질 평가는 기존 RagRetrievalEvalTest로 별도 수행한다. 운영 DB를 테스트 대상으로 지정하지 않는다.

공식 문서: [Spring AI Chroma](https://docs.spring.io/spring-ai/reference/api/vectordbs/chroma.html). 프로젝트는 Spring AI 1.1.8 API를 기준으로 컴파일하며 최신 문서와 패키지명이 다를 수 있다.
