(() => {
    'use strict';
    const byId = id => document.getElementById(id);
    const base = byId('policy-ai').dataset.api;
    const select = byId('policy-select');
    const conversations = new Map();
    let policies = [], busy = false;
    const safeUrl = value => {
        try { const url = new URL(value); return ['https:', 'http:'].includes(url.protocol) ? url.href : null; }
        catch { return null; }
    };
    const status = text => { byId('status').textContent = text; };
    function controls() {
        select.disabled = busy || !policies.length;
        byId('policy-filter').disabled = busy || !policies.length;
        byId('reload').disabled = busy;
        ['ask', 'checklist', 'clear'].forEach(id => { byId(id).disabled = busy || !select.value; });
        byId('question').disabled = busy;
    }
    function render() {
        const area = byId('messages'); area.replaceChildren();
        for (const message of conversations.get(select.value) || []) {
            const article = document.createElement('article'); article.className = message.role;
            const title = document.createElement('strong'); title.textContent = message.role === 'user' ? '내 질문' : '정책 AI';
            const text = document.createElement('p'); text.textContent = message.content;
            article.append(title, text);
            if (message.result) {
                const result = message.result;
                const note = document.createElement('p'); note.className = 'notice';
                note.textContent = (result.uncited || result.status !== 'answered' ? '출처 검증이 충분하지 않습니다. 공식 공고를 확인해 주세요.' : '답변에 인용된 공식 자료')
                    + ' · 처리 시간 ' + (result.elapsed_ms / 1000).toFixed(1) + '초';
                article.append(note);
                for (const source of result.sources || []) {
                    const url = safeUrl(source.url); if (!url) continue;
                    const link = document.createElement('a'); link.href = url; link.target = '_blank'; link.rel = 'noopener noreferrer';
                    link.textContent = '[근거 ' + (source.number ?? '?') + '] ' + (source.title || '공식 자료') + (source.page > 0 ? ' · ' + source.page + '쪽' : '');
                    article.append(link);
                }
            }
            area.append(article);
        }
        const policy = policies.find(p => p.id === select.value);
        const url = safeUrl(policy?.source_url);
        const official = byId('official-link'); official.hidden = !url;
        if (url) official.href = url; else official.removeAttribute('href');
        controls();
    }
    function filter() {
        const previous = select.value;
        const term = byId('policy-filter').value.trim().toLocaleLowerCase();
        select.replaceChildren(new Option('정책을 선택해 주세요.', ''));
        for (const policy of policies.filter(p => (p.name + ' ' + p.region).toLocaleLowerCase().includes(term))) {
            select.add(new Option(policy.name + ' · ' + policy.region, policy.id));
        }
        select.value = [...select.options].some(o => o.value === previous) ? previous : '';
        render();
    }
    async function request(path, options = {}) {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), 130000);
        try {
            const response = await fetch(base + path, {...options, signal: controller.signal, credentials: 'same-origin'});
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || '요청을 처리할 수 없습니다.');
            return data;
        } finally { clearTimeout(timer); }
    }
    async function load() {
        busy = true; controls(); status('정책 목록을 불러오는 중입니다.');
        try { policies = await request('/policies'); filter(); status(policies.length + '개 정책 중 상담할 정책을 선택해 주세요.'); }
        catch (error) { status(error.message + ' 목록 새로고침으로 다시 시도할 수 있습니다.'); }
        finally { busy = false; controls(); }
    }
    async function ask(mode) {
        if (busy || !select.value) return;
        const question = mode === 'checklist' ? '이 정책의 신청 자격, 준비 서류, 신청 절차와 확인이 필요한 사항을 체크리스트로 정리해 주세요.' : byId('question').value.trim();
        if (!question) { status('질문을 입력해 주세요.'); return; }
        const id = select.value;
        const history = conversations.get(id) || [];
        busy = true; controls(); status('공식 자료를 확인하고 있습니다. 첫 상담은 시간이 걸릴 수 있습니다.');
        try {
            const result = await request('/ask', {method: 'POST', headers: {'Content-Type': 'application/json', 'X-Policy-CSRF': document.querySelector('meta[name="policy-csrf"]').content},
                body: JSON.stringify({policy_id: id, question, mode, history: history.slice(-8).map(m => ({role: m.role, content: m.content.slice(0, 3000)}))})});
            if (typeof result.text !== 'string') throw new Error('답변 형식을 확인할 수 없습니다.');
            conversations.set(id, [...history, {role: 'user', content: question}, {role: 'assistant', content: result.text, result}].slice(-40));
            if (mode === 'chat') byId('question').value = '';
            render(); status('답변을 확인해 주세요.');
        } catch (error) { status(error.name === 'AbortError' ? '응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.' : error.message); }
        finally { busy = false; controls(); }
    }
    byId('question-form').addEventListener('submit', e => { e.preventDefault(); ask('chat'); });
    byId('checklist').addEventListener('click', () => ask('checklist'));
    byId('clear').addEventListener('click', () => { conversations.delete(select.value); render(); status('현재 정책의 대화를 지웠습니다.'); });
    byId('policy-filter').addEventListener('input', filter);
    select.addEventListener('change', () => { byId('question').value = ''; status(''); render(); });
    byId('reload').addEventListener('click', load);
    load();
})();
