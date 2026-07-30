// Test persistence: change a config value, save, reload page, verify value persists
const puppeteer = require('puppeteer-core')
const EDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const BASE = 'http://127.0.0.1:5178'

async function main() {
  const browser = await puppeteer.launch({
    executablePath: EDGE,
    headless: 'new',
    defaultViewport: { width: 1440, height: 900 },
    args: ['--no-sandbox']
  })
  const ctx = await browser.createBrowserContext()
  const page = await ctx.newPage()

  await page.goto(`${BASE}/`, { waitUntil: 'networkidle0' })
  await page.evaluate(() => localStorage.setItem('app.locale', 'zh-CN'))

  // Login + populate pinia
  await page.goto(`${BASE}/#/login`, { waitUntil: 'networkidle0' })
  await page.waitForSelector('#login-username', { timeout: 10000 })
  await page.type('#login-username', 'super_admin')
  await page.type('#login-password', 'Abc12345')
  await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('button'))
    const t = btns.find((b) => /登 录/.test(b.textContent || ''))
    if (t) t.click()
  })
  await page.waitForFunction(() => /satoken=/.test(document.cookie), { timeout: 15000 })
  await new Promise((r) => setTimeout(r, 1000))
  await page.evaluate(async () => {
    const resp = await fetch('/web/account/current', { credentials: 'include' })
    const json = await resp.json()
    const u = json.data
    const appEl = document.getElementById('app')
    const ctx = appEl.__vue_app__._context
    const pinia = ctx.config.globalProperties.$pinia
    const permMod = await import('/src/stores/permission.ts')
    const userMod = await import('/src/stores/user.ts')
    const perm = permMod.usePermissionStore(pinia)
    const user = userMod.useUserStore(pinia)
    perm.setRoles([u.role])
    perm.setCodes(u.permission || [])
    user.$patch(u)
    user.loaded = true
  })

  // 1. Read original value
  await page.goto(`${BASE}/#/systemConfig`, { waitUntil: 'networkidle0' })
  await new Promise((r) => setTimeout(r, 2500))
  const before = await page.evaluate(() => {
    const inputs = document.querySelectorAll('.config-form input.el-input__inner')
    return Array.from(inputs).map((e) => e.value)
  })
  console.log('Before:', before)

  // 2. Change first sound URI to a test value
  const newVal = '/data/sound/test-e5-' + Date.now() + '.mp3'
  await page.evaluate((v) => {
    const inputs = document.querySelectorAll('.config-form input.el-input__inner')
    if (inputs[0]) {
      const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set
      setter.call(inputs[0], v)
      inputs[0].dispatchEvent(new Event('input', { bubbles: true }))
      inputs[0].dispatchEvent(new Event('change', { bubbles: true }))
    }
  }, newVal)
  // Also change sound_play_count
  await page.evaluate(() => {
    const numInput = document.querySelector('.config-form input.el-input__inner[role="spinbutton"]')
    if (numInput) {
      const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set
      setter.call(numInput, '5')
      numInput.dispatchEvent(new Event('input', { bubbles: true }))
      numInput.dispatchEvent(new Event('change', { bubbles: true }))
    }
  })

  // 3. Click save
  await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('.glass-btn, .el-button'))
    const t = btns.find((b) => /保存/.test(b.textContent || ''))
    if (t) t.click()
  })
  await new Promise((r) => setTimeout(r, 3000))
  const afterSave = await page.evaluate(() => {
    const inputs = document.querySelectorAll('.config-form input.el-input__inner')
    return Array.from(inputs).map((e) => e.value)
  })
  console.log('After save (should show new value):', afterSave)

  // 4. Reload page + repopulate pinia (Login.vue doesn't auto-populate)
  await page.reload({ waitUntil: 'networkidle0' })
  await new Promise((r) => setTimeout(r, 1500))
  // Re-populate pinia
  await page.evaluate(async () => {
    const resp = await fetch('/web/account/current', { credentials: 'include' })
    const json = await resp.json()
    const u = json.data
    const appEl = document.getElementById('app')
    const ctx = appEl.__vue_app__._context
    const pinia = ctx.config.globalProperties.$pinia
    const permMod = await import('/src/stores/permission.ts')
    const userMod = await import('/src/stores/user.ts')
    const perm = permMod.usePermissionStore(pinia)
    const user = userMod.useUserStore(pinia)
    perm.setRoles([u.role])
    perm.setCodes(u.permission || [])
    user.$patch(u)
    user.loaded = true
  })
  // Now navigate
  await page.goto(`${BASE}/#/systemConfig`, { waitUntil: 'networkidle0' })
  await new Promise((r) => setTimeout(r, 3000))
  const afterReload = await page.evaluate(() => {
    const inputs = document.querySelectorAll('.config-form input.el-input__inner')
    return Array.from(inputs).map((e) => e.value)
  })
  console.log('After reload+repopulate (should still show new value):', afterReload)
  console.log('Persistence test:', afterReload[0] === newVal ? 'PASS' : 'FAIL')

  // 5. Verify with backend
  const verify = await page.evaluate(async () => {
    const r = await fetch('/web/system-config', { credentials: 'include' })
    const j = await r.json()
    return j.data
  })
  console.log('Backend now:', JSON.stringify(verify))

  // 6. Restore original
  await page.evaluate(async (origVal) => {
    const inputs = document.querySelectorAll('.config-form input.el-input__inner')
    if (inputs[0]) {
      const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set
      setter.call(inputs[0], origVal)
      inputs[0].dispatchEvent(new Event('input', { bubbles: true }))
      inputs[0].dispatchEvent(new Event('change', { bubbles: true }))
    }
  }, before[0])
  await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('.glass-btn, .el-button'))
    const t = btns.find((b) => /保存/.test(b.textContent || ''))
    if (t) t.click()
  })
  await new Promise((r) => setTimeout(r, 2500))
  console.log('Restored original value')

  await browser.close()
}

main().catch((e) => { console.error(e); process.exit(1) })
