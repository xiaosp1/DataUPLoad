@echo off
cd /d E:\DEMO\数据采集\DataupLoad
E:\DEMO\数据采集\DataupLoad\jdk\bin\hik-java.exe -cp "lib\*;target\classes" -Dfile.encoding=UTF-8 -Dspring.config.location=./config/ -Dspring.config.name=application -Dserver.port=80 com.hikrobotics.solution.Application
