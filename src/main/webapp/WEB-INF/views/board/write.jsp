<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>STARROAD</title>
    <link rel="icon" href="${path}/resources/static/image/home/logo1.png" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="/resources/static/css/board/write.css">
    <link href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" rel="stylesheet">
    <script src="//stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"></script>
    <script src="//code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="/resources/static/js/common.js"></script>
    <script type="text/javascript">
        $(function () {
            $("#navbar").load("${path}/resources/common_jsp/navbar.jsp");
        });
        $(document).ready(function () {
            var detailTypes = {
                "F": ["거지방", "잡담", "정보공유"],
                "C": ["챌린지", "자산인증"]
            }

            $('#type').change(function () {
                var selectedType = $(this).val();

                // detailType select box 초기화
                $('#detailType').empty();
                $('#detailType').append($('<option>').text('말머리 선택').attr('value', '').attr('disabled', true).attr('selected', true));
                // 선택된 type에 따라 detailTypes에서 해당 배열 가져와서 option으로 추가
                $.each(detailTypes[selectedType], function (index, value) {
                    $('#detailType').append($('<option>').text(value).attr('value', value));
                });
            });


        });
        $(document).ready(function () {
            $('#image').change(function () {
                // File has been selected
                if (this.files && this.files[0]) {
                    $('#file-name').text(this.files[0].name);
                    var reader = new FileReader();

                    reader.onload = function (e) {
                        $('#preview').attr('src', e.target.result);
                        $('#preview').show(); // Show the image preview

                    }

                    reader.readAsDataURL(this.files[0]);
                }
            });
        });
        // 글 등록: 폼 제출을 가로채 API 로 보내고, 성공하면 글을 쓴 게시판 목록으로 간다
        $(document).ready(function () {
            $('#writeForm').on('submit', async function (event) {
                event.preventDefault();
                const type = $('#type').val();
                try {
                    await api.post('/api/starroad/boards', new FormData(this));
                    location.href = '/starroad/board/free?type=' + encodeURIComponent(type);
                } catch (e) {
                    if (e.status !== 401) alert(e.message);   // 401 은 common.js 가 이미 알리고 로그인 화면으로 보낸다
                }
            });
        });


    </script>

</head>
<body>
<div id="navbar"></div>

<form id="writeForm" method="post" enctype="multipart/form-data">


    <div class="container">
        <div class="dkssud">



            <%-- <div class="update-button"> --%>
            <div id="update_btn_container">
                <h2 class="topTitle"> 글쓰기 </h2>
                <button type="submit" class="buttonStyle" id="updateBtn">등 록</button>
            </div>

            <hr style="border: solid 1px lightgrey;">
            <div>
                <div class="row labelStyle" style="margin-left: 0;">
                    <div class="col-md-4 col-sm-12 form-group d-flex ">

                        <select id="type" name="type" required class="form-control">
                            <option value="" disabled selected>게시판 선택</option>
                            <option value="F">자유게시판</option>
                            <option value="C">인증게시판</option>
                        </select>
                    </div>

                    <div class="col-md-4 col-sm-12 form-group d-flex ">


                        <select id="detailType" name="detailType" required class="form-control">
                            <option value="" disabled selected>말머리 선택</option>
                            <!-- Add 'disabled' and 'selected' here -->
                        </select>

                    </div>
                    <div class="col-md-4 col-sm-12 image-upload d-flex">
                        <div class="image-upload">
                            <label for="image">
                                <img src="${path}/resources/static/image/board/imagee.png" height="35" width="35"/>

                                <input type="file" id="image" name="image" style="display: none;"/>
                            </label>
                            <p>사진</p>
                        </div>

                    </div>


                </div>

                <div class="title">
                    <input name="title" class="titleStyle" type="text" id="title" placeholder="제목을 입력하세요" required/>
                </div>

                <div class="content">
                    <textarea name="content" class="contentStyle" id="content" placeholder="내용을 입력하세요"
                              required></textarea>


                </div>
            </div>
            <div style="display: flex; align-items: end;">
                <img id="preview" style="display: none; width: 200px; height: 200px; margin-right: 10px;"/> <span
                    id="file-name"></span>
            </div>
        </div>
    </div>

</form>

</body>
</html>
