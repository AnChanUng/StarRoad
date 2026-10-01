<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>STARROAD</title>
    <link rel="icon" href="${path}/resources/static/image/home/logo1.png" type="image/x-icon">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.7.0/jquery.min.js"></script>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC" crossorigin="anonymous">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="https://unpkg.com/aos@next/dist/aos.css"/>
    <script src="https://unpkg.com/aos@next/dist/aos.js"></script>
    <script src="/resources/static/js/common.js"></script>
    <script type="text/javascript">
        // 로그인한 회원에게만 관심 정책 하트를 보여 준다
        const LOGGED_IN = ${currentUser ne null};

        $(function () {
            AOS.init();
            $("#navbar").load("${pageContext.request.contextPath}/resources/common_jsp/navbar.jsp");
        })
    </script>
    <script src="/resources/static/js/policy.js"></script>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/resources/static/css/common.css">
    <link rel="stylesheet" type="text/css"
          href="${pageContext.request.contextPath}/resources/static/css/policy/policy.css">
</head>
<body>
<div id="policy-div-wrap">
    <div id="navbar"></div>
    <div class="title">청년 금융 정책</div>
    <main>
        <div id="main_box">
            <div class="search_box bg-light">
                <form name="policyForm" id="policyForm" class="search_form" method="get"
                      action="${pageContext.request.contextPath}/starroad/policy/result">
                    <div class="search_small_box">
                        <div class="search_box_title">지역</div>
                        <div class="search_box_option">
                            <select class="location_select_box" name="location">
                                <option disabled selected>선택해주세요</option>
                                <option id="location_서울" value="서울">서울</option>
                                <option id="location_경기" value="경기">경기</option>
                                <option id="location_중앙부처" value="중앙부처">중앙부처</option>
                            </select>
                        </div>
                    </div>

                    <div class="search_small_box">
                        <div class="search_box_title">태그</div>
                        <div class="search_box_option">

                            <ul class="ks-cboxtags">
                                <li><input type="checkbox" id="금융지원" name="tag1" value="금융지원"><label
                                        for="금융지원">금융지원</label></li>
                                <li><input type="checkbox" id="교육" name="tag2" value="교육"><label for="교육">교육</label>
                                </li>
                                <li><input type="checkbox" id="생활지원" name="tag3" value="생활지원"><label
                                        for="생활지원">생활지원</label></li>
                                <li><input type="checkbox" id="금융자산" name="tag4" value="금융자산"><label for="금융자산">금융자산
                                    형성</label></li>
                            </ul>

                        </div>
                    </div>

                    <div class="search_small_box">
                        <div class="search_box_title">정책명</div>
                        <div class="search_box_option">
                            <input class="search_input form-control me-2 search_bar" name="keyword" id="keyword"
                                   type="text" placeholder="키워드를 입력해주세요">
                            <button id="final" class="submit_btn" type="submit">검색</button>
                        </div>
                    </div>

                </form>
            </div>

            <%-- 정책 목록과 페이지 번호는 policy.js 가 /api/starroad/policies 응답으로 그린다 --%>
            <div class="policy_box"></div>

            <div aria-label="Page navigation example">
                <ul class="pagination"></ul>
            </div>
        </div>

    </main>
</div>

</body>
</html>