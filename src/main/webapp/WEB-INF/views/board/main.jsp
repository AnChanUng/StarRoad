<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" type="text/css" href="${path}/resources/static/css/common.css">
    <link rel="stylesheet" type="text/css" href="${path}/resources/static/css/nav.css">
    <link rel="stylesheet" type="text/css" href="${path}/resources/static/css/board/board1.css">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.1/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-4bw+/aepP/YC94hEpVNVgiZdgIC5+VKNBQNGCHeKRQN+PtmoHDEXuppvnDJzQIu9" crossorigin="anonymous">
    <title>STARROAD</title>
    <link rel="icon" href="${path}/resources/static/image/home/logo1.png" type="image/x-icon">
    <script src="//code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="/resources/static/js/common.js"></script>

    <script type="text/javascript">
        $(function () {
            $("#navbar").load("${path}/resources/common_jsp/navbar.jsp");
            loadBoards();
        });

        // 게시판 메인: 인기글·자유게시판·인증방 글 목록을 받아 그린다
        async function loadBoards() {
            try {
                const data = await api.get('/api/starroad/boards/main');
                renderBoardList('popularBoardList', data.popularBoard);
                renderBoardList('freeBoardList', data.freeBoard);
                renderBoardList('authBoardList', data.authBoard);
            } catch (e) {
                console.error('게시글 목록을 불러오지 못했습니다:', e.message);
            }
        }

        function renderBoardList(containerId, boards) {
            let html = '';
            (boards || []).slice(0, 6).forEach(function (board) {
                html += '<p>'
                    + '<div class="detailType">' + escapeHtml(board.detailType) + '</div>'
                    + '<a href="/starroad/board/detail?no=' + encodeURIComponent(board.no) + '">' + escapeHtml(board.title) + ' </a>'
                    + '<div class="right-align">'
                    + '<img src="/resources/static/image/board/likes.png" alt="좋아요" class="like-icon"> '
                    + escapeHtml(board.likes)
                    + '</div>'
                    + '</p>';
            });
            document.getElementById(containerId).innerHTML = html;
        }
    </script>


</head>

<body>
<div id="navbar"></div>
<!-- 본문 내용 -->
<div id="carouselExampleAutoplaying" class="carousel slide" data-bs-ride="carousel">
    <div class="carousel-inner">
        <div class="carousel-item active" data-bs-interval="3000">
            <img src="${path}/resources/static/image/board/main/ad_1.png" class="d-block w-100" alt="...">
        </div>
        <div class="carousel-item" data-bs-interval="3000">
            <img src="${path}/resources/static/image/board/main/ad_2.png" class="d-block w-100" alt="...">
        </div>
        <div class="carousel-item" data-bs-interval="3500">
            <img src="${path}/resources/static/image/board/main/ad_3.png" class="d-block w-100" alt="...">
            <div class="carousel-caption d-none d-md-block">
                <div class="banner_title">
                    <h3> 신상품 출시! </h3>
                    <a class="bounce" href="https://otalk.kbstar.com/quics?page=C019392&cc=b032271:b032271"><h1>「KB청년도약계좌」</h1></a>
                </div>
            </div>
        </div>
    </div>
    <button class="carousel-control-prev" type="button" data-bs-target="#carouselExampleAutoplaying"
            data-bs-slide="prev">
        <span class="carousel-control-prev-icon" aria-hidden="true"></span>
        <span class="visually-hidden">Previous</span>
    </button>
    <button class="carousel-control-next" type="button" data-bs-target="#carouselExampleAutoplaying"
            data-bs-slide="next">
        <span class="carousel-control-next-icon" aria-hidden="true"></span>
        <span class="visually-hidden">Next</span>
    </button>
</div>

<div class="board-box">
    <div class="board">
        <div class="board-header">
            <img src="${path}/resources/static/image/board/popular1.png" alt="인기글">
            <h2>인기게시판</h2>
            <a href="/starroad/board/popular" class="board-detail">더보기 ></a>
        </div>
        <div class="board-list" id="popularBoardList"></div>
    </div>

    <div class="board">
        <div class="board-header">
            <img src="${path}/resources/static/image/board/talk.png" alt="자유게시글">
            <h2>자유게시판</h2>
            <a href="/starroad/board/free?type=F" class="board-detail">더보기 ></a>
        </div>
        <div class="board-list" id="freeBoardList"></div>
    </div>

    <div class="board">
        <div class="board-header">
            <img src="${path}/resources/static/image/board/auth.png" alt="인증게시글">
            <h2>인증게시판</h2>
            <a href="/starroad/board/free?type=C" class="board-detail">더보기 ></a>
        </div>
        <div class="board-list" id="authBoardList"></div>
    </div>
</div>

</body>
</html>