@echo off
REM Build and run WaveShooter; automatically prefer a modern JDK if available

:: try to find a JDK‑24 java on the PATH first
set "JAVACMD="
for /f "delims=" %%i in ('where java 2^>nul') do (
    echo checking PATH candidate: %%i
    echo %%i | findstr /i "jdk-24" >nul && set "JAVACMD=%%i"
)

:: if the PATH scan didn't find anything, fall back to JAVA_HOME (it may still be 1.8)
if not defined JAVACMD if defined JAVA_HOME (
    set "JAVACMD=%JAVA_HOME%\bin\java"
)

:: if we still have no candidate, try the standard jdk-24 install
if not defined JAVACMD if exist "C:\Program Files\Java\jdk-24\bin\java.exe" (
    set "JAVACMD=C:\Program Files\Java\jdk-24\bin\java.exe"
)

:: final fallback: whichever "java" the shell finds
if not defined JAVACMD set "JAVACMD=java"

:: helper: show what java we're about to invoke
echo Using JVM: %JAVACMD%
"%JAVACMD%" -version

:: ensure we have a bin directory
if not exist bin mkdir bin

:: copy audio resources to bin directory
if exist audio xcopy audio bin\audio /E /I /Y >nul

:: compile sources (use JAVA_HOME javac if defined, otherwise whatever is on PATH)
if defined JAVA_HOME (
    "%JAVA_HOME%\bin\javac" --release 8 -d bin game\*.java 2>nul || javac --release 8 -d bin game\*.java
) else (
    javac --release 8 -d bin game\*.java
)
if errorlevel 1 (
    echo Compilation failed.
    exit /b 1
)

:: run the game with the selected JVM
"%JAVACMD%" -cp bin game.Main
