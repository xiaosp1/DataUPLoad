@echo off
setlocal enabledelayedexpansion
set JAVA_HOME=X:\DataupLoad\jdk
set SRC=X:\DataupLoad\src\main\java
set CP=X:\DataupLoad\target\classes
for %%j in (X:\DataupLoad\lib\*.jar) do set CP=!CP!;%%j
set OUT=X:\DataupLoad\target\classes

dir /s /b %SRC%\*.java > X:\sources.txt
"%JAVA_HOME%\bin\javac" -encoding UTF-8 -parameters -cp "%CP%" -d "%OUT%" @X:\sources.txt 2> X:\compile.err
set EXITCODE=%ERRORLEVEL%
echo javac exit code: %EXITCODE%
if %EXITCODE% NEQ 0 (
    echo === ERRORS ===
    type X:\compile.err
)
endlocal
exit /b %EXITCODE%
