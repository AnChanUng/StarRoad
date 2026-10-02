# Starroad 💫
💸 청년들의 효과적인 자산 운용을 위한 웹 서비스

<img width="1280" alt="starroadscreenshot" src="https://github.com/KB-StarRoad/.github/assets/98302932/ac6f42fe-1693-419f-bc52-cf6cdf697dde">


<br/>
<br/>


<br/>

## 🐳 프로젝트 개요 
정부 혹은 지자체에서 청년층에게 자산을 마련하거나 경제적으로 도움을 주는 여러 청년 정책을 시행하고 있습니다. 하지만 청년층은 청년 정책에 대해 잘 알지 못할 뿐만 아니라 본인에게 필요한 정책을 쉽게 알 수 없다고 합니다. 또한, 정부에서 지원하는 청년 희망 적금 서비스를 중도 해지한 사람이 청년층 4명 중 1명 꼴이라고 합니다. 

즉, 현재 청년층은 다양한 청년 정책을 효율적으로 이용하지 못하고 예적금 만기까지 달성하지 못하는 비율이 적지 않음을 알 수 있습니다. 

따라서 청년층에게 필요한 청년 정책을 쉽게 찾을 수 있고 현재 본인의 자산을 기반으로 적합한 금융 상품을 추천하고 지속할 수 있도록 돕는 서비스, **스타로드**를 기획하게 되었습니다. 


<br/>

## 🐳 기술 스택

| 구분 | 기술 |
|---|---|
| 언어·빌드 | Java 21, Maven (WAR) |
| 서버 | Spring Boot 3.5.16, Spring MVC, Spring Data JPA, Spring Security |
| 화면 | JSP · JSTL, JavaScript(fetch), jQuery, Bootstrap 5, Chart.js |
| DB | Oracle (운영), H2 인메모리 (dev 프로필) |
| AI | Spring AI 1.1.8 — 생성 모델 Anthropic Claude · Google Gemini · Ollama(로컬) 중 선택 |
| 임베딩·검색 | JVM 내장 ONNX(multilingual-e5-small), SimpleVectorStore |
| API 문서·관측 | springdoc-openapi 2.8.17 (Swagger UI), Spring Boot Actuator |
| 테스트 | JUnit 5 (spring-boot-starter-test) |

<br/>

## 🐳 주요 기능
### 🍿 KB 예적금 상품 추천
국민 은행 내의 금융 상품 중 사용자의 우대 조건을 포함한 최대 이율 및 월 수입과 저금 목표치를 고려하여 상품 만기 시 받을 수 있는 최대 금액을 계산하여 제공합니다. 

같은 상품이라도 충족한 우대 조건이 달라 실제로 받는 이율은 사람마다 다릅니다. 계산은 `MaturityCalculator`가 세 단계로 합니다.

| 단계 | 계산 |
|---|---|
| 적용 이율 | 기본 금리(최고 금리 − 최대 우대금리, 기간을 고르면 그 기간의 기본 금리) + **회원이 충족한 우대금리**. 최고 금리를 넘지 않습니다. |
| 가입 기간 | 회원이 고른 기간(상품 최장 기간 이내). 고르지 않으면 최고 금리가 적용되는 기간. |
| 세후 수령액 | 월 납입액 × n + 세전 이자 − 세금. 세전 이자 = 월 납입액 × n(n+1)/2 × 연 이율 / 12 (단리). 일반과세 15.4% 또는 비과세. |

월 납입액은 `월 수입 × 저축 목표(%) − 이미 납입 중인 적금`입니다. 
예: 최고 연 5.00%(우대 1.20%p 포함) 적금에 월 30만원을 24개월 넣으면, 우대 조건을 못 채운 회원은 연 3.80%로 7,441,110원, 
0.50%p를 채운 회원은 연 4.30%로 7,472,835원을 받습니다. 계산 규칙은 `MaturityCalculatorTest`, `ProductEstimateTest`로 검증합니다.

### 🍿 자산 차트 및 적금 챌린지
현재까지 KB국민은행과 거래한 내역을 토대로 구성한 자산 차트를 제공합니다. 매달 적금 납입 시 제공하는 별을 모두 모으면 리워드(포인트리)를 지급합니다. 
또한, 커뮤니티에서 사람들과 금융 정보를 공유하고 챌린지를 독려합니다.

### 🍿 한눈에 보는 청년 금융 정책
관심 있는 정책을 '즐겨찾기'할 수 있습니다. 
관심 정책의 마감 날짜가 임박 시 홈 화면에서 알림을 제공합니다.

### 🍿 AI 정책 상담 챗봇 (RAG)
등록된 청년정책·예적금 상품 데이터에서 근거를 찾아 답변하는 챗봇입니다.

일반적인 챗봇은 모르는 것을 그럴듯하게 지어냅니다. 금융 정책은 숫자 하나가 틀리면 
사용자가 실제로 손해를 보기 때문에, 환각상태를 방지하는 RAG기반 ChatBot을 개발했습니다.

<br/>

## 🐳 환각(Hallucination)을 막는 4단계

