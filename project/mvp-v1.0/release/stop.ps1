[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$pidFile = Join-Path $PSScriptRoot 'run\app.pid'
if (-not (Test-Path -LiteralPath $pidFile)) {
    Write-Host '没有 PID 文件，服务未由 start.ps1 启动。'
    return
}
$record = (Get-Content -LiteralPath $pidFile -Raw).Trim().Split('|')
if ($record.Length -ne 2) {
    throw 'PID 文件格式不正确，请先核对运行中的 Java 进程。'
}
$appPid = [int]$record[0]
$startTicks = [long]$record[1]
$process = Get-Process -Id $appPid -ErrorAction SilentlyContinue
if (-not $process) {
    Remove-Item -LiteralPath $pidFile -Force
    Write-Host '进程已退出，已清理 PID 文件。'
    return
}
if ($process.ProcessName -notmatch '^javaw?$' -or $process.StartTime.ToUniversalTime().Ticks -ne $startTicks) {
    throw "PID $appPid 不是此脚本启动的 Java 进程；为避免误停其他程序，已停止操作。"
}
Stop-Process -Id $appPid -Force
Remove-Item -LiteralPath $pidFile -Force
Write-Host "已停止服务，PID：$appPid"
