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
    <link rel="stylesheet" href="${path}/resources/static/css/mypage/asset.css">
    <!-- 차트 링크 -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0"></script>
    <!-- jquery 링크, navbar -->
    <script src="//code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="/resources/static/js/common.js"></script>
    <script type="text/javascript">
        $(function () {
            $("#navbar").load("${path}/resources/common_jsp/navbar.jsp");

            api.get("/api/starroad/mypage/asset").then(function (assets) {
                $("#asset_title").text(assets.name + "님의 자산");
                $("#asset_point").text(assets.point + "P");
                $("#asset_deposit").text(Math.floor(assets.deposit * 0.1) + "만원");
                $("#asset_savings").text(Math.floor(assets.savings * 0.1) + "만원");
                $("#asset_investment").text(Math.floor(assets.investment * 0.1) + "만원");

                if (assets.savings + assets.deposit + assets.investment === 0) {
                    $("#asset").css("display", "none")
                    $("#asset_info").css("display", "none")
                    $("#info").css("display", "block")
                    $("#myChart").css("display", "none")
                }

                drawChart(assets);
            }).catch(function (error) {
                if (error.status !== 401) {
                    alert(error.message);
                }
            });
        });

        // 차트
        function drawChart(assets) {
            const data = {
                datasets: [{
                    backgroundColor: ['#FFBC00', '#545045', '#8D744A'],
                    data: [assets.savings * 0.1, assets.deposit * 0.1, assets.investment * 0.1]
                }],
                labels: ['적금', '예금', '투자금']
            };

            const ctx = document.getElementById("myChart");
            new Chart(ctx, {
                type: 'doughnut',
                data: data,
                options: {
                    responsive: false,
                    plugins: {
                        legend: false, // 범례 숨기기
                        datalabels: {
                            display: false // 데이터 레이블 숨기기
                        },
                    }
                }
            });
        }
    </script>
</head>
<body>
<div id="navbar"></div>
<main>
    <aside>
        <div id='sidebar_title'>마이페이지</div>
        <ul>
            <li><a class='sidebar_menu' href='/starroad/mypage/asset' id='selected'>나의 자산</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/challenge'>적금 챌린지</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/board'>작성한 글 보기</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/info'>정보 수정</a></li>
            <li><a class='sidebar_menu' href='/starroad/mypage/password'>비밀번호 수정</a></li>
        </ul>
    </aside>
    <article id="asset_a">
        <h1 id="asset_title"></h1>
        <div id="asset_container">
            <div id="asset_div">
                <section>
                    <div>
                        <canvas id="myChart"></canvas>
                    </div>
                    <div id="info">등록된 자산이 없습니다.</div>
                </section>

                <section id="asset_info">
                    <table id="asset_info_table">
                        <tbody>
                        <tr>
                            <td>포인트리</td>
                            <td id="asset_point"></td>
                        </tr>
                        <tr>
                            <td>
                                <div id="deposit_color" class="color_class"></div>
                                예금
                            </td>
                            <td id="asset_deposit"></td>
                        </tr>
                        <tr>
                            <td>
                                <div id="savings_color" class="color_class"></div>
                                적금
                            </td>
                            <td id="asset_savings"></td>
                        </tr>
                        <tr>
                            <td>
                                <div id="invest_color" class="color_class"></div>
                                투자금
                            </td>
                            <td id="asset_investment"></td>
                        </tr>
                        </tbody>
                    </table>
                </section>
            </div>
        </div>
    </article>
</main>
</body>
</html>