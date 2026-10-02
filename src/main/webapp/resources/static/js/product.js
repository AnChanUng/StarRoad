/**
 * 예적금 상품 화면(product.jsp, product_result.jsp).
 * 주소창의 검색 조건(type, period, rate, query, page)을 그대로 API 에 넘겨 목록을 받아 그린다.
 */
$(function () {
    const params = new URLSearchParams(location.search);

    restoreSearchForm(params);

    api.get('/api/starroad/products?' + params.toString())
        .then(function (page) {
            renderProducts(page.productItems, page.user);
            renderPagination(document.querySelector('.pagination'), page.currentPage, page.pageEndIndex, function (pageNo) {
                params.set('page', pageNo);
                location.href = location.pathname + '?' + params.toString();
            });
        })
        .catch(function (e) {
            alert(e.message);
        });

    // 검색한 조건을 검색 폼에 다시 표시한다
    function restoreSearchForm(params) {
        if (params.get('type')) {
            $('#type').val(params.get('type'));
        }
        if (params.get('period')) {
            $('#period_' + params.get('period')).prop('checked', true);
        }
        if (params.get('rate')) {
            $('#rate_' + params.get('rate')).prop('checked', true);
        }
        if (params.get('query')) {
            $('#searchInput').val(params.get('query'));
        }
    }

    /**
     * @param user 로그인한 회원 이름. 비로그인이면 null — 만기 예상 금액 칸을 그리지 않는다
     */
    function renderProducts(productItems, user) {
        const html = productItems.map(function (item, index) {
            return '<li id="product_item" data-aos="fade-up" data-aos-delay="' + (200 * index) + '" data-aos-duration="400">'
                + '<div id="product">'
                + '<div class="sub">'
                + typeHtml(item.type)
                + '<div class="attribute">' + escapeHtml(item.attribute) + '</div>'
                + '</div>'
                + '<div class="title">'
                + '<div class="name">' + escapeHtml(item.name) + '</div>'
                + '<div class="explain">' + escapeHtml(item.explain) + '</div>'
                + '</div>'
                + '<div class="rate">'
                + '최고 연 <span class="max_rate"><span>' + formatRate(item.maxRate, 1) + '</span>%</span>'
                + (item.maxRatePeriod !== null ? ' (' + item.maxRatePeriod + '개월)' : '')
                + '</div>'
                + '</div>'
                + (user !== null ? estimateHtml(item.estimate, user) : '')
                + '<div class="content">'
                + '<button class="search_link_btn"><a href="' + escapeHtml(item.link) + '">자세히</a></button>'
                + '</div>'
                + '</li>';
        }).join('');

        $('#product_list > ul').html(html);
        AOS.refreshHard();
    }

    function typeHtml(type) {
        if (type === 'S') return '<div class="type">적금</div>';
        if (type === 'D') return '<div class="type">예금</div>';
        return '';
    }

    // 계산은 서버(MaturityCalculator)가 한다. 고른 기간·과세 구분과 회원이 충족한 우대금리가 반영된 금액이다.
    function estimateHtml(estimate, user) {
        if (estimate === null) {
            return '<div id="member" class="content">'
                + '이미 저축 목표만큼 납입하고 있어<br>'
                + '만기 예상 금액을 계산하지 않았습니다.'
                + '</div>';
        }
        return '<div id="member" class="content">'
            + '현재 ' + escapeHtml(user) + '님의 자산으로 계산된<br>'
            + '만기 예상 금액은<br>'
            + (params.get('rate') === 'none' ? '비과세' : '세후') + ' <span>' + formatNumber(estimate.total) + '</span>원 '
            + '입니다.<br>'
            + '(' + estimate.months + '개월 · 연 ' + formatRate(estimate.appliedRate, 2) + '% 적용)'
            + '</div>';
    }

    // 5 → "5.0" 처럼 소수 자리를 최소 digits 자리까지 보여 준다
    function formatRate(rate, digits) {
        const fixed = Number(rate).toFixed(digits);
        return String(rate).length > fixed.length ? String(rate) : fixed;
    }

    $('#searchInput').keyup(function (event) {
        if (event.which === 13) {
            event.preventDefault();
            $('form').submit();
        }
    });
});
