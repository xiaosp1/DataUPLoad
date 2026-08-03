// Preload: 暴露安全的 IPC bridge 给渲染层
const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('manager', {
  // 服务状态
  svcOverview: () => ipcRenderer.invoke('svc:overview'),
  // 后端
  serverStart: () => ipcRenderer.invoke('server:start'),
  serverStop: () => ipcRenderer.invoke('server:stop'),
  serverRestart: () => ipcRenderer.invoke('server:restart'),
  serverStatus: () => ipcRenderer.invoke('server:status'),
  serverLog: (lines) => ipcRenderer.invoke('server:log', lines),
  // 数据库
  pgStatus: () => ipcRenderer.invoke('pg:status'),
  pgInstall: () => ipcRenderer.invoke('pg:install'),
  pgStart: () => ipcRenderer.invoke('pg:start'),
  pgStop: () => ipcRenderer.invoke('pg:stop'),
  // 配置
  configLoad: () => ipcRenderer.invoke('config:load'),
  configSave: (patch) => ipcRenderer.invoke('config:save', patch),
  configGetYml: () => ipcRenderer.invoke('config:getYml'),
  configSetYml: (text) => ipcRenderer.invoke('config:setYml', text),
  // 部署
  deployCheck: () => ipcRenderer.invoke('deploy:check'),
  deployInit: () => ipcRenderer.invoke('deploy:init'),
  // 设置
  settingsGet: () => ipcRenderer.invoke('settings:get'),
  settingsSet: (patch) => ipcRenderer.invoke('settings:set', patch),
  settingsTestConnect: (cfg) => ipcRenderer.invoke('settings:testConnect', cfg),
  // 看门狗
  watchdogStatus: () => ipcRenderer.invoke('watchdog:status'),
  watchdogStart: () => ipcRenderer.invoke('watchdog:start'),
  watchdogStop: () => ipcRenderer.invoke('watchdog:stop')
})
