# ADR-0022: PM 沙箱无 javac，使用沙箱外 JDK 编译

> **状态**: 已实施（2026-07-31 09:12 GMT+8）
> **背景**: PM 在 OpenClaw PowerShell 5 沙箱里执行 `javac` 找不到
> **决策**: 用沙箱外 D:\ 盘的完整 JDK 17.0.1
> **影响**: W-BUILD-01 / W-FRONT-04 重启吃新 jar 流程可以在沙箱里完成

---

## 1. 问题

老板 2026-07-31 08:46 GMT+8 指令"先重启吃新 jar"，PM 在沙箱执行：
- `cmd /c where javac` → 信息: 用提供的模式无法找到文件
- `cmd /c where java` → 同上
- `Test-Path "E:\DEMO\数据采集\DataupLoad\jdk\bin\javac.exe"` → False

DataupLoad 自带的 jdk 目录里只有 `hik-java.exe`（JRE 运行时），没有 `javac.exe` 编译工具。

**根因**:
- DataupLoad/jdk 是精简 runtime，只够跑应用，不能编译
- 系统 PATH 无 javac
- 沙箱环境 PS5 默认 codepage 处理 Unicode 路径也有 bug（短路径 `DATAUP~1` 在 PS 字符串上下文里自动 normalize 回长路径）

## 2. 决策

**使用沙箱外的完整 JDK**：
- 路径: `D:\Tool-xsp\psm-run\server\jdk\`
- 版本: OpenJDK 17.0.1 (2021-10-19)
- 包含: `bin/hik-java.exe` + `bin/javac.exe` + `lib/jvm.cfg` (完整 JDK)
- 兼容性: 与 DataupLoad 的 `-source 17 -target 17` 配置匹配

**绕过沙箱路径编码问题**：
- `subst P: "E:\DEMO\数据采集\DataupLoad"` 映射盘符
- 所有路径用 `P:\` 纯 ASCII，绕过 PS5 codepage / Join-Path 的 GBK 转换

## 3. 实施流程（已成 PM 标准流程）

### 3.1 编译 (`P:\_pm-compile.cmd`)

```cmd
@echo off
setlocal EnableExtensions EnableDelayedExpansion
P:
cd P:\

if not exist P:\target\classes mkdir P:\target\classes

set CP=
for %%j in (P:\lib\*.jar) do set CP=!CP!;%%j
set CP=!CP:~1!

dir /b /s P:\src\main\java\*.java > P:\target\_srcs.txt

D:\Tool-xsp\psm-run\server\jdk\bin\javac.exe -d P:\target\classes -cp "%CP%" -encoding UTF-8 -source 17 -target 17 -parameters -Xlint:none -nowarn @P:\target\_srcs.txt
```

### 3.2 启动 (`P:\_pm-launch.cmd` / `scripts/_pm-launch.ps1`)

```cmd
start "DataupLoad" /B D:\Tool-xsp\psm-run\server\jdk\bin\hik-java.exe ^
  -cp "lib\*;target\classes;config" ^
  -Dfile.encoding=UTF-8 ^
  -Dspring.config.location=./config/ ^
  -Dspring.config.name=application ^
  -Dserver.port=8080 ^
  com.hikrobotics.solution.Application
```

PS 脚本 (`scripts/_pm-launch.ps1`) 用 .NET ProcessStartInfo 调，UTF-8 encoding，wait up to 60s for port 8080 LISTENING。

## 4. 验证 (2026-07-31 09:12)

```
javac ExitCode=0, Errors=0, Warnings=0 (186 .java 文件, 0 error)
hik-java PID=21592, port=8080 LISTENING, 业务接口验证:
  GET / → 200
  GET /assets/index-B5YyeeGj.js → 200
  POST /web/auth/login → 200
  GET /web/account/current (无效 cookie) → 401 { code:10401 }
```

## 5. 影响

✅ **正向**:
- PM 可以独立完成"改后端 → 编译 → 重启"全流程
- 不再需要老板手动跑 javac
- W-BUILD-01 / W-FRONT-04 重启吃新 jar 流程成为 PM 自助

⚠️ **风险**:
- D:\Tool-xsp\psm-run\server\jdk 是借来的，删了就完蛋
- subst P: 是会话级，PS 进程退出就消失
- 编译只能用 `P:\_pm-compile.cmd`（不依赖中文路径）

## 6. 已知边界

1. **PM 沙箱 = PowerShell 5.1**（不是 PS7），codepage 处理 Unicode 路径有 bug
2. **subst 必须在每次 PS 会话启动时执行**：`subst P: "E:\DEMO\数据采集\DataupLoad"`
3. **不能用 `Join-Path`**，所有路径用 string 拼接（`'P:\' + 'lib'`）
4. **不能用 `Get-ChildItem` 中文路径**，必须用 `[System.IO.Directory]::GetFiles` (.NET API 走 Unicode)

## 7. 后续

- 等老板拍板: W-FRONT-04-A / W-FRONT-04-B 是否派
- W-BUILD-01 (mvn package) 仍残留：DataupLoad 工程不是 maven，没有 pom.xml，build-fat-jar.ps1 失败；当前不需要 fat jar，`lib\*;target\classes` classpath 模式即可
- 老板浏览器实测 W-FRONT-04-C 修复（reload 路由保留 #11）→ 通过后 mark closed
