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
    <link rel="stylesheet" href="${path}/resources/static/css/mypage/reward.css">
    <!-- jquery 링크, navbar -->
    <script src="//code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="/resources/static/js/common.js"></script>
    <script type="text/javascript">
        $(function () {
            $("#navbar").load("${path}/resources/common_jsp/navbar.jsp")

            const subNo = getQueryParam("subNo")
            if (!subNo) {
                location.href = "/starroad/mypage/challenge"
                return
            }
            const rewardUrl = "/api/starroad/mypage/challenges/" + encodeURIComponent(subNo) + "/reward"

            // 받을 수 없는 리워드면 메시지를 보여 주고 챌린지 화면으로 돌아간다
            function backToChallenge(error) {
                if (error.status !== 401) {
                    alert(error.message)
                    location.href = "/starroad/mypage/challenge"
                }
            }

            api.get(rewardUrl).then(function (reward) {
                $("#prod_name").text(reward.name)
                $("#reward_t").text(reward.period + "개월동안 고생하셨습니다!")
                $("#rew").text(reward.reward + "P")
            }).catch(backToChallenge)

            $("#reward_m").on("submit", function (e) {
                e.preventDefault()
                api.post(rewardUrl).then(function () {
                    location.href = "/starroad/mypage/asset"
                }).catch(backToChallenge)
            })
        });
    </script>
</head>
<div id="navbar"></div>
<main>
    <aside>
        <div id='sidebar_title'>마이페이지</div>
        <ul>
            <li><a class='sidebar_menu' href='/starroad/mypage/asset'>나의 자산</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/challenge' id='selected'>적금 챌린지</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/board'>작성한 글 보기</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/info'>정보 수정</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/password'>비밀번호 수정</a></li>
        </ul>
    </aside>
    <article>
        <div id="prod_name"></div>
        <section id="reward_c">
            <form id="reward_m" method="post">
                <div id="reward_t"></div>
                <img id="wallet_img" src="/resources/static/image/mypage/wallet.png">
                <div id="r_content">버튼을 눌러 포인트리 <span id="rew"></span>를 받으세요</div>
                <button id="reward_btn">리워드 받기</button>
            </form>
        </section>
    </article>
</main>
</html>