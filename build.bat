@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ============================================
echo   Netdisk - Full Build
echo ============================================
echo.

REM ---------- 1. Frontend ----------
echo [1/4] Building frontend...
cd front_end
call npx vite build
if %errorlevel% neq 0 (
    echo [FAIL] Frontend build failed!
    exit /b %errorlevel%
)
cd ..
echo [OK] Frontend built -> back_end/src/main/resources/static/
echo.

REM ---------- 2. Backend compile ----------
echo [2/4] Compiling backend...
cd back_end
call mvn compile -q
if %errorlevel% neq 0 (
    echo [FAIL] Backend compile failed!
    exit /b %errorlevel%
)
echo [OK] Backend compiled.
echo.

REM ---------- 3. Unit tests ----------
echo [3/4] Running unit tests...
call mvn test -q
if %errorlevel% neq 0 (
    echo [FAIL] Tests failed! Check target/surefire-reports/
    exit /b %errorlevel%
)
echo [OK] All tests passed.
echo.

REM ---------- 4. Package jar ----------
echo [4/4] Packaging jar...
call mvn package -DskipTests -q
if %errorlevel% neq 0 (
    echo [FAIL] Package failed!
    exit /b %errorlevel%
)
echo [OK] Package complete.
echo.

echo ============================================
echo   Build successful!
echo   Output: back_end\target\Netdisk-0.0.1-SNAPSHOT.jar
echo ============================================
echo.
echo Run with: java -jar back_end\target\Netdisk-0.0.1-SNAPSHOT.jar
pause
