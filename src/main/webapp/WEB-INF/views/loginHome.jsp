<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" type="text/css" href="${path}/resources/static/css/common.css">
    <link rel="stylesheet" type="text/css" href="${path}/resources/static/css/home/home.css">
    <link rel="stylesheet" href="https://unpkg.com/aos@next/dist/aos.css"/>
    <script src="https://unpkg.com/aos@next/dist/aos.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/fullPage.js/4.0.20/fullpage.min.js"
            integrity="sha512-LGiXf+jHGTHcIybSsOWO3I/in+OObCkcEsWIZ7IyhzfD6RzD5qDUw2CK+JveuI7zTSEcDG//bIOvOpAJW2BWsg=="
            crossorigin="anonymous" referrerpolicy="no-referrer"></script>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.1/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-4bw+/aepP/YC94hEpVNVgiZdgIC5+VKNBQNGCHeKRQN+PtmoHDEXuppvnDJzQIu9" crossorigin="anonymous">
    <link rel="icon" href="${path}/resources/static/image/home/logo1.png" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="${path}/resources/static/css/home/login_home.css">
    <script src="//code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="/resources/static/js/common.js"></script>
    <script type="text/javascript">
        $(function () {
            AOS.init();
            $("#footer").load("${path}/resources/common_jsp/footer.jsp");
            $("#navbar").load("${path}/resources/common_jsp/navbar.jsp");
            $("#modal").css("display", "block");
            if(sessionStorage.getItem("modal") === "1"){
                $("#modal").css("display", "none");
            }

            window.addEventListener("wheel", function (e) {
                e.preventDefault();
            }, {passive: false});

            var mHtml = $("html");
            var page = 1;
            mHtml.animate({scrollTop: 0}, 10);
            $(window).on("wheel", function (e) {
                if (mHtml.is(":animated")) return;
                if (e.originalEvent.deltaY > 0) {
                    if (page == 5) return;
                    page++;
                } else if (e.originalEvent.deltaY < 0) {
                    if (page == 1) return;
                    page--;
                }
                var posTop = (page - 1) * $(window).height();
                mHtml.animate({scrollTop: posTop});
            })
        });
    </script>
    <title>STARROAD</title>
</head>

<body>
<div id="navbar"></div>
<div class="section" id="section2">
    <div class="content">
        <div>
            <div class="title" data-aos="zoom-in" data-aos-duration="2000">
                STAR ROAD
            </div>
            <div id="section2_detail" class="detail" data-aos="fade-up" data-aos-duration="3000" data-aos-delay="1000">
                KB와 함께 지속 가능 금융으로 나아가는 길,<br>
                청년의 행복과 더 나은 세상을 만들어갑니다
            </div>
        </div>
    </div>
    <div class="scroll bounce">
        <img class="fa fa-angle-double-down" src="${path}/resources/static/image/home/scroll.png">
    </div>
</div>
<div class="section" id="section3">
    <div class="page_container">
        <div>
            <div class="title" data-aos="fade-up" data-aos-duration="2000">
                예적금 상품 추천
            </div>
            <div id="section3_detail" class="detail" data-aos="fade-up" data-aos-duration="3000">
                다양한 국민은행 상품을 추천받아요<br>개인별 자산을 적용한 만기 예상 금액을 볼 수 있어요
            </div>
            <button class="main_btn" data-aos="fade-up" data-aos-duration="3000"><a href="/starroad/product">상품 추천
                받기</a></button>
        </div>
        <img src="${path}/resources/static/image/home/section3.gif" class="main_3d_img" alt="...">
    </div>
