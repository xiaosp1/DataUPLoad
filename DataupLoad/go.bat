@echo off
setlocal enabledelayedexpansion
set CP=X:\DataupLoad\target\classes
for %%j in (X:\DataupLoad\lib\*.jar) do set CP=!CP!;%%j
start "" /B X:\DataupLoad\jdk\bin\java.exe -cp "!CP!" -Dfile.encoding=UTF-8 -Dspring.config.location=classpath:/,file:X:\DataupLoad\config/ -Dspring.config.name=application -Dserver.port=80 com.hikrobotics.solution.Application > X:\app.log 2>&1
endlocal
