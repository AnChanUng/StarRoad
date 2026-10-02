<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>STARROAD</title>
    <link rel="icon" href="${path}/resources/static/image/home/logo1.png" type="image/x-icon">
    <script src="//code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="//code.jquery.com/jquery-latest.min.js"></script>
    <script src="/resources/static/js/common.js"></script>
    <link rel="stylesheet" type="text/css" href="${path}/resources/static/css/common.css">
    <link rel="stylesheet" type="text/css" href="${path}/resources/static/css/member/member.css">
    <script type="text/javascript">
        $(function () {
            $("#navbar").load("${path}/resources/common_jsp/navbar.jsp");

            let idFlag = false;
            $("#checkId").click(function () {
                let id = $("#id").val();
                // 정규 표현식을 사용하여 아이디 유효성 검사
                const idPattern = /^[a-zA-Z0-9]{6,12}$/;

                if (idPattern.test(id)) {
                    api.get("/api/starroad/members/check-id?id=" + encodeURIComponent(id)).then(function (data) {
                        if (!data.duplicated) {
                            result = "사용 가능한 아이디입니다.";
                            $("#result_checkId").html(result).css("color", "green");
                            idFlag = true;
                        } else {
                            result = "이미 사용중인 아이디입니다.";
                            $("#result_checkId").html(result).css("color", "red");
                            idFlag = false;
                        }
                    }).catch(function (error) {
                        idFlag = false;
                        alert("오류 발생");
                    });
                } else {
                    alert("아이디는 6~12자의 영문자와 숫자 조합이어야 합니다.");
                    id = false;
                    result = "6~12자리 영문/숫자 조합";
                    $("#result_checkId").html(result).css("color", "red");
                    $("#id").val("").trigger("focus");
                }
            });

            let errorFlag = false;
            $("#password").blur(function () {
                let password = $("#password").val();
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

            let emailFlag = false;

            // 이메일 유효성을 검사하는 함수
            function isValidEmail(email) {
                const emailPattern = /^[\w-]+(\.[\w-]+)*@([\w-]+\.)+[a-zA-Z]{2,7}$/;
                return emailPattern.test(email);
            }

            $("#checkEmail").click(function () {
                let email = $("#email").val();

                if (isValidEmail(email)) {
                    // 정규 표현식을 사용하여 이메일 유효성 검사
                    api.get("/api/starroad/members/check-email?email=" + encodeURIComponent(email)).then(function (data) {
                        if (!data.duplicated) {
                            result = "사용 가능한 이메일입니다.";
                            $("#result_checkEmail").html(result).css("color", "green");
                            emailFlag = true;
                        } else {
                            result = "이미 사용중인 이메일입니다.";
                            $("#result_checkEmail").html(result).css("color", "red");
                            emailFlag = false;
                        }
                    }).catch(function (error) {
                        emailFlag = false;
                    });
                } else {
                    emailFlag = false;
                    result = "이메일 주소 형식이 올바르지 않습니다.";
                    $("#result_checkEmail").html(result).css("color", "red");
                    $("#email").val("").focus();
                }
            });

            $("#confirmPassword").blur(function () {
                let password = $("#password").val();
                let confirmPassword = $("#confirmPassword").val();

                if (password !== confirmPassword) {
                    result = "비밀번호를 다시 입력해주세요";
                    $("#result_checkPassword").html(result).css("color", "red");
                } else {
                    result = "비밀번호가 일치합니다";
                    $("#result_checkPassword").html(result).css("color", "green");
                }
            });

            // 고르지 않았으면(disabled 안내 옵션이 선택된 상태) null
            function selectedText(select) {
                const option = select.options[select.selectedIndex];
                return (!option || option.disabled) ? null : option.text;
            }

            function joinValues(selector, separator) {
                return $(selector).map(function () {
                    return this.value;
                }).get().join(separator);
            }

            $("#member_form").on("submit", async function (e) {
                e.preventDefault();
                var requiredFields = $("input[required]");

                // 모든 필수 필드가 valid한지 확인
                var allValid = true;
                requiredFields.each(function () {
                    if (!this.checkValidity()) {
                        allValid = false;
                        return false; // 검증 실패 시 반복문 종료
                    }
                });

                // 모든 필수 필드가 valid하다면 가입 요청
                if (allValid && emailFlag && errorFlag && idFlag) {
                    try {
                        await api.post("/api/starroad/members", {
                            name: $("#name").val(),
                            id: $("#id").val(),
                            password: $("#password").val(),
                            birthday: $("#birthday").val(),
                            phone: joinValues("input[name=phone]", "-"),
                            email: $("#email").val(),
                            address: joinValues("input[name=address]", ","),
                            job: selectedText(document.getElementById("job")),
                            purpose: selectedText(document.getElementById("purpose")),
                            source: selectedText(document.getElementById("source")),
                            salary: Number($("#salary").val()),
                            goal: Number($("#goal").val())
                        });
                        alert("회원가입이 완료되었습니다.");
                        location.href = "/starroad/login";
                    } catch (error) {
                        alert(error.message);
                    }
                } else {
                    alert("회원정보를 다시 확인해주세요");
                }
            });

        });
    </script>

    <script src="//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>

    <script>
        window.onload = function () {
            document.getElementById("address").addEventListener("click", function () { //주소입력칸을 클릭하면
                //카카오 지도 발생
                new daum.Postcode({
                    oncomplete: function (data) { //선택시 입력값 세팅
                        document.getElementById("address").value = data.address; // 주소 넣기
                        document.querySelector("input[id=address_detail]").focus(); //상세입력 포커싱
                    }
                }).open();
            });

            document.getElementById("address_btn").addEventListener("click", function () { //주소입력칸을 클릭하면
                //카카오 지도 발생
                new daum.Postcode({
                    oncomplete: function (data) { //선택시 입력값 세팅
                        document.getElementById("address").value = data.address; // 주소 넣기
                        document.querySelector("input[id=address_detail]").focus(); //상세입력 포커싱
                    }
                }).open();
            });
        }
    </script>
</head>
<body>
<div id="navbar"></div>
<div class="container">
    <div class="form-container">
        <h1>회원가입</h1>
        <form id="member_form" method="post">
            <h2>기본정보 <span class="required"><span class="star">*</span>&nbsp;표시는 필수 입력입니다</span></h2>
            <table>
                <tr>
                    <th>성명 <span class="star">*</span></th>
                    <td><input type="text" name="name" id="name" required
                               pattern="^[가-힣]{2,5}|[a-zA-Z]{2,10}\s[a-zA-Z]{2,10}"></td>
                </tr>
                <tr>
                    <th>아이디 <span class="star">*</span></th>
                    <td>
                        <input type="text" name="id" id="id" required>
                        <a id="checkId" class="memberClick">중복확인</a><br>
                        <div class='valid'><span id="result_checkId" style="font-size:12px;">6~12자리 영문/숫자 조합</span>
                        </div>
                    </td>
                </tr>
                <tr>
                    <th>비밀번호 <span class="star">*</span></th>
                    <td>
                        <input type="password" name="password" id="password" required>
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
            </table>
            <br>
            <h2>추가정보</h2>

            <table>
                <tr>
                    <th>생년월일 <span class="star">*</span></th>
                    <td><input type="date" name="birthday" id="birthday" required max="9999-12-31"></td>
                </tr>
                <tr>
                    <th>전화번호 <span class="star">*</span></th>
                    <td>
                        <input type="text" name="phone" pattern="\d{2,3}"/> -
                        <input type="text" name="phone" pattern="\d{4}"/> -
                        <input type="text" name="phone" pattern="\d{4}"/>
                    </td>
                </tr>
                <tr>
                    <th>이메일 <span class="star">*</span></th>
                    <td>
                        <input type="text" id="email" name="email" required>
                        <a id="checkEmail" class="memberClick">중복확인</a><br>
                        <div class='valid'><span id="result_checkEmail" style="font-size:12px;">이메일을 적어주세요</span></div>
                    </td>
                </tr>
                <tr>
                    <th>자택주소 <span class="star">*</span></th>
                    <td>
                        <input type="text" name="address" id="address" required>
                        <a id="address_btn" class="memberClick">주소검색</a><br>
                        <input type="text" name="address" id="address_detail" placeholder="상세주소를 입력하세요" required>
                    </td>
                </tr>
                <tr>
                    <th>직업구분</th>
                    <td>
                        <select name="job" id="job">
                            <option disabled selected>직업을 선택하세요</option>
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
                        <input type="number" name="salary" id="salary" required><span>천원</span>
                        <div class='valid'>1,000원 단위 (소득이 없을시 0)</div>
                    </td>
                </tr>
                <th>거래목적</th>
                <td>
                    <select name="purpose" id="purpose">
                        <option disabled selected>거래목적을 선택하세요</option>
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
                            <option disabled selected>원천을 선택하세요</option>
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
                        <input type="number" name="goal" id="goal" min="0" max="100" required>
                        <span class="per">%</span>
                        <div class='valid'>퍼센트 단위 (0~100사이 숫자 입력)</div>
                    </td>
                </tr>
            </table>
            <br>
            <div class="agree-check">
                <input type="checkbox" required/> 이용약관 개인정보 수집 및 이용, 마케팅 활용선택에 모두 동의합니다.
            </div>
            <br>
            <button type="submit" class="submit-button">회원가입</button>
            <br>
            <br>
        </form>
    </div>
</div>
</body>
</html>