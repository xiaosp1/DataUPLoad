// =============================================================================
// W-FRONT-02-E4 sha256 hex 计算（前端）
//
// 设计要点：
//   1) 使用浏览器原生 crypto.subtle.digest('SHA-256', ...)，无第三方依赖
//   2) 返回小写 hex 字符串（与后端 Hutool DigestUtil.sha256Hex 兼容）
//   3) 当前被以下场景使用：
//      - 登录（api/auth.ts）：sha256Hex(明文密码) → 后端 bcryptCheck
//      - 修改密码（api/account.ts changePwd）：旧/新/确认 都传 sha256Hex
//      - 重置 super_admin 密码（resetAdminPwd）：传 sha256Hex
//      - 新增账号（add）/ 重置用户密码（resetPwd）：前端只传 sha256Hex，
//        后端按 ADR-0014 决定是否再 bcrypt 一次
//
// 注意：前端不做 bcrypt！无 bcrypt 库依赖，也无 bcrypt.js / bcrypt-ts 引用。
// =============================================================================

/**
 * 异步 SHA-256 → 16 进制字符串（小写）
 *
 * 浏览器原生：crypto.subtle.digest
 * Node 环境（仅用于测试）：用 node:crypto 同步 API 后包成 Promise
 */
export async function sha256Hex(text: string): Promise<string> {
  const data = new TextEncoder().encode(text)
  const buf = await crypto.subtle.digest('SHA-256', data)
  return Array.from(new Uint8Array(buf))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}
