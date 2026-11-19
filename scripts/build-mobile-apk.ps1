# ============================================================
# 酷猫音乐 · Android APK 一键构建
# ------------------------------------------------------------
# 用法：
#   pwsh -File scripts\build-mobile-apk.ps1 -Mode wifi
#   pwsh -File scripts\build-mobile-apk.ps1 -Mode usb -Install
#   pwsh -File scripts\build-mobile-apk.ps1 -Mode wifi -ApiBase http://192.168.1.20:8080/api
#
# Mode wifi：API 基址默认取本机局域网 IPv4，适合手机与电脑同一 Wi-Fi
# Mode usb ：API 基址固定 http://127.0.0.1:8080/api，配合 adb reverse 使用
# ============================================================
param(
    [ValidateSet('wifi', 'usb')]
    [string]$Mode = 'wifi',
    [string]$ApiBase = '',
    [switch]$Install
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$uniapp = Join-Path $root 'kumao-uniapp'
$apkProject = Join-Path $root 'kumao-mobile-apk'
$assetsWww = Join-Path $apkProject 'app\src\main\assets\www'
$downloads = Join-Path $root 'kumao-web\public\downloads'

function Get-LanIp {
    $route = Get-NetRoute -DestinationPrefix '0.0.0.0/0' -ErrorAction SilentlyContinue |
        Where-Object { $_.NextHop -and $_.NextHop -ne '0.0.0.0' } |
        Sort-Object RouteMetric |
        Select-Object -First 1
    if ($route) {
        $address = Get-NetIPAddress -InterfaceIndex $route.InterfaceIndex -AddressFamily IPv4 -ErrorAction SilentlyContinue |
            Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254*' } |
            Select-Object -First 1
        if ($address) { return $address.IPAddress }
    }
    $address = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object {
            $_.IPAddress -notlike '127.*' -and
            $_.IPAddress -notlike '169.254*' -and
            $_.IPAddress -notlike '172.1[7-9].*' -and
            $_.IPAddress -notlike '172.2[0-9].*' -and
            $_.IPAddress -notlike '172.3[0-1].*' -and
            $_.InterfaceAlias -notmatch 'VMware|WSL|Loopback|vEthernet|本地连接\*'
        } |
        Select-Object -First 1
    if (-not $address) { throw '找不到可用的局域网 IPv4，请手动传 -ApiBase' }
    $address.IPAddress
}

if (-not $ApiBase) {
    $ApiBase = if ($Mode -eq 'usb') { 'http://127.0.0.1:8080/api' } else { "http://$(Get-LanIp):8080/api" }
}
if ($ApiBase -notmatch '^https?://') { throw "ApiBase 必须是绝对地址：$ApiBase" }

$env:VITE_API_BASE = $ApiBase
Write-Host "==> API 基址：$ApiBase" -ForegroundColor Cyan

Push-Location $uniapp
try {
    Write-Host '==> 构建 uniapp H5 ...' -ForegroundColor Cyan
    & npm run build:h5
    if ($LASTEXITCODE -ne 0) { throw 'uniapp 构建失败' }
} finally {
    Pop-Location
}

$dist = Join-Path $uniapp 'dist\build\h5'
if (-not (Test-Path (Join-Path $dist 'index.html'))) { throw "H5 产物不存在：$dist" }

Write-Host '==> 同步到 APK assets/www ...' -ForegroundColor Cyan
Remove-Item (Join-Path $assetsWww '*') -Recurse -Force
Copy-Item (Join-Path $dist '*') $assetsWww -Recurse -Force

$gradle = Get-Command gradle -ErrorAction SilentlyContinue
if (-not $gradle) {
    $candidates = Get-ChildItem (Join-Path $env:USERPROFILE '.gradle\wrapper\dists') -Recurse -File -Filter gradle.bat -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending
    $candidate = $candidates | Where-Object { $_.FullName -match 'gradle-8\.5' } | Select-Object -First 1
    if (-not $candidate) { $candidate = $candidates | Where-Object { $_.FullName -match 'gradle-8\.' } | Select-Object -First 1 }
    if (-not $candidate) { $candidate = $candidates | Select-Object -First 1 }
    if (-not $candidate) { throw '找不到 gradle，请安装 Gradle 8.x 或 Android Studio' }
    $gradle = $candidate.FullName
} else {
    $gradle = $gradle.Source
}

Write-Host "==> Gradle：$gradle" -ForegroundColor Cyan
& $gradle -p $apkProject assembleDebug --offline
if ($LASTEXITCODE -ne 0) { throw 'Gradle 构建 APK 失败' }

$apk = Join-Path $apkProject 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) { throw "APK 不存在：$apk" }

$gradleText = Get-Content (Join-Path $apkProject 'app\build.gradle') -Raw
$version = if ($gradleText -match "versionName\s+'([^']+)'") { $Matches[1] } else { 'dev' }
$suffix = if ($Mode -eq 'usb') { '-usb' } else { '' }
$out = Join-Path $downloads "KuMao-Android-v$version$suffix.apk"
Copy-Item $apk $out -Force

Write-Host ''
Write-Host "构建完成：$out" -ForegroundColor Green
Write-Host "APK 接口基址：$ApiBase" -ForegroundColor Green

if ($Install) {
    $adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
    if (-not (Test-Path $adb)) { $adb = (Get-Command adb -ErrorAction Stop).Source }
    Write-Host '==> 安装到已连接设备 ...' -ForegroundColor Cyan
    & $adb install -r $apk
    if ($LASTEXITCODE -ne 0) { throw 'adb install 失败' }
    if ($Mode -eq 'usb') {
        foreach ($port in 8080, 8088, 5174, 1935) { & $adb reverse "tcp:$port" "tcp:$port" | Out-Null }
        & $adb reverse --list
    }
}