</div>
<div class="section" id="section4">
    <div class="page_container">
        <div>
            <img id="section4_img_chart"
                 src="${path}/resources/static/image/home/section4_1.png" class="main_3d_img" alt="...">
            <img id="section4_img_plant" data-aos="zoom-in" data-aos-duration="2000"
                 src="${path}/resources/static/image/home/section4_2.png" class="main_3d_img" alt="...">
        </div>
        <div>
            <div class="title" data-aos="fade-up" data-aos-duration="2000">
                My Asset & Challenge
            </div>
            <div id="section4_detail" class="detail" data-aos="fade-up" data-aos-duration="3000">
                나의 자산 상태를 차트로 파악하고,<br>
                적금 유지하여 포인트리를 받아요<br>
                커뮤니티를 통해 사람들과 함께 적금 만기까지 달려보세요!
            </div>
            <c:choose>
                <c:when test="${currentUser ne null}">
                    <button class="main_btn" data-aos="fade-up" data-aos-duration="3000"><a
                            href="/starroad/mypage/asset">자산 상태 보러가기</a></button>
                </c:when>
                <c:otherwise>
                    <button class="main_btn" data-aos="fade-up" data-aos-duration="3000"><a href="/starroad/login">자산 상태
                        보러가기</a></button>
                </c:otherwise>
            </c:choose>


        </div>
    </div>
</div>
<div class="section" id="section5">
    <div class="page_container">
        <div>
            <div class="title" data-aos="fade-up" data-aos-duration="2000">
                Policy
            </div>
            <div id="section5_detail" class="detail" data-aos="fade-up" data-aos-duration="3000">
                청년을 위한 금융 정책을 살펴보고<br>
                추가적인 혜택을 누려보세요<br>
                관심등록해두면 알림 서비스를 받을 수 있어요!
            </div>
            <button class="main_btn" data-aos="fade-up" data-aos-duration="3000"><a href="/starroad/policy">청년 정책
                보러가기</a></button>
        </div>
        <img src="${path}/resources/static/image/home/section5_1.gif" class="main_3d_img" alt="...">
    </div>
</div>

<!-- 모달창 -->
<div class="modal" id="modal">
    <div class="modal_body">
        <div class="modal_total_content">
            <div class="modal_close">
                <button class="modal_btn">✖ 닫기</button>
            </div>
            <div class="modal_content">
                <p id="modal_text"></p>
            </div>
            <div class="modal_btn_div">
                <button class="modal_btn_link grow2" id="modal_policy_link">👉 정책 신청하러 가기🏃</button>
                <button class="modal_btn_link grow2" onclick="location.href='/starroad/policy';">👉 더 많은 정책 보러 가기🏃</button>
            </div>
            <div class="modal_allDay_close">
                <p><input type="checkbox" id="modal_btn_allDay">&nbsp;&nbsp;오늘 하루 보지 않기</p>
            </div>
        </div>
    </div>
</div>

<script>
    const modal = document.querySelector("#modal");
    const modal_btn = document.querySelector(".modal_btn");
    const modal_btn_allDay = document.querySelector("#modal_btn_allDay");
    modal_btn.addEventListener('click', () => {
        modal.style.display = 'none';
    });
    modal_btn_allDay.addEventListener('click', () => {
        sessionStorage.setItem('modal', "1");
    });

    // 관심 정책 마감 알림
    let policyLink = null;
    document.querySelector("#modal_policy_link").addEventListener('click', () => {
        if (policyLink) {
            window.open(policyLink);
        }
    });

    api.get("/api/starroad/home/policy-notice").then(function (notice) {
        const text = document.querySelector("#modal_text");
        if (notice.exists) {
            const day = (notice.dday === 0) ? " 마감 당일" : " D" + notice.dday;
            text.innerHTML = '<span class="modal_user_name">' + escapeHtml(notice.userName) + '</span>님의 관심 정책으로 등록한 <br>'
                + '"<span class="modal_policy">' + escapeHtml(notice.policyName) + '</span>" 정책 '
                + '<span class="modal_day">' + escapeHtml(day) + '</span>입니다.';
            policyLink = notice.link;
        } else {
            text.innerHTML = '<span>' + escapeHtml(notice.message) + '</span>';
        }
    }).catch(function () {
        // 알림을 못 가져와도 홈 화면은 그대로 쓴다
    });
</script>

<div id="footer"></div>
</body>
</html>