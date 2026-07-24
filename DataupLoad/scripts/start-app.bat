@echo off
setlocal
cd /d E:\DEMO\数据采集\DataupLoad
set CP=lib\*;target\classes
start "DataupLoad" /B "E:\DEMO\数据采集\DataupLoad\jdk\bin\hik-java.exe" -cp "lib\*;target\classes" -Dfile.encoding=UTF-8 -Dspring.config.location=./config/ -Dspring.config.name=application -Dserver.port=80 com.hikrobotics.solution.Application
endlocal
