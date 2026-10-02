<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>STARROAD</title>
    <link rel="icon" href="${path}/resources/static/image/home/logo1.png" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="/resources/static/css/board/update.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.3/css/all.min.css">
    <script src="//code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="/resources/static/js/common.js"></script>
    <script type="text/javascript">
        $(function () {
            $("#navbar").load("/resources/common_jsp/navbar.jsp");
        });
    </script>
</head>
<body>
<div class="container">
    <div id="navbar"></div>

    <div class="title">
        <span class="title-text"></span> <br>
        <div class="something">

            <div class="authorStyle">
                <i class="fas fa-user-circle"></i>
                <span class="memberId author-id"></span>
            </div>
            <hr class="separator">
            <span class="regdate">
                <i class="fas fa-clock"></i>
                <span id="boardRegdate"></span>
            </span>
        </div>
    </div>

    <div class="content">

        <img id="boardImage" alt="" width="200" height="200"
             style="margin-bottom: 30px; display: none;"/>
        <span id="boardContent"></span>
        <div class="like-section">
        </div>
    </div>
    <div class="comment1">댓글 수정</div>
    <div class="comment">
        <div class="comment-input">
            <form id="commentForm">
                <div class="comment-section">
<%--                    <label>댓글 내용:</label><br>--%>
                    <textarea id="commentText" name="content" rows="4" cols="50" autofocus></textarea><br>
                    <button id="submitComment" type="submit">수정 완료</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    const CURRENT_USER_ID = "${currentUser.id}";
    const COMMENT_NO = getQueryParam('no');
    const BOARD_NO = getQueryParam('boardNo');
    const COMMENT_API = '/api/starroad/comments/' + encodeURIComponent(COMMENT_NO);
    const DETAIL_PAGE = '/starroad/board/detail?no=' + encodeURIComponent(BOARD_NO);

    loadPage();

    async function loadPage() {
        let board, comment;
        try {
            [board, comment] = await Promise.all([
                api.get('/api/starroad/boards/' + encodeURIComponent(BOARD_NO)),
                api.get(COMMENT_API)
            ]);
        } catch (e) {
            alert(e.message);
            location.href = DETAIL_PAGE;
            return;
        }
        if (comment.memberId !== CURRENT_USER_ID) {
            alert("다른 사용자의 댓글을 수정할 수 없습니다.");
            location.href = DETAIL_PAGE;
            return;
        }

        document.querySelector('.title-text').textContent = board.title;
        document.querySelector('.author-id').textContent = board.memberId;
        document.getElementById('boardRegdate').textContent = formatDateTime(board.regdate);
        document.getElementById('boardContent').textContent = board.content;
        if (board.imageBase64) {
            const image = document.getElementById('boardImage');
            image.src = 'data:image/jpeg;base64,' + board.imageBase64;
            image.style.display = '';
        }
        document.getElementById('commentText').value = comment.content;
    }

    document.getElementById('commentForm').addEventListener('submit', async function (event) {
        event.preventDefault();
        try {
            await api.put(COMMENT_API, {content: document.getElementById('commentText').value});
            location.href = DETAIL_PAGE;
        } catch (e) {
            if (e.status !== 401) alert(e.message);   // 401 은 common.js 가 이미 알리고 로그인 화면으로 보낸다
        }
    });
</script>
</body>
</html>
