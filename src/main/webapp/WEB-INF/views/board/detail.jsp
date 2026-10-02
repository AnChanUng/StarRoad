<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>

<head>

    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.3/css/all.min.css">
    <meta charset="UTF-8">
    <title>STARROAD</title>
    <link rel="icon" href="${path}/resources/static/image/home/logo1.png" type="image/x-icon">
    <script src="//code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="/resources/static/js/common.js"></script>

    <link rel="stylesheet" type="text/css" href="/resources/static/css/board/detail.css">
    <script type="text/javascript">
            $(function() {
                $("#navbar").load("/resources/common_jsp/navbar.jsp");
            });

    </script>
</head>
<body>
    <div class="container">
        <div id="navbar"></div>

        <div class="title">
            <span class="title-text"></span> <br>
            <div class ="something">

            <div class = "authorStyle">
            <i class="fas fa-user-circle"></i>
            <span class="memberId author-id"></span>
            </div>
            <hr class="separator">
            <span class="regdate">
                <i class="fas fa-clock"></i>
                <span id="boardRegdate"></span>
            </span>
            </div>

            <%-- 작성자 본인일 때만 보인다 --%>
            <div class="title-buttons more-actions" id="boardActions" style="visibility: hidden;">
                <span class="icon">°°°</span>
                <div class="actions">
                    <button id="editBtn">수정</button>
                    <button id="deleteBtn">삭제</button>
                </div>
            </div>
        </div>

        <div class="content">

            <img id="boardImage" alt="" width="400" height="400" style="margin-bottom: 30px; display: none;"/>
            <span id="boardContent"></span>
            <div class="like-section">
            <form id="likeForm">
                <img src="https://ifh.cc/g/aw0vjY.png" id="like-icon" alt="Like Icon" style="vertical-align: middle; width: 30px; height: 30px;" >
            </form>
                <span class="likes-count"></span>
            </div>
        </div>

        <div class="comment1"> 댓글<span id="commentNum"></span></div>

        <div class="comment">
            <div class="comment-input">
                 <form id="commentForm">
                     <textarea id="commentText" name="content" placeholder="댓글을 입력하세요" rows="4" cols="50"></textarea>
                     <button id="submitComment" type="submit">등록</button>
                 </form>
            </div>
        </div>

        <div class="comments-list"></div>
    </div>

   <script>
        const CURRENT_USER_ID = "${currentUser.id}";   // 비로그인이면 빈 문자열
        const BOARD_NO = getQueryParam('no');
        const BOARD_API = '/api/starroad/boards/' + encodeURIComponent(BOARD_NO);

        loadBoard();

        async function loadBoard() {
            let board;
            try {
                board = await api.get(BOARD_API);
            } catch (e) {
                alert(e.message);
                location.href = '/starroad/board/main';
                return;
            }

            document.querySelector('.title-text').textContent = board.title;
            document.querySelector('.author-id').textContent = board.memberId;
            document.getElementById('boardRegdate').textContent = formatDateTime(board.regdate);
            document.getElementById('boardContent').textContent = board.content;
            document.querySelector('.likes-count').textContent = ' ' + board.likes + ' ';
            document.getElementById('commentNum').textContent = board.commentNum;

            if (board.imageBase64) {
                const image = document.getElementById('boardImage');
                image.src = 'data:image/jpeg;base64,' + board.imageBase64;
                image.style.display = '';
            }
            if (CURRENT_USER_ID !== '' && CURRENT_USER_ID === board.memberId) {
                document.getElementById('boardActions').style.visibility = '';
            }
            renderComments(board.comments || []);
        }

        function renderComments(comments) {
            let html = '';
            comments.forEach(function (comment) {
                // 더보기(수정·삭제) 메뉴는 댓글 작성자 본인에게만 그린다
                const mine = CURRENT_USER_ID !== '' && CURRENT_USER_ID === comment.memberId;
                const actions = mine
                    ? '<span class="icon">°°°</span>'
                        + '<div class="actions">'
                        + '<button class="comment-edit" data-id="' + escapeHtml(comment.no) + '">수정</button>'
                        + '<button class="comment-delete" data-id="' + escapeHtml(comment.no) + '">삭제</button>'
                        + '</div>'
                    : '';
                html += '<div class="comment-item">'
                    + '<i class="fas fa-user-circle"></i> '
                    + '<strong class="currentUser">' + escapeHtml(comment.memberId) + '</strong> <br>'
                    + '<div class="comment-content">'
                    + '<span class="comment-content">' + escapeHtml(comment.content) + '</span> <br>'
                    + '</div>'
                    + '<div class="comment-date">'
                    + '<span class="comment-date">'
                    + '<i class="fas fa-clock"></i> '
                    + formatDateTime(comment.regdate)
                    + '</span>'
                    + '</div>'
                    + '<div class="more-actions">' + actions + '</div>'
                    + '</div>';
            });
            document.querySelector('.comments-list').innerHTML = html;
        }

        <%-- 게시글 삭제 / 수정 --%>
        document.getElementById("deleteBtn").addEventListener("click", async function() {
            if (confirm("정말로 삭제하시겠습니까?")) {
                try {
                    await api.del(BOARD_API);
                    location.href = '/starroad/board/main';
                } catch (e) {
                    if (e.status !== 401) alert(e.message);
                }
            }
        });
        document.getElementById("editBtn").addEventListener("click", function() {
            location.href = '/starroad/board/update?no=' + encodeURIComponent(BOARD_NO);
        });

        <%-- 댓글 삭제 / 수정 (댓글은 나중에 그려지므로 목록에 위임한다) --%>
        document.querySelector('.comments-list').addEventListener('click', async function (event) {
            const button = event.target.closest('.comment-delete, .comment-edit');
            if (!button) return;
            const commentNo = button.getAttribute('data-id');

            if (button.classList.contains('comment-edit')) {
                location.href = '/starroad/comment/update?no=' + encodeURIComponent(commentNo)
                    + '&boardNo=' + encodeURIComponent(BOARD_NO);
                return;
            }
            if (confirm("정말로 댓글을 삭제하시겠습니까?")) {
                try {
                    await api.del('/api/starroad/comments/' + encodeURIComponent(commentNo));
                    location.reload();
                } catch (e) {
                    if (e.status !== 401) alert(e.message);
                }
            }
        });

        <%-- 댓글 등록 --%>
        document.getElementById("commentForm").addEventListener("submit", async function(event) {
            event.preventDefault();
            var commentContent = document.getElementById("commentText").value.trim();

            if (!commentContent) {
                alert("댓글 내용을 입력해주세요.");
                return;
            }
            try {
                await api.post('/api/starroad/comments', {boardNo: Number(BOARD_NO), content: commentContent});
                location.reload();
            } catch (e) {
                if (e.status !== 401) alert(e.message);
            }
        });

        <%-- 좋아요 기능 --%>
        document.getElementById("like-icon").addEventListener("click", async function() {
            try {
                const result = await api.post(BOARD_API + '/likes');
                document.querySelector('.likes-count').textContent = ' ' + result.likes + ' ';
            } catch (e) {
                if (e.status !== 401) alert(e.message);   // 401 은 common.js 가 이미 알리고 로그인 화면으로 보낸다
            }
        });

        <%-- 게시글·댓글 더보기 (댓글 메뉴는 나중에 그려지므로 document 에 위임한다) --%>
        $(document).on('click', function(e) {
            const $target = $(e.target);
            if ($target.closest('.more-actions .actions').length) {
                return;                                     // 메뉴 안쪽을 눌렀을 때는 닫지 않는다
            }
            if ($target.is('.more-actions .icon')) {
                $target.siblings('.actions').toggle();      // 누른 메뉴만 열고 닫는다
                return;
            }
            $('.more-actions .actions').hide();             // 다른 곳을 누르면 모두 닫는다
        });

   </script>
</body>
</html>
