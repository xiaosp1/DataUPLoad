@echo off
chcp 65001 > nul
set BASE=E:\DEMO\数据采集
set OUT=%BASE%\heartbeat-current.txt
set YML=%BASE%\DataupLoad\config\application-prod.yml
set LOG=%BASE%\DataupLoad\log\DataupLoad\info.log

set PROC=
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":80.*LISTENING"') do set PROC=%%p

set PG=
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":5433.*LISTENING"') do set PG=%%p

findstr /C:"enable: true" "%YML%" > nul
if %errorlevel%==0 (set YK=[ON]) else (set YK=[OFF-MELTDOWN])

set WORKERS=
for /f %%i in ('tasklist /fi "imagename eq codex.exe" ^| find /c "codex.exe"') do set WORKERS=%%i

set ALARM=
if exist "%LOG%" (
    powershell -ExecutionPolicy Bypass -Command "Get-Content '%LOG%' -Tail 30 | Select-String -Pattern 'alarm|Alarm|YK' | Select-Object -First 5 -ExpandProperty Line" > "%BASE%\heartbeat-log-tmp.txt" 2>nul
    set /p ALARM=<"%BASE%\heartbeat-log-tmp.txt"
    del "%BASE%\heartbeat-log-tmp.txt" 2>nul
)

(
echo [PM HEARTBEAT %date% %time:~0,8%]
echo - hik-java 80 port PID: %PROC%
echo - PG 5433 PID: %PG%
echo - yk push: %YK%
echo - Worker procs: %WORKERS%
echo - last alarm: %ALARM%
) > "%OUT%"

type "%OUT%"
