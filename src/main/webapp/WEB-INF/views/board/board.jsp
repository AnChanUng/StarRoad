<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html lang="ko">
<head>
    <title>STARROAD</title>
    <link rel="icon" href="${path}/resources/static/image/home/logo1.png" type="image/x-icon">
    <!-- 메타 정보, 스타일 등 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC" crossorigin="anonymous">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.3/css/all.min.css">
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/resources/static/css/common.css">
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/resources/static/css/board/board2.css">
    <script src="//code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="/resources/static/js/common.js"></script>
    <script type="text/javascript">
        // 이 화면은 /starroad/board/free?type=F|C 와 /starroad/board/popular 두 주소에서 함께 쓴다
        const IS_POPULAR = /\/popular\/?$/.test(location.pathname);
        const BOARD_TYPE = IS_POPULAR ? '' : (getQueryParam('type') || 'F');

        $(function() {
            $("#navbar").load("${path}/resources/common_jsp/navbar.jsp");
            if (BOARD_TYPE === 'F') {
                $("#nav_typeF").css("color", "#543d0d").css("font-weight", 800)
                    .css("box-shadow", "inset 0 -10px 0 #FFBC00FF");
            }else if (BOARD_TYPE === 'C') {
                $("#nav_typeC").css("color", "#543D0DFF").css("font-weight", 800)
                    .css("box-shadow", "inset 0 -10px 0 #FFBC00FF");
            }
            else {
                $("#nav_popular").css("color", "#543D0DFF").css("font-weight", 800)
                    .css("box-shadow", "inset 0 -10px 0 #FFBC00FF");
            }
            loadBoards();
        });

        async function loadBoards() {
            try {
                if (IS_POPULAR) {
                    const boards = await api.get('/api/starroad/boards/popular');
                    document.getElementById('free').style.display = 'none';
                    renderBoards(document.getElementById('popular'), boards.slice(0, 6));   // 인기글은 6개까지만
                } else {
                    const boards = await api.get('/api/starroad/boards?type=' + encodeURIComponent(BOARD_TYPE));
                    document.getElementById('popular').style.display = 'none';
                    renderBoards(document.getElementById('free'), boards);
                }
            } catch (e) {
                console.error('게시글 목록을 불러오지 못했습니다:', e.message);
            }
        }

        function renderBoards(container, boards) {
            let html = '';
            boards.forEach(function (board) {
                const image = board.imageBase64
                    ? '<img class="img_detail1" src="data:image/jpeg;base64,' + escapeHtml(board.imageBase64) + '" alt=""/>'
                    : '';
                html += '<div class="item_box item grow" rel="grow" style="cursor: pointer;" onclick="location.href=\'/starroad/board/detail?no=' + Number(board.no) + '\';">'
                    + '<div class="item_img" style="background-color: lightyellow">' + image + '</div>'
                    + '<div class="item_tag">'
                    + '<span class="item_tag_text">' + escapeHtml(board.detailType) + '</span>'
                    + '</div>'
                    + '<div class="item_title">' + escapeHtml(board.title) + '</div>'
                    + '<div class="item_content">' + escapeHtml(board.content) + '</div>'
                    + '<div class="item_footer">'
                    + '<div class="item_id_date">'
                    + '<div class="item_user_icon">'
                    + '<i class="fas fa-user-circle"></i>'
                    + '</div>'
                    + '<div>'
                    + '<span class="icon_id">' + escapeHtml(board.memberId || 'imkiki') + '</span> <br>'
                    + '<span class="icon_text_date">' + formatDate(board.regdate) + '</span>'
                    + '</div>'
                    + '</div>'
                    + '<div class="item_icon">'
                    + '<i class="far fa-thumbs-up"></i><span class="icon_text">' + escapeHtml(board.likes) + '</span>'
                    + ' <i class="far fa-comment"></i><span class="icon_text"> ' + escapeHtml(board.commentNum) + '</span>'
                    + '</div>'
                    + '</div>'
                    + '</div>';
            });
            container.innerHTML = html;
        }
    </script>
</head>
<body>
<div id="navbar"></div>


<!-- 네비게이션 바 -->
<div class="board_nav">
    <div class="board_nav_type">
        <ul class="board_nav_type_list">
            <li class="sidebar_menu"><a href= "popular"><p id="nav_popular">인기글</p></a></li>
            <li class="sidebar_menu"><a href= "free?type=F"><p id="nav_typeF">자유게시판</p></a></li>
            <li class="sidebar_menu"><a href= "free?type=C"><p id="nav_typeC">인증방</p></a></li>
        </ul>
    </div>
    <div class="board_nav_btn">
        <button id="nav_btn" onclick="location.href='/starroad/board/write';">글쓰기</button>
    </div>
</div>


<!-- 자유게시판 / 인증방 -->
<main>
    <div class="main_box">
        <div class="board_items menu-content" id="free"></div>
    </div>
</main>


<!-- 인기게시판 -->
<main>
    <div class="main_box">
        <div class="menu-content board_items" id="popular"></div>

    </div>
</main>

</body>
</html>
