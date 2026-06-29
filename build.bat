@echo off
setlocal
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot
echo JAVA_HOME установлен: %JAVA_HOME%
echo.
echo Сборка мода...
gradlew.bat clean build
echo.
pause