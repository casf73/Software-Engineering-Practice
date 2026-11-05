@echo off
chcp 65001 >nul
call "%~dp0第三轮验收测试数据\单接口测试\run-tests.cmd" %*
exit /b %ERRORLEVEL%
