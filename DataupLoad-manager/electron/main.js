// DataupLoad Manager - Electron 主进程
const { app, BrowserWindow, ipcMain } = require('electron')
const path = require('path')

const services = {
  system: require('./services/system'),
  config: require('./services/config'),
  pg: require('./services/pg')
}

let mainWindow = null

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 1024,
    minHeight: 700,
    backgroundColor: '#0a1220',
    title: 'DataupLoad 部署管理',
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  const devUrl = process.env.VITE_DEV_SERVER_URL
  if (devUrl) {
    mainWindow.loadURL(devUrl)
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }
}

// ---------- IPC：渲染层请求系统操作 ----------
function registerIpc() {
  // 服务状态总览
  ipcMain.handle('svc:overview', () => services.system.getOverview())

  // 后端管理
  ipcMain.handle('server:start', () => services.system.startServer())
  ipcMain.handle('server:stop', () => services.system.stopServer())
  ipcMain.handle('server:restart', () => services.system.restartServer())
  ipcMain.handle('server:status', () => services.system.getServerStatus())
  ipcMain.handle('server:log', (e, lines) => services.system.tailServerLog(lines || 200))

  // 数据库
  ipcMain.handle('pg:status', () => services.pg.getStatus())
  ipcMain.handle('pg:install', () => services.pg.install())
  ipcMain.handle('pg:start', () => services.pg.start())
  ipcMain.handle('pg:stop', () => services.pg.stop())

  // 配置
  ipcMain.handle('config:load', () => services.config.loadAll())
  ipcMain.handle('config:save', (e, patch) => services.config.save(patch))
  ipcMain.handle('config:getYml', () => services.config.getYmlText())
  ipcMain.handle('config:setYml', (e, text) => services.config.setYmlText(text))

  // 部署
  ipcMain.handle('deploy:check', () => services.system.checkDeployment())
  ipcMain.handle('deploy:init', () => services.system.initDeployment())

  // 设置
  ipcMain.handle('settings:get', () => services.system.getSettings())
  ipcMain.handle('settings:set', (e, patch) => services.system.setSettings(patch))
  ipcMain.handle('settings:testConnect', (e, cfg) => services.pg.testConnect(cfg))

  // 看门狗
  ipcMain.handle('watchdog:status', () => services.system.watchdogStatus())
  ipcMain.handle('watchdog:start', () => services.system.watchdogStart())
  ipcMain.handle('watchdog:stop', () => services.system.watchdogStop())
}

app.whenReady().then(() => {
  registerIpc()
  createWindow()
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
