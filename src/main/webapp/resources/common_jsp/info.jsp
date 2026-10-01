<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>mypage</title>
    <link rel="stylesheet" href="${path}/resources/static/css/common.css">
    <link rel="stylesheet" href="${path}/resources/static/css/mypage/sidebar.css">
    <link rel="stylesheet" href="${path}/resources/static/css/mypage/check_password.css">
    <link rel="stylesheet" type="text/css" href="${path}/resources/static/css/member/member.css">
    <%-- jQuery 와 common.js 는 이 조각을 불러 쓰는 화면(mypage/info.jsp)이 이미 넣었다 --%>
    <script src="//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>
    <script>
        $("body").on("click", "#address", function (event) {
            new daum.Postcode({
                oncomplete: function (data) {
                    $("#address").val(data.address); // 주소 넣기
                    $("input[id=address_detail]").focus(); // 상세입력 포커싱
                }
            }).open();
        });

    </script>

</head>
<body>
<h1 class="info_h1">${fn:escapeXml(currentUser.name)}님의 정보</h1>
<div class="info_div">
    <div>
        <form id="info_form" method="post">
            <table>
                <tr>
                    <th>이메일</th>
                    <td>
                        ${fn:escapeXml(currentUser.email)}
                    </td>
                </tr>
                <tr>
                    <th>전화번호 <span class="star">*</span></th>
                    <td>
                        <input type="text" name="phone" value="${fn:escapeXml(currentUser.phone.substring(0,3))}"
                               onclick="clearInput(this)"/> -
                        <input type="text" name="phone" value="${fn:escapeXml(currentUser.phone.substring(4,8))}"
                               onclick="clearInput(this)"/> -
                        <input type="text" name="phone" value="${fn:escapeXml(currentUser.phone.substring(9,13))}"
                               onclick="clearInput(this)"/>
                    </td>
                </tr>

                <tr>
                    <th>자택주소 <span class="star">*</span></th>
                    <td>
                        <input type="text" name="address" id="address"
                               value="${fn:escapeXml(currentUser.address.substring(0, currentUser.address.indexOf(',')))}"
                               required>
                        <a id="address_btn" class="memberClick">주소검색</a><br>
                        <input type="text" name="address" id="address_detail"
                               value="${fn:escapeXml(currentUser.address.substring(currentUser.address.indexOf(',') + 1).trim())}"
                               onclick="clearInput(this)" required>
                    </td>
                </tr>
                </tr>
                <tr>
                    <th>직업구분</th>
                    <td>
                        <select name="job" id="job">
                            <option disabled selected> ${fn:escapeXml(currentUser.job)} </option>
                            <option>학생</option>
                            <option>직장인</option>
                            <option>사업주</option>
                            <option>프리랜서</option>
                            <option>전문직</option>
                            <option>주부</option>
                            <option>기타</option>
                        </select>
                    </td>
                </tr>
                <tr>
                    <th>월수입</th>
                    <td>
                        <input type="number" name="salary" id="salary" value="${fn:escapeXml(currentUser.salary)}"
                               onclick="clearInput(this)" required><span>천원</span>
                        <div class='valid'>1,000원 단위 (소득이 없을시 0)</div>
                    </td>
                </tr>
                <th>거래목적</th>
                <td>
                    <select name="purpose" id="purpose">
                        <option disabled selected> ${fn:escapeXml(currentUser.purpose)} </option>
                        <option>급여 및 생활비</option>
                        <option>저축 및 투자</option>
                        <option>사업상 거래</option>
                        <option>결제</option>
                        <option>대출</option>
                    </select>
                </td>
                </tr>
                <tr>
                    <th>거래자금의 원천</th>
                    <td>
                        <select name="source" id="source">
                            <option disabled selected> ${fn:escapeXml(currentUser.source)} </option>
                            <option>근로 및 연금소득</option>
                            <option>퇴직소득</option>
                            <option>사업소득</option>
                            <option>임대소득</option>
                            <option>매매소득</option>
                            <option>금융소득</option>
                            <option>용돈/생활비/상속</option>
                            <option>대출금</option>
                        </select>

                    </td>
                </tr>
                <tr>
                    <th>저금목표치</th>
                    <td>
                        <span class="source"> 거래자금의 원천의</span>
                        <input type="number" name="goal" id="goal" min="0" max="100" value="${fn:escapeXml(currentUser.goal)}"
                               onclick="clearInput(this)" required>
                        <span class="per">%</span>
                        <div class='valid'>퍼센트 단위 (1~100사이 숫자 입력)</div>
                    </td>
                </tr>
            </table>
            <button type="submit" class="submit-button" id="update_btn">정보 수정</button>
        </form>
    </div>
</div>
<script>
    $("#info_form").on("submit", async function (e) {
        e.preventDefault();
        const form = this;
        var requiredFields = $(form).find("input[required]");

        // 모든 필수 필드가 valid한지 확인
        var allValid = true;
        requiredFields.each(function () {
            if (!this.checkValidity()) {
                allValid = false;
                return false; // 검증 실패 시 반복문 종료
            }
        });
        if (!allValid) {
            return;
        }

        // 고르지 않았으면(disabled 로 보여 주는 기존 값이 선택된 상태) null — 서버가 기존 값을 유지한다
        function selectedText(select) {
            const option = select.options[select.selectedIndex];
            return (!option || option.disabled) ? null : option.text;
        }

        function joinValues(name, separator) {
            return $(form).find("input[name=" + name + "]").map(function () {
                return this.value;
            }).get().join(separator);
        }

        try {
            await api.put("/api/starroad/mypage/info", {
                phone: joinValues("phone", "-"),
                address: joinValues("address", ","),
                job: selectedText(document.getElementById("job")),
                salary: Number($("#salary").val()),
                purpose: selectedText(document.getElementById("purpose")),
                source: selectedText(document.getElementById("source")),
                goal: Number($("#goal").val())
            });
            alert("개인정보수정이 완료되었습니다.");
            location.href = "/starroad";
        } catch (error) {
            if (error.status !== 401) {
                alert(error.message);
            }
        }
    });
</script>
</body>
</html>