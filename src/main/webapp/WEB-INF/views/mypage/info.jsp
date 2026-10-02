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
    <link rel="stylesheet" href="${path}/resources/static/css/mypage/check_password.css">
    <!-- jquery 링크, navbar -->
    <script src="//code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="/resources/static/js/common.js"></script>
    <script type="text/javascript">
        $(function () {
            $("#navbar").load("${path}/resources/common_jsp/navbar.jsp");

            function check_password(input_pw) {
                api.post("/api/starroad/mypage/check-password", {password: input_pw}).then(function (data) {
                    if (!data.matched) {
                        $("#check_msg").text("비밀번호를 잘못 입력했습니다. 다시 입력해주세요.");
                        $("#password").css({"border": "red solid 1px", "outline-color": "red"})
                    } else {
                        $("#check_c").css("display", "none")
                        $("#info_c").load("${path}/resources/common_jsp/info.jsp");
                    }
                }).catch(function (error) {
                    if (error.status !== 401) {
                        alert("잠시 후 시도해주세요.");
                    }
                });
            }

            document.getElementById("check_btn").addEventListener('click', (e) => {
                check_password($("#password").val());
            });

            document.getElementById("password").addEventListener('keydown', (e) => {
                if (e.key === "Enter") {
                    e.preventDefault();
                    check_password($("#password").val());
                }
            });
        });
    </script>
</head>
<div id="navbar"></div>
<main>
    <aside>
        <div id='sidebar_title'>마이페이지</div>
        <ul>
            <li><a class='sidebar_menu' href='/starroad/mypage/asset'>나의 자산</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/challenge'>적금 챌린지</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/board'>작성한 글 보기</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/info' id='selected'>정보 수정</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/password'>비밀번호 수정</a></li>
        </ul>
    </aside>
    <article>
        <section id="check_c">
            <form id="check_m" onsubmit="return false;">
                <div id="m_title">비밀번호를 입력해주세요</div>
                <label>
                    <input id="password" name="password" type="password" placeholder=" 8~12자리 영문/숫자 조합 (대소문자 구분)">
                </label>
                <div id="check_msg"></div>
                <button id="check_btn" type="button">확인</button>
            </form>
        </section>
        <section id="info_c"></section>
    </article>
</main>
</html>