[CmdletBinding()]
param(
    [string]$SellerPassword = '123456',
    [string]$ProxyUrl,
    [switch]$SkipImageExport
)
$ErrorActionPreference = 'Stop'
if ($ProxyUrl) {
    $env:HTTP_PROXY = $ProxyUrl
    $env:HTTPS_PROXY = $ProxyUrl
    $env:NO_PROXY = 'localhost,127.0.0.1,::1'
}
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$release = Join-Path $projectRoot 'release'
$dockerCommand = Get-Command docker.exe -ErrorAction SilentlyContinue
$docker = if ($dockerCommand) { $dockerCommand.Source } else { $null }
if (-not $docker) {
    foreach ($candidate in @(
        (Join-Path $env:LOCALAPPDATA 'Programs\DockerDesktop\resources\bin\docker.exe'),
        'C:\Program Files\Docker\Docker\resources\bin\docker.exe'
    )) {
        if (Test-Path -LiteralPath $candidate) { $docker = $candidate; break }
    }
}
if (-not $docker) { throw '找不到 Docker。请先安装并启动 Docker Desktop。' }
$env:PATH = (Split-Path -Parent $docker) + [IO.Path]::PathSeparator + $env:PATH
& $docker info --format '{{.ServerVersion}}'
if ($LASTEXITCODE -ne 0) { throw 'Docker Engine 未就绪。请先启动 Docker Desktop；如果刚启用 WSL/虚拟机平台，请先重启 Windows。' }
if (-not (Test-Path -LiteralPath (Join-Path $release 'app.jar'))) { throw '缺少 release/app.jar，请先执行 scripts/build.ps1。' }
$envFile = Join-Path $release '.env'
if (-not (Test-Path -LiteralPath $envFile)) {
    if ($SellerPassword -match '[\r\n]') { throw '密码不能包含换行符。' }
    'APP_SELLER_PASSWORD=' + $SellerPassword | Set-Content -LiteralPath $envFile -Encoding utf8
}
Push-Location $release
try {
    & $docker compose config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Compose 配置检查失败。' }
    & $docker compose up --build -d
    if ($LASTEXITCODE -ne 0) { throw '容器构建或启动失败。' }
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        try {
            $homeResponse = Invoke-WebRequest -Uri 'http://localhost:8080/' -UseBasicParsing -TimeoutSec 5
            $login = Invoke-WebRequest -Uri 'http://localhost:8080/seller/login' -UseBasicParsing -TimeoutSec 5
            $session = Invoke-RestMethod -Uri 'http://localhost:8080/api/seller/session' -TimeoutSec 5
            if ($homeResponse.StatusCode -eq 200 -and $login.StatusCode -eq 200) { $ready = $true; break }
        } catch { Start-Sleep -Seconds 2 }
    }
    if (-not $ready) {
        & $docker compose logs --tail 100 store
        throw '容器已启动，但网页未在等待时间内就绪。'
    }
    $imageDetails = (& $docker image inspect online-shopping-system:1.0.0 --format '{{.Id}} {{.Os}}/{{.Architecture}}') -join ''
    $containerDetails = (& $docker compose ps) -join [Environment]::NewLine
    $imageTar = Join-Path $projectRoot 'online-shopping-system-1.0.0-image.tar'
    $exportDescription = '本次跳过镜像导出。'
    if (-not $SkipImageExport) {
        & $docker save --output $imageTar online-shopping-system:1.0.0
        if ($LASTEXITCODE -ne 0) { throw '容器部署成功，但镜像 TAR 导出失败。' }
        $hash = (Get-FileHash -LiteralPath $imageTar -Algorithm SHA256).Hash
        "$hash  online-shopping-system-1.0.0-image.tar" | Set-Content -LiteralPath (Join-Path $projectRoot 'DOCKER-SHA256SUMS.txt') -Encoding utf8
        $exportDescription = "镜像文件：online-shopping-system-1.0.0-image.tar；SHA256：$hash"
    }
    @(
        '# Docker 部署结果'
        ''
        "- 执行时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')"
        '- 镜像：online-shopping-system:1.0.0'
        "- 镜像详情：$imageDetails"
        '- 访问地址：http://localhost:8080/'
        '- 卖家登录：http://localhost:8080/seller/login'
        '- 网页检查：首页和登录页均返回 HTTP 200，会话接口可访问。'
        '- 持久化目录：release/docker-data（H2 数据库及上传图片）'
        "- $exportDescription"
        ''
        '## 容器状态'
        ''
        '```text'
        $containerDetails
        '```'
        ''
        '停止容器：在 release 目录执行 docker compose down。'
        '重新启动：在 release 目录执行 docker compose up -d。'
    ) | Set-Content -LiteralPath (Join-Path $release 'Docker部署结果.md') -Encoding utf8
    Write-Host 'Docker 部署完成：http://localhost:8080/'
    Write-Host '卖家账号：admin；密码使用 release/.env 中的 APP_SELLER_PASSWORD。'
    Write-Host $exportDescription
} finally { Pop-Location }
