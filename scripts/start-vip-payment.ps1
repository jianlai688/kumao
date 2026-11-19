# ============================================================
# 酷猫音乐 · 启动带微信支付的 vip-service
# ------------------------------------------------------------
# 使用 vip-service/certs/wxpay.properties 中的演示商户参数；
# 正式环境请改为自己的商户参数，并把 NotifyBaseUrl 换成公网 HTTPS。
#
# 用法：
#   pwsh -File scripts\start-vip-payment.ps1
#   pwsh -File scripts\start-vip-payment.ps1 -NotifyBaseUrl https://xxxx.ngrok-free.app
# ============================================================
param(
    # 不传时优先使用 wxpay.properties 里的 wxpay.notify-domain（ngrok/cpolar 公网地址）；
    # 两者都为空时退回 example.com，仅用于让 Native 下单通过微信的 HTTPS 校验。
    [string]$NotifyBaseUrl = '',
    [string]$WechatNotifyUrl = ''
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$certDir = Join-Path $root 'vip-service\certs'
$propsFile = Join-Path $certDir 'wxpay.properties'

if (!(Test-Path $propsFile)) {
    throw "未找到 $propsFile，请先放入微信商户 wxpay.properties"
}

$props = @{}
Get-Content $propsFile | ForEach-Object {
    $line = $_.Trim()
    if (!$line -or $line.StartsWith('#')) { return }
    $idx = $line.IndexOf('=')
    if ($idx -lt 0) { $idx = $line.IndexOf(':') }
    if ($idx -lt 0) { return }
    $key = $line.Substring(0, $idx).Trim()
    $value = $line.Substring($idx + 1).Trim()
    $props[$key] = $value
}

$keyPath = Join-Path $certDir ($props['wxpay.private-key-path'] ?? 'apiclient_key.pem')
if (!(Test-Path $keyPath)) {
    throw "未找到微信商户私钥：$keyPath"
}

# 回调地址优先级：命令行参数 > wxpay.properties 的 wxpay.notify-url >
# wxpay.notify-domain（ngrok/cpolar 公网域名）> example.com
if (!$NotifyBaseUrl) { $NotifyBaseUrl = $props['wxpay.notify-domain'] }
if (!$NotifyBaseUrl) { $NotifyBaseUrl = 'https://example.com' }
if (!$WechatNotifyUrl) { $WechatNotifyUrl = $props['wxpay.notify-url'] }

$env:MYSQL_PORT = $env:MYSQL_PORT ?? '3307'
$env:REDIS_HOST = $env:REDIS_HOST ?? '::1'
$env:WECHAT_PAY_ENABLED = 'true'
$env:WECHAT_APP_ID = $props['wxpay.appid']
$env:WECHAT_MCH_ID = $props['wxpay.mch-id']
$env:WECHAT_MERCHANT_SERIAL_NO = $props['wxpay.mch-serial-no']
$env:WECHAT_PRIVATE_KEY_PATH = $keyPath
$env:WECHAT_API_V3_KEY = $props['wxpay.api-v3-key']
$env:PAY_NOTIFY_BASE_URL = $NotifyBaseUrl
if ($WechatNotifyUrl) {
    $env:WECHAT_NOTIFY_URL = $WechatNotifyUrl
}
$env:PAY_MOCK_ENABLED = $env:PAY_MOCK_ENABLED ?? 'true'

Write-Host "启动 vip-service（微信支付已启用）..." -ForegroundColor Cyan
Write-Host "WECHAT_APP_ID=$env:WECHAT_APP_ID"
Write-Host "WECHAT_MCH_ID=$env:WECHAT_MCH_ID"
Write-Host "WECHAT_PRIVATE_KEY_PATH=$env:WECHAT_PRIVATE_KEY_PATH"
Write-Host "PAY_NOTIFY_BASE_URL=$env:PAY_NOTIFY_BASE_URL"
Write-Host "WECHAT_NOTIFY_URL=$env:WECHAT_NOTIFY_URL"
Write-Host "支付宝：设置 ALIPAY_ENABLED=true 及 ALIPAY_* 环境变量后启用" -ForegroundColor Yellow

Push-Location $root
try {
    mvn -o -pl vip-service spring-boot:run
} finally {
    Pop-Location
}
