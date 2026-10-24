@echo off
setlocal
pushd "%~dp0..\project\mvp-v1.0\backend" || exit /b 1
call mvn "-Dtest=IterationThreeFixtureIntegrationTest" test
set "test_exit_code=%ERRORLEVEL%"
popd
exit /b %test_exit_code%
