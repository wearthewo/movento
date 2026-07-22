@ECHO OFF
SETLOCAL
SET "MAVEN_VERSION=3.9.9"
SET "MAVEN_DIR=%~dp0.mvn\apache-maven-%MAVEN_VERSION%"
IF NOT EXIST "%MAVEN_DIR%\bin\mvn.cmd" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $zip='%TEMP%\movento-maven.zip'; Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile $zip; Expand-Archive -Force $zip '%~dp0.mvn'; Remove-Item $zip"
  IF ERRORLEVEL 1 EXIT /B 1
)
CALL "%MAVEN_DIR%\bin\mvn.cmd" %*
