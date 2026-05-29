@echo off
if not exist out mkdir out

dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt

if errorlevel 1 (
    echo Compile failed.
    pause
    exit /b 1
)

java -cp "out;." game.main.Main
pause