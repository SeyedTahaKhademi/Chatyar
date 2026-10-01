@echo off
setlocal
cd /d "%~dp0"
set "STUDIO=%ProgramFiles%\Android\Android Studio\bin\studio64.exe"
if exist "%STUDIO%" (
    start "" "%STUDIO%" "%CD%"
    exit /b 0
)
echo Android Studio was not found in the default location.
echo Open Android Studio manually and select this folder:
echo %CD%
pause
