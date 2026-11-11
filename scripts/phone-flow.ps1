param(
    [int]$Port = 9223,
    [string]$Account = 'zhangsan',
    [string]$Password = '123456',
    [string]$RoomId = '11001'
)

$ErrorActionPreference = 'Stop'

function Get-Target {
    $targets = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/json/list" -NoProxy
    $targets | Where-Object { $_.type -eq 'page' } | Select-Object -First 1
}

$page = Get-Target
if (-not $page) { throw 'no devtools page' }

$ws = [System.Net.WebSockets.ClientWebSocket]::new()
$ct = [Threading.CancellationToken]::None
$null = $ws.ConnectAsync([Uri]$page.webSocketDebuggerUrl, $ct).GetAwaiter().GetResult()

$script:msgId = 0

function Read-CdpRaw {
    $buf = New-Object byte[] 1048576
    $seg = [ArraySegment[byte]]::new($buf)
    $sb = [Text.StringBuilder]::new()
    do {
        $r = $ws.ReceiveAsync($seg, $ct).GetAwaiter().GetResult()
        if ($r.MessageType -eq [Net.WebSockets.WebSocketMessageType]::Close) { throw 'socket closed' }
        $null = $sb.Append([Text.Encoding]::UTF8.GetString($buf, 0, $r.Count))
    } while (-not $r.EndOfMessage)
    $sb.ToString()
}

function Eval([string]$expression) {
    $script:msgId++
    $id = $script:msgId
    $payload = @{
        id     = $id
        method = 'Runtime.evaluate'
        params = @{
            expression    = $expression
            returnByValue = $true
            awaitPromise  = $false
        }
    } | ConvertTo-Json -Depth 12 -Compress
    $bytes = [Text.Encoding]::UTF8.GetBytes($payload)
    $seg = [ArraySegment[byte]]::new($bytes)
    $null = $ws.SendAsync($seg, [Net.WebSockets.WebSocketMessageType]::Text, $true, $ct).GetAwaiter().GetResult()
    for ($i = 0; $i -lt 200; $i++) {
        $msg = (Read-CdpRaw) | ConvertFrom-Json
        if ($msg.id -eq $id) {
            if ($msg.result.exceptionDetails) { return 'EXC: ' + $msg.result.exceptionDetails.text }
            return $msg.result.result.value
        }
    }
    return 'TIMEOUT'
}

$findChip = @"
(() => {
  const nodes = [...document.querySelectorAll('*')].filter(e => {
    const t = (e.textContent || '').trim();
    return t.startsWith('$Account') && t.length < 16;
  });
  if (!nodes.length) return 'nochip';
  nodes.sort((a, b) => a.getBoundingClientRect().width * a.getBoundingClientRect().height - b.getBoundingClientRect().width * b.getBoundingClientRect().height);
  const el = nodes[0];
  el.click();
  return 'chip:' + el.tagName + ':' + Math.round(el.getBoundingClientRect().y);
})()
"@

$clickLogin = @"
(() => {
  const nodes = [...document.querySelectorAll('uni-button,button,uni-view,div')].filter(e => {
    const t = (e.textContent || '').replace(/\s/g, '');
    return t === '登录';
  });
  if (!nodes.length) return 'nologin';
  nodes.sort((a, b) => a.getBoundingClientRect().width * a.getBoundingClientRect().height - b.getBoundingClientRect().width * b.getBoundingClientRect().height);
  nodes[0].click();
  return 'login-clicked';
})()
"@

Write-Output ('nav: ' + (Eval "location.hash='#/pages/login/login'; 'ok'"))
Start-Sleep -Milliseconds 2500
Write-Output ('chip: ' + (Eval $findChip))
Start-Sleep -Milliseconds 900
Write-Output ('inputs: ' + (Eval "JSON.stringify([...document.querySelectorAll('input')].map(i=>i.value))"))
Write-Output ('login: ' + (Eval $clickLogin))
Start-Sleep -Seconds 4
Write-Output ('after: ' + (Eval "JSON.stringify({hash:location.hash, token:!!uni?.getStorageSync?.('km_token')})"))

$ws.Dispose()
