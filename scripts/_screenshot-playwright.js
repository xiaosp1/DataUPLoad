// W-PERF-C screenshot via Playwright (chromium)
// Uses full SPA navigation control: set cookie, route, wait for data.
const { chromium } = require('playwright');
const crypto = require('crypto');
const path = require('path');

const BASE = 'http://127.0.0.1:8080';
const OUT_DIR = 'E:\\DEMO\\数据采集\\docs\\work-orders';
const OUT_ALARM = path.join(OUT_DIR, 'W-PERF-C-alarm.png');
const OUT_REALTIME = path.join(OUT_DIR, 'W-PERF-C-realtime.png');

async function login() {
  const pw = crypto.createHash('sha256').update('Abc12345').digest('hex');
  const r = await fetch(BASE + '/web/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'super_admin', password: pw }),
  });
  const setCookie = r.headers.get('set-cookie') || '';
  const m = setCookie.match(/satoken=([^;]+)/);
  if (!m) throw new Error('No satoken');
  console.log('satoken: ' + m[1].substring(0, 8) + '...');
  return m[1];
}

async function shoot(token, targetHash, outPath, label) {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    locale: 'zh-CN',
  });
  const page = await ctx.newPage();

  // Collect console errors
  const errors = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error') errors.push(msg.text());
  });
  page.on('pageerror', (err) => errors.push('PAGEERROR: ' + err.message));

  // Set cookie BEFORE navigating
  await ctx.addCookies([
    {
      name: 'satoken',
      value: token,
      domain: '127.0.0.1',
      path: '/',
      httpOnly: false,
      sameSite: 'Lax',
    },
  ]);

  console.log(`[${label}] navigating to ${BASE}/${targetHash}`);
  await page.goto(`${BASE}/${targetHash}`, {
    waitUntil: 'networkidle',
    timeout: 30000,
  });

  // Give a bit more time for async rendering
  await page.waitForTimeout(2000);

  await page.screenshot({ path: outPath, fullPage: false });
  const sz = require('fs').statSync(outPath).size;
  console.log(`[${label}] screenshot: ${outPath} (${sz} bytes)`);

  // Check what the page shows
  const title = await page.title();
  const url = page.url();
  console.log(`[${label}] title='${title}' url='${url}'`);

  // Try to find some content markers
  try {
    const bodyText = await page.locator('body').innerText();
    console.log(`[${label}] body text (first 300): '${bodyText.substring(0, 300).replace(/\n/g, ' | ')}'`);
  } catch (e) {
    console.log(`[${label}] body text unreadable: ${e.message}`);
  }

  await browser.close();
  console.log(`[${label}] console errors: ${errors.length}`);
  return sz;
}

(async () => {
  let token;
  try {
    token = await login();
  } catch (e) {
    console.error('LOGIN FAILED:', e.message);
    process.exit(1);
  }

  // Remove old screenshots
  try { require('fs').unlinkSync(OUT_ALARM); } catch (e) {}
  try { require('fs').unlinkSync(OUT_REALTIME); } catch (e) {}

  const alarm = await shoot(token, '#/alarm', OUT_ALARM, 'ALARM');
  const realtime = await shoot(token, '#/realtime', OUT_REALTIME, 'REALTIME');

  console.log('\n=== RESULT ===');
  console.log('alarm.png:', alarm, 'bytes');
  console.log('realtime.png:', realtime, 'bytes');

  const h1 = require('crypto').createHash('sha256').update(require('fs').readFileSync(OUT_ALARM)).digest('hex');
  const h2 = require('crypto').createHash('sha256').update(require('fs').readFileSync(OUT_REALTIME)).digest('hex');
  console.log('alarm sha:', h1.substring(0, 16));
  console.log('realtime sha:', h2.substring(0, 16));
  console.log('identical:', h1 === h2 ? 'YES (WARNING)' : 'NO (OK)');

  process.exit(0);
})().catch((e) => {
  console.error('FATAL:', e.message);
  process.exit(2);
});
