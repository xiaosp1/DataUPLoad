// PG 管理：嵌入式 PostgreSQL 安装/启停/状态
const fs = require('fs')
const path = require('path')
const { execFileSync, spawn } = require('child_process')
const { PG_DIR, PG_INSTALLER, PG_INSTALL_DIR, PG_DATA_DIR, exists } = require('./paths')

function pgCtl() {
  return path.join(PG_INSTALL_DIR, 'bin', 'pg_ctl.exe')
}
function pgCtlExists() {
  return exists(pgCtl())
}

function getStatus() {
  const installed = pgCtlExists()
  let running = false
  let pid = null
  if (installed) {
    try {
      const out = execFileSync(pgCtl(), ['status', '-D', PG_DATA_DIR], { encoding: 'utf8', timeout: 10000 })
      running = out.includes('server is running')
      const m = out.match(/PID:\s*(\d+)/)
      if (m) pid = m[1]
    } catch (e) {
      running = false
    }
  }
  return {
    installed,
    running,
    pid,
    dataDir: PG_DATA_DIR,
    installerExists: exists(PG_INSTALLER),
    pgDir: PG_DIR
  }
}

function install() {
  // 静默安装（PSM 同款参数）
  if (pgCtlExists()) return { ok: true, message: 'PG 已安装' }
  if (!exists(PG_INSTALLER)) return { ok: false, message: `安装包不存在: ${PG_INSTALLER}` }
  try {
    const args = [
      '--mode', 'unattended',
      '--superpassword', 'Abc12345',
      '--serverport', '5432',
      '--prefix', `"${PG_INSTALL_DIR}"`,
      '--datadir', `"${PG_DATA_DIR}"`
    ]
    const r = execFileSync(PG_INSTALLER, args, { encoding: 'utf8', timeout: 300000 })
    return { ok: true, message: '安装完成', detail: String(r).slice(0, 500) }
  } catch (e) {
    return { ok: false, message: String(e && e.message || e) }
  }
}

function start() {
  if (!pgCtlExists()) return { ok: false, message: 'PG 未安装' }
  try {
    const r = execFileSync(pgCtl(), ['start', '-D', PG_DATA_DIR, '-l', path.join(PG_DATA_DIR, 'pg.log')], { encoding: 'utf8', timeout: 30000 })
    return { ok: true, message: 'PG 已启动', detail: String(r) }
  } catch (e) {
    return { ok: false, message: String(e && e.message || e) }
  }
}

function stop() {
  if (!pgCtlExists()) return { ok: false, message: 'PG 未安装' }
  try {
    const r = execFileSync(pgCtl(), ['stop', '-D', PG_DATA_DIR, '-m', 'fast'], { encoding: 'utf8', timeout: 30000 })
    return { ok: true, message: 'PG 已停止', detail: String(r) }
  } catch (e) {
    return { ok: false, message: String(e && e.message || e) }
  }
}

// 测试数据库连接（用 psql 或 pg_isready）
function testConnect(cfg) {
  const pgIsReady = path.join(PG_INSTALL_DIR, 'bin', 'pg_isready.exe')
  if (!exists(pgIsReady)) return { ok: false, message: 'pg_isready 不存在（PG 未安装）' }
  try {
    const r = execFileSync(pgIsReady, ['-h', cfg.host || '127.0.0.1', '-p', String(cfg.port || 5432)], { encoding: 'utf8', timeout: 10000 })
    return { ok: true, message: r.trim() }
  } catch (e) {
    return { ok: false, message: String(e && e.message || e) }
  }
}

module.exports = { getStatus, install, start, stop, testConnect, pgCtl, pgCtlExists }
