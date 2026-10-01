@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

echo ==================================================
echo Chatyar Android - Windows bootstrap and debug build
echo ==================================================
echo.

if not defined JAVA_HOME (
    if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
    ) else if exist "%ProgramFiles%\Android\Android Studio\jre\bin\java.exe" (
        set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jre"
    )
)

where java >nul 2>nul
if errorlevel 1 (
    if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
)

where java >nul 2>nul
if errorlevel 1 (
    echo ERROR: Java 17+ was not found.
    echo Install Android Studio, then run this file again.
    pause
    exit /b 1
)

if not defined ANDROID_HOME (
    if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
)

if not defined ANDROID_HOME (
    echo ERROR: Android SDK was not found.
    echo Open Android Studio once and install Android SDK 35.
    pause
    exit /b 1
)

if not exist local.properties (
    set "SDK_ESC=!ANDROID_HOME:\=\\!"
    > local.properties echo sdk.dir=!SDK_ESC!
)

set "GRADLE_VERSION=8.9"
set "BOOT_DIR=%CD%\.gradle-bootstrap"
set "GRADLE_DIR=%BOOT_DIR%\gradle-%GRADLE_VERSION%"
set "GRADLE_ZIP=%BOOT_DIR%\gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%GRADLE_DIR%\bin\gradle.bat" (
    if not exist "%BOOT_DIR%" mkdir "%BOOT_DIR%"
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_ZIP%'"
    if errorlevel 1 (
        echo ERROR: Gradle download failed. Check your internet connection.
        pause
        exit /b 1
    )
    echo Extracting Gradle...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%GRADLE_ZIP%' -DestinationPath '%BOOT_DIR%' -Force"
    if errorlevel 1 (
        echo ERROR: Could not extract Gradle.
        pause
        exit /b 1
    )
)

if not exist gradlew.bat (
    echo Creating Gradle wrapper...
    call "%GRADLE_DIR%\bin\gradle.bat" --no-daemon wrapper --gradle-version %GRADLE_VERSION% --distribution-type bin
    if errorlevel 1 (
        echo ERROR: Could not create the Gradle wrapper.
        pause
        exit /b 1
    )
)

echo Building Chatyar debug APK...
call gradlew.bat --no-daemon assembleDebug
if errorlevel 1 (
    echo.
    echo BUILD FAILED. Read the Gradle error above.
    pause
    exit /b 1
)

echo.
echo BUILD SUCCESSFUL
echo APK: app\build\outputs\apk\debug\app-debug.apk
start "" "app\build\outputs\apk\debug"
pause