| | 방어 | 동작 |
|---|---|---|
| **L1** | 검색 게이트 | 유사도가 기준(0.85) 미만이면 **LLM을 호출조차 하지 않고** 답변을 거부합니다. 묻지 않으면 지어낼 기회가 없습니다. |
| **L2** | 컨텍스트 한정 | 검색된 자료 밖의 지식 사용을 프롬프트로 금지합니다. |
| **L3** | 인용 검증 | 답변의 `[n]` 인용번호를 파싱해 실제 DB 레코드와 연결합니다. 범위를 벗어난 번호는 버리고, 하나도 인용하지 않으면 화면에 경고를 띄웁니다. |
| **L4** | 숫자 비위임 | 금리·기간은 DB 원본값을 출처 카드에 그대로 실어, LLM이 쓴 숫자와 화면에서 대조되게 합니다. |

L1의 임계값 0.85는 짐작이 아니라 실측값입니다. `RagRetrievalEvalTest`가 평가셋으로 
top-k 4종 × 임계값 8종을 모두 비교합니다. 정답 자료의 최저점은 0.8727, 무관한 질문의 최고점은 
0.8242("오늘 서울 날씨 어때?")였습니다. 처음 쓰던 0.83은 날씨 질문과 0.006 차이밖에 나지 않아, 
두 값의 중간인 0.85로 올렸습니다. 수정 전후 기록은 [포트폴리오 정리](docs/PORTFOLIO.md#6-수정-전후-기록)에 있습니다.

**정책 공고문은 나눠서 색인합니다 (Chunk Size 200자 · Chunk Overlap 50자)**

제출 서류나 제외 대상 같은 세부 질문에 답하려고 정책 공고문 원문을 함께 색인합니다. 
한 건이 1,000자를 넘어 문장 단위로 나누고, 조각마다 정책명·지역·분류를 머리말로 붙였습니다. 
`RagChunkingEvalTest`로 크기 7종 × 겹침 3종을 비교한 결과입니다(공고문 질문 18문항).

| | 나누지 않음 | 200자 · 겹침 50자 |
|---|---|---|
| 질문과 근거 자료의 유사도 (평균 / 최저) | 0.8935 / 0.8665 | 0.9021 / 0.8829 |
| LLM에 보내는 자료 길이 | 평균 3,205자 | 평균 873자 |
| 근거 포함 (임계값 0.85 / 0.88) | 18/18 · 16/18 | 18/18 · 18/18 |

겹침을 0으로 두면 근거가 조각 경계에 걸려 18문항 중 1문항을 놓쳤고, 머리말을 빼면 3문항을 놓쳤습니다.

**실측 동작**

| 질문 | 결과 |
|---|---|
| "서울 사는 청년인데 월세 지원 받을 수 있는 정책 있어?" | 13.2초 · 근거 자료의 조건을 정확히 인용해 답변 |
| "KB청년희망적금 최고 금리가 몇 퍼센트야?" | 연 5.00% / 최소 10,000원 — DB 원본과 일치 |
| "부산시 월세 지원 정책 조건이 어떻게 돼?" | 자료에 없다고 답하고 **지어내지 않음** (L1 통과 → L2가 차단) |
| "오늘 서울 날씨 어때?" | **0.069초** — L1에서 차단, LLM 미호출 |

<br/>

## 🐳 가드레일과 관측성

입력·출력 검사를 Spring AI Advisor(`GuardrailAdvisor`) 한곳에 모았습니다.

| 단계 | 막는 것 | 결과 |
|---|---|---|
| 입력 | 프롬프트 주입 금지 문구, 주민번호·휴대폰·카드번호, 500자 초과 | 검색도 LLM 호출도 하지 않고 차단 |
| 출력 | 답변 속 개인정보, 시스템 프롬프트 문구 유출 | 안내 문구로 교체 |

전체 요청 수와 차단 횟수는 Spring Boot Actuator로 확인합니다.

```bash
curl localhost:8080/actuator/metrics/starroad.chat.requests      # outcome 별 요청 수
curl localhost:8080/actuator/metrics/starroad.guardrail.blocked  # stage·rule 별 차단 수
```

Postman 컬렉션(`docs/postman/`)으로 정상 요청과 차단할 요청을 보내 볼 수 있습니다. 
문자열 규칙이라 말 바꾸기나 다른 언어 같은 우회는 막지 못합니다. 이 한계는 테스트와 문서에 정리해 두었습니다.

<br/>

## 🐳 품질 평가

| 테스트 | 무엇을 보나 | LLM |
|---|---|---|
| `GuardrailRulesTest`, `ChatPipelineTest` | 차단 규칙, 차단 시 LLM 미호출, 지표 기록 | 불필요 |
| `RagRetrievalEvalTest` | 평가셋 51문항으로 hit@k·거부율 측정, 설정 조합 비교, 기준선 대비 회귀 확인 | 불필요 |
| `RagChunkingEvalTest` | Chunk Size 7종 × Chunk Overlap 3종으로 근거 유사도·근거 포함·주입 글자 수 비교 | 불필요 |
| `TextChunkerTest` | 문장을 자르지 않는지, 겹침과 소제목 처리가 맞는지 | 불필요 |
| `RagAnswerEvalTest` | 인용·핵심 숫자 규칙 검사 + LLM 평가자 + 사람 검토용 리포트 | 필요 |

```bash
mvnw test                                                                    # LLM 없이 도는 테스트 전부
mvnw test -Dtest=RagAnswerEvalTest -Deval.llm=true -Deval.profiles=dev,ollama # 답변 평가
```

설계 선택의 이유와 수정 전후 기록은 [docs/PORTFOLIO.md](docs/PORTFOLIO.md)에 정리했습니다.

<br/>

## 🐳 서버 구조 — 화면과 API 분리

컨트롤러는 화면을 내려 주는 것과 데이터를 내려 주는 것으로 나뉩니다.

| 구분 | URL | 반환 |
|---|---|---|
| `PageController` | `/starroad/**` | JSP 화면만. 모델에 데이터를 담지 않습니다. |
| 그 밖의 `@RestController` | `/api/starroad/**` | `ResponseEntity<DTO>` (JSON) |

화면은 뜬 뒤에 `resources/static/js/common.js` 의 `api.get/post/put/del` 로 API 를 호출해 받은 JSON 으로 그립니다.

```java
@GetMapping("/asset")
public ResponseEntity<MypageResponseDto> asset(@LoginMember MemberDto loginMember) {
    return ResponseEntity.ok(memberService.getAssets(loginMember.getNo()));
}
```

- **로그인 회원** — `@LoginMember` 가 세션에서 꺼내 줍니다. 로그인하지 않았으면 401 로 응답합니다.
- **실패 응답** — 실패의 상태 코드와 안내 문구는 `ErrorCode` enum 한곳에 모여 있습니다. 서비스가
  `throw new StarroadException(ErrorCode.BOARD_NOT_FOUND)` 로 던지면 `GlobalExceptionHandler` 가
  `{"code": "BOARD_NOT_FOUND", "message": "게시글을 찾을 수 없습니다."}` 로 바꿉니다. 잘못된 요청 값, 없는 주소,
  예상하지 못한 예외도 같은 핸들러가 같은 모양으로 응답하고, 스택 트레이스 같은 내부 정보는 로그에만 남깁니다.
  컨트롤러는 성공 응답만 만듭니다.
- **객체 생성** — 엔티티와 응답 DTO 는 생성자·빌더를 막고 이름 있는 정적 팩토리 메서드로만 만듭니다.
  `Board.write(작성자, ...)`, `Comment.write(게시글, 작성자, 내용)`, `Member.join(...)`, `Subscription.subscribe(...)`,
  `Heart.of(회원, 게시글)`, `BoardResponseDto.from(board)` — 호출하는 쪽만 봐도 무엇을 만드는지 알 수 있습니다.

API 명세는 `/swagger-ui.html` 에서 볼 수 있습니다.

<br/>

## 🐳 챗봇 실행 방법

Oracle 없이 인메모리 H2와 샘플 데이터로 바로 띄울 수 있습니다.
dev 프로필에는 체험용 계정(`starroad1` / `starroad1234`)이 함께 들어갑니다.

```bash
# 1) 로컬 LLM — API 키 불필요, 인터넷 없이 동작
#    https://ollama.com/download 설치만 하면 모델(기본값 exaone3.5:2.4b)은 기동 시 자동으로 받습니다
mvnw spring-boot:run -Dspring-boot.run.profiles=dev,ollama

# 2) Google Gemini — AI Studio 무료 키 (결제수단 등록 없음)
set GEMINI_API_KEY=...
mvnw spring-boot:run -Dspring-boot.run.profiles=dev,gemini

# 3) Anthropic Claude
set ANTHROPIC_API_KEY=...
mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

생성 모델은 `spring.ai.model.chat` 하나로 갈아끼웁니다. **임베딩은 어느 쪽이든 JVM 내장 
ONNX(multilingual-e5-small)를 쓰므로 외부 API를 타지 않습니다.** 따라서 `ollama` 프로필은 
검색·생성 양쪽 모두 로컬에서 돌아 외부 의존이 0입니다.

<br/>

## 🐳 시연 영상 
**회원 가입**

![part1](https://github.com/KB-StarRoad/.github/assets/98302932/d4a27a32-4db7-4a0b-85ae-f52c9ebf63a5)

<br/>

**청년 금융 정책**

![part2](https://github.com/KB-StarRoad/.github/assets/98302932/2b7783d4-c973-4e5e-8572-6dee6acf80dd)

<br/>

**KB 예적금 상품 추천 및 자산 차트 및 적금 챌린지**

![part3](https://github.com/KB-StarRoad/.github/assets/98302932/9a4bff6e-5369-4d81-9a57-c04751ea0496)

<br/>

**커뮤니티**

![part4](https://github.com/KB-StarRoad/.github/assets/98302932/127c1d04-cb73-4928-93c0-e3b78a15cb81)
