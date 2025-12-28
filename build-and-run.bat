@echo off
REM SDPTool Build and Run Script (Windows Batch)
REM Quick build and run for SDPTool

setlocal

set MAVEN_HOME=C:\apache-maven-3.9.11
set PROJECT_DIR=C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main
set MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd

echo ========================================
echo    SDPTool Build and Run Script
echo    Software Defect Prediction Tool
echo ========================================
echo.

REM Check Maven
echo [1/4] Checking Maven...
if not exist "%MAVEN_CMD%" (
    echo ERROR: Maven not found at %MAVEN_CMD%
    echo Please install Maven or update the path in this script
    pause
    exit /b 1
)
echo Maven found: %MAVEN_HOME%
echo.

REM Check Java
echo [2/4] Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found. Please install JDK 11+
    pause
    exit /b 1
)
echo Java found
echo.

REM Navigate to project
cd /d "%PROJECT_DIR%"

REM Build
echo [3/4] Building project...
echo Running: mvn clean package -DskipTests
echo This may take a few minutes on first build...
echo.

call "%MAVEN_CMD%" clean package -DskipTests

if errorlevel 1 (
    echo.
    echo Build failed!
    echo Check the error messages above.
    pause
    exit /b 1
)

echo.
echo Build successful!
echo.

REM Check artifact
if exist "target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar" (
    echo Build Artifact:
    echo   File: SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar
    dir "target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar" | findstr "SDPTool"
    echo.
) else (
    echo ERROR: JAR file not found!
    pause
    exit /b 1
)

REM Run application
echo [4/4] Starting SDPTool...
echo Running: java -jar target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar
echo.
echo Press Ctrl+C to stop the application
echo.

java -jar "target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar"

echo.
echo SDPTool closed
pause
