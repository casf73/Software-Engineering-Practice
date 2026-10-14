[CmdletBinding()]
param(
    [string]$Tag = 'online-shopping-system:1.0.0'
)

$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$release = Join-Path $projectRoot 'release'
$jar = Join-Path $release 'app.jar'
if (-not (Test-Path -LiteralPath $jar)) {
    throw '缺少 release/app.jar；请先运行 scripts/build.ps1。'
}
$docker = Get-Command docker.exe -ErrorAction SilentlyContinue
if (-not $docker) {
    throw '本机没有 Docker CLI。请安装并启动 Docker Desktop，或在有 Docker Engine 的机器上运行此脚本。'
}
& $docker.Source info --format '{{.ServerVersion}}'
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Engine 未启动。'
}
& $docker.Source build --tag $Tag --file (Join-Path $release 'Dockerfile') $release
if ($LASTEXITCODE -ne 0) {
    throw 'Docker 镜像构建失败。'
}
$tar = Join-Path $projectRoot 'online-shopping-system-1.0.0-image.tar'
& $docker.Source save --output $tar $Tag
if ($LASTEXITCODE -ne 0) {
    throw 'Docker 镜像导出失败。'
}
$hash = (Get-FileHash -LiteralPath $tar -Algorithm SHA256).Hash
Write-Host "镜像：$Tag"
Write-Host "镜像文件：$tar"
Write-Host "SHA256：$hash"
