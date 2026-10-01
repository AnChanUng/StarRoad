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
    <link rel="stylesheet" href="${path}/resources/static/css/common.css">
    <link rel="stylesheet" type="text/css" href="/resources/static/css/auth/login.css">
    <script type="text/javascript">
        $(function () {
            $("#navbar").load("/resources/common_jsp/navbar.jsp");

            $("#login_form").on("submit", async function (e) {
                e.preventDefault();
                try {
                    // 비밀번호가 틀리면 401 이다 — 로그인 화면에 그대로 머물며 메시지만 보여 준다
                    await api.post("/api/starroad/login", {
                        id: this.elements["id"].value,
                        password: this.elements["password"].value
                    }, {redirectOn401: false});
                    location.href = "/starroad";
                } catch (error) {
                    document.querySelector(".error-message").textContent = error.message;
                }
            });
        });
    </script>
</head>
<body>
<div id="navbar"></div>
<main id="login_main">
    <div id="egg_c">
        <img id="egg" src="/resources/static/image/member/egg.png">
    </div>

    <div id="egg_c2">
        <img id="egg2" src="/resources/static/image/member/egg.png">
    </div>

    <article id="login_m">
        <div id="login_title">로그인</div>
        <div id="login_exp">청춘을 위한 Star Road</div>
        <form id="login_form" method="post" class="text-container">
            <input type="text" name="id" placeholder="아이디"><br>
            <input type="password" name="password" placeholder="비밀번호">
            <div class="error-message">${error}</div>
            <input type="submit" value="로그인" class="login-button1">
        </form>
        <div class="link-container">
            <div class="signup-options">
                <span id="signup_e">계정이 없으신가요?</span>
                <a id="signup_a" href="/starroad/member">회원가입</a>
            </div>
        </div>
    </article>
</main>
</body>
</html>