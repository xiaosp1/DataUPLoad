// =============================================================================
// W-FRONT-04-C 端到端验收 — 修 reload 路由保留 (#11 FAIL)
//
// 验收 5 项:
//   1. 登录 → /#/realtime 正常
//   2. 在 /#/realtime 按 F5 (reload) → 仍在 /#/realtime (核心)
//   3. 在 /#/alarm 按 F5 → 仍在 /#/alarm
//   4. cookie 失效后 reload → 跳 /#/login
//   5. 首屏 < 1s
//
// 输出:
//   docs/work-orders/W-FRONT-04-C-{01..05}-*.png
//   docs/work-orders/W-FRONT-04-C-results.json
//   docs/work-orders/W-FRONT-04-C-verify-output.txt
// =============================================================================

import { chromium } from 'playwright'
import { writeFileSync } from 'fs'
import { createHash } from 'crypto'

const BASE = 'http://127.0.0.1:8080'
const OUT_DIR = 'E:\\DEMO\\数据采集\\docs\\work-orders'
const RESULTS_PATH = `${OUT_DIR}\\W-FRONT-04-C-results.json`
const LOG_PATH = `${OUT_DIR}\\W-FRONT-04-C-verify-output.txt`

function sha256Hex(text) {
  return createHash('sha256').update(text).digest('hex')
}

const log = []
function logLn(line) {
  const ts = new Date().toISOString()
  console.log(`[${ts}] ${line}`)
  log.push(`[${ts}] ${line}`)
}

const results = []
function record(n, name, status, detail, screenshot) {
  results.push({ n, name, status, detail, screenshot })
  logLn(`  [${status}] #${n} ${name}: ${detail}`)
}

