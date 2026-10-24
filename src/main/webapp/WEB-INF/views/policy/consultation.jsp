<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="policy-csrf" content="${policyAiCsrf}">
    <title>공식 정책 AI 상담 | STARROAD</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/static/css/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/static/css/policy/consultation.css">
</head>
<body>
<main id="policy-ai" data-api="${pageContext.request.contextPath}/starroad/policy/ai">
    <a href="${pageContext.request.contextPath}/starroad/policy">← 청년 금융 정책</a>
    <h1>공식 정책 AI 상담</h1>
    <p>공식 자료를 바탕으로 지원 내용과 신청 준비 사항을 확인해 보세요.</p>
    <section class="selection" aria-label="정책 선택">
        <label for="policy-filter">정책명·지역 검색</label>
        <input id="policy-filter" type="search" placeholder="예: 서울, 주거" disabled>
        <label for="policy-select">상담할 정책</label>
        <select id="policy-select" disabled><option value="">정책을 불러오는 중입니다.</option></select>
        <a id="official-link" target="_blank" rel="noopener noreferrer" hidden>공식 공고 확인 ↗</a>
        <button id="reload" type="button">목록 새로고침</button>
    </section>
    <p class="notice">신청 가능 여부와 접수 기간은 최신 공식 공고를 확인해 주세요. 주민등록번호·전화번호는 입력하지 마세요.</p>
    <p id="status" role="status" aria-live="polite"></p>
    <section id="messages" aria-label="정책별 상담 기록" aria-live="polite"></section>
    <form id="question-form">
        <label for="question">궁금한 내용</label>
        <textarea id="question" rows="3" maxlength="1000" placeholder="지원 대상과 신청에 필요한 서류를 알려주세요."></textarea>
        <div class="actions">
            <button id="ask" type="submit" disabled>질문하기</button>
            <button id="checklist" type="button" disabled>신청 체크리스트</button>
            <button id="resume" type="button" hidden>진행 상황 다시 확인</button>
            <button id="clear" type="button" disabled>현재 대화 지우기</button>
        </div>
    </form>
    <p class="notice">대화와 진행 조회 정보는 화면을 닫거나 새로고침하면 사라집니다. 접수된 작업은 계속 처리되며 서버 재시작 시 소실됩니다. AI 답변은 최종 자격 판정을 대신하지 않습니다.</p>
</main>
<script src="${pageContext.request.contextPath}/resources/static/js/policy-consultation.js" defer></script>
</body>
</html>
