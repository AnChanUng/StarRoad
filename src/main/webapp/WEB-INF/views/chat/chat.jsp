<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>STARROAD</title>
    <link rel="icon" href="${path}/resources/static/image/home/logo1.png" type="image/x-icon">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.7.0/jquery.min.js"></script>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC" crossorigin="anonymous">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <script type="text/javascript">
        $(function () {
            $("#navbar").load("${pageContext.request.contextPath}/resources/common_jsp/navbar.jsp");
        })
    </script>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/resources/static/css/common.css">
    <link rel="stylesheet" type="text/css"
          href="${pageContext.request.contextPath}/resources/static/css/chat/chat.css">
</head>
<body>
<div id="chat-div-wrap">
    <div id="navbar"></div>
    <div class="title">AI 정책 상담</div>

    <main>
        <div id="chat_box">
            <div id="chat_log">
                <div class="msg bot">
                    <div class="bubble">
                        안녕하세요. 등록된 <b>청년정책</b>과 <b>KB 예적금 상품</b> 자료를 근거로 답변드립니다.<br>
                        자료에 없는 내용은 지어내지 않고 "답변드릴 수 없습니다"라고 말씀드려요.
                    </div>
                </div>
            </div>

            <form id="chat_form" autocomplete="off">
                <input type="text" id="question" placeholder="예) 서울 사는 청년이 받을 수 있는 월세 지원 정책 있어?"
                       maxlength="300">
                <button type="submit" id="send_btn"><i class="fa-solid fa-paper-plane"></i></button>
            </form>
        </div>
    </main>
</div>

<script type="text/javascript">
    const CTX = "${pageContext.request.contextPath}";

    function escapeHtml(s) {
        return String(s).replace(/[&<>"']/g, c => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
        }[c]));
    }

    // 답변 본문의 [n] 을 배지로 바꿔 눈에 띄게 한다
    function renderAnswer(text) {
        return escapeHtml(text)
            .replace(/\[(\d+)\]/g, '<sup class="cite">$1</sup>')
            .replace(/\n/g, '<br>');
    }

    function sourceCard(s) {
        const kind = s.type === 'policy' ? '청년정책' : '예적금 상품';
        const link = s.link
            ? '<a href="' + escapeHtml(s.link) + '" target="_blank" rel="noopener">원문 보기 <i class="fa-solid fa-arrow-up-right-from-square"></i></a>'
            : '';
        // score 는 검색 유사도. 임계값 튜닝 시 참고용으로 노출한다.
        const score = (s.score === null || s.score === undefined) ? '' :
            '<span class="score">유사도 ' + Number(s.score).toFixed(3) + '</span>';

        return '<div class="source ' + (s.cited ? 'cited' : 'uncited') + '">'
            + '<div class="source_head">'
            + '<span class="badge_no">' + s.citationNo + '</span>'
            + '<span class="kind">' + kind + '</span>'
            + '<span class="name">' + escapeHtml(s.name) + '</span>'
            + '</div>'
            // factLine 은 DB 원본값이다. LLM 이 만든 숫자가 아니므로 답변과 대조할 수 있다.
            + '<div class="fact">' + escapeHtml(s.factLine || '') + '</div>'
            + '<div class="source_foot">' + link + score + '</div>'
            + '</div>';
    }

    function appendUser(text) {
        $("#chat_log").append(
            '<div class="msg user"><div class="bubble">' + escapeHtml(text) + '</div></div>');
    }

    function appendBot(data) {
        let html = '<div class="msg bot"><div class="bubble">';

        if (data.blocked) {
            html += '<div class="nogrounds blocked"><i class="fa-solid fa-shield-halved"></i> 안전 정책에 따라 처리하지 않은 요청입니다</div>';
        } else if (!data.grounded) {
            html += '<div class="nogrounds"><i class="fa-solid fa-circle-info"></i> 근거 자료를 찾지 못했습니다</div>';
        }
        html += renderAnswer(data.answer);

        if (data.uncited) {
            html += '<div class="warn"><i class="fa-solid fa-triangle-exclamation"></i> '
                + '이 답변은 출처 번호를 달지 않았습니다. 아래 자료와 직접 대조해 주세요.</div>';
        }

        if (data.sources && data.sources.length > 0) {
            html += '<div class="sources"><div class="sources_title">근거 자료</div>'
                + data.sources.map(sourceCard).join('') + '</div>';
        }

        html += '</div></div>';
        $("#chat_log").append(html);
    }

    function scrollBottom() {
        const log = document.getElementById("chat_log");
        log.scrollTop = log.scrollHeight;
    }

    $("#chat_form").on("submit", function (e) {
        e.preventDefault();

        const q = $("#question").val().trim();
        if (!q) return;

        appendUser(q);
        $("#question").val("");
        $("#send_btn").prop("disabled", true);
        $("#chat_log").append('<div class="msg bot pending"><div class="bubble">자료를 찾는 중…</div></div>');
        scrollBottom();

        $.ajax({
            url: CTX + "/starroad/chat/ask",
            type: "POST",
            contentType: "application/json; charset=UTF-8",
            data: JSON.stringify({question: q})
        }).done(function (data) {
            $(".pending").remove();
            appendBot(data);
        }).fail(function () {
            $(".pending").remove();
            appendBot({
                answer: "요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
                grounded: false, sources: [], uncited: false
            });
        }).always(function () {
            $("#send_btn").prop("disabled", false);
            scrollBottom();
        });
    });
</script>
</body>
</html>
