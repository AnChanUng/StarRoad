// Run with Node + Playwright installed. This is a browser contract test with stubbed API responses.
const {chromium} = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const web = path.join(root, 'src/main/webapp');
(async () => {
    const browser = await chromium.launch({headless: true, channel: 'msedge'});
    try {
        const page = await browser.newPage({viewport: {width: 1280, height: 900}});
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        let requests = [], failure = false;
        const html = fs.readFileSync(path.join(web, 'WEB-INF/views/policy/consultation.jsp'), 'utf8')
            .replace(/<%@[^%]*%>/g, '').replaceAll('${pageContext.request.contextPath}', '').replaceAll('${policyAiCsrf}', 'test-csrf');
        await page.route('http://starroad.test/**', async route => {
            const url = new URL(route.request().url());
            if (url.pathname === '/starroad/policy/ai') return route.fulfill({contentType: 'text/html', body: html});
            if (url.pathname === '/starroad/policy/ai/policies') return route.fulfill({json: [
                {id: '001-a', name: '주거 지원 정책', region: '서울', source_url: 'https://example.org/policy'},
                {id: '002-b', name: '취업 준비 지원', region: '경기', source_url: 'https://example.org/jobs'}]});
            if (url.pathname === '/starroad/policy/ai/ask') {
                const request = route.request().postDataJSON(); requests.push(request);
                assert.equal(route.request().headers()['x-policy-csrf'], 'test-csrf');
                if (failure) return route.fulfill({status: 504, json: {message: '응답 시간이 초과되었습니다.'}});
                return route.fulfill({json: {text: '<img src=x onerror=alert(1)> 준비 서류 [근거 1]', uncited: false,
                    status: 'answered', elapsed_ms: 1234, sources: [
                        {number: 1, title: '공식 공고', url: 'https://example.org/notice', page: 2},
                        {number: 2, title: 'invalid link', url: 'javascript:alert(1)'}]}});
            }
            if (url.pathname.startsWith('/resources/')) {
                const file = path.join(web, url.pathname);
                return route.fulfill({body: fs.readFileSync(file), contentType: file.endsWith('.js') ? 'application/javascript' : 'text/css'});
            }
            return route.abort();
        });
        await page.goto('http://starroad.test/starroad/policy/ai');
        await page.locator('#policy-select option[value="001-a"]').waitFor({state: 'attached'});
        await page.selectOption('#policy-select', '001-a');
        await page.fill('#question', '지원 대상은?'); await page.click('#ask');
        await page.waitForFunction(() => document.querySelectorAll('#messages article').length === 2);
        assert.equal(await page.locator('#messages img').count(), 0);
        assert.equal(await page.locator('#messages a').count(), 1);
        await page.selectOption('#policy-select', '002-b');
        assert.equal(await page.locator('#messages article').count(), 0);
        await page.click('#checklist');
        await page.waitForFunction(() => document.querySelectorAll('#messages article').length === 2);
        assert.equal(requests[1].mode, 'checklist'); assert.deepEqual(requests[1].history, []);
        await page.selectOption('#policy-select', '001-a');
        assert.equal(await page.locator('#messages article').count(), 2);
        await page.fill('#question', '후속 질문'); await page.click('#ask');
        await page.waitForFunction(() => document.querySelectorAll('#messages article').length === 4);
        assert.equal(requests[2].history.length, 2); assert.equal(requests[2].policy_id, '001-a');
        failure = true;
        await page.fill('#question', '실패해도 질문 유지'); await page.click('#ask');
        await page.getByRole('status').filter({hasText: '응답 시간이 초과'}).waitFor();
        assert.equal(await page.inputValue('#question'), '실패해도 질문 유지');
        assert.equal(await page.locator('#messages article').count(), 4);
        await page.click('#clear'); assert.equal(await page.locator('#messages article').count(), 0);
        await page.selectOption('#policy-select', '002-b');
        assert.equal(await page.locator('#messages article').count(), 2);
        if (process.env.UI_SCREENSHOT) await page.screenshot({path: process.env.UI_SCREENSHOT, fullPage: true});
        await page.setViewportSize({width: 390, height: 844});
        assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
        assert.deepEqual(errors, []);
        console.log('PASS: policy isolation, checklist/history contract, safe rendering, failure recovery, clear, mobile overflow');
    } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
