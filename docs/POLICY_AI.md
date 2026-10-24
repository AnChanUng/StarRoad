# StarRoad × 청년 정책 RAG

청년 금융 정책 목록/검색 결과에서 **공식 정책 AI 상담 · 신청 체크리스트**를 누릅니다.
정책명·지역으로 공식 정책을 선택하고 질문하거나 신청 체크리스트를 요청할 수 있습니다.
답변에는 실제 인용 출처, 처리 시간, 근거 부족 상태를 표시합니다.

## 연결 범위

브라우저 → Spring `/starroad/policy/ai` → Python FastAPI → 기존 PolicyService/LangGraph/Chroma.
기존 금융상품 챗봇, 정책 목록, 찜 API는 그대로 사용합니다.
공식 정책 목록의 문자열 `id`를 그대로 전달하며 StarRoad DB의 숫자 `Policy.no`로 변환하지 않습니다.
따라서 기존 찜 정책과 AI 정책의 자동 연결/동기화는 이번 구현에 포함되지 않습니다.
후속 연결은 검증한 외부 정책 ID를 별도 저장해야 하며 이름 일치만으로 연결하면 안 됩니다.

## 실행

두 저장소의 연동 변경사항이 모두 필요합니다. Python 저장소 루트에서:

```powershell
py -3.12 -m venv .venv
.venv/Scripts/python.exe -m pip install -r youth_policy/requirements-api.txt
# youth_policy/.env에 기존 GMS_KEY 등 모델 설정을 준비합니다.
# 두 서버에 동일한 임의의 긴 비밀값을 설정합니다. 저장소에 커밋하지 않습니다.
$env:POLICY_AI_TOKEN = '<공유 비밀값>'
.venv/Scripts/python.exe -m youth_policy index
.venv/Scripts/python.exe -m uvicorn youth_policy.api:app --host 127.0.0.1 --port 8001 --workers 1
```

다른 터미널의 StarRoad 루트에서 JDK 21을 사용합니다:

```powershell
$env:POLICY_AI_TOKEN = '<동일한 공유 비밀값>'
$env:POLICY_AI_BASE_URL = 'http://127.0.0.1:8001'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
```

StarRoad 기본 설정에서 요구하는 기존 챗봇/임베딩 모델 설정도 준비되어 있어야 합니다.
dev 프로필은 H2 샘플 DB를 사용하고 종료 시 삭제합니다. 운영 DB에 dev 프로필을 적용하지 않습니다.
기동 후 `/starroad/policy/ai`를 엽니다. Python의 `/health`는 프로세스 생존 확인용이며 모델/색인 준비 완료를 보장하지 않습니다.
메타 색인은 `index`로 준비하고 상담 중 필요한 정책 문서를 선별·색인합니다.
Python 데이터 폴더의 색인을 다른 Streamlit/CLI 프로세스에서 동시에 수정하지 마세요.

## 계약과 오류 처리

- Python 데이터/생성 API는 `Authorization: Bearer <POLICY_AI_TOKEN>`을 요구합니다. 미설정이면 503으로 닫힙니다.
- Spring은 비밀값을 브라우저에 전달하지 않습니다. 브라우저 POST는 별도의 세션 CSRF 값을 검증합니다.
- 질문은 1,000자, 과거 대화는 최대 8개/각 3,000자, 역할은 user/assistant로 제한합니다.
- 대화는 페이지 메모리에 정책별로 보관하며 새로고침하면 사라집니다. 최근 40개 메시지 중 8개만 전송합니다.
- `text`, `sources`, `uncited`, `status`, `elapsed_ms`만 사용하고 내부 trace/문서 본문은 API에서 제외합니다.
- 없는 정책/작업 404, 접수 한도 429, 요청 번호 충돌 409, 서비스 장애 503, 연결/읽기 시간초과 504를 안내합니다. 접수된 작업의 처리 실패는 failed 상태로 전달합니다. 실패한 질문은 입력란에 남습니다.
- 연결 제한은 3초, 읽기 제한은 기본 120초입니다. `POLICY_AI_READ_TIMEOUT_SECONDS`로 변경할 수 있습니다.
  기존 동기 API에 해당하는 제한입니다. 새 화면은 10초 제한의 짧은 접수/상태 조회 요청을 사용하며 작업 자체는 계속 진행됩니다.
- 상담 작업은 Python 단일 프로세스의 대기열에서 실행합니다. 같은 정책은 순서대로, 다른 정책은 최대 2건 병렬 처리합니다. 자세한 한도와 재시도 보장은 [동시성 설계](POLICY_CONCURRENCY.md)를 참고하세요.
- 답변은 텍스트로 출력하며 출처는 http/https 링크만 허용합니다.

## 검증

```powershell
.\mvnw.cmd '-Dtest=PolicyAiIntegrationTest' test
# 별도 Node 환경에 Playwright 및 Edge가 있을 때
node docs/policy-ai-ui-test.cjs
```

Java 연동 테스트 10개: 문자열 ID 유지, Bearer 인증, 체크리스트 계약, 검증 부족 상태 유지,
CSRF/역할 검증, 미설정 차단, 404/인증 오류/시간초과 변환을 검증합니다.
브라우저 테스트는 가짜 API 응답으로 정책별 기록 분리, 체크리스트, 후속 대화, HTML/위험 링크 차단,
실패 시 입력 보존, 대화 삭제, 모바일 가로 넘침을 확인합니다.

전체 검증 결과는 [TESTING_POLICY_AI](TESTING_POLICY_AI.md)를 참고하세요.
실제 모델 호출은 별도 API 키가 필요합니다. 사용자 인증/사용량 제한을 추가하지 않은 개발용 연동이므로 공개 서비스 운영 구성은 별도 작업입니다.

## 로컬 비밀값 관리

StarRoad는 루트 `.env`를 선택적으로 읽습니다. `.env.example`을 복사한 뒤 값을 설정하거나 환경변수로 주입합니다.
Python은 기존 `youth_policy/.env`를 읽습니다. 두 서버의 `POLICY_AI_TOKEN`은 같은 값이어야 합니다.
`.env`와 `.env.*`는 Git에서 제외하고 예제 파일에는 비밀값을 넣지 않습니다.
이 토큰은 서버 간 인증용이며 모델 API 키와 다릅니다. Codex 로그인 정보를 모델 API 키로 복사하지 않습니다.
