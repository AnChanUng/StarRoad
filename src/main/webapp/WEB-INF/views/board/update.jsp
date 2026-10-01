<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>STARROAD</title>
    <link rel="icon" href="${path}/resources/static/image/home/logo1.png" type="image/x-icon">
    <script src="//code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="/resources/static/js/common.js"></script>

    <link rel="stylesheet" type="text/css" href="/resources/static/css/board/update.css">
    <script type="text/javascript">
        $(function () {
            $("#navbar").load("/resources/common_jsp/navbar.jsp");
        });

    </script>
</head>
<body>


<form id="updateForm" method="post" enctype="multipart/form-data">
<div class="container">
    <div id="navbar"></div>
    <div class="title">
            <input name="title" class="titleStyle" type="text" id="title" placeholder="제목을 입력하세요" required/>
            <br>


        <span class="regdate"></span>
        <span class="likes">
                <img src="https://ifh.cc/g/aw0vjY.png" alt="Like Icon" style="vertical-align: middle; width: 20px; height: 20px;">
                <span id="boardLikes"></span>
        </span>
    </div>

    <div class="content">
        <textarea name="content" class="contentStyle" id="content" placeholder="내용을 입력하세요" required></textarea>
        <img id="boardImage" alt="" width="200" height="200" style="display: none;">

        <div class="image-input">
            <input type="file" name="newImage" id="newImage">

        </div>


        <div class="update-button">
            <button type="submit" class="buttonStyle">등록</button>
        </div>
        </div>
</div>


</form>

<script>
    const CURRENT_USER_ID = "${currentUser.id}";
    const BOARD_NO = getQueryParam('no');
    const BOARD_API = '/api/starroad/boards/' + encodeURIComponent(BOARD_NO);
    const DETAIL_PAGE = '/starroad/board/detail?no=' + encodeURIComponent(BOARD_NO);

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
        // 작성자가 아니면 수정 화면을 쓸 수 없다 — 상세 화면으로 돌려보낸다
        if (board.memberId !== CURRENT_USER_ID) {
            location.href = DETAIL_PAGE;
            return;
        }

        document.getElementById('title').value = board.title;
        document.getElementById('content').value = board.content;
        document.querySelector('.regdate').textContent = formatDateTime(board.regdate);
        document.getElementById('boardLikes').textContent = board.likes;
        if (board.imageBase64) {
            const image = document.getElementById('boardImage');
            image.src = 'data:image/jpeg;base64,' + board.imageBase64;
            image.style.display = '';
        }
    }

    document.getElementById('updateForm').addEventListener('submit', async function (event) {
        event.preventDefault();
        try {
            await api.put(BOARD_API, new FormData(this));   // title, content, newImage
            location.href = DETAIL_PAGE;
        } catch (e) {
            if (e.status !== 401) alert(e.message);   // 401 은 common.js 가 이미 알리고 로그인 화면으로 보낸다
        }
    });
</script>

</body>
</html>
