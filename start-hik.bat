@echo off
X:
cd X:\DataupLoad
start "" /B jdk\bin\hik-java.exe -cp lib\*;target\classes -Dfile.encoding=UTF-8 -Dspring.config.location=classpath:/,file:X:/DataupLoad/config/ -Dspring.config.name=application -Dserver.port=80 com.hikrobotics.solution.Application
