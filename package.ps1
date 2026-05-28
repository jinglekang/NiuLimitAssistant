# 小牛限速助手 自动化构建脚本

$projectName = "NiuLimitAssistant"
$distDir = "build/release"
$releaseApkPath = "app/build/outputs/apk/release/app-release.apk"

# 0. 自动从 build.gradle.kts 读取版本号
$version = "unknown"
$gradleFile = "app/build.gradle.kts"
if (Test-Path $gradleFile) {
    $content = Get-Content $gradleFile -Raw
    if ($content -match 'versionName\s*=\s*"([^"]+)"') {
        $version = $matches[1]
        Write-Host "检测到版本号: v$version" -ForegroundColor Cyan
    }
}

# 1. 加载 .env 环境变量
if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        if ($_ -match "(.+)=(.+)") {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim().Trim("'").Trim('"')
            [System.Environment]::SetEnvironmentVariable($name, $value)
        }
    }
}

Write-Host "停止之前的 Gradle 构建进程..." -ForegroundColor Gray
.\gradlew.bat --stop

Write-Host "开始编译 Release 正式版本..." -ForegroundColor Cyan
.\gradlew.bat assembleRelease

if ($LASTEXITCODE -eq 0) {
    # 3. 创建输出目录
    if (!(Test-Path $distDir)) {
        New-Item -ItemType Directory -Path $distDir | Out-Null
    }

    # 4. 复制并重命名 APK
    $outputName = "$projectName-v$version.apk"
    $targetPath = Join-Path $distDir $outputName

    if (Test-Path $releaseApkPath) {
        Copy-Item $releaseApkPath $targetPath -Force
        Write-Host "`n构建成功!" -ForegroundColor Green
        Write-Host "输出路径: $targetPath" -ForegroundColor Yellow
    } else {
        Write-Host "`n错误: 未找到生成的 APK 文件。" -ForegroundColor Red
    }
} else {
    Write-Host "`n编译失败，请检查上方错误日志。" -ForegroundColor Red
}
