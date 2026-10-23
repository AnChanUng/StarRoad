# 정책 AI 통합 검증 — 2026-10-24

최신 `origin/dev`의 REST API 전환, 정책 화면 통합, 공고문 청킹, 만기 계산 변경을 유지한 상태에서 검증했습니다.

| 검증 | 결과 |
| --- | --- |
| JDK 21 `mvnw clean verify` | 85개 중 84개 성공, 실패/오류 0, 실제 LLM 평가 1개 기본 제외, WAR 빌드 성공 |
| Python 3.12 전체 unittest | 46개 성공, 실패/오류 0 |
| Playwright UI 계약 테스트 | 정책별 기록, 체크리스트, 후속 질문, 위험 HTML/URL, 실패 복구, 삭제, 모바일 가로 넘침 통과 |
| 실제 HTTP 연결 | Edge → 실행 중인 Spring WAR → Bearer 인증 FastAPI → 실제 정책 로더 통과 |
| 실제 HTTP 예외 처리 | CSRF 누락 403, 없는 정책 404, AI 서비스 실패 503, Python 무인증 401 통과 |
| 비밀값 관리 | 두 로컬 `.env`가 Git 제외 대상임을 확인, 테스트/서버 로그에 공유 토큰 없음 |

모델 API 키가 없어 사용자 동의하에 **실제 모델 호출은 제외**했습니다.
HTTP 통합 테스트는 PolicyService의 실제 정책 목록을 읽되 `ask`만 결정적 응답으로 대체합니다.
따라서 실제 생성 답변의 품질, 모델 서비스의 인증·과금·지연까지 검증한 결과는 아닙니다.

## 재현

StarRoad 루트에서:

```powershell
.\mvnw.cmd clean verify
node docs/policy-ai-ui-test.cjs
```

이번 Windows 환경에서 JDK 소켓의 임시 디렉터리가 `CTS25-~1` 형식일 때
`UnixDomainSockets.connect: Invalid argument`가 발생했습니다.
테스트를 생략하지 않고 `jdk.net.unixdomain.tmpdir`를 존재하는 짧은 이름 없는 절대 경로로 지정해 해결했습니다.
예를 들어 별도 임시 폴더를 만든 후 해당 경로를 JVM 옵션으로 설정합니다.

```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\java-sockets"
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=$env:USERPROFILE\java-sockets"
.\mvnw.cmd clean verify
```

Python 저장소 루트에서:

```powershell
python -m unittest discover -s youth_policy/tests -v
```

실제 HTTP 통합 테스트는 두 저장소와 API 의존성을 준비하고 두 서버에 같은
`POLICY_AI_TOKEN`을 설정한 뒤 다음 순서로 실행합니다. 모델 API 키는 필요하지 않습니다.

1. Python API 의존성이 설치된 환경에서 `python <StarRoad>/docs/policy-ai-fixture.py` 실행.
   기본적으로 StarRoad와 같은 상위 폴더의 RAG_Youth-Policy를 사용합니다.
   다른 위치라면 `POLICY_AI_REPO`에 Python 저장소 루트를 설정합니다.
2. StarRoad에서 `POLICY_AI_BASE_URL=http://127.0.0.1:18001`을 설정하고 빌드한 WAR를
   `java -jar target/starroad-0.0.1-SNAPSHOT.war --spring.profiles.active=dev --server.port=18080`으로 실행.
   기존 임베딩 모델 초기화/색인이 끝날 때까지 기다립니다.
3. Node와 Playwright 및 Edge가 있는 환경에서 `node docs/policy-ai-e2e.cjs` 실행.
4. 검증 후 두 테스트 서버를 종료합니다. fixture 서버는 테스트 전용이며 운영에 사용하지 않습니다.
