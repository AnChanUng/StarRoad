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

## 🐳 주요 기능
### 🍿 KB 예적금 상품 추천
국민 은행 내의 금융 상품 중 사용자의 우대 조건을 포함한 최대 이율 및 월 수입과 저금 목표치를 고려하여 상품 만기 시 받을 수 있는 최대 금액을 계산하여 제공합니다. 

### 🍿 자산 차트 및 적금 챌린지
현재까지 KB국민은행과 거래한 내역을 토대로 구성한 자산 차트를 제공합니다. 매달 적금 납입 시 제공하는 별을 모두 모으면 리워드(포인트리)를 지급합니다. 
또한, 커뮤니티에서 사람들과 금융 정보를 공유하고 챌린지를 독려합니다.

### 🍿 한눈에 보는 청년 금융 정책
관심 있는 정책을 '즐겨찾기'할 수 있습니다. 
관심 정책의 마감 날짜가 임박 시 홈 화면에서 알림을 제공합니다.

### 🍿 AI 정책 상담 챗봇 (RAG)
Spring AI가 질문과 DB의 정책·상품 설명을 로컬 KLUE-BERT 계열 인코더로 벡터화하고 ChromaDB에서 근거를 검색합니다. 답변 생성은 별도의 LLM이 담당합니다. 근거가 없으면 생성 호출을 건너뛰며, 답변에는 인용과 색인 당시의 수치가 담긴 출처 카드를 붙입니다.

개발 환경에서는 먼저 `docker compose up -d --build`로 ChromaDB와 인코더를 시작합니다.
그다음 Ollama가 실행 중인 환경에서 다음을 실행합니다.

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev,ollama"
```

[모델·설계·실행 방법과 실제 평가 결과](docs/PORTFOLIO.md) · [검색 평가셋](src/test/resources/eval/rag-eval-set.json)

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
