@echo off
setlocal
set BASE_DIR=%~dp0
set MAVEN_VERSION=3.9.9
set MAVEN_HOME=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%
set ARCHIVE=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%-bin.tar.gz
set URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.tar.gz
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  if not exist "%BASE_DIR%.mvn" mkdir "%BASE_DIR%.mvn"
  curl.exe --fail --location --output "%ARCHIVE%" "%URL%" || exit /b 1
  tar.exe -xzf "%ARCHIVE%" -C "%BASE_DIR%.mvn" || exit /b 1
)
call "%MAVEN_HOME%\bin\mvn.cmd" %*
