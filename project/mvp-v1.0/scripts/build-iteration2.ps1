[CmdletBinding()]
param([switch]$InstallDependencies, [switch]$Offline)
$ErrorActionPreference = 'Stop'
$iterationRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$iterationBackend = Join-Path $iterationRoot 'backend'
$iterationFrontend = Join-Path $iterationRoot 'frontend'
$iterationRelease = Join-Path $iterationRoot 'release-iteration2'
Push-Location $iterationFrontend
try {
    if ($InstallDependencies) {
        & npm.cmd ci --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) { throw 'npm ci 失败。' }
    }
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw '前端构建失败。' }
} finally { Pop-Location }
# 两个目录都是构建生成的静态文件目录；删除前限定在后端项目以内。
$iterationBackendPrefix = [System.IO.Path]::GetFullPath($iterationBackend).TrimEnd('\') + '\'
foreach ($iterationStatic in @((Join-Path $iterationBackend 'src\main\resources\static'), (Join-Path $iterationBackend 'target\classes\static'))) {
    $iterationStaticFull = [System.IO.Path]::GetFullPath($iterationStatic)
    if (-not $iterationStaticFull.StartsWith($iterationBackendPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "静态资源目录超出项目范围：$iterationStaticFull"
    }
    if (Test-Path -LiteralPath $iterationStaticFull) { Remove-Item -LiteralPath $iterationStaticFull -Recurse -Force }
    New-Item -ItemType Directory -Path $iterationStaticFull -Force | Out-Null
    Copy-Item -Path (Join-Path $iterationFrontend 'dist\*') -Destination $iterationStaticFull -Recurse -Force
}
Push-Location $iterationBackend
try {
    $iterationMavenArgs = @('--batch-mode', '-Dmaven.test.skip=true', 'package')
    if ($Offline) { $iterationMavenArgs = @('--offline') + $iterationMavenArgs }
    & mvn.cmd @iterationMavenArgs
    if ($LASTEXITCODE -ne 0) { throw '后端打包失败。' }
} finally { Pop-Location }
[xml]$iterationPom = Get-Content -LiteralPath (Join-Path $iterationBackend 'pom.xml')
$iterationJar = Join-Path $iterationBackend ("target\single-seller-store-" + $iterationPom.project.version + '.jar')
New-Item -ItemType Directory -Path $iterationRelease -Force | Out-Null
Copy-Item -LiteralPath $iterationJar -Destination (Join-Path $iterationRelease 'app.jar') -Force
@(
    '在线购物系统第二轮集成包'
    '范围：第二轮三项基础改进 + 挑战需求 A.1 五项'
    "构建时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')"
    '前端：生产构建成功，当前静态资源已装入 JAR'
    '后端：编译打包成功；打包步骤跳过测试，独立回归结果见 tests/第二轮测试说明.md'
    '运行：在本目录执行 java -jar app.jar，访问 http://localhost:8080'
    '数据库与上传文件：默认在运行目录的 data 下；保留该目录即可持久化'
    '此前 Docker 镜像与发布包没有在本脚本中更新'
) | Set-Content -LiteralPath (Join-Path $iterationRelease 'BUILD-INFO.txt') -Encoding utf8
Write-Host "第二轮集成包：$(Join-Path $iterationRelease 'app.jar')"
