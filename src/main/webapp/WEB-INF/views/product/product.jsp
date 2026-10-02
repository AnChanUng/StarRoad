<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>STARROAD</title>
    <link rel="icon" href="${path}/resources/static/image/home/logo1.png" type="image/x-icon">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC" crossorigin="anonymous">
    <link rel="stylesheet" type="text/css" href="${path}/resources/static/css/common.css"/>
    <link rel="stylesheet" type="text/css" href="${path}/resources/static/css/nav.css">
    <link rel="stylesheet" type="text/css" href="${path}/resources/static/css/product/product.css">
    <link rel="stylesheet" href="https://unpkg.com/aos@next/dist/aos.css"/>
    <script src="https://unpkg.com/aos@next/dist/aos.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.7.0/jquery.min.js"></script>
    <script src="/resources/static/js/common.js"></script>
    <script type="text/javascript">
        $(function () {
            AOS.init();
            $("#navbar").load("${path}/resources/common_jsp/navbar.jsp");
        });
    </script>
    <script src="/resources/static/js/product.js"></script>
</head>
<body>
<div id="navbar"></div>
<div class="main_title">예적금 상품 추천</div>
<div id="product_search">
    <nav class="navbar navbar-light bg-light" id="product_search_nav">
        <div class="container-fluid">
            <form id="searchForm" class="d-flex" action="/starroad/product/result">
                <div>
                    <div class="search_type content">상품 유형</div>
                    <select name="type" id="type" class="content">
                        <option value="S">적금</option>
                        <option value="D">예금</option>
                    </select>
                </div>

                <div>
                    <div class="search_type content">최대 가능<br>가입 기간</div>
                    <ul id="period" class="content">
                        <li>
                            <input type="radio" name="period" value="6" id="period_6" class="btn period_btn">
                            <label for="period_6">6개월</label>
                            </input>
                        </li>
                        <li>
                            <input type="radio" name="period" value="12" id="period_12" class="btn period_btn">
                            <label for="period_12">12개월</label>
                            </input>
                        </li>
                        <li>
                            <input type="radio" name="period" value="24" id="period_24" class="btn period_btn">
                            <label for="period_24">24개월</label></input>
                        </li>
                        <li>
                            <input type="radio" name="period" value="36" id="period_36" class="btn period_btn">
                            <label for="period_36">36개월</label></input>
                        </li>
                        <li>
                            <input type="radio" name="period" value="60" id="period_60" class="btn period_btn">
                            <label for="period_60">60개월 이상</label></input>
                        </li>
                    </ul>
                </div>

                <div>
                    <div class="search_type content">이자 과세</div>
                    <c:choose>
                        <c:when test="${currentUser ne null}">
                            <ul id="rate" class="content">
                                <li>
                                    <input type="radio" name="rate" value="base" id="rate_base" class="btn period_btn"
                                           checked>
                                    <label for="rate_base">일반과세</label>
                                    </input>
                                </li>
                                <li>
                                    <input type="radio" name="rate" value="none" id="rate_none" class="btn period_btn">
                                    <label for="rate_none">비과세</label>
                                    </input>
                                </li>
                            </ul>
                        </c:when>
                        <c:otherwise>
                            <ul id="rate" class="content">
                                <li>
                                    <input type="radio" name="rate" value="base" class="btn period_btn" disabled>
                                    <label for="rate_base">일반과세</label>
                                    </input>
                                </li>
                                <li>
                                    <input type="radio" name="rate" value="none" class="btn period_btn" disabled>
                                    <label for="rate_none">비과세</label>
                                    </input>
                                </li>
                                <span>로그인하시면 회원 정보와 이자 과세를 적용한 만기예상금액을 보실 수 있습니다.</span>
                            </ul>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div>
                    <div class="search_type content">상품명</div>
                    <input id="searchInput" name="query" class="form-control me-2 search_bar" type="text"
                           placeholder="예적금 상품명을 적어주세요"
                           aria-label="Search">
                    <button id="submitButton" class="search_link_btn" type="submit">검색</button>
                </div>

            </form>
        </div>
    </nav>
</div>
<%-- 상품 목록과 페이지 번호는 product.js 가 /api/starroad/products 응답으로 그린다 --%>
<div id="product_list">
    <ul></ul>
</div>
<div aria-label="Page navigation example">
    <ul class="pagination"></ul>
</div>
</body>
</html>