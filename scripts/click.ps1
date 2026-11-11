param(
    [string]$Text,
    [string]$Selector,
    [int]$Index = 0,
    [int]$Port = 9223,
    [int]$AfterMs = 1200,
    [double]$X = 0,
    [double]$Y = 0
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
    for ($i = 0; $i -lt 200; $i++) {
        $msg = (Read-Cdp) | ConvertFrom-Json
        if ($msg.id -eq $myId) { return $msg }
    }
    throw 'cdp timeout'
}

if ($X -gt 0 -and $Y -gt 0) {
    $pt = @{ x = $X; y = $Y; tag = "coords"; w = 0; h = 0 }
    $res = $null
}
elseif ($Selector) {
    $expr = "(()=>{const e=document.querySelector('$Selector');if(!e)return null;const r=e.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2}})()"
}
else {
    $escaped = $Text.Replace("'", "\'")
    $expr = @"
(()=>{
  const want = '$escaped';
  const nodes = [...document.querySelectorAll('*')].filter(e => ((e.textContent||'').replace(/\s/g,'') === want.replace(/\s/g,'')) && e.getBoundingClientRect().width > 0);
  if(!nodes.length) return null;
  nodes.sort((a,b)=>a.getBoundingClientRect().width*a.getBoundingClientRect().height - b.getBoundingClientRect().width*b.getBoundingClientRect().height);
  const e = nodes[$Index] || nodes[0];
  const r = e.getBoundingClientRect();
  return {x:r.x+r.width/2,y:r.y+r.height/2,tag:e.tagName,w:Math.round(r.width),h:Math.round(r.height)};
})()
"@
}

if (-not $pt) {
    $res = Invoke-Cdp 'Runtime.evaluate' @{ expression = $expr; returnByValue = $true }
    $pt = $res.result.result.value
}
if (-not $pt) { $ws.Dispose(); throw "target not found: $Text$Selector" }

foreach ($type in 'mousePressed', 'mouseReleased') {
    Invoke-Cdp 'Input.dispatchMouseEvent' @{
        type       = $type
        x          = [double]$pt.x
        y          = [double]$pt.y
        button     = 'left'
        clickCount = 1
    } | Out-Null
}

$ws.Dispose()
"clicked $($pt.tag) at $([math]::Round($pt.x)),$([math]::Round($pt.y)) size $($pt.w)x$($pt.h)"
Start-Sleep -Milliseconds $AfterMs

