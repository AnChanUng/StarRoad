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
    <link rel="stylesheet" href="${path}/resources/static/css/mypage/challenge.css">
    <!-- jquery 링크, navbar -->
    <script src="//code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="/resources/static/js/common.js"></script>
    <script type="text/javascript">
        $(function () {
            let challenges = []
            let selected = null
            $("#navbar").load("${path}/resources/common_jsp/navbar.jsp")

            api.get("/api/starroad/mypage/challenges").then(function (data) {
                challenges = data
                const sel_sub = document.getElementById("sel_sub")
                challenges.forEach((challenge, idx) => {
                    let option = document.createElement("option")
                    option.value = idx
                    option.textContent = String.fromCharCode(160) + challenge.name   // 앞에 &nbsp; 한 칸
                    sel_sub.appendChild(option)
                })
            }).catch(function (error) {
                if (error.status !== 401) {
                    alert(error.message)
                }
            })

            $("#sel_sub").change(function () {
                selected = challenges[Number($(this).val())]
                if (!selected) return

                $("#sub_name").text(selected.name)
                $("#sub_attr").text(selected.attribute)
                $("#sub_exp").text(selected.explain)
                $("#sub_period").text(selected.period + "개월")
                $("#sub_price").text(selected.price * 0.1 + "만원")

                let logs = selected.paymentDays
                let dates = selected.paymentMonths
                let status = selected.status

                const star_cont = document.querySelector('#star_container')
                star_cont.innerHTML = ''
                logs.forEach((st, idx) => {
                    let star_b = document.createElement("div")
                    star_b.className = "star_b"
                    if (st !== 0) {
                        let star = document.createElement("img")
                        star.src = "${path}/resources/static/image/mypage/stars/" + encodeURIComponent(st) + ".png"
                        star.className = "star"
                        star.style.left = String(Math.random() * 75 + 5) + "px"
                        star.style.top = String(Math.random() * (parseInt(300 / parseInt(logs.length/6))-40)) + "px"
                        star.title = dates[idx]
                        document.querySelector('#star_container').appendChild(star_b).append(star)
                    } else {
                        document.querySelector('#star_container').appendChild(star_b)
                    }
                })

                $("#sub_info_s").css("display", "block")
                $("#star_container").css({"display": "flex", "visibility": "visible"})
                $("#star_section").css("display", "block")
                $("#sel_pic").css("display", "none")
                $("#pic_exp").css("display", "none")

                if (status === -1) {          // 성공
                    $("#reward_btn").text("리워드를 받으세요 🥳")
                        .attr("disabled", false)
                        .css({
                            "display": "block",
                            "background": "var(--main-kb-yellow-positive)",
                            "color": "black",
                            "cursor": "pointer"
                        })
                } else if (status === -2) {   // 리워드를 이미 받았을 때, 끝났을 때
                    $("#reward_btn").text("완주 성공! 😎")
                        .attr("disabled", true)
                        .css({
                            "display": "block",
                            "background": "var(--sub-kb-gold)",
                            "color": "white",
                            "cursor": "unset"
                        })
                } else {
                    $("#reward_btn").text("").append("<strong>" + Number(status) + "</strong>" + "개월 남았어요 💪")
                        .attr("disabled", true)
                        .css({
                            "display": "block",
                            "background": "var(--sub-kb-gold)",
                            "color": "white",
                            "cursor": "unset"
                        })
                }
            })

            // 리워드 받기 화면으로 이동
            $("#reward_btn").click(function () {
                if (selected) {
                    location.href = "/starroad/mypage/reward?subNo=" + encodeURIComponent(selected.subNo)
                }
            })
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
            <li><a class='sidebar_menu' href='/starroad/mypage/challenge' id='selected'>적금 챌린지</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/board'>작성한 글 보기</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/info'>정보 수정</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/password'>비밀번호 수정</a></li>
        </ul>
    </aside>
    <article id="sub_article">
        <div>
            <select name="subscription" id="sel_sub">
                <option disabled selected>가입하신 적금을 선택해주세요</option>
            </select>

            <section id="sel_pic_exp">
                <img id="sel_pic" src="${path}/resources/static/image/mypage/sel_pic.png">
                <div id="pic_exp">가입하신 적금을 선택해주세요</div>
            </section>

            <section id="sub_info_s">
                <div id="sub_na_c">
                    <span id="sub_name"></span>
                    <span id="sub_attr"></span>
                </div>
                <div id="sub_be_c">
                    <div id="bef_exp"></div>
                    <div id="sub_exp"></div>
                </div>
                <div id="sub_pp_c">
                    <div id="sub_period"></div>
                    <div id="sub_price"></div>
                </div>
            </section>

            <section id="star_section">
                <div style="margin: 0 0 20px 5px">❕ 별 위에 마우스를 올리면 납부 날짜가 나와요</div>
                <div id="star_container"></div>
            </section>

            <div style="margin-top:70px">
                <button id="reward_btn" type="button"></button>
            </div>
        </div>
    </article>
</main>
</body>
</html>