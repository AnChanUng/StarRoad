/**
 * 화면 공통 스크립트.
 *
 * 화면(JSP)은 데이터를 담지 않고 뜬다. 뜬 뒤에 api.get(...) 등으로 /api/starroad/** 를 호출해
 * 받은 JSON 으로 화면을 그린다.
 */

/**
 * API 호출. 성공하면 응답 JSON 을(본문이 없으면 null), 실패하면 ApiError 를 던진다.
 * 로그인이 필요한 API(401)는 로그인 화면으로 보낸다 — 단, 호출할 때 {redirectOn401: false} 를 주면 보내지 않는다.
 */
const api = (function () {

    function ApiError(status, message) {
        this.status = status;
        this.message = message;
    }

    async function request(method, url, body, options) {
        const init = {method: method, headers: {}, credentials: 'same-origin'};

        if (body instanceof FormData) {
            init.body = body;                       // multipart — Content-Type 은 브라우저가 붙인다
        } else if (body !== undefined && body !== null) {
            init.headers['Content-Type'] = 'application/json';
            init.body = JSON.stringify(body);
        }

        const response = await fetch(url, init);
        const text = await response.text();
        let data = null;
        try {
            data = text ? JSON.parse(text) : null;
        } catch (e) {
            data = null;
        }

        if (response.ok) {
            return data;
        }

        const message = (data && data.message) ? data.message : '요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.';
        if (response.status === 401 && !(options && options.redirectOn401 === false)) {
            alert(message);
            location.href = '/starroad/login';
        }
        throw new ApiError(response.status, message);
    }

    return {
        get: function (url, options) { return request('GET', url, null, options); },
        post: function (url, body, options) { return request('POST', url, body, options); },
        put: function (url, body, options) { return request('PUT', url, body, options); },
        del: function (url, options) { return request('DELETE', url, null, options); }
    };
})();

/** 서버에서 받은 글자를 HTML 에 넣기 전에 반드시 거친다 (XSS 방지) */
function escapeHtml(value) {
    if (value === null || value === undefined) return '';
    return String(value).replace(/[&<>"']/g, function (c) {
        return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c];
    });
}

function pad2(n) {
    return n < 10 ? '0' + n : '' + n;
}

/** ISO 날짜 문자열 → yyyy-MM-dd */
function formatDate(value) {
    if (!value) return '';
    const d = new Date(value);
    return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());
}

/** ISO 날짜 문자열 → yyyy-MM-dd HH:mm */
function formatDateTime(value) {
    if (!value) return '';
    const d = new Date(value);
    return formatDate(value) + ' ' + pad2(d.getHours()) + ':' + pad2(d.getMinutes());
}

/** 12345678 → 12,345,678 */
function formatNumber(value) {
    return Number(value).toLocaleString('ko-KR');
}

/** 주소창의 ?name=value 값. 없으면 null */
function getQueryParam(name) {
    return new URLSearchParams(location.search).get(name);
}

/**
 * 페이지 번호 목록을 그린다.
 * @param container  <ul class="pagination"> 요소
 * @param currentPage 현재 페이지 (1부터)
 * @param pageEndIndex 마지막 페이지
 * @param onMove      이동할 페이지 번호를 받는 함수
 */
function renderPagination(container, currentPage, pageEndIndex, onMove) {
    let html = '<li class="page-item"><a class="page-link" href="#" data-page="' + (currentPage - 1)
        + '" aria-label="Previous"><span aria-hidden="true">&lt;</span></a></li>';
    for (let i = 1; i <= pageEndIndex; i++) {
        const style = (i === currentPage)
            ? ' style="color:#FFCC00FF;text-decoration:underline;font-weight:bold"' : '';
        html += '<li class="page-item"><a class="page-link" href="#" data-page="' + i + '"' + style + '>' + i + '</a></li>';
    }
    html += '<li class="page-item"><a class="page-link" href="#" data-page="' + (currentPage + 1)
        + '" aria-label="Next"><span aria-hidden="true">&gt;</span></a></li>';
    container.innerHTML = html;

    container.querySelectorAll('.page-link').forEach(function (link) {
        link.addEventListener('click', function (event) {
            event.preventDefault();
            const page = Number(link.getAttribute('data-page'));
            if (page >= 1 && page <= pageEndIndex && page !== currentPage) {
                onMove(page);
            }
        });
    });
}
