/**
 * 청년정책 화면(policy.jsp, policy_result.jsp).
 * 주소창의 검색 조건(location, tag1~4, keyword, pageIndex)을 그대로 API 에 넘겨 목록을 받아 그린다.
 * JSP 에서 LOGGED_IN(로그인 여부)을 먼저 정의해 둬야 한다.
 */
$(function () {
    const params = new URLSearchParams(location.search);

    restoreSearchForm(params);

    api.get('/api/starroad/policies?' + params.toString())
        .then(function (page) {
            renderPolicies(page.policyList);
            renderPagination(document.querySelector('.pagination'), page.currentPage, page.pageEndIndex, function (pageIndex) {
                params.set('pageIndex', pageIndex);
                location.href = location.pathname + '?' + params.toString();
            });
        })
        .catch(function (e) {
            alert(e.message);
        });

    // 검색한 조건을 검색 폼에 다시 표시한다
    function restoreSearchForm(params) {
        if (params.get('location')) {
            $('.location_select_box').val(params.get('location'));
        }
        ['tag1', 'tag2', 'tag3', 'tag4'].forEach(function (name) {
            if (params.get(name)) {
                $('input[name=' + name + ']').prop('checked', true);
            }
        });
        if (params.get('keyword')) {
            $('#keyword').val(params.get('keyword'));
        }
    }

    function renderPolicies(policyList) {
        const html = policyList.map(function (item, index) {
            return '<div class="policy" data-aos="fade-up" data-aos-delay="' + (200 * index) + '" data-aos-duration="400">'
                + likeHtml(item)
                + '<div class="name">' + escapeHtml(item.name) + '</div>'
                + '<div class="explain">' + escapeHtml(item.explain) + '</div>'
                + '<div class="tag">#' + escapeHtml(item.tag) + '</div>'
                + '<div class="btn_div">'
                + '<button class="link_btn"><a href="' + escapeHtml(item.link) + '">더보기</a></button>'
                + '</div>'
                + '</div>';
        }).join('');

        $('.policy_box').html(html);
        AOS.refreshHard();
    }

    // 관심 정책 하트. 비로그인이면 자리만 차지하는 빈 칸이다
    function likeHtml(item) {
        if (!LOGGED_IN) {
            return '<div class="like"></div>';
        }
        return '<div class="like' + (item.liked ? '' : ' heart_icon') + '" data-policy-no="' + item.no + '">'
            + '<i class="fa-solid fa-heart" id="' + (item.liked ? 'yellowHeart' : 'whiteHeart') + '"></i>'
            + '</div>';
    }

    // 하트를 누르면 관심 정책으로 등록·해제하고, 화면을 다시 받지 않고 하트만 바꾼다
    $('.policy_box').on('click', '.like[data-policy-no]', function () {
        const like = $(this);
        api.post('/api/starroad/policies/' + like.data('policy-no') + '/like')
            .then(function (result) {
                like.toggleClass('heart_icon', !result.liked);
                like.find('i').attr('id', result.liked ? 'yellowHeart' : 'whiteHeart');
            })
            .catch(function (e) {
                if (e.status !== 401) alert(e.message);
            });
    });

    $('.search_input').keyup(function (event) {
        if (event.which === 13) {
            event.preventDefault();
            $('form').submit();
        }
    });
});
