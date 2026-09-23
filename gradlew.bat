@echo off
setlocal
set "GRADLE_VERSION=8.2.1"
set "APP_HOME=%~dp0"
if "%GRADLE_USER_HOME%"=="" (
  set "CACHE_ROOT=%USERPROFILE%\.gradle\hoc-wrapper"
) else (
  set "CACHE_ROOT=%GRADLE_USER_HOME%\hoc-wrapper"
)
set "DIST_DIR=%CACHE_ROOT%\gradle-%GRADLE_VERSION%"
set "GRADLE_BIN=%DIST_DIR%\gradle-%GRADLE_VERSION%\bin\gradle.bat"
set "ZIP_FILE=%CACHE_ROOT%\gradle-%GRADLE_VERSION%-bin.zip"
set "DIST_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%GRADLE_BIN%" (
  if not exist "%ZIP_FILE%" (
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%DIST_URL%' -OutFile '%ZIP_FILE%'"
    if errorlevel 1 exit /b 1
  )
  if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
  mkdir "%DIST_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP_FILE%' '%DIST_DIR%'"
  if errorlevel 1 exit /b 1
)

cd /d "%APP_HOME%"
call "%GRADLE_BIN%" %*
endlocal
