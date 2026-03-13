@echo off
cd /d "C:\Users\jwate\VSCode\WaveShooter-VSCode\WaveShooter"

REM Recompile
echo Recompiling...
javac -cp "bin;lib\jinput-2.0.7.jar" -d bin game\*.java 2>&1 >compile.log

REM Remove old log
if exist gamepad.log del gamepad.log

REM Run game briefly and capture output
echo Testing...
timeout /t 1 >nul
start /min java -Djava.library.path="lib\natives" -cp "bin;lib\jinput-2.0.7.jar" game.Main

REM Wait a bit for initialization
timeout /t 2 >nul

REM Kill game process
taskkill /im java.exe /f >nul 2>&1

REM Wait a bit more to ensure file is written
timeout /t 1 >nul

REM Show what we have
echo.
echo === Contents of gamepad.log ===
if exist gamepad.log (
    type gamepad.log
) else (
    echo gamepad.log not found
)
