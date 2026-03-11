@echo off
REM Build and run WaveShooter with Java 8 compatibility

:: ensure we have a in directory
if not exist bin mkdir bin

:: compile all sources for release 8 (classfile version 52)
javac --release 8 -d bin game\*.java
if errorlevel 1 (
    echo Compilation failed.
    exit /b 1
)

:: run the game using whatever java is on the PATH
java -cp bin game.Main
