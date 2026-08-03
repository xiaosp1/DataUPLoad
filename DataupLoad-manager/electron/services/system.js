// 系统服务：后端进程管理 / 日志 / 部署检查 / 看门狗 / 开机自启
const fs = require('fs')
const path = require('path')
const { execFile, execFileSync } = require('child_process')
const {
  SERVER_DIR, SERVER_LIB, SERVER_CLASSES, SERVER_CONFIG, SERVER_WEB,
  LOG_DIR, AUTOSTART_BAT, MANAGER_CONFIG_DIR, WATCHDOG_JSON, ROOT_DIR, exists, getJdkBin
} = require('./paths')

const APP_MAIN = 'com.hikrobotics.solution.Application'
const PROCESS_NAME = 'hik-java'

// ---------- 部署检查 ----------
function checkDeployment() {
  const checks = {
    serverDir: exists(SERVER_DIR),
    libJars: exists(SERVER_LIB),
    classes: exists(SERVER_CLASSES) && fs.readdirSync(SERVER_CLASSES).length > 0,
    config: exists(SERVER_CONFIG),
    web: exists(SERVER_WEB),
    jdk: !!getJdkBin(),
    sql: exists(path.join(SERVER_DIR, 'sql'))
  }
  const allOk = Object.values(checks).every(Boolean)
  return { ok: allOk, checks }
}

// ---------- 后端进程 ----------
function getServerStatus() {
  let pid = null
  let running = false
  try {
    const out = execFileSync('powershell', [
      '-NoProfile', '-Command',
      `Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='hik-java.exe'" | Where-Object {$_.CommandLine -match 'hikrobotics.solution.Application'} | Select-Object -ExpandProperty ProcessId`
    ], { encoding: 'utf8', timeout: 15000 })
    const ids = out.split(/\r?\n/).map(s => s.trim()).filter(s => /^\d+$/.test(s))
    if (ids.length > 0) {
      running = true
      pid = ids[0]
    }
  } catch (e) {
    running = false
  }
  return { running, pid }
}

function buildStartArgs() {
  const jdk = getJdkBin()
  const cp = `"${path.join(SERVER_LIB, '*')};${SERVER_CLASSES}"`
  return {
    jdk,
    args: [
      '-cp', cp,
      '-Dfile.encoding=UTF-8',
      '-Dspring.config.location=./config/',
      '-Dspring.config.name=application',
      '-Dserver.port=8080',
      APP_MAIN
    ]
  }
}

function startServer() {
  const { jdk, args } = buildStartArgs()
  if (!jdk) return { ok: false, message: '未找到 JDK' }
  try {
    const child = spawn(jdk, args, {
      cwd: SERVER_DIR,
      detached: true,
      stdio: 'ignore',
      windowsHide: true
    })
    child.unref()
    return { ok: true, message: '后端启动中', pid: child.pid }
  } catch (e) {
    return { ok: false, message: String(e && e.message || e) }
  }
}

function stopServer() {
  try {
    execFileSync('powershell', [
      '-NoProfile', '-Command',
      `Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='hik-java.exe'" | Where-Object {$_.CommandLine -match 'hikrobotics.solution.Application'} | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }`
    ], { encoding: 'utf8', timeout: 20000 })
    return { ok: true, message: '后端已停止' }
  } catch (e) {
    return { ok: false, message: String(e && e.message || e) }
  }
}

async function restartServer() {
  const st = stopServer()
  await new Promise(r => setTimeout(r, 3000))
  const st2 = startServer()
  return { stop: st, start: st2 }
}

// ---------- 日志 ----------
function tailServerLog(lines) {
  const file = path.join(LOG_DIR, 'error.log')
  const runFile = path.join(LOG_DIR, 'run.log')
  const target = exists(runFile) ? runFile : (exists(file) ? file : null)
  if (!target) return { ok: false, message: '日志文件不存在' }
  try {
    const buf = fs.readFileSync(target, 'utf8')
    const arr = buf.split(/\r?\n/).filter(s => s.trim().length > 0)
    return { ok: true, lines: arr.slice(-lines) }
  } catch (e) {
    return { ok: false, message: String(e) }
  }
}

// ---------- 看门狗（简单守护：检查进程，挂了拉起） ----------
function getWatchdogConfig() {
  const file = WATCHDOG_JSON
  if (!exists(file)) {
    const def = {
      checkInterval: 10,
      processes: [
        { name: 'postgres', startFile: '', isCheck: false },
        { name: PROCESS_NAME, startFile: '', isCheck: true, restartDelay: 5 }
      ]
    }
    try {
      fs.mkdirSync(MANAGER_CONFIG_DIR, { recursive: true })
      fs.writeFileSync(file, JSON.stringify(def, null, 2), 'utf8')
    } catch (e) {}
    return def
  }
  try { return JSON.parse(fs.readFileSync(file, 'utf8')) } catch { return { processes: [] } }
}

