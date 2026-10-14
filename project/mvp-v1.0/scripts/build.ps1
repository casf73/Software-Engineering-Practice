[CmdletBinding()]
param(
    [switch]$SkipNpmInstall,
    [switch]$MavenOffline,
    [string]$NodeExe,
    [string]$NpmCli
)

$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$frontend = Join-Path $projectRoot 'frontend'
$backend = Join-Path $projectRoot 'backend'
$release = Join-Path $projectRoot 'release'

if (-not (Test-Path -LiteralPath (Join-Path $frontend 'package-lock.json'))) {
    throw '缺少前端 package-lock.json。'
}
if (-not (Test-Path -LiteralPath (Join-Path $backend 'pom.xml'))) {
    throw '缺少后端 pom.xml。'
}

if (-not $NodeExe) {
    $NodeExe = (Get-Command node.exe -ErrorAction Stop).Source
}
$NodeExe = [System.IO.Path]::GetFullPath($NodeExe)
$nodeVersion = & $NodeExe --version
if ($LASTEXITCODE -ne 0 -or $nodeVersion -notmatch '^v(\d+)\.') {
    throw '无法读取 Node.js 版本。'
}
if ([int]$Matches[1] -lt 22) {
    throw "当前 Node.js 为 $nodeVersion；锁定依赖要求 Node.js 22 或更高版本。"
}

if (-not $NpmCli) {
    $candidate = Join-Path (Split-Path -Parent $NodeExe) 'node_modules\npm\bin\npm-cli.js'
    if (Test-Path -LiteralPath $candidate) {
        $NpmCli = $candidate
    }
}
if (-not $NpmCli) {
    throw '找不到 npm-cli.js。请通过 -NpmCli 指定 npm 安装目录中的 bin/npm-cli.js。'
}
$NpmCli = [System.IO.Path]::GetFullPath($NpmCli)
if (-not (Test-Path -LiteralPath $NpmCli)) {
    throw "npm-cli.js 不存在：$NpmCli"
}

function Invoke-Npm {
    param([string[]]$NpmArguments)
    & $NodeExe $NpmCli @NpmArguments
    if ($LASTEXITCODE -ne 0) {
        throw "npm $($NpmArguments -join ' ') 失败，退出码 $LASTEXITCODE。"
    }
}

$oldPath = $env:PATH
$env:PATH = (Split-Path -Parent $NodeExe) + [System.IO.Path]::PathSeparator + $oldPath
try {
    Push-Location $frontend
    try {
        if ($SkipNpmInstall) {
            if (-not (Test-Path -LiteralPath (Join-Path $frontend 'node_modules\vite\bin\vite.js'))) {
                throw '已指定 -SkipNpmInstall，但 frontend/node_modules 不完整。'
            }
        } else {
            Invoke-Npm -NpmArguments @('ci', '--no-audit', '--no-fund')
        }
        Invoke-Npm -NpmArguments @('run', 'build')
    } finally {
        Pop-Location
    }

    $static = Join-Path $backend 'src\main\resources\static'
    $backendFull = [System.IO.Path]::GetFullPath($backend).TrimEnd('\') + '\'
    $staticFull = [System.IO.Path]::GetFullPath($static)
    if (-not $staticFull.StartsWith($backendFull, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "拒绝清理后端目录之外的路径：$staticFull"
    }
    if (Test-Path -LiteralPath $staticFull) {
        Remove-Item -LiteralPath $staticFull -Recurse -Force
    }
    New-Item -ItemType Directory -Path $staticFull -Force | Out-Null
    Copy-Item -Path (Join-Path $frontend 'dist\*') -Destination $staticFull -Recurse -Force

    Push-Location $backend
    try {
        $mavenArguments = @('--batch-mode', '-DskipTests', 'package')
        if ($MavenOffline) {
            $mavenArguments = @('--offline') + $mavenArguments
        }
        & mvn.cmd @mavenArguments
        if ($LASTEXITCODE -ne 0) {
            throw "Maven 打包失败，退出码 $LASTEXITCODE。"
        }
    } finally {
        Pop-Location
    }
} finally {
    $env:PATH = $oldPath
}

$jar = Join-Path $backend 'target\single-seller-store-1.0.0.jar'
if (-not (Test-Path -LiteralPath $jar)) {
    throw "构建结束但找不到 JAR：$jar"
}
New-Item -ItemType Directory -Path $release -Force | Out-Null
$releasedJar = Join-Path $release 'app.jar'
Copy-Item -LiteralPath $jar -Destination $releasedJar -Force
$hash = (Get-FileHash -LiteralPath $releasedJar -Algorithm SHA256).Hash
$npmVersion = & $NodeExe $NpmCli --version
$mavenVersion = (& mvn.cmd -version | Where-Object { $_ -match '^Apache Maven ' } | Select-Object -First 1)
$buildInfo = @(
    '在线购物系统构建结果'
    "构建时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')"
    "Node.js：$nodeVersion"
    "npm：$npmVersion"
    "Maven：$mavenVersion"
    'Java：17'
    '前端：Vite build 成功，静态资源已装入 JAR'
    '后端：Maven package 成功；跳过测试执行'
    "JAR：app.jar"
    "JAR SHA256：$hash"
)
$buildInfo | Set-Content -LiteralPath (Join-Path $release 'BUILD-INFO.txt') -Encoding utf8
$zip = Join-Path $projectRoot 'online-shopping-system-1.0.0.zip'
Compress-Archive -Path (Join-Path $release '*') -DestinationPath $zip -Force
Write-Host "构建完成：$releasedJar"
Write-Host "部署压缩包：$zip"
Write-Host "SHA256：$hash"
