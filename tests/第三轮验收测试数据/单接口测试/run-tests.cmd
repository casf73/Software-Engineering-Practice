@echo off
setlocal
pushd "%~dp0..\..\..\project\mvp-v1.0\backend" || exit /b 1
if "%~1"=="" (
 call mvn -Psingle-api-tests test
) else (
 call mvn -Psingle-api-tests "-Dtest=%~1" test
)
set "test_exit_code=%ERRORLEVEL%"
popd
exit /b %test_exit_code%
