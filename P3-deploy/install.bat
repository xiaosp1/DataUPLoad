@echo off
chcp 65001 >nul
REM ============================================================
REM  DataupLoad P3 一键安装脚本
REM  放在部署包根目录（与 server/ pg/ 同级），以管理员运行
REM  功能：1) 检查环境  2) 安装 PG  3) 启动 PG  4) 建库 intco
REM        5) 启动后端  6) 生成开机自启
REM ============================================================
setlocal enabledelayedexpansion

set ROOT=%~dp0
set SERVER=%ROOT%server
set PG_ROOT=%ROOT%pg
set PG_INSTALL=%PG_ROOT%postgres
set PG_DATA=%PG_INSTALL%\data
set PG_CTL=%PG_INSTALL%\bin\pg_ctl.exe
set PSQL=%PG_INSTALL%\bin\psql.exe

echo.
echo ==========================================
echo   DataupLoad P3 部署安装
echo   部署目录: %ROOT%
echo ==========================================
echo.

REM ---------- 1. 检查 JDK ----------
if not exist "%SERVER%\jdk\bin\hik-java.exe" (
  if not exist "%SERVER%\jdk\bin\java.exe" (
    echo [错误] 未找到 JDK: %SERVER%\jdk\bin\ 下没有 hik-java.exe/java.exe
    goto :end
  )
  set JAVAEXE=%SERVER%\jdk\bin\java.exe
) else (
  set JAVAEXE=%SERVER%\jdk\bin\hik-java.exe
)
echo [OK] JDK: %JAVAEXE%

REM ---------- 2. 检查 lib/classes ----------
if not exist "%SERVER%\lib" ( echo [错误] 缺少 lib 目录 & goto :end )
if not exist "%SERVER%\target\classes" ( echo [错误] 缺少 target\classes & goto :end )
echo [OK] 后端组件完整

REM ---------- 3. 安装 PG（若未装） ----------
if not exist "%PG_CTL%" (
  echo [安装] 静默安装 PostgreSQL ... 约 1-3 分钟
  if not exist "%PG_ROOT%\postgresql.exe" ( echo [错误] PG 安装包不存在 & goto :end )
  "%PG_ROOT%\postgresql.exe" --mode unattended --superpassword Abc12345 --serverport 5432 --prefix "%PG_INSTALL%" --datadir "%PG_DATA%"
  if errorlevel 1 ( echo [错误] PG 安装失败 & goto :end )
  echo [OK] PG 安装完成
) else (
  echo [OK] PG 已安装
)

REM ---------- 4. 启动 PG ----------
"%PG_CTL%" status -D "%PG_DATA%" >nul 2>&1
if errorlevel 1 (
  echo [启动] PostgreSQL ...
  "%PG_CTL%" start -D "%PG_DATA%" -l "%PG_DATA%\pg.log"
  if errorlevel 1 ( echo [错误] PG 启动失败，查看 %PG_DATA%\pg.log & goto :end )
  echo [OK] PG 已启动
) else (
  echo [OK] PG 已在运行
)

REM ---------- 5. 建库 intco（Flyway 自动建表） ----------
"%PSQL%" -h 127.0.0.1 -p 5432 -U postgres -c "SELECT 1 FROM pg_database WHERE datname='intco'" 2>nul | findstr /C:"1" >nul
if errorlevel 1 (
  echo [建库] 创建数据库 intco ...
  "%PSQL%" -h 127.0.0.1 -p 5432 -U postgres -c "CREATE DATABASE intco" 
  if errorlevel 1 ( echo [警告] 建库失败，后续 Flyway 会处理 & goto :start_server )
  echo [OK] 数据库 intco 创建完成
) else (
  echo [OK] 数据库 intco 已存在
)

REM 写入白名单种子（视觉产线 IP）
"%PSQL%" -h 127.0.0.1 -p 5432 -U postgres -d intco -c "INSERT INTO white_ip (ip) VALUES ('127.0.0.1'),('192.168.137.180'),('192.168.135.50'),('192.168.135.51'),('192.168.135.52'),('192.168.135.53'),('192.168.135.54'),('192.168.135.55'),('192.168.135.56'),('192.168.135.57'),('192.168.135.58'),('192.168.135.59'),('192.168.135.60'),('192.168.135.61'),('192.168.135.62'),('192.168.135.63'),('192.168.135.64'),('192.168.135.65'),('*.*.*.*') ON CONFLICT DO NOTHING" >nul 2>&1
echo [OK] 白名单已写入

:start_server
REM ---------- 6. 启动后端 ----------
echo [启动] 后端服务 (port 8080) ...
cd /d "%SERVER%"
start "DataupLoad" /B "%JAVAEXE%" -cp "lib\*;target\classes" -Dfile.encoding=UTF-8 -Dspring.config.location=./config/ -Dspring.config.name=application -Dserver.port=8080 com.hikrobotics.solution.Application
echo [OK] 后端启动指令已下发（约 30 秒后可通过 http://127.0.0.1:8080 访问）

REM ---------- 7. 开机自启 ----------
echo [配置] 生成开机自启 ...
set STARTUP=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
(
echo @echo off
echo cd /d "%SERVER%"
echo start "DataupLoad" /B "%JAVAEXE%" -cp "lib\*;target\classes" -Dfile.encoding=UTF-8 -Dspring.config.location=./config/ -Dspring.config.name=application -Dserver.port=8080 com.hikrobotics.solution.Application
) > "%STARTUP%\DataupLoad.bat"
echo [OK] 开机自启已配置

echo.
echo ==========================================
echo   安装完成！
echo   Web 地址: http://127.0.0.1:8080
echo   账号:     super_admin / Abc12345
echo   PG 端口:  5432 (密码 Abc12345)
echo ==========================================
echo.

:end
pause
endlocal
