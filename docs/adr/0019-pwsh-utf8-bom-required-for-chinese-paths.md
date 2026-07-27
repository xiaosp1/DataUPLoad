# ADR-0019: PowerShell 脚本必须保存为 UTF-8 with BOM（中文路径）

- **状态**: ✅ 已发现并修复（2026-07-27 15:25）
- **触发**: W-FRONT-02-A PM 验收脚本 15 项 check 全 FAIL，但文件实际存在
- **影响范围**: 所有 PM 在 `E:\DEMO\数据采集\` 下创建的 .ps1 脚本

## 问题现象

PM 写的 verify 脚本第一版：
```powershell
$p = 'E:\DEMO\数据采集\DataupLoad-web\package.json'
Test-Path $p  # 返回 False（应该 True）
```

但**命令行直接调用同样代码**返回 True。

## 根因

PowerShell 5.1 在 Windows 上的默认行为：
- **脚本文件默认编码**：UTF-8 **无 BOM**
- **OEM codepage**：936 (GBK)
- **运行时解码**：PS 把 UTF-8 无 BOM 脚本当成 ANSI/GBK 解析 → 中文路径在内存里被错误解码
- **症状**：`Test-Path` / `[System.IO.File]::Exists` 都返回 False
- **副作用**：`Resolve-Path` 也救不了，因为路径字符串本身已经被破坏

## 解决

所有 PM 写的 .ps1 脚本**必须保存为 UTF-8 with BOM**：
```powershell
$content = Get-Content 'script.ps1' -Raw -Encoding UTF8
$utf8Bom = New-Object System.Text.UTF8Encoding $true  # $true = include BOM
[System.IO.File]::WriteAllText('script.ps1', $content, $utf8Bom)
```

或者直接用支持 BOM 的编辑器保存（如 VSCode 改编码为 "UTF-8 with BOM"）。

## 验证

修复后：
```powershell
$p = 'E:\DEMO\数据采集\DataupLoad-web\package.json'
Test-Path $p  # True ✅
[System.IO.File]::Exists($p)  # True ✅
Get-Content -LiteralPath $p  # 正常读取 ✅
```

## 已修复的脚本

- `scripts/verify-w-front-02-A.ps1`
- `scripts/backup-orphan.ps1`
- `scripts/cleanup-hik.ps1`
- `scripts/cleanup-hik2.ps1`

## 后续规范

- PM 写新 .ps1 时：用 VSCode 或 PowerShell ISE 保存（默认带 BOM），或者用上面的 BOM 添加脚本
- 验收脚本若发现 FAIL 但文件实际存在，第一时间检查 BOM
- 后续工单的 verify 脚本也照此规范
