[CmdletBinding()]
param(
    [int]$Port = 8080,
    [string]$BindAddress = '127.0.0.1'
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$jar = Join-Path $root 'app.jar'
if (-not (Test-Path -LiteralPath $jar)) {
    throw "找不到 app.jar：$jar"
}
$java = (Get-Command java.exe -ErrorAction Stop).Source
$majorLine = (& $java -version 2>&1 | Select-Object -First 1).ToString()
if ($majorLine -notmatch '"(\d+)\.') {
    throw "无法识别 Java 版本：$majorLine"
}
if ([int]$Matches[1] -lt 17) {
    throw "需要 Java 17 或更高版本：$majorLine"
}
$runDir = Join-Path $root 'run'
$logsDir = Join-Path $root 'logs'
New-Item -ItemType Directory -Path $runDir,$logsDir,(Join-Path $root 'data') -Force | Out-Null
$pidFile = Join-Path $runDir 'app.pid'
if (Test-Path -LiteralPath $pidFile) {
    $oldPid = [int](Get-Content -LiteralPath $pidFile -Raw).Trim()
    if (Get-Process -Id $oldPid -ErrorAction SilentlyContinue) {
        throw "已有运行中的进程 PID $oldPid；请先运行 stop.ps1。"
    }
    Remove-Item -LiteralPath $pidFile -Force
}
$arguments = @('-jar', ('"{0}"' -f $jar), "--server.address=$BindAddress", "--server.port=$Port")
$process = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $root -RedirectStandardOutput (Join-Path $logsDir 'server.log') -RedirectStandardError (Join-Path $logsDir 'server-error.log') -WindowStyle Hidden -PassThru
"$($process.Id)|$($process.StartTime.ToUniversalTime().Ticks)" | Set-Content -LiteralPath $pidFile -Encoding ascii
Write-Host "已启动，PID：$($process.Id)"
Write-Host "访问地址：http://$BindAddress`:$Port/"
Write-Host "日志目录：$logsDir"
