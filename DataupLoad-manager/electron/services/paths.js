// 路径常量：P3 部署包根目录
// 约定：桌面端（Manager）与 server/ 在同一父目录下
const path = require('path')
const fs = require('fs')

// Manager 自身所在目录（electron 主进程）
const MANAGER_DIR = __dirname.replace(/[\\/]services$/, '')

// 部署包根 = Manager 的上级目录
const ROOT_DIR = path.dirname(MANAGER_DIR)

// 服务端目录（lib/target/classes/config/web）
const SERVER_DIR = path.join(ROOT_DIR, 'server')
const SERVER_LIB = path.join(SERVER_DIR, 'lib')
const SERVER_CLASSES = path.join(SERVER_DIR, 'target', 'classes')
const SERVER_CONFIG = path.join(SERVER_DIR, 'config')
const SERVER_WEB = path.join(SERVER_DIR, 'web')

// JDK（优先 server/jdk，回退部署包 jdk）
const JDK_DIR = path.join(SERVER_DIR, 'jdk')
const JDK_FALLBACK = path.join(ROOT_DIR, 'jdk')
const HIK_JAVA = 'hik-java.exe'
const JAVA = 'java.exe'

// PG（嵌入式）
const PG_DIR = path.join(ROOT_DIR, 'pg')
const PG_INSTALLER = path.join(PG_DIR, 'postgresql.exe')
const PG_INSTALL_DIR = path.join(PG_DIR, 'postgres')
const PG_DATA_DIR = path.join(PG_INSTALL_DIR, 'data')

// SQL 脚本
const SQL_DIR = path.join(SERVER_DIR, 'sql')

// 日志
const LOG_DIR = path.join(SERVER_DIR, 'log', 'DataupLoad')

// 开机自启脚本
const AUTOSTART_BAT = path.join(ROOT_DIR, 'autostart.bat')

// 看门狗配置
const WATCHDOG_JSON = path.join(MANAGER_DIR, 'config', 'dog.json')
const MANAGER_CONFIG_DIR = path.join(MANAGER_DIR, 'config')

function exists(p) {
  try { return fs.existsSync(p) } catch { return false }
}

function getJdkBin() {
  const primary = path.join(JDK_DIR, 'bin')
  if (exists(path.join(primary, HIK_JAVA))) return path.join(primary, HIK_JAVA)
  if (exists(path.join(primary, JAVA))) return path.join(primary, JAVA)
  const fb = path.join(JDK_FALLBACK, 'bin')
  if (exists(path.join(fb, HIK_JAVA))) return path.join(fb, HIK_JAVA)
  if (exists(path.join(fb, JAVA))) return path.join(fb, JAVA)
  return null
}

module.exports = {
  MANAGER_DIR,
  ROOT_DIR,
  SERVER_DIR,
  SERVER_LIB,
  SERVER_CLASSES,
  SERVER_CONFIG,
  SERVER_WEB,
  JDK_DIR,
  JDK_FALLBACK,
  PG_DIR,
  PG_INSTALLER,
  PG_INSTALL_DIR,
  PG_DATA_DIR,
  SQL_DIR,
  LOG_DIR,
  AUTOSTART_BAT,
  WATCHDOG_JSON,
  MANAGER_CONFIG_DIR,
  exists,
  getJdkBin
}
