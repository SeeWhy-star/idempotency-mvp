@echo off
setlocal
set BASE_DIR=%~dp0
set MAVEN_VERSION=3.9.16
set MAVEN_HOME=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%
set ARCHIVE=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%-bin.tar.gz
set URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.tar.gz
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  if not exist "%BASE_DIR%.mvn" mkdir "%BASE_DIR%.mvn"
  curl.exe --fail --location --output "%ARCHIVE%" "%URL%" || exit /b 1
  for /f %%i in ('powershell.exe -NoProfile -Command "(Get-FileHash -Algorithm SHA256 -LiteralPath '%ARCHIVE%').Hash.ToLower()"') do set ACTUAL_SHA256=%%i
  if /I not "%ACTUAL_SHA256%"=="80ffca22aed9e8b9713a232f3394fd81d7f20322df75efdb2b047dbd3e3a23bb" (
    echo Maven distribution checksum mismatch 1>&2
    exit /b 1
  )
  tar.exe -xzf "%ARCHIVE%" -C "%BASE_DIR%.mvn" || exit /b 1
)
call "%MAVEN_HOME%\bin\mvn.cmd" %*
