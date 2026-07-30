// W-PERF-F i18n 3 语言验证（顺序执行版）

import { chromium } from 'playwright'
import { writeFileSync } from 'node:fs'

const BASE = process.env.BASE_URL || 'http://127.0.0.1:8080'
const REPORT_DIR = 'E:/DEMO/数据采集/docs/work-orders'

const checks = []
function check(n, name, ok, info = '') {
    checks.push({ n, name, ok: !!ok, info })
    console.log(`[${n}] ${ok ? '✅' : '❌'} ${name}${info ? ' — ' + info : ''}`)
}

const LANGUAGES = [
    { code: 'zh-CN', locale: 'zh-CN', expected: '操作历史暂不可用', tag: 'warning-text-zh' },
    { code: 'en-US', locale: 'en-US', expected: 'Operation history temporarily unavailable', tag: 'warning-text-en' },
    { code: 'id-ID', locale: 'id-ID', expected: 'Riwayat operasi sementara tidak tersedia', tag: 'warning-text-id' },
]

for (const lang of LANGUAGES) {
    const browser = await chromium.launch({ headless: true })
    const ctx = await browser.newContext({
        viewport: { width: 1440, height: 900 },
        ignoreHTTPSErrors: true,
        locale: lang.locale,
    })
    const page = await ctx.newPage()

    try {
        await page.addInitScript((code) => {
            localStorage.setItem('app.locale', code)
        }, lang.code)

        await page.goto(`${BASE}/`, { waitUntil: 'networkidle', timeout: 30_000 })
        await page.waitForSelector('#app > *', { timeout: 15_000 })
        await page.waitForTimeout(800)
        await page.locator('#login-username').first().fill('super_admin')
        await page.locator('#login-password').first().fill('Abc12345')
        await page
            .locator('button:has-text("登 录"), button:has-text("登录"), button[type="submit"]')
            .first()
            .click({ timeout: 5_000 })
        try {
            await page.waitForURL(
                (url) => /#\/(realtime|alarm|defect|account|systemConfig|log|userManage|screen)/.test(url.toString()),
                { timeout: 25_000 },
            )
        } catch {
            const errMsg = await page
                .evaluate(() => document.querySelector('.login-card__error, .el-message--error')?.innerText || '')
                .catch(() => '')
            console.error(`  [${lang.code}] login failed, errMsg="${errMsg.trim()}", url=${page.url()}`)
            // retry
            await page
                .locator('button:has-text("登 录"), button:has-text("登录"), button[type="submit"]')
                .first()
                .click({ timeout: 5_000 })
                .catch(() => {})
            await page.waitForTimeout(2000)
        }
        await page.waitForTimeout(800)

        await page.goto(`${BASE}/#/userManage`, { waitUntil: 'networkidle', timeout: 15_000 })
        await page.waitForSelector('.user-table table tbody tr', { timeout: 10_000 }).catch(() => {})
        const rowCount = await page.locator('.user-table table tbody tr').count()
        check(`${lang.code}-list`, `[${lang.code}] 列表渲染`, rowCount > 0, `rows=${rowCount}`)

        const detailBtn = page
            .locator('button:has-text("详情"), button:has-text("Detail")')
            .first()
        if ((await detailBtn.count()) === 0) {
            check(`${lang.code}-btn`, `[${lang.code}] 详情按钮缺失`, false, '')
            await browser.close()
            continue
        }
        await detailBtn.click({ timeout: 5_000 })

        await page.waitForSelector('.el-alert.user-detail__history-alert', { timeout: 15_000 }).catch(() => {})
        await page.waitForTimeout(500)

        const alertText = await page
            .locator('.el-alert.user-detail__history-alert')
            .first()
            .innerText()
            .catch(() => '')

        const ok = alertText.includes(lang.expected)
        check(
            `${lang.code}-alert`,
            `[${lang.code}] 警告文案 = "${lang.expected}"`,
            ok,
            `actual="${alertText.replace(/\s+/g, ' ').trim().slice(0, 100)}"`,
        )

        await page.screenshot({ path: `${REPORT_DIR}/W-PERF-F-${lang.tag}.png`, fullPage: false })
    } catch (e) {
        check(`${lang.code}-err`, `[${lang.code}] 测试异常`, false, e.message)
    }

    await browser.close()
}

const summary = {
    base: BASE,
    timestamp: new Date().toISOString(),
    checks,
}
writeFileSync(`${REPORT_DIR}/W-PERF-F-i18n-results.json`, JSON.stringify(summary, null, 2))
console.log('\n=== i18n summary ===')
console.log(`Total: ${checks.filter(c => c.ok).length}/${checks.length} pass`)
process.exit(checks.every((c) => c.ok) ? 0 : 1)
