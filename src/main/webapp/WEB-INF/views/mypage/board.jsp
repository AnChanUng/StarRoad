<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>STARROAD</title>
    <link rel="icon" href="${path}/resources/static/image/home/logo1.png" type="image/x-icon">
    <link rel="stylesheet" href="${path}/resources/static/css/common.css">
    <link rel="stylesheet" href="${path}/resources/static/css/mypage/sidebar.css">
    <link rel="stylesheet" href="${path}/resources/static/css/mypage/board.css">
    <!-- jquery 링크, navbar -->
    <script src="//code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="/resources/static/js/common.js"></script>
    <script type="text/javascript">
        $(function () {
            $("#navbar").load("${path}/resources/common_jsp/navbar.jsp");

            api.get("/api/starroad/mypage/boards").then(function (writings) {
                let html = '';
                writings.forEach(function (writing) {
                    html += '<div class="b_contents">'
                        + '<span class="b_type">' + (writing.type === "F" ? "자유게시판" : "인증게시판") + '</span><br>'
                        + '<div class="b_title"><a href="/starroad/board/detail?no=' + encodeURIComponent(writing.no)
                        + '" class="b_title">' + escapeHtml(writing.title) + '</a></div>'
                        + '<div class="w_d_l">'
                        + '<div class="w_date">' + escapeHtml(formatDate(writing.regdate)) + '</div>'
                        + '<div id="likes_cc">'
                        + '<div class="likes_c">'
                        + '<img class="thumb" src="/resources/static/image/board/likes.png" alt="thumb">'
                        + '<div class="likes_n">' + escapeHtml(writing.likes) + '</div>'
                        + '</div>'
                        + '</div>'
                        + '</div>'
                        + '</div>';
                });
                document.getElementById("writings_list").innerHTML = html;
            }).catch(function (error) {
                if (error.status !== 401) {
                    alert(error.message);
                }
            });
        });
    </script>
</head>
<body>
<div id="navbar"></div>
<main>
    <aside>
        <div id='sidebar_title'>마이페이지</div>
        <ul>
            <li><a class='sidebar_menu' href='/starroad/mypage/asset'>나의 자산</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/challenge'>적금 챌린지</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/board' id='selected'>작성한 글 보기</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/info'>정보 수정</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/password'>비밀번호 수정</a></li>
        </ul>
    </aside>
    <article id="writings_c">
        <div id="sub_menu">
            <a id="sel" href="/starroad/mypage/board">작성글</a>&nbsp;
            <a id="not_sel" href="/starroad/mypage/comment">작성댓글</a>
        </div>
        <section id="writings_list"></section>
    </article>
</main>
</body>
</html>