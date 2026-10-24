@echo off
setlocal
pushd "%~dp0..\project\mvp-v1.0\backend" || exit /b 1
call mvn "-Dtest=StoreFlowIntegrationTest,IterationOneAdditionalFixtureIntegrationTest" test
set "test_exit_code=%ERRORLEVEL%"
popd
exit /b %test_exit_code%
