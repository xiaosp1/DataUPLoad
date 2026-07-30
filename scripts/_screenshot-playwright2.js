// W-PERF-C screenshot via Playwright
// Uses actual form login, then navigates to alarm + realtime
const { chromium } = require('playwright');
const path = require('path');

const BASE = 'http://127.0.0.1:8080';
const OUT_DIR = 'E:\\DEMO\\数据采集\\docs\\work-orders';
const OUT_ALARM = path.join(OUT_DIR, 'W-PERF-C-alarm.png');
const OUT_REALTIME = path.join(OUT_DIR, 'W-PERF-C-realtime.png');

async function formLogin(page) {
  console.log('  Filling login form...');
  await page.waitForTimeout(500);

  // Try finding inputs. The login page uses GlassInput which might render
  // differently.
  // Let's look at all input elements
  const inputCount = await page.locator('input').count();
  console.log('  input count:', inputCount);

  // Try filling by type
  const userInput = page.locator('input[type="text"], input:not([type="password"])').first();
  const pwdInput = page.locator('input[type="password"]').first();
  
  const userExists = (await userInput.count()) > 0;
  const pwdExists = (await pwdInput.count()) > 0;
  console.log('  user input found:', userExists, 'pwd input found:', pwdExists);

  if (userExists) {
    await userInput.fill('super_admin');
    console.log('  filled username');
  }
  if (pwdExists) {
    await pwdInput.fill('Abc12345');
    console.log('  filled password');
  }

  // Try all buttons
  const buttons = page.locator('button');
  const btnCount = await buttons.count();
  console.log('  button count:', btnCount);
  
  // Click the login button — usually contains "登" or "Log"
  let clicked = false;
  for (let i = 0; i < btnCount; i++) {
    const text = await buttons.nth(i).textContent();
    console.log('  button', i, 'text:', JSON.stringify(text));
    if (text.includes('登') || text.includes('Log') || text.includes('log')) {
      await buttons.nth(i).click();
      clicked = true;
      console.log('  clicked button', i);
      break;
    }
  }

  if (!clicked) {
    console.log('  no login button found, trying first button');
    if (btnCount > 0) {
      await buttons.first().click();
      clicked = true;
    }
  }

  // Wait for navigation
  await page.waitForTimeout(3000);
  console.log('  after login URL:', page.url());
  return page.url();
}

(async () => {
  // Clean old files
  try { require('fs').unlinkSync(OUT_ALARM); } catch (e) {}
  try { require('fs').unlinkSync(OUT_REALTIME); } catch (e) {}

  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    locale: 'zh-CN',
  });

  const page = await ctx.newPage();
  page.on('console', (msg) => {
    if (msg.type() === 'error') console.log('  [console.error]', msg.text().substring(0, 250));
  });
  page.on('pageerror', (err) => console.log('  [pageerror]', err.message.substring(0, 250)));

  console.log('[1] Loading /#/login...');
  await page.goto(BASE + '/#/login', { waitUntil: 'networkidle', timeout: 15000 });
  await page.waitForTimeout(1000);

  const url = await formLogin(page);

  if (url.includes('login')) {
    console.log('[2] Still on login page — trying alternate form approach...');
    // Maybe inputs are inside a shadow DOM or different. Let's just try
    // setting the cookie from the Node.js side after getting it via fetch
    const crypto = require('crypto');
    const pw = crypto.createHash('sha256').update('Abc12345').digest('hex');
    const r = await fetch(BASE + '/web/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'super_admin', password: pw }),
    });
    const setCookie = r.headers.get('set-cookie') || '';
    const m = setCookie.match(/satoken=([^;]+)/);
    if (m) {
      const token = m[1];
      console.log('  Got satoken via Node.js fetch:', token.substring(0, 8) + '...');
      await ctx.addCookies([
        { name: 'satoken', value: token, domain: '127.0.0.1', path: '/', httpOnly: false, sameSite: 'Lax' },
      ]);
      console.log('  Cookie added to browser context via API');
    } else {
      console.log('  No satoken found in Set-Cookie');
      // Read response body
      const body = await r.text();
      console.log('  Login response:', body.substring(0, 200));
    }
  }

  // Navigate to alarm
  console.log('[3] Navigating to /#/alarm...');
  await page.goto(BASE + '/#/alarm', { waitUntil: 'networkidle', timeout: 30000 });
  await page.waitForTimeout(3000);

  let title = await page.title();
  let url2 = page.url();
  console.log('  title:', title, 'url:', url2);
  
  const bodyText = await page.locator('body').innerText();
  console.log('  body (first 500):', bodyText.substring(0, 500).replace(/\n/g, ' | '));

  await page.screenshot({ path: OUT_ALARM, fullPage: false });
  console.log('  alarm.png saved');

  // Navigate to realtime
  console.log('[4] Navigating to /#/realtime...');
  await page.goto(BASE + '/#/realtime', { waitUntil: 'networkidle', timeout: 30000 });
  await page.waitForTimeout(3000);

  title = await page.title();
  url2 = page.url();
  console.log('  title:', title, 'url:', url2);
  
  const bodyText2 = await page.locator('body').innerText();
  console.log('  body (first 500):', bodyText2.substring(0, 500).replace(/\n/g, ' | '));

  await page.screenshot({ path: OUT_REALTIME, fullPage: false });
  console.log('  realtime.png saved');

  await browser.close();

  // Compare files
  const fs = require('fs');
  const crypto = require('crypto');
  if (fs.existsSync(OUT_ALARM) && fs.existsSync(OUT_REALTIME)) {
    const h1 = crypto.createHash('sha256').update(fs.readFileSync(OUT_ALARM)).digest('hex');
    const h2 = crypto.createHash('sha256').update(fs.readFileSync(OUT_REALTIME)).digest('hex');
    console.log('\nalarm sha:', h1.substring(0, 16));
    console.log('realtime sha:', h2.substring(0, 16));
    console.log('Files are', h1 === h2 ? 'IDENTICAL (WARNING)' : 'DIFFERENT ✓');
  }

  console.log('\nDone');
  process.exit(0);
})().catch((e) => {
  console.error('FATAL:', e.message);
  process.exit(2);
});
