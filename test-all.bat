@echo off
cd /d "%~dp0..\back_end"
echo ========================================
echo   Netdisk - Run All Unit Tests
echo ========================================
call mvn test
echo.
echo ========================================
echo   Test reports:
echo   %~dp0..\back_end\target\surefire-reports\
echo ========================================
pause
