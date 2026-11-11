param(
    [Parameter(Mandatory = $true)][string]$Expression,
    [int]$Port = 9223,
    [switch]$AwaitPromise
)

$ErrorActionPreference = 'Stop'

$targets = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/json/list" -NoProxy
$page = $targets | Where-Object { $_.type -eq 'page' } | Select-Object -First 1
if (-not $page) { throw 'no devtools page' }

$ws = [System.Net.WebSockets.ClientWebSocket]::new()
$ct = [Threading.CancellationToken]::None
$null = $ws.ConnectAsync([Uri]$page.webSocketDebuggerUrl, $ct).GetAwaiter().GetResult()

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
        if ($r.MessageType -eq [Net.WebSockets.WebSocketMessageType]::Close) { throw 'socket closed' }
        $null = $sb.Append([Text.Encoding]::UTF8.GetString($buf, 0, $r.Count))
    } while (-not $r.EndOfMessage)
    return $sb.ToString()
}

Send-Cdp @{
    id     = 1
    method = 'Runtime.evaluate'
    params = @{
        expression    = $Expression
        returnByValue = $true
        awaitPromise  = [bool]$AwaitPromise
    }
}

$result = $null
for ($i = 0; $i -lt 60; $i++) {
    $msg = (Read-Cdp) | ConvertFrom-Json
    if ($msg.id -eq 1) { $result = $msg; break }
}
$ws.Dispose()

if ($result.result.exceptionDetails) {
    'EXCEPTION: ' + ($result.result.exceptionDetails.text)
}
elseif ($result.result.result.value -ne $null) {
    $result.result.result.value
}
else {
    $result.result.result.description
}
