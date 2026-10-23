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
- 없는 정책 404, 생성 중 429, 서비스 장애 503, 연결/읽기 시간초과 504를 안내합니다. 실패한 질문은 입력란에 남습니다.
- 연결 제한은 3초, 읽기 제한은 기본 120초입니다. `POLICY_AI_READ_TIMEOUT_SECONDS`로 변경할 수 있습니다.
  브라우저 제한은 130초이므로 서버 제한을 늘리면 프런트 제한도 함께 조정해야 합니다.
- 중복 생성은 Python 단일 프로세스 잠금으로 막습니다. 여러 worker/replica는 공유 색인과 잠금 구조를 먼저 변경해야 합니다.
- 답변은 텍스트로 출력하며 출처는 http/https 링크만 허용합니다.

## 검증

```powershell
.\mvnw.cmd '-Dtest=PolicyAiIntegrationTest' test
# 별도 Node 환경에 Playwright 및 Edge가 있을 때
node docs/policy-ai-ui-test.cjs
```

Java 연동 테스트 6개: 문자열 ID 유지, Bearer 인증, 체크리스트 계약, 검증 부족 상태 유지,
CSRF/역할 검증, 미설정 차단, 404/인증 오류/시간초과 변환을 검증합니다.
브라우저 테스트는 가짜 API 응답으로 정책별 기록 분리, 체크리스트, 후속 대화, HTML/위험 링크 차단,
실패 시 입력 보존, 대화 삭제, 모바일 가로 넘침을 확인합니다.

이번 작업 환경에서는 위 테스트가 통과했습니다. 기존 전체 테스트는 변경 전 기준 42개 중
실패 3개·오류 4개·건너뜀 1개가 있었고 Windows 루프백 소켓 초기화 및 애플리케이션 컨텍스트 오류가 포함됩니다.
실제 모델을 통한 두 서버의 전체 상담 흐름은 검증하지 않았습니다. 서비스 설정 후 별도 확인이 필요합니다.
사용자 인증/사용량 제한을 추가하지 않은 개발용 연동이므로 공개 서비스 운영 구성은 별도 작업입니다.
