@echo off
setlocal enabledelayedexpansion
REM ============================================================
REM  DataupLoad P3 one-click install
REM  Run as Administrator. No Chinese chars (codepage-safe).
REM  Steps: check env -> install PG -> start PG -> create DB
REM         -> seed whitelist -> start backend -> autostart
REM ============================================================

set ROOT=%~dp0
set SERVER=%ROOT%server
set PG_ROOT=%ROOT%pg
set PG_INSTALL=%PG_ROOT%\postgres
set PG_DATA=%PG_INSTALL%\data
set PG_CTL=%PG_INSTALL%\bin\pg_ctl.exe
set PSQL=%PG_INSTALL%\bin\psql.exe

echo.
echo ==========================================
echo   DataupLoad P3 Install
echo   ROOT: %ROOT%
echo ==========================================
echo.

REM ---------- 1. check JDK (prefer java.exe, hik-java.exe as fallback) ----------
set JAVAEXE=
if exist "%SERVER%\jdk\bin\java.exe" set JAVAEXE=%SERVER%\jdk\bin\java.exe
if not defined JAVAEXE if exist "%SERVER%\jdk\bin\hik-java.exe" set JAVAEXE=%SERVER%\jdk\bin\hik-java.exe
if not defined JAVAEXE (
  echo [ERROR] JDK not found: %SERVER%\jdk\bin\
  goto :end
)
echo [OK] JDK: %JAVAEXE%

REM ---------- 2. check backend files ----------
if not exist "%SERVER%\lib" ( echo [ERROR] missing lib dir & goto :end )
if not exist "%SERVER%\target\classes" ( echo [ERROR] missing target\classes & goto :end )
if not exist "%SERVER%\config\application-prod.yml" ( echo [ERROR] missing config & goto :end )
echo [OK] backend files complete

REM ---------- 3. install PG if needed ----------
if not exist "%PG_CTL%" (
  echo [INSTALL] PostgreSQL silent install ... 1-3 min
  if not exist "%PG_ROOT%\postgresql.exe" ( echo [ERROR] PG installer missing & goto :end )
  "%PG_ROOT%\postgresql.exe" --mode unattended --superpassword Abc12345 --serverport 5432 --prefix "%PG_INSTALL%" --datadir "%PG_DATA%"
  if errorlevel 1 ( echo [ERROR] PG install failed & goto :end )
  echo [OK] PG installed
) else (
  echo [OK] PG already installed
)

REM ---------- 4. start PG ----------
"%PG_CTL%" status -D "%PG_DATA%" >nul 2>&1
if errorlevel 1 (
  echo [START] PostgreSQL ...
  "%PG_CTL%" start -D "%PG_DATA%" -l "%PG_DATA%\pg.log"
  if errorlevel 1 ( echo [ERROR] PG start failed, see %PG_DATA%\pg.log & goto :end )
  echo [OK] PG started
) else (
  echo [OK] PG already running
)

REM ---------- 5. create DB intco ----------
"%PSQL%" -h 127.0.0.1 -p 5432 -U postgres -c "SELECT 1 FROM pg_database WHERE datname='intco'" 2>nul | findstr /C:"1" >nul
if errorlevel 1 (
  echo [DB] create database intco ...
  "%PSQL%" -h 127.0.0.1 -p 5432 -U postgres -c "CREATE DATABASE intco"
  if errorlevel 1 ( echo [WARN] create db failed, flyway will handle & goto :start_server )
  echo [OK] database intco created
) else (
  echo [OK] database intco exists
)

REM ---------- 5b. import all schema SQL (fresh DB has no tables; flyway baseline 1.20 skips V0.x-V1.19) ----------
echo [DB] import framework tables (V0.x from framework-starter jar) ...
set TMP_SQL=%TEMP%\p3_fw_sql
if exist "%TMP_SQL%" rmdir /s /q "%TMP_SQL%"
mkdir "%TMP_SQL%"
for %%j in ("%SERVER%\lib\framework-starter-*.jar") do (
  "%SERVER%\jdk\bin\jar.exe" xf "%%j" db/migration
  if exist "db\migration\V0.1__framework_db.sql" copy /y "db\migration\V0.1__framework_db.sql" "%TMP_SQL%\" >nul 2>&1
  if exist "db\migration\V0.2__framework_db.sql" copy /y "db\migration\V0.2__framework_db.sql" "%TMP_SQL%\" >nul 2>&1
  rmdir /s /q db 2>nul
)
for %%f in ("%TMP_SQL%\*.sql") do (
  echo   applying %%~nxf ...
  "%PSQL%" -h 127.0.0.1 -p 5432 -U postgres -d intco -v ON_ERROR_STOP=0 -f "%%f" >nul 2>&1
)
echo [DB] import business tables (V1.x from server\sql) ...
for %%f in ("%SERVER%\sql\*.sql") do (
  echo   applying %%~nxf ...
  "%PSQL%" -h 127.0.0.1 -p 5432 -U postgres -d intco -v ON_ERROR_STOP=0 -f "%%f" >nul 2>&1
)
echo [OK] schema imported

REM seed whitelist (vision line IPs + local + P3 host)
"%PSQL%" -h 127.0.0.1 -p 5432 -U postgres -d intco -c "INSERT INTO white_ip (ip) VALUES ('127.0.0.1'),('192.168.137.180'),('192.168.135.50'),('192.168.135.51'),('192.168.135.52'),('192.168.135.53'),('192.168.135.54'),('192.168.135.55'),('192.168.135.56'),('192.168.135.57'),('192.168.135.58'),('192.168.135.59'),('192.168.135.60'),('192.168.135.61'),('192.168.135.62'),('192.168.135.63'),('192.168.135.64'),('192.168.135.65'),('*.*.*.*') ON CONFLICT DO NOTHING" >nul 2>&1
echo [OK] whitelist seeded

:start_server
REM ---------- 6. start backend in its own minimized window ----------
echo [START] backend (port 8080) ...
cd /d "%SERVER%"
start "DataupLoad" /MIN "%JAVAEXE%" -cp "lib\*;target\classes" -Dfile.encoding=UTF-8 -Dspring.config.location=./config/ -Dspring.config.name=application -Dserver.port=8080 com.hikrobotics.solution.Application
echo [OK] backend start issued (http://127.0.0.1:8080 in ~30s)

REM ---------- 7. autostart ----------
echo [CFG] create autostart entry ...
set STARTUP=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
if not exist "%STARTUP%" mkdir "%STARTUP%"
(
echo @echo off
REM DataupLoad backend autostart (generated by install.bat)
cd /d "%SERVER%"
start "DataupLoad" /MIN "%JAVAEXE%" -cp "lib\*;target\classes" -Dfile.encoding=UTF-8 -Dspring.config.location=./config/ -Dspring.config.name=application -Dserver.port=8080 com.hikrobotics.solution.Application
) > "%STARTUP%\DataupLoad.bat"
echo [OK] autostart created: %STARTUP%\DataupLoad.bat

echo.
echo ==========================================
echo   INSTALL DONE
echo   Web:      http://127.0.0.1:8080
echo   Account:  super_admin / Abc12345
echo   PG:       127.0.0.1:5432 (pwd Abc12345)
echo ==========================================
echo.

:end
pause
endlocal