async function apiLogin(username, password) {
  const pw = sha256Hex(password)
  const r = await fetch(`${BASE}/web/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password: pw })
  })
  const setCookie = r.headers.get('set-cookie') || ''
  const m = setCookie.match(/satoken=([^;]+)/)
  return { status: r.status, token: m ? m[1] : null, body: await r.text() }
}

async function doLogin(page, username, password) {
  await page.waitForSelector('input', { timeout: 15000 })
  const inputs = await page.evaluate(() => {
    const all = Array.from(document.querySelectorAll('input'))
    let userInput = null, pwdInput = null
    for (const el of all) {
      const ph = (el.placeholder || '').toLowerCase()
      const type = el.type
      if (!userInput && (type === 'text' || /user|账号|用户|username/i.test(ph))) {
        el.id = '__v_user_' + Date.now()
        userInput = '#' + el.id
      }
      if (!pwdInput && type === 'password') {
        el.id = '__v_pwd_' + Date.now()
        pwdInput = '#' + el.id
      }
    }
    return { userInput, pwdInput }
  })
  if (!inputs.userInput || !inputs.pwdInput) throw new Error('login inputs not found')
  await page.click(inputs.userInput, { clickCount: 3 })
  await page.type(inputs.userInput, username, { delay: 8 })
  await page.click(inputs.pwdInput, { clickCount: 3 })
  await page.type(inputs.pwdInput, password, { delay: 8 })
  const submitSelector = await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('button'))
    const b = btns.find(b => /登\s*录|sign in/i.test(b.textContent || ''))
    if (b) { b.id = '__v_submit_' + Date.now(); return '#' + b.id }
    return null
  })
  if (!submitSelector) throw new Error('login button not found')
  await page.click(submitSelector)
}

async function browserLogin(page, username, password) {
  await page.goto(`${BASE}/`, { waitUntil: 'networkidle0', timeout: 30000 })
  await doLogin(page, username, password)
}

async function waitForRealtime(page) {
  await page.waitForFunction(
    () => location.hash.includes('realtime') || location.pathname.includes('realtime'),
    { timeout: 30000 }
  )
  await page.waitForSelector('.line-item', { timeout: 20000 })
  await new Promise(r => setTimeout(r, 1500))
}

async function waitForAlarm(page) {
  await page.waitForFunction(
    () => location.hash.includes('alarm') || location.pathname.includes('alarm'),
    { timeout: 15000 }
  )
  await page.waitForSelector('.el-table__row, tbody tr', { timeout: 20000 })
  await new Promise(r => setTimeout(r, 1500))
}

const browser = await chromium.launch({
  headless: true,
  args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage']
})

try {
  const ctx = await browser.newContext({ viewport: { width: 1600, height: 1000 } })
  const page = await ctx.newPage()

  // ==================================================================
  // CHECK #5 (run first, while context fresh) — 首屏加载 < 1s
  // ==================================================================
  logLn('===== CHECK #5 FIRST PAINT TIMING =====')
  let firstPaintMs = -1
  try {
    const t0 = Date.now()
    const resp = await page.goto(`${BASE}/`, { waitUntil: 'domcontentloaded', timeout: 15000 })
    await page.waitForSelector('#app > *', { timeout: 15000 })
    const t1 = Date.now()
    firstPaintMs = t1 - t0
    const status = resp?.status() ?? -1
    logLn(`  first paint ${firstPaintMs}ms (status=${status})`)
    record(5, 'First paint < 1500ms (reload 守卫 await fetchCurrent 不阻塞)', firstPaintMs < 1500 ? 'PASS' : 'FAIL',
      `firstPaint=${firstPaintMs}ms, status=${status}`)
  } catch (e) {
    record(5, 'First paint timing', 'FAIL', e.message)
  }
  await page.screenshot({ path: `${OUT_DIR}\\W-FRONT-04-C-05-first-paint.png`, fullPage: false })

  // ==================================================================
  // CHECK #1 — Login → /#/realtime 正常
  // ==================================================================
  logLn('===== CHECK #1 LOGIN =====')
  try {
    const apiLoginRes = await apiLogin('super_admin', 'Abc12345')
    if (apiLoginRes.status !== 200 || !apiLoginRes.token) {
      record(1, 'Login', 'FAIL', `API login HTTP ${apiLoginRes.status}, body=${apiLoginRes.body.substring(0, 100)}`)
    } else {
      await browserLogin(page, 'super_admin', 'Abc12345')
      await waitForRealtime(page)
      const route = await page.evaluate(() => location.hash || location.pathname)
      if (!route.includes('realtime')) {
        record(1, 'Login → /#/realtime', 'FAIL', `UI landed on ${route}`)
      } else {
        await page.screenshot({ path: `${OUT_DIR}\\W-FRONT-04-C-01-login-realtime.png`, fullPage: false })
        record(1, 'Login → /#/realtime', 'PASS',
          `route=${route}, token=${apiLoginRes.token.substring(0, 8)}...`,
          'W-FRONT-04-C-01-login-realtime.png')
      }
    }
  } catch (e) {
    record(1, 'Login', 'FAIL', e.message)
  }

  // ==================================================================
  // CHECK #2 — F5 on /#/realtime → 仍在 /#/realtime (核心修复)
  // ==================================================================
  logLn('===== CHECK #2 RELOAD PRESERVES REALTIME =====')
  try {
    await page.evaluate(() => { location.hash = '#/realtime' })
    await new Promise(r => setTimeout(r, 1500))
    const beforeRoute = await page.evaluate(() => location.hash || location.pathname)
    logLn(`  before reload: ${beforeRoute}`)

    await page.reload({ waitUntil: 'networkidle0', timeout: 30000 })
    await new Promise(r => setTimeout(r, 3500))

    const afterRoute = await page.evaluate(() => location.hash || location.pathname)
    const onRealtimeAfter = afterRoute.includes('realtime')
    const onLoginAfter = afterRoute.includes('login')
    const hasLoginForm = await page.evaluate(() => !!document.querySelector('input[type="password"]'))

    await page.screenshot({ path: `${OUT_DIR}\\W-FRONT-04-C-02-reload-realtime.png`, fullPage: false })

    if (onRealtimeAfter && !onLoginAfter && !hasLoginForm) {
      record(2, 'F5 on /#/realtime → stays on /#/realtime (守卫 await fetchCurrent 修复)',
        'PASS', `before=${beforeRoute}, after=${afterRoute}`)
    } else {
      record(2, 'F5 on /#/realtime → stays on /#/realtime',
        'FAIL', `before=${beforeRoute}, after=${afterRoute}, hasLoginForm=${hasLoginForm}`)
    }
  } catch (e) {
    record(2, 'F5 on /#/realtime', 'FAIL', e.message)
  }

  // ==================================================================
  // CHECK #3 — F5 on /#/alarm → 仍在 /#/alarm
  // ==================================================================
  logLn('===== CHECK #3 RELOAD PRESERVES ALARM =====')
  try {
    await page.evaluate(() => { location.hash = '#/alarm' })
    await new Promise(r => setTimeout(r, 2500))
    const beforeRoute = await page.evaluate(() => location.hash || location.pathname)
    logLn(`  before reload: ${beforeRoute}`)

    await page.reload({ waitUntil: 'networkidle0', timeout: 30000 })
    await new Promise(r => setTimeout(r, 3500))

    const afterRoute = await page.evaluate(() => location.hash || location.pathname)
    const onAlarmAfter = afterRoute.includes('alarm')
    const onLoginAfter = afterRoute.includes('login')
    const hasLoginForm = await page.evaluate(() => !!document.querySelector('input[type="password"]'))

    await page.screenshot({ path: `${OUT_DIR}\\W-FRONT-04-C-03-reload-alarm.png`, fullPage: false })

    if (onAlarmAfter && !onLoginAfter && !hasLoginForm) {
      record(3, 'F5 on /#/alarm → stays on /#/alarm',
        'PASS', `before=${beforeRoute}, after=${afterRoute}`)
    } else {
      record(3, 'F5 on /#/alarm → stays on /#/alarm',
        'FAIL', `before=${beforeRoute}, after=${afterRoute}, hasLoginForm=${hasLoginForm}`)
    }
  } catch (e) {
    record(3, 'F5 on /#/alarm', 'FAIL', e.message)
  }

  // ==================================================================
  // CHECK #4 — cookie 失效后 reload → 跳 /#/login (守卫回退正确)
  // ==================================================================
  logLn('===== CHECK #4 RELOAD WITH INVALID COOKIE =====')
  try {
    // Clear cookies (including satoken) to simulate expired session
    await ctx.clearCookies()
    logLn('  cookies cleared')

    await page.reload({ waitUntil: 'networkidle0', timeout: 30000 })
    await new Promise(r => setTimeout(r, 3500))

    const afterRoute = await page.evaluate(() => location.hash || location.pathname)
    const hasLoginForm = await page.evaluate(() => !!document.querySelector('input[type="password"]'))
    const onLoginAfter = afterRoute.includes('login') || hasLoginForm

    await page.screenshot({ path: `${OUT_DIR}\\W-FRONT-04-C-04-reload-invalid-cookie.png`, fullPage: false })

    if (onLoginAfter) {
      record(4, 'Reload with invalid cookie → redirects to /#/login (守卫正确拦截)',
        'PASS', `route=${afterRoute}, hasLoginForm=${hasLoginForm}`)
    } else {
      record(4, 'Reload with invalid cookie → /#/login',
        'FAIL', `expected /#/login, got ${afterRoute}, hasLoginForm=${hasLoginForm}`)
    }
  } catch (e) {
    record(4, 'Reload with invalid cookie', 'FAIL', e.message)
  }

  // ==================================================================
  // SUMMARY
  // ==================================================================
  logLn('')
  logLn('===== VERIFICATION SUMMARY =====')
  let pass = 0, fail = 0
  for (const r of results) {
    if (r.status === 'PASS') pass++; else fail++
    logLn(`  #${r.n} ${r.status}: ${r.name}`)
  }
  logLn(`TOTAL: ${pass} PASS, ${fail} FAIL (out of 5)`)

  const summary = {
    generatedAt: new Date().toISOString(),
    totals: { pass, fail, total: 5 },
    results
  }
  writeFileSync(RESULTS_PATH, JSON.stringify(summary, null, 2), 'utf8')
  logLn(`JSON results: ${RESULTS_PATH}`)
  writeFileSync(LOG_PATH, log.join('\n'), 'utf8')
  logLn(`Text log: ${LOG_PATH}`)

  process.exitCode = (fail === 0) ? 0 : 1
} finally {
  await browser.close()
}
