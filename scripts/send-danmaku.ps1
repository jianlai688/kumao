param(
    [string]$Message = '真机自发弹幕测试',
    [int]$Port = 9223
)

$ErrorActionPreference = 'Stop'

$targets = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/json/list" -NoProxy
$page = $targets | Where-Object { $_.type -eq 'page' } | Select-Object -First 1
if (-not $page) { throw 'no devtools page' }

$ws = [System.Net.WebSockets.ClientWebSocket]::new()
$ct = [Threading.CancellationToken]::None
$null = $ws.ConnectAsync([Uri]$page.webSocketDebuggerUrl, $ct).GetAwaiter().GetResult()

$script:id = 0
function Send-Cdp($obj) {
    $json = $obj | ConvertTo-Json -Depth 20 -Compress
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    $seg = [ArraySegment[byte]]::new($bytes)
    $null = $ws.SendAsync($seg, [Net.WebSockets.WebSocketMessageType]::Text, $true, $ct).GetAwaiter().GetResult()
}
function Read-Cdp {
    $buf = New-Object byte[] 1048576
    $seg = [ArraySegment[byte]]::new($buf)
    $sb = [Text.StringBuilder]::new()
    do {
        $r = $ws.ReceiveAsync($seg, $ct).GetAwaiter().GetResult()
        if ($r.MessageType -eq [Net.WebSockets.WebSocketMessageType]::Close) { throw 'closed' }
        $null = $sb.Append([Text.Encoding]::UTF8.GetString($buf, 0, $r.Count))
    } while (-not $r.EndOfMessage)
    $sb.ToString()
}
function Invoke-Cdp($method, $params) {
    $script:id++
    $myId = $script:id
    Send-Cdp @{ id = $myId; method = $method; params = $params }
    for ($i = 0; $i -lt 300; $i++) {
        $msg = (Read-Cdp) | ConvertFrom-Json
        if ($msg.id -eq $myId) { return $msg }
    }
    throw 'timeout'
}

# 1. 找到弹幕输入框
$rect = Invoke-Cdp 'Runtime.evaluate' @{
    expression    = "(()=>{const i=document.querySelector('input');if(!i)return null;const r=i.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2}})()"
    returnByValue = $true
}
$pt = $rect.result.result.value
if (-not $pt) { $ws.Dispose(); throw 'input not found' }

# 2. 点击输入框获得焦点
foreach ($type in 'mousePressed', 'mouseReleased') {
    Invoke-Cdp 'Input.dispatchMouseEvent' @{ type = $type; x = [double]$pt.x; y = [double]$pt.y; button = 'left'; clickCount = 1 } | Out-Null
}
Start-Sleep -Milliseconds 700

# 3. 输入文本
Invoke-Cdp 'Input.insertText' @{ text = $Message } | Out-Null
Start-Sleep -Milliseconds 500

$val = Invoke-Cdp 'Runtime.evaluate' @{ expression = "document.querySelector('input').value"; returnByValue = $true }
"input=$($val.result.result.value)"

# 4. 点击发送
$btn = Invoke-Cdp 'Runtime.evaluate' @{
    expression    = "(()=>{const n=[...document.querySelectorAll('*')].filter(e=>(e.textContent||'').replace(/\s/g,'')==='发送'&&e.getBoundingClientRect().width>0);if(!n.length)return null;n.sort((a,b)=>a.getBoundingClientRect().width*a.getBoundingClientRect().height-b.getBoundingClientRect().width*b.getBoundingClientRect().height);const r=n[0].getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2}})()"
    returnByValue = $true
}
$bp = $btn.result.result.value
if (-not $bp) { $ws.Dispose(); throw 'send button not found' }
foreach ($type in 'mousePressed', 'mouseReleased') {
    Invoke-Cdp 'Input.dispatchMouseEvent' @{ type = $type; x = [double]$bp.x; y = [double]$bp.y; button = 'left'; clickCount = 1 } | Out-Null
}

$ws.Dispose()
Start-Sleep -Seconds 3
"sent: $Message"
