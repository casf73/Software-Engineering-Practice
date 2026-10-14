[CmdletBinding()]
param(
    [string]$Destination = (Join-Path $env:LOCALAPPDATA 'OnlineShoppingSystem')
)

$ErrorActionPreference = 'Stop'
$source = $PSScriptRoot
$jar = Join-Path $source 'app.jar'
if (-not (Test-Path -LiteralPath $jar)) {
    throw '安装包中缺少 app.jar。'
}
$destinationFull = [System.IO.Path]::GetFullPath($Destination)
New-Item -ItemType Directory -Path $destinationFull -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $destinationFull 'data'),(Join-Path $destinationFull 'logs'),(Join-Path $destinationFull 'run') -Force | Out-Null
foreach ($name in @('app.jar', 'start.ps1', 'stop.ps1', 'BUILD-INFO.txt')) {
    $item = Join-Path $source $name
    if (Test-Path -LiteralPath $item) {
        Copy-Item -LiteralPath $item -Destination (Join-Path $destinationFull $name) -Force
    }
}
Write-Host "安装完成：$destinationFull"
Write-Host "数据目录：$(Join-Path $destinationFull 'data')"
Write-Host "启动命令：& '$(Join-Path $destinationFull 'start.ps1')'"
