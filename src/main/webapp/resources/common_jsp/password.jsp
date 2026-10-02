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
    <%-- jQuery 와 common.js 는 이 조각을 불러 쓰는 화면(mypage/password.jsp)이 이미 넣었다 --%>
    <link rel="stylesheet" type="text/css" href="${path}/resources/static/css/member/member.css">

    <script type="text/javascript">
        $(function () {
            let errorFlag = false;
            $("#password1").blur(function () {
                let password = $("#password1").val();
                let confirmPassword = $("#confirmPassword").val();
                const passwordPattern = /^[a-zA-Z0-9]{8,12}$/;

                if (passwordPattern.test(password)) {
                    result = "비밀번호가 조건에 일치합니다";
                    $("#result_Password").html(result).css("color", "green");
                    errorFlag = true; // 비밀번호가 일치하면 오류 플래그를 리셋합니다.
                } else {
                    result = "비밀번호를 다시 입력해주세요 (8~12자리 영문/숫자 조합)";
                    $("#result_Password").html(result).css("color", "red");
                    errorFlag = false;
                }

                if (!errorFlag) { // 오류 플래그가 false인 경우에만 alert를 띄웁니다.
                    alert("비밀번호는 8~12자의 영문자와 숫자 조합이어야 합니다");
                }
            });

            let checkPassword = false;

            $("#confirmPassword").blur(function () {
                let password = $("#password1").val();
                let confirmPassword = $("#confirmPassword").val();

                if (password !== confirmPassword) {
                    result = "비밀번호를 다시 입력해주세요";
                    $("#result_checkPassword").html(result).css("color", "red");
                    checkPassword = false;

                } else {
                    result = "비밀번호가 일치합니다";
                    $("#result_checkPassword").html(result).css("color", "green");
                    checkPassword = true;
                }
            });
            $("#password_form").on("submit", async function (e) {
                e.preventDefault();
                var requiredFields = $(this).find("input[required]");

                // 모든 필수 필드가 valid한지 확인
                var allValid = true;
                requiredFields.each(function () {
                    if (!this.checkValidity()) {
                        allValid = false;
                        return false; // 검증 실패 시 반복문 종료
                    }
                });

                // 모든 필수 필드가 valid하다면 수정 요청
                if (allValid && checkPassword && errorFlag) {
                    try {
                        await api.put("/api/starroad/mypage/password", {password: $("#password1").val()});
                        alert("비밀번호수정이 완료되었습니다.");
                        location.href = "/starroad";
                    } catch (error) {
                        if (error.status !== 401) {
                            alert(error.message);
                        }
                    }
                } else {
                    alert("비밀번호를 다시 확인해주세요.");
                }
            });

        });


    </script>

</head>
<body>
<div>
    <h1>${fn:escapeXml(currentUser.name)}님의 정보</h1>
    <div class="password-container">
        <form id="password_form" method="post">
            <table>
                <tr>
                    <th>비밀번호 수정 <span class="star">*</span></th>
                    <td>
                        <input type="password" name="password" id="password1" required>
                        <div class='valid' id="result_Password">8~12자리 영문/숫자 조합 (대소문자)</div>
                    </td>
                </tr>
                <tr>
                    <th>비밀번호 확인 <span class="star">*</span></th>
                    <td>
                        <input type="password" name="confirm_password" id="confirmPassword" required>
                        <div class='valid' id="result_checkPassword">비밀번호를 다시한번 입력하세요</div>
                    </td>
                </tr>
                <br>
            </table>
            <br>
            <br>
            <button type="submit" class="submit-button">비밀번호 수정</button>
        </form>
    </div>
</div>
</body>
</html>