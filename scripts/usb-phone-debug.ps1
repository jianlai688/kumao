<#
  酷猫音乐 · 手机 USB 调试一键准备
  ------------------------------------------------------------
  作用：把手机上的 127.0.0.1:8080/8088/5174/1935 反向映射到本机，
        这样手机不连 Wi-Fi、不开防火墙也能调后端（走数据线）。
  前提：手机已开「开发者选项 → USB 调试」，且 APK 是 127.0.0.1 基址构建的 USB 版。

  用法：
    pwsh -File scripts\usb-phone-debug.ps1                # 建映射 + 自检
    pwsh -File scripts\usb-phone-debug.ps1 -Install       # 再装一次 app-debug.apk
    pwsh -File scripts\usb-phone-debug.ps1 -Launch        # 再拉起 App
    pwsh -File scripts\usb-phone-debug.ps1 -Launch -Logcat # 拉起并盯 WebView 控制台日志
#>
param(
    [string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$Package = 'com.kumao.music',
    [string]$Apk = 'D:\kumao\kumao-mobile-apk\app\build\outputs\apk\debug\app-debug.apk',
    [int[]]$Ports = @(8080, 8088, 5174, 1935),
    [switch]$Install,
    [switch]$Launch,
    [switch]$Logcat
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path $Adb)) {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if (-not $cmd) { throw "找不到 adb，请确认 Android SDK platform-tools 路径：$Adb" }
    $Adb = $cmd.Source
}

function Get-Device {
    $lines = & $Adb devices | Select-Object -Skip 1 | Where-Object { $_.Trim() }
    $ready = $lines | Where-Object { $_ -match '\sdevice$' }
    if (-not $ready) {
        $state = ($lines -join ' / ')
        throw "没有可用的 USB 设备（当前状态：$state）。请检查数据线、USB 调试授权弹窗。"
    }
    (@($ready)[0] -split '\s+')[0]
}

$serial = Get-Device
Write-Host "设备已连接：$serial" -ForegroundColor Green

Write-Host "重置端口反向映射 ..."
& $Adb reverse --remove-all | Out-Null
foreach ($p in $Ports) {
    & $Adb reverse "tcp:$p" "tcp:$p" | Out-Null
}

Write-Host ""
Write-Host "端口映射（手机 127.0.0.1 → 本机 127.0.0.1）：" -ForegroundColor Cyan
& $Adb reverse --list
Write-Host ""

# 自检：手机侧直接访问网关
$code = (& $Adb shell "curl -s -m 6 -o /dev/null -w '%{http_code}' 'http://127.0.0.1:8080/api/music/rank/hot?limit=1'").Trim()
if ($code -eq '200') {
    Write-Host "自检通过：手机经数据线访问网关 HTTP 200" -ForegroundColor Green
} else {
    Write-Host "自检失败：手机访问网关返回 '$code'。请确认本机 8080 网关在运行。" -ForegroundColor Yellow
}

function Get-ApkApiBase([string]$path) {
    # 粗略识别 APK 里打包的接口基址：USB 版应为 http://127.0.0.1:8080，Wi-Fi 版是 http://192.168.x
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($path)
        try {
            $hits = @()
            foreach ($e in $zip.Entries) {
                if ($e.FullName -like 'assets/www/assets/*.js' -and $e.Length -lt 3MB) {
                    $reader = New-Object System.IO.StreamReader($e.Open())
                    $text = $reader.ReadToEnd()
                    $reader.Close()
                    if ($text -match 'http://192\.168\.\d+\.\d+:8080') { $hits += 'LAN' }
                }
            }
            if ($hits -contains 'LAN') { return 'Wi-Fi(192.168.x:8080)' }
            return 'USB(127.0.0.1:8080 或相对路径)'
        } finally { $zip.Dispose() }
    } catch { return '未识别' }
}

if ($Install) {
    if (-not (Test-Path $Apk)) { throw "APK 不存在：$Apk" }
    $kind = Get-ApkApiBase $Apk
    Write-Host "APK 接口基址：$kind"
    if ($kind -match 'Wi-Fi') {
        Write-Host "警告：这个包是 Wi-Fi 版，USB 调试时 App 连不上。请先按 docs\手机USB调试.md 第六节构建 127.0.0.1 的 USB 版。" -ForegroundColor Yellow
    }
    Write-Host "安装 APK：$Apk"
    & $Adb install -r $Apk
}

if ($Launch) {
    & $Adb shell am force-stop $Package | Out-Null
    & $Adb shell am start -n "$Package/.MainActivity" | Out-Null
    Write-Host "已启动 $Package" -ForegroundColor Green
}

Write-Host ""
Write-Host "下一步：" -ForegroundColor Cyan
Write-Host "  1) 手机浏览器/App 验证：http://127.0.0.1:8080/api/music/rank/hot?limit=1"
Write-Host "  2) WebView 调试：Chrome 打开 chrome://inspect 即可看到 com.kumao.music"
Write-Host "     （或 adb forward tcp:9222 localabstract:webview_devtools_remote_<pid>，再访问 http://127.0.0.1:9222/json）"
Write-Host "  3) 控制台日志：adb logcat -s KuMaoWeb:* AndroidRuntime:E"

if ($Logcat) {
    Write-Host ""
    Write-Host "开始跟踪日志（Ctrl+C 退出）..." -ForegroundColor Cyan
    & $Adb logcat -s 'KuMaoWeb:*' 'AndroidRuntime:E'
}
