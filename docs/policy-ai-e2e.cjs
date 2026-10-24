// Requires real Spring on 18080 and policy-ai-fixture.py on 18001.
const {chromium} = require('playwright');
const assert = require('node:assert/strict');
(async () => {
  const browser = await chromium.launch({headless: true, channel: 'msedge'});
  try {
    const page = await browser.newPage();
    const errors = []; page.on('pageerror', e => errors.push(e.message));
    const response = await page.goto('http://127.0.0.1:18080/starroad/policy/ai');
    assert.equal(response.status(), 200);
    await page.waitForFunction(() => document.querySelector('#policy-select').options.length > 100);
    const ids = await page.locator('#policy-select option').evaluateAll(options => options.slice(1,3).map(o=>o.value));
    await page.selectOption('#policy-select', ids[0]);
    await page.fill('#question', '지원 대상은?'); await page.click('#ask');
    await page.waitForFunction(() => document.querySelectorAll('#messages article').length === 2);
    assert.equal(await page.locator('#messages a').count(), 1);
    await page.selectOption('#policy-select', ids[1]);
    assert.equal(await page.locator('#messages article').count(), 0);
    await page.click('#checklist');
    await page.waitForFunction(() => document.querySelectorAll('#messages article').length === 2);
    assert.ok((await page.locator('#messages').textContent()).includes('신청 체크리스트 테스트 응답'));
    await page.selectOption('#policy-select', ids[0]);
    assert.equal(await page.locator('#messages article').count(), 2);
    await page.fill('#question', 'TEST_SERVICE_FAILURE'); await page.click('#ask');
    await page.waitForFunction(() => document.querySelector('#status').textContent.includes('상담을 완료하지 못했습니다'));
    assert.equal(await page.inputValue('#question'), 'TEST_SERVICE_FAILURE');
    const csrf = await page.locator('meta[name="policy-csrf"]').getAttribute('content');
    const payload = {policy_id:ids[0],question:'질문',mode:'chat',history:[]};
    const denied = await page.request.post('http://127.0.0.1:18080/starroad/policy/ai/ask',{data:payload});
    assert.equal(denied.status(),403);
    const missing = await page.request.post('http://127.0.0.1:18080/starroad/policy/ai/ask',{
      data:{...payload,policy_id:'missing'},headers:{'X-Policy-CSRF':csrf}});
    assert.equal(missing.status(),404);
    const anonymous = await page.request.get('http://127.0.0.1:18001/policies');
    assert.equal(anonymous.status(),401);
    const body = {request_id: 'dedup-000000000000001', request: payload};
    const attempts = await Promise.all(Array.from({length:8}, () => page.request.post('http://127.0.0.1:18080/starroad/policy/ai/jobs', {
      data:body, headers:{'X-Policy-CSRF':csrf}})));
    assert.ok(attempts.every(r => r.status() === 202));
    const submitted = await Promise.all(attempts.map(r=>r.json()));
    assert.equal(new Set(submitted.map(j=>j.job_id)).size, 1);
    const other = await browser.newContext();
    await other.request.get('http://127.0.0.1:18080/starroad/policy/ai');
    const foreign = await other.request.get('http://127.0.0.1:18080/starroad/policy/ai/jobs/' + submitted[0].job_id);
    assert.equal(foreign.status(),404);
    await other.close();
    const changed = await page.request.post('http://127.0.0.1:18080/starroad/policy/ai/jobs', {
      data:{...body,request:{...payload,question:'changed'}},headers:{'X-Policy-CSRF':csrf}});
    assert.equal(changed.status(),409);
    assert.deepEqual(errors,[]);
    console.log('PASS real HTTP: JSP -> Spring -> authenticated FastAPI -> catalog/model fixture; chat/checklist/sources/isolation/failed-state/404/CSRF/401/parallel-idempotency/foreign-owner/409');
  } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
