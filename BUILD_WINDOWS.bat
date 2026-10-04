@echo off
setlocal
cd /d "%~dp0"
call gradlew.bat clean assembleDebug
if errorlevel 1 exit /b 1
echo.
echo APK: app\build\outputs\apk\debug\app-debug.apk
pause
