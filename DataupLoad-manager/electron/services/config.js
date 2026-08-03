// 配置服务：读改写 server/config/application*.yml
const fs = require('fs')
const path = require('path')
const yaml = require('js-yaml')
const { SERVER_CONFIG } = require('./paths')

const PROD_YML = path.join(SERVER_CONFIG, 'application-prod.yml')
const BASE_YML = path.join(SERVER_CONFIG, 'application.yml')

function readYml(file) {
  if (!fs.existsSync(file)) return null
  try {
    return yaml.load(fs.readFileSync(file, 'utf8'))
  } catch (e) {
    return { __error: String(e) }
  }
}

function writeYml(file, obj) {
  const text = yaml.dump(obj, { indent: 2, lineWidth: -1, noRefs: true })
  fs.writeFileSync(file, text, 'utf8')
}

// 加载全部配置给 UI
function loadAll() {
  const prod = readYml(PROD_YML) || {}
  const base = readYml(BASE_YML) || {}
  return {
    prod,
    base,
    files: {
      prod: PROD_YML,
      base: BASE_YML
    }
  }
}

// 保存配置（结构化 patch：{ prod: { path: [...], value } }）
function save(patch) {
  const result = { ok: true, saved: [] }
  if (!patch || typeof patch !== 'object') return { ok: false, message: 'patch 无效' }
  if (patch.prod) {
    const obj = readYml(PROD_YML) || {}
    applyPatch(obj, patch.prod)
    writeYml(PROD_YML, obj)
    result.saved.push('application-prod.yml')
  }
  if (patch.base) {
    const obj = readYml(BASE_YML) || {}
    applyPatch(obj, patch.base)
    writeYml(BASE_YML, obj)
    result.saved.push('application.yml')
  }
  return result
}

// 按路径数组设置值：{ 'server.port': 8080 } → { server: { port: 8080 } }
function applyPatch(obj, patchObj) {
  for (const [keyPath, value] of Object.entries(patchObj)) {
    const keys = keyPath.split('.')
    let cur = obj
    for (let i = 0; i < keys.length - 1; i++) {
      const k = keys[i]
      if (!cur[k] || typeof cur[k] !== 'object') cur[k] = {}
      cur = cur[k]
    }
    cur[keys[keys.length - 1]] = value
  }
}

// 原始文本读取/写入（高级编辑）
function getYmlText() {
  return fs.existsSync(PROD_YML) ? fs.readFileSync(PROD_YML, 'utf8') : ''
}

function setYmlText(text) {
  try {
    const parsed = yaml.load(text)
    if (!parsed || typeof parsed !== 'object') throw new Error('YAML 解析结果不是对象')
    writeYml(PROD_YML, parsed)
    return { ok: true }
  } catch (e) {
    return { ok: false, message: String(e) }
  }
}

module.exports = { loadAll, save, getYmlText, setYmlText, readYml, writeYml, PROD_YML, BASE_YML }