function watchdogStatus() {
  const cfg = getWatchdogConfig()
  return { enabled: !!cfg.enabled, config: cfg }
}

// 看门狗由渲染层定时器调用，这里实现检查逻辑（简化版：仅报告，不做守护循环，守护由 autostart + 系统计划任务负责）
function watchdogStart() {
  const cfg = getWatchdogConfig()
  cfg.enabled = true
  fs.writeFileSync(WATCHDOG_JSON, JSON.stringify(cfg, null, 2), 'utf8')
  return { ok: true, message: '看门狗已启用' }
}

function watchdogStop() {
  const cfg = getWatchdogConfig()
  cfg.enabled = false
  fs.writeFileSync(WATCHDOG_JSON, JSON.stringify(cfg, null, 2), 'utf8')
  return { ok: true, message: '看门狗已停用' }
}

// ---------- 设置（开机自启） ----------
function getSettings() {
  const cfg = getWatchdogConfig()
  return {
    autoStart: cfg.autoStart || false,
    serverPort: 8080,
    watchdog: cfg.enabled || false
  }
}

function setSettings(patch) {
  const cfg = getWatchdogConfig()
  if (typeof patch.autoStart === 'boolean') {
    cfg.autoStart = patch.autoStart
    applyAutoStart(patch.autoStart)
  }
  if (typeof patch.watchdog === 'boolean') cfg.enabled = patch.watchdog
  fs.writeFileSync(WATCHDOG_JSON, JSON.stringify(cfg, null, 2), 'utf8')
  return { ok: true }
}

// 开机自启：写 autostart.bat + 注册到启动文件夹
function applyAutoStart(enable) {
  const bat = AUTOSTART_BAT
  try {
    if (enable) {
      const jdk = getJdkBin()
      const cp = `"${path.join(SERVER_LIB, '*')};${SERVER_CLASSES}"`
      const content = `@echo off\r\ncd /d "${SERVER_DIR}"\r\nstart "DataupLoad" /B "${jdk}" -cp ${cp} -Dfile.encoding=UTF-8 -Dspring.config.location=./config/ -Dspring.config.name=application -Dserver.port=8080 ${APP_MAIN}\r\n`
      fs.writeFileSync(bat, content, 'utf8')
      // 注册到启动文件夹
      const startupDir = path.join(process.env.APPDATA, 'Microsoft', 'Windows', 'Start Menu', 'Programs', 'Startup')
      const lnk = path.join(startupDir, 'DataupLoad-autostart.vbs')
      const vbs = `Set ws = CreateObject("Wscript.Shell")\r\nws.Run "${bat}", 0, False\r\n`
      fs.writeFileSync(lnk, vbs, 'utf8')
    } else {
      const startupDir = path.join(process.env.APPDATA, 'Microsoft', 'Windows', 'Start Menu', 'Programs', 'Startup')
      const lnk = path.join(startupDir, 'DataupLoad-autostart.vbs')
      if (fs.existsSync(lnk)) fs.unlinkSync(lnk)
    }
    return { ok: true }
  } catch (e) {
    return { ok: false, message: String(e) }
  }
}

// ---------- 总览 ----------
function getOverview() {
  const server = getServerStatus()
  const checks = checkDeployment()
  let pgRunning = false
  try {
    const out = execFileSync('powershell', [
      '-NoProfile', '-Command', `Get-Process postgres -ErrorAction SilentlyContinue | Measure-Object | Select-Object -ExpandProperty Count`
    ], { encoding: 'utf8', timeout: 10000 })
    pgRunning = parseInt(out.trim()) > 0
  } catch (e) { pgRunning = false }

  // 产线连接数
  let conns = 0
  try {
    const out = execFileSync('powershell', [
      '-NoProfile', '-Command', `(Get-NetTCPConnection -LocalPort 8080 -State Established -ErrorAction SilentlyContinue).Count`
    ], { encoding: 'utf8', timeout: 10000 })
    conns = parseInt(out.trim()) || 0
  } catch (e) { conns = 0 }

  return {
    deployment: checks,
    server,
    pgRunning,
    connections: conns,
    rootDir: ROOT_DIR
  }
}

module.exports = {
  checkDeployment, getServerStatus, startServer, stopServer, restartServer,
  tailServerLog, getOverview, getSettings, setSettings, watchdogStatus, watchdogStart, watchdogStop
}
