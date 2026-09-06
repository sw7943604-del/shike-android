param([string[]]$Tasks = @('assembleDebug'))
$ErrorActionPreference = 'Stop'
chcp 65001 > $null
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$projectRoot = Split-Path -Parent $PSScriptRoot
# 环境变量只作用于当前构建进程。
if ((-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin/jlink.exe'))) -and (Test-Path 'H:/android-toolchain/jdk17/bin/jlink.exe')) {
    $env:JAVA_HOME = 'H:/android-toolchain/jdk17'
}
if (-not $env:ANDROID_HOME -and (Test-Path 'H:/android-toolchain/sdk')) {
    $env:ANDROID_HOME = 'H:/android-toolchain/sdk'
}
# Java 17 在 Windows 上读取参数文件时可能损坏中文类路径，使用目录联接保留源文件位置。
$buildRoot = $projectRoot
if ($env:OS -eq 'Windows_NT' -and $projectRoot -match '[^\x00-\x7F]') {
    $linkRoot = Join-Path $env:LOCALAPPDATA 'ShikeBuild'
    New-Item -ItemType Directory -Force -Path $linkRoot | Out-Null
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try { $key = ([System.BitConverter]::ToString($sha.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($projectRoot)))).Replace('-', '').Substring(0, 12) }
    finally { $sha.Dispose() }
    $buildRoot = Join-Path $linkRoot $key
    if (-not (Test-Path -LiteralPath $buildRoot)) {
        New-Item -ItemType Junction -Path $buildRoot -Target $projectRoot | Out-Null
    } elseif ((Get-Item -LiteralPath $buildRoot).Target -ne $projectRoot) {
        throw "构建联接指向其他位置：$buildRoot"
    }
}
Push-Location $buildRoot
try {
    & "$buildRoot/gradlew.bat" -p $buildRoot @Tasks --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle 构建失败，退出码：$LASTEXITCODE" }
} finally {
    Pop-Location
}
