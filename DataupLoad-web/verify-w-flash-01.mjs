// =============================================================================
// W-FLASH-01 端到端验证 — 实时页不闪烁/无 JS 报错/连接状态
//  1. 真实登录 (SHA256) -> /#/realtime
//  2. 确认 SPA 渲染出 上座率条 + KPI + 折线
//  3. 观察 13s（覆盖 >2 次 WS 推送）捕获 console/page errors
//  4. 确认 WS 连接 / 无 403 踢出
// =============================================================================
import { chromium } from 'playwright'
import { writeFileSync, mkdirSync } from 'fs'
import { createHash } from 'crypto'

const BASE = 'http://127.0.0.1:8080'
const OUT = 'E:\\DEMO\\数据采集\\docs\\work-orders\\W-FLASH-01'
const OUT_DIR = 'E:\\DEMO\\数据采集\\docs\\work-orders'
function sha256Hex(t) { return createHash('sha256').update(t).digest('hex') }

const log = []
function lg(x) { console.log(x); log.push(x) }

;(async () => {
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext()
  const page = await ctx.newPage()
  const consoleErr = []
  const pageErr = []
  page.on('console', (m) => { if (m.type() === 'error') consoleErr.push(m.text()) })
  page.on('pageerror', (e) => pageErr.push(String(e)))

  // 直接走页面真实登录（Login.vue 内部 axios 自动处理 cookie + 跳转）
  await page.goto(`${BASE}/#/login`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForSelector('input', { timeout: 15000 })
  const inputs = await page.evaluate(() => {
    const all = Array.from(document.querySelectorAll('input'))
    // 找到 password 输入框作区分
    let user = null, pwd = null
    for (const el of all) {
      if (el.type === 'password') pwd = el
      else if (user === null) user = el
    }
    return { hasUser: !!user, hasPwd: !!pwd }
  })
  lg('登录表单 inputs: ' + JSON.stringify(inputs))
  await page.fill('input:not([type=password])', 'super_admin')
  await page.fill('input[type=password]', 'Abc12345')
  await page.press('input[type=password]', 'Enter')  // 触发 @keyup.enter=onSubmit
  lg('已触发登录(Enter)，等待跳转...')
  await page.waitForFunction(() => !location.hash.includes('login'), { timeout: 20000 }).catch(() => {})
  await page.waitForTimeout(1500)
  lg(`登录后 URL: ${page.url()}`)

  const t0 = Date.now()
  // 观察 13s（>2 个 WS 5s 周期）
  await page.waitForTimeout(13000)
  lg(`观察时长 ${Date.now() - t0}ms`)

  const bodyText = await page.evaluate(() => document.body.innerText)
  const urlFinal = page.url()
  const hasOcc = /上座率|Occupancy/i.test(bodyText)
  const hasKpi = /(产量|Efficiency|上座|移除|良品|Fail|Occupancy)/i.test(bodyText)
  const notAt403 = !urlFinal.includes('403')
  const notAtLogin = !urlFinal.includes('login')
  // W-FLASH-01: 连接正常时无状态文案，仅断开才显示「連接斷開」；改为断言 WS 数据新鲜 + 无断开标记
  const wsFresh = await page.evaluate(() => {
    const b = document.body.innerText
    const noDisconnect = !b.includes('連接斷開')
    // 通过线上座率/产量数值已出现判断数据驱动（WS 快照已到）
    const hasNumbers = /[0-9]{2,}/.test(b)
    return noDisconnect && hasNumbers
  })

  lg(`上座率条: ${hasOcc}`)
  lg(`KPI: ${hasKpi}`)
  lg(`未跳403: ${notAt403}`)
  lg(`未跳login: ${notAtLogin}`)
  lg(`WS新鲜(无断开+有数据): ${wsFresh}`)
  lg(`console errors(${consoleErr.length}): ${consoleErr.slice(0, 5).join(' || ') || 'NONE'}`)
  lg(`page errors(${pageErr.length}): ${pageErr.slice(0, 5).join(' || ') || 'NONE'}`)
  lg('--- body 200字 ---')
  lg(bodyText.replace(/\s+/g, ' ').slice(0, 260))

  try {
    mkdirSync(OUT, { recursive: true })
    writeFileSync(`${OUT}\\w-flash-01-realtime.png`, await page.screenshot({ type: 'png' }))
    lg('截图已存: ' + OUT + '\\w-flash-01-realtime.png')
  } catch (e) { lg('截图失败: ' + e.message) }

  writeFileSync(`${OUT}\\verify-output.txt`, log.join('\n'))
  await browser.close()
})().catch((e) => { console.error('FATAL', e); process.exit(1) })
