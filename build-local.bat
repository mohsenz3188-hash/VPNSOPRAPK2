@echo off
setlocal
set GRADLE_VERSION=8.9
set GRADLE_HOME_BASE=%USERPROFILE%\.gradle\vpnsopr-bootstrap
set GRADLE_HOME_LOCAL=%GRADLE_HOME_BASE%\gradle-%GRADLE_VERSION%

where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  set GRADLE_BIN=gradle
) else (
  if exist "%GRADLE_HOME_LOCAL%\bin\gradle.bat" (
    set GRADLE_BIN=%GRADLE_HOME_LOCAL%\bin\gradle.bat
  ) else (
    if not exist "%GRADLE_HOME_BASE%" mkdir "%GRADLE_HOME_BASE%"
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_HOME_BASE%\gradle-%GRADLE_VERSION%-bin.zip'; Expand-Archive -Force '%GRADLE_HOME_BASE%\gradle-%GRADLE_VERSION%-bin.zip' '%GRADLE_HOME_BASE%'"
    set GRADLE_BIN=%GRADLE_HOME_LOCAL%\bin\gradle.bat
  )
)

if "%ANDROID_HOME%"=="" if "%ANDROID_SDK_ROOT%"=="" (
  echo Set ANDROID_HOME or ANDROID_SDK_ROOT to your Android SDK folder first.
  exit /b 2
)

call %GRADLE_BIN% --no-daemon :app:assembleDebug
if errorlevel 1 exit /b %ERRORLEVEL%
echo.
echo APK: app\build\outputs\apk\debug\app-debug.apk
endlocal
