// W-FRONT-02-E5 screenshot script (final)
// Approach:
//   1. Open login, type credentials, click submit
//   2. Wait for Set-Cookie
//   3. Fetch /web/account/current from page context to grab user
//   4. Import pinia stores and call setRoles/setCodes manually (Login.vue doesn't do this)
//   5. Navigate to /systemConfig, cycle 3 tabs, screenshot for each language
const puppeteer = require('puppeteer-core')
const fs = require('fs')
const path = require('path')

const EDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const BASE = 'http://127.0.0.1:5178'
const OUT_DIR = process.argv[2] || 'E:\\DEMO\\数据采集\\docs\\work-orders'

const LANGUAGES = ['zh-CN', 'en-US', 'id-ID']
const TABS = [
  { key: 'system', n: 1 },
  { key: 'line', n: 2 },
  { key: 'defectType', n: 3 }
]

async function loginAndPopulatePinia(page, lang) {
  await page.goto(`${BASE}/#/login`, { waitUntil: 'networkidle0' })
  await page.waitForSelector('#login-username', { timeout: 10000 })
  await page.type('#login-username', 'super_admin', { delay: 20 })
  await page.type('#login-password', 'Abc12345', { delay: 20 })
  await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('button'))
    const t = btns.find((b) => /登 录|登录|Sign in|Masuk/i.test(b.textContent || ''))
    if (t) t.click()
  })
  // Wait for satoken cookie
  await page.waitForFunction(() => /satoken=/.test(document.cookie), { timeout: 15000 })
  await new Promise((r) => setTimeout(r, 1500))

  // Populate pinia stores
  return await page.evaluate(async () => {
    const resp = await fetch('/web/account/current', { credentials: 'include' })
    const json = await resp.json()
    if (!json || (json.code !== 200 && json.code !== 0) || !json.data) {
      return { ok: false, json }
    }
    const u = json.data
    const appEl = document.getElementById('app')
    const ctx = appEl.__vue_app__._context
    const pinia = ctx.config.globalProperties.$pinia
    const permMod = await import('/src/stores/permission.ts')
    const userMod = await import('/src/stores/user.ts')
    const perm = permMod.usePermissionStore(pinia)
    const user = userMod.useUserStore(pinia)
    perm.setRoles([u.role || 'super_admin'])
    perm.setCodes(u.permission || [])
    user.$patch(u)
    user.loaded = true
    return { ok: true, user: u, roles: perm.roles, codes: perm.codes }
  })
}

async function main() {
  // Clean old screenshots
  for (const f of fs.readdirSync(OUT_DIR)) {
    if (f.startsWith('W-FRONT-02-E5-') && f.endsWith('.png')) {
      fs.unlinkSync(path.join(OUT_DIR, f))
    }
  }

  const browser = await puppeteer.launch({
    executablePath: EDGE,
    headless: 'new',
    defaultViewport: { width: 1440, height: 900 },
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage']
  })

  try {
    for (const lang of LANGUAGES) {
      const context = await browser.createBrowserContext()
      const page = await context.newPage()
      await page.setViewport({ width: 1440, height: 900 })

      console.log(`\n=== ${lang} ===`)
      // 1) root + locale
      await page.goto(`${BASE}/`, { waitUntil: 'networkidle0' })
      await page.evaluate((l) => localStorage.setItem('app.locale', l), lang)

      // 2) login + pinia populate
      const r = await loginAndPopulatePinia(page, lang)
      console.log(`  pinia populate:`, JSON.stringify(r))
      if (!r.ok) {
        console.log(`  POPULATE FAILED for ${lang}`)
        await page.close()
        continue
      }

      // 3) navigate to /systemConfig
      await page.goto(`${BASE}/#/systemConfig`, { waitUntil: 'networkidle0' })
      await new Promise((r2) => setTimeout(r2, 2500))
      const init = await page.evaluate(() => ({
        hash: location.hash,
        tabs: document.querySelectorAll('.el-tabs__item').length,
        title: document.querySelector('.glass-page__title')?.textContent?.trim()
      }))
      console.log(`  loaded:`, JSON.stringify(init))
      if (init.tabs === 0) {
        console.log(`  NO TABS for ${lang}`)
        await page.screenshot({ path: path.join(OUT_DIR, `W-FRONT-02-E5-${lang}-FAIL.png`) })
        await page.close()
        continue
      }

      // 4) cycle 3 tabs
      for (const tab of TABS) {
        const ok = await page.evaluate((n) => {
          const items = document.querySelectorAll('.el-tabs__item')
          const el = items[n - 1]
          if (el) { el.click(); return true }
          return false
        }, tab.n)
        console.log(`  [${lang}/${tab.key}] tab click=${ok}`)
        // Wait for content swap + table load
        await new Promise((r2) => setTimeout(r2, 2000))

        const filename = path.join(OUT_DIR, `W-FRONT-02-E5-${lang}-${tab.key}.png`)
        await page.screenshot({ path: filename, fullPage: false })
        console.log(`    -> ${filename}`)
      }

      await page.close()
      await context.close()
    }
  } finally {
    await browser.close()
  }
}

main().catch((e) => { console.error('FATAL:', e); process.exit(1) })
