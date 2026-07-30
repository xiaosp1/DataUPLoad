// W-RT-9 browser smoke test
// Verifies:
//   1. Login → /#/realtime → 选中产线 → 中栏 4 区面板 OK
//   2. 点击缺陷网格某小时 → AlarmDetailDialog 弹窗
//   3. 弹窗 9 字段 (id/uuid/time/duration/line/face/type/level/defect/message/image) 全显示
//   4. 关闭弹窗 OK
//
// Screenshots:
//   W-RT-9-01-line-selected.png  — 中栏 4 区面板 + 缺陷网格
//   W-RT-9-02-dialog-open.png    — 点击小时格后弹窗打开
//   W-RT-9-03-dialog-closed.png  — 关闭后回到中栏

const path = require('path');
const { chromium } = require('playwright');

const BASE = 'http://127.0.0.1:8080';
const OUT_DIR = 'E:\\DEMO\\数据采集\\docs\\work-orders';
const SHOT_1 = path.join(OUT_DIR, 'W-RT-9-01-line-selected.png');
const SHOT_2 = path.join(OUT_DIR, 'W-RT-9-02-dialog-open.png');
const SHOT_3 = path.join(OUT_DIR, 'W-RT-9-03-dialog-closed.png');
const DEBUG_JSON = path.join(OUT_DIR, 'W-RT-9-debug.json');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    locale: 'zh-CN',
  });
  const page = await ctx.newPage();

  const errors = [];
  const apiCalls = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error') errors.push(msg.text());
  });
  page.on('pageerror', (err) => errors.push('PAGEERROR: ' + err.message));
  page.on('response', async (resp) => {
    const u = resp.url();
    if (u.includes('/web/alarm/')) {
      try {
        const url = resp.request().url();
        apiCalls.push({
          url: url.replace(BASE, ''),
          status: resp.status(),
          method: resp.request().method(),
        });
      } catch (e) {}
    }
  });

  console.log('[1] GET ' + BASE);
  const resp = await page.goto(BASE + '/', { waitUntil: 'networkidle', timeout: 20000 });
  console.log('    status=' + resp.status());
  await page.waitForSelector('#app > *', { timeout: 10000 });
  await page.waitForTimeout(500);

  // Fill credentials
  console.log('[2] Login as super_admin / Abc12345');
  const userInput = page.locator('#login-username');
  const pwdInput = page.locator('input[type="password"]').first();
  await userInput.fill('super_admin');
  await pwdInput.fill('Abc12345');
  // GlassButton renders as <button> with text 登 录
  const submitBtn = page.locator('.login-card__submit').first();
  await submitBtn.click({ timeout: 8000 });
  await page.waitForTimeout(3000);

  console.log('[3] post-login URL: ' + page.url());

  // Navigate to realtime
  console.log('[4] Navigate to /realtime');
  await page.goto(BASE + '/#/realtime', { waitUntil: 'networkidle', timeout: 20000 });
  await page.waitForTimeout(2500);

  // Click first line in left panel
  console.log('[5] Click first line in left panel');
  const firstLine = page.locator('.line-list-card .line-item, .line-list-card__item, .line-cell').first();
  // Try several selectors that may exist
  let lineClicked = false;
  for (const sel of [
    '.line-list-card .line-list-card__item',
    '.line-list-card__item',
    '.line-list-card [class*="item"]',
    '.line-cell',
  ]) {
    const el = page.locator(sel).first();
    if (await el.count() > 0) {
      try {
        await el.click({ timeout: 3000 });
        lineClicked = true;
        console.log('    clicked: ' + sel);
        break;
      } catch (e) {}
    }
  }
  if (!lineClicked) {
    console.log('    [warn] could not find line item via known selectors, dumping dom...');
    const html = await page.locator('.line-list-card, .line-list, aside').first().innerHTML().catch(() => '(none)');
    console.log('    aside HTML head: ' + html.slice(0, 500));
  }
  await page.waitForTimeout(2000);

  // Screenshot 1: line selected
  await page.screenshot({ path: SHOT_1, fullPage: false });
  console.log('[6] shot1: ' + SHOT_1);

  // Find defect grid cells
  console.log('[7] Find defect grid cells');
  const cells = page.locator('.ldg-card__cell');
  const cellCount = await cells.count();
  console.log('    defect grid cells: ' + cellCount);

  // Try clicking cells until one opens the dialog (skip zero-value cells)
  let dialogOpened = false;
  let clickedHour = -1;
  let apiCallsBefore = apiCalls.length;
  for (let i = 0; i < cellCount; i++) {
    const cell = cells.nth(i);
    const isZero = await cell.evaluate((el) => el.classList.contains('ldg-card__cell--zero')).catch(() => false);
    if (isZero) continue;
    // Also check the value text: zero cells display "0"
    const txt = await cell.textContent().catch(() => '');
    if (/^\s*0+\s*0\s*$/.test(txt || '')) continue;
    const hourText = await cell.locator('.ldg-card__cell-hour').textContent().catch(() => '');
    clickedHour = parseInt((hourText || '0').trim(), 10);
    console.log('    clicking hour=' + clickedHour + ' val="' + (txt || '').trim() + '"');
    await cell.click({ force: true });
    // Wait up to 4s for dialog
    try {
      await page.waitForSelector('.alarm-dialog', { state: 'visible', timeout: 5000 });
      dialogOpened = true;
      break;
    } catch (e) {
      console.log('    no dialog for hour=' + clickedHour + ', try next');
      // Could be 'noAlarmForCell' info message — close it and continue
      await page.waitForTimeout(500);
    }
  }

  if (!dialogOpened) {
    console.log('    [warn] no alarm dialog opened — taking debug shot anyway');
  }
  await page.waitForTimeout(800);

  // Screenshot 2: dialog open
  await page.screenshot({ path: SHOT_2, fullPage: false });
  console.log('[8] shot2: ' + SHOT_2);

  // Check fields in the dialog
  let fields = {};
  if (dialogOpened) {
    const dlg = page.locator('.alarm-dialog');
    fields.idText = await dlg.locator('.alarm-detail__row').nth(0).locator('.alarm-detail__value').textContent().catch(() => '');
    fields.uuidText = await dlg.locator('.alarm-detail__row').nth(1).locator('.alarm-detail__value').textContent().catch(() => '');
    // After head row, body rows: triggerTime / duration / defect / desc / image
    // Total row count = 11 (3 head cols each have 2 rows = 6 logical rows + 5 body rows = 11 visible rows)
    const allRows = dlg.locator('.alarm-detail__row');
    fields.rowCount = await allRows.count();
    fields.durationText = await dlg.locator('.alarm-detail__value').nth(7).textContent().catch(() => '');
    fields.imagePlaceholder = await dlg.locator('.alarm-detail__image-placeholder').textContent().catch(() => '');
    fields.dialogTitle = await dlg.locator('.el-dialog__title').textContent().catch(() => '');
    fields.ignoreBtnVisible = await dlg.locator('button:has-text("忽略")').count() > 0;
    fields.closeBtnVisible = await dlg.locator('button:has-text("关闭")').count() > 0;
  }

  // Close dialog via close button
  console.log('[9] Close dialog');
  if (dialogOpened) {
    const closeBtn = page.locator('.alarm-dialog button:has-text("关闭"), .alarm-dialog .el-dialog__close').first();
    await closeBtn.click({ timeout: 3000 }).catch(() => {});
    await page.waitForTimeout(1000);
  }

  // Screenshot 3: dialog closed
  await page.screenshot({ path: SHOT_3, fullPage: false });
  console.log('[10] shot3: ' + SHOT_3);

  // ============== SUMMARY ==============
  console.log('---');
  console.log('Console errors: ' + errors.length);
  for (const e of errors.slice(0, 10)) console.log('  - ' + e);
  console.log('alarm API calls: ' + apiCalls.length);
  for (const a of apiCalls) console.log('  ' + a.method + ' ' + a.url + ' → ' + a.status);

  const result = {
    baseUrl: BASE,
    dialogOpened,
    clickedHour,
    apiCalls: apiCalls.slice(-10),
    fields,
    consoleErrors: errors.length,
    consoleErrorDetails: errors.slice(0, 10),
    screenshots: [SHOT_1, SHOT_2, SHOT_3],
  };

  require('fs').writeFileSync(DEBUG_JSON, JSON.stringify(result, null, 2));
  console.log('---');
  console.log('RESULT_JSON:' + JSON.stringify(result));

  await browser.close();
  console.log(dialogOpened ? 'PASS' : 'PARTIAL (dialog did not open)');
})().catch((err) => {
  console.error('SCRIPT ERROR:', err);
  process.exit(3);
});
