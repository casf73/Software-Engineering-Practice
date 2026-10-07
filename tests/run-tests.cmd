@echo off
setlocal
pushd "%~dp0..\backend" || exit /b 1
call mvn test
set "test_exit_code=%ERRORLEVEL%"
popd
exit /b %test_exit_code%
