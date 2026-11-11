# ============================================================
# 酷猫音乐平台 · API 闭环自动化测试
# 运行前：中间件、11 个后端服务、前端需已启动
# 用法：pwsh -File tests/e2e-api-test.ps1
# ============================================================
$ErrorActionPreference = 'Stop'
$Base = 'http://127.0.0.1:8080'
$Stamp = Get-Date -Format 'MMddHHmmss'
$TestUser = "e2e_$Stamp"
$TestPass = 'E2e123456'
$script:Results = @()
$script:StartedAt = Get-Date

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        $Body = $null,
        [string]$Token = '',
        $Form = $null
    )
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }
    $req = @{
        Uri = "$Base$Path"
        Method = $Method
        Headers = $headers
        TimeoutSec = 30
        SkipHttpErrorCheck = $true
    }
    if ($Form) {
        $req.Form = $Form
    } elseif ($null -ne $Body) {
        $req.Body = ($Body | ConvertTo-Json -Depth 12)
        $req.ContentType = 'application/json'
    }
    try {
        $resp = Invoke-WebRequest @req
        $json = $null
        try { $json = $resp.Content | ConvertFrom-Json } catch {}
        return [pscustomobject]@{
            Http = [int]$resp.StatusCode
            Code = if ($json) { $json.code } else { $null }
            Message = if ($json) { $json.message } else { '' }
            Data = if ($json) { $json.data } else { $null }
            Raw = $resp.Content
        }
    } catch {
        return [pscustomobject]@{
            Http = 0
            Code = 0
            Message = $_.Exception.Message
            Data = $null
            Raw = ''
        }
    }
}

function Check {
    param([string]$Id, [string]$Module, [string]$Name, [bool]$Pass, [string]$Detail)
    $script:Results += [pscustomobject]@{
        id = $Id
        module = $Module
        name = $Name
        result = $(if ($Pass) { 'PASS' } else { 'FAIL' })
        detail = $Detail
        time = (Get-Date).ToString('HH:mm:ss')
    }
    $color = if ($Pass) { 'Green' } else { 'Red' }
    Write-Host ("[{0}] {1,-10} {2}" -f $(if ($Pass) { 'PASS' } else { 'FAIL' }), $Id, $Name) -ForegroundColor $color
}

function Login([string]$username, [string]$password) {
    return Invoke-Api -Method POST -Path '/api/auth/login' -Body @{
        type = 'PASSWORD'; username = $username; password = $password; device = 'PC'
    }
}

# ------------------------------------------------------------
# 1. 基础设施与网关
# ------------------------------------------------------------
$servicePorts = @{
    8080 = 'gateway'; 8101 = 'user'; 8102 = 'music'; 8103 = 'playlist'; 8104 = 'comment';
    8105 = 'file'; 8106 = 'live'; 8107 = 'search'; 8108 = 'vip'; 8109 = 'musician'; 8110 = 'ad'; 8111 = 'review'
}
$healthOk = 0
$healthDetail = @()
foreach ($p in ($servicePorts.Keys | Sort-Object)) {
    try {
        $r = Invoke-WebRequest "http://127.0.0.1:$p/actuator/health" -TimeoutSec 5
        if ($r.StatusCode -eq 200) { $healthOk++; $healthDetail += "$p=UP" } else { $healthDetail += "$p=HTTP$($r.StatusCode)" }
    } catch {
        $healthDetail += "$p=DOWN"
    }
}
Check 'ENV-01' '基础设施' '网关与 11 个业务服务健康检查' ($healthOk -eq $servicePorts.Count) ($healthDetail -join ', ')
$nacosOk = $false
try {
    $nacosResp = Invoke-WebRequest 'http://127.0.0.1:8848/nacos/v1/console/health/readiness' -TimeoutSec 5
    $nacosOk = $nacosResp.StatusCode -eq 200
} catch {}
Check 'ENV-02' '基础设施' 'Nacos 就绪' $nacosOk "ready=$nacosOk"
$songPublic = Invoke-Api -Method GET -Path '/api/music/songs?pageNum=1&pageSize=5'
Check 'ENV-03' '网关' '公开只读接口免登录' ($songPublic.Http -eq 200 -and $songPublic.Code -eq 200) "HTTP=$($songPublic.Http), code=$($songPublic.Code)"

# ------------------------------------------------------------
# 2. 认证与用户
# ------------------------------------------------------------
$reg = Invoke-Api -Method POST -Path '/api/auth/register' -Body @{
    type = 'PASSWORD'; username = $TestUser; password = $TestPass
}
Check 'AUTH-01' '认证' '新用户注册' ($reg.Code -eq 200 -and $reg.Data) "code=$($reg.Code), userId=$($reg.Data)"
$userId = $reg.Data

$login = Login $TestUser $TestPass
Check 'AUTH-02' '认证' '账号密码登录' ($login.Code -eq 200 -and $login.Data.token) "code=$($login.Code), userId=$($login.Data.userId)"
$userToken = $login.Data.token

$me = Invoke-Api -Method GET -Path '/api/users/me' -Token $userToken
Check 'AUTH-03' '用户' '获取当前用户资料' ($me.Code -eq 200 -and $me.Data.username -eq $TestUser) "code=$($me.Code), username=$($me.Data.username)"

$newNick = "E2E用户$Stamp"
$profile = Invoke-Api -Method PUT -Path '/api/users/me' -Token $userToken -Body @{ nickname = $newNick; intro = '自动化测试账号' }
$me2 = Invoke-Api -Method GET -Path '/api/users/me' -Token $userToken
Check 'AUTH-04' '用户' '修改昵称并回读' ($profile.Code -eq 200 -and $me2.Data.nickname -eq $newNick) "code=$($profile.Code), nickname=$($me2.Data.nickname)"

$wrong = Login $TestUser 'WrongPass123'
Check 'AUTH-05' '认证' '错误密码登录被拒绝' ($wrong.Code -ne 200) "code=$($wrong.Code), message=$($wrong.Message)"

$noToken = Invoke-Api -Method GET -Path '/api/comment/likes/mine'
Check 'AUTH-06' '网关鉴权' '未登录访问受保护接口返回 401' ($noToken.Http -eq 401) "HTTP=$($noToken.Http), code=$($noToken.Code)"

$adminLogin = Login 'admin' 'admin123'
$adminToken = $adminLogin.Data.token
Check 'AUTH-07' '认证' '管理员登录' ($adminLogin.Code -eq 200 -and $adminToken) "code=$($adminLogin.Code), roles=$($adminLogin.Data.roles)"

# ------------------------------------------------------------
# 2.1 DeepSeek / AI 审核闭环（无外部密钥时验证本地规则降级）
# ------------------------------------------------------------
$aiStatus = Invoke-Api -Method GET -Path '/api/admin/ai/status' -Token $adminToken
Check 'DEEPSEEK-01' 'AI审核' '审核模型状态可查询' ($aiStatus.Code -eq 200 -and $aiStatus.Data.mode) "mode=$($aiStatus.Data.mode), provider=$($aiStatus.Data.provider)"

$aiReview = Invoke-Api -Method POST -Path '/api/admin/ai/review' -Token $adminToken -Body @{
    category = 'COMMENT'; itemId = "e2e-$Stamp"; title = 'E2E 审核'; content = '这是一条正常评论'; metadata = @{ songId = 3003 }
}
Check 'DEEPSEEK-02' 'AI审核' '审核建议生成并返回来源' ($aiReview.Code -eq 200 -and $aiReview.Data.decision -in @('PASS','REVIEW','REJECT') -and $aiReview.Data.source) "decision=$($aiReview.Data.decision), source=$($aiReview.Data.source)"

$adMaterialReview = Invoke-Api -Method POST -Path '/api/admin/ai/review' -Token $adminToken -Body @{
    category = 'AD_MATERIAL'; itemId = "e2e-material-$Stamp"; title = 'E2E 品牌素材'; content = '品牌活动宣传素材'; metadata = @{ type = 'IMAGE_TEXT'; mediaUrl = 'https://example.com/e2e-banner.png' }
}
$adPlanReview = Invoke-Api -Method POST -Path '/api/admin/ai/review' -Token $adminToken -Body @{
    category = 'AD_PLAN'; itemId = "e2e-plan-$Stamp"; title = 'E2E 首页投放'; content = '版位：HOME_BANNER，支付状态：PAID'; metadata = @{ slot = 'HOME_BANNER'; paymentStatus = 'PAID' }
}
Check 'DEEPSEEK-05' 'AI审核' '广告素材审核建议可生成' ($adMaterialReview.Code -eq 200 -and $adMaterialReview.Data.decision -in @('PASS','REVIEW','REJECT') -and $adMaterialReview.Data.source) "decision=$($adMaterialReview.Data.decision), source=$($adMaterialReview.Data.source)"
Check 'DEEPSEEK-06' 'AI审核' '广告投放审核建议可生成' ($adPlanReview.Code -eq 200 -and $adPlanReview.Data.decision -in @('PASS','REVIEW','REJECT') -and $adPlanReview.Data.source) "decision=$($adPlanReview.Data.decision), source=$($adPlanReview.Data.source)"

$aiStats = Invoke-Api -Method GET -Path '/api/admin/ai/stats' -Token $adminToken
Check 'DEEPSEEK-03' 'AI审核' '审核日志统计可查询' ($aiStats.Code -eq 200 -and $null -ne $aiStats.Data) "code=$($aiStats.Code)"

$autoStatus = Invoke-Api -Method GET -Path '/api/admin/ai/auto/status' -Token $adminToken
Check 'DEEPSEEK-04' 'AI审核' '自动审核调度状态可查询' ($autoStatus.Code -eq 200 -and $null -ne $autoStatus.Data) "code=$($autoStatus.Code), enabled=$($autoStatus.Data.enabled)"

$normalAdmin = Invoke-Api -Method GET -Path '/api/vip/vip/admin/statistics' -Token $userToken
Check 'AUTH-08' '权限' '普通用户访问后台接口被拒绝' ($normalAdmin.Http -eq 403 -or $normalAdmin.Code -eq 403) "HTTP=$($normalAdmin.Http), code=$($normalAdmin.Code)"

$smsPhone = '139' + (Get-Random -Minimum 10000000 -Maximum 99999999)
$sms = Invoke-Api -Method GET -Path "/api/auth/sms-code?phone=$smsPhone"
Check 'AUTH-09' '认证' '短信验证码演示接口' ($sms.Code -eq 200 -and $sms.Data.mockCode) "code=$($sms.Code), mockCode=$($sms.Data.mockCode)"

# ------------------------------------------------------------
# 3. 音乐内容
# ------------------------------------------------------------
$songs = Invoke-Api -Method GET -Path '/api/music/songs?pageNum=1&pageSize=20'
Check 'MUS-01' '音乐' '歌曲列表分页' ($songs.Code -eq 200 -and $songs.Data.total -ge 10) "total=$($songs.Data.total)"

$song = Invoke-Api -Method GET -Path '/api/music/songs/3003'
Check 'MUS-02' '音乐' '歌曲详情' ($song.Code -eq 200 -and $song.Data.name -eq '泡沫') "code=$($song.Code), name=$($song.Data.name)"

$lyric = Invoke-Api -Method GET -Path '/api/music/songs/3003/lyric'
Check 'MUS-03' '音乐' '歌词时间轴解析' ($lyric.Code -eq 200 -and $lyric.Data.Count -ge 5) "lines=$($lyric.Data.Count)"

$songMv = Invoke-Api -Method GET -Path '/api/music/songs/3003/mv'
Check 'MUS-04' '音乐' '歌曲关联 MV' ($songMv.Code -eq 200 -and $songMv.Data.songId -eq 3003) "mvId=$($songMv.Data.id), songId=$($songMv.Data.songId)"

$mvNoVip = Invoke-Api -Method GET -Path '/api/music/mv/4002/play' -Token $userToken
Check 'MUS-05' '音乐' '非会员播放高清 MV 被拦截' ($mvNoVip.Code -eq 403) "code=$($mvNoVip.Code), message=$($mvNoVip.Message)"

$mvAdmin = Invoke-Api -Method GET -Path '/api/music/mv/4002/play' -Token $adminToken
Check 'MUS-06' '音乐' '管理员播放高清 MV' ($mvAdmin.Code -eq 200 -and $mvAdmin.Data.videoUrl) "code=$($mvAdmin.Code)"

$rank = Invoke-Api -Method GET -Path '/api/music/rank/hot?limit=5'
Check 'MUS-07' '音乐' '热门榜单' ($rank.Code -eq 200 -and $rank.Data.Count -gt 0) "count=$($rank.Data.Count)"

$keyword = [uri]::EscapeDataString('晴天')
$search = Invoke-Api -Method GET -Path "/api/search/search?keyword=$keyword&type=SONG&pageNum=1&pageSize=10"
$searchHit = @($search.Data.items) | Where-Object { $_.bizId -eq 3001 }
Check 'MUS-08' '搜索' '歌曲全文检索' ($search.Code -eq 200 -and $search.Data.total -ge 1 -and $searchHit) "total=$($search.Data.total), hit=$($searchHit.title)"

$hotWords = Invoke-Api -Method GET -Path '/api/search/search/hot-keywords'
Check 'MUS-09' '搜索' '热门搜索词' ($hotWords.Code -eq 200 -and $hotWords.Data.Count -gt 0) "count=$($hotWords.Data.Count)"

# ------------------------------------------------------------
# 4. 歌单
# ------------------------------------------------------------
$plCreate = Invoke-Api -Method POST -Path '/api/playlist/playlists' -Token $userToken -Body @{
    name = "E2E 测试歌单 $Stamp"; description = '自动化测试'; isPublic = 0
}
$playlistId = $plCreate.Data
Check 'PLA-01' '歌单' '创建歌单' ($plCreate.Code -eq 200 -and $playlistId) "id=$playlistId"

$plAdd = Invoke-Api -Method POST -Path "/api/playlist/playlists/$playlistId/songs/3003" -Token $userToken
Check 'PLA-02' '歌单' '添加歌曲到歌单' ($plAdd.Code -eq 200) "code=$($plAdd.Code)"

$plDetail = Invoke-Api -Method GET -Path "/api/playlist/playlists/$playlistId" -Token $userToken
$plSongs = @($plDetail.Data.songs)
Check 'PLA-03' '歌单' '歌单详情包含歌曲' ($plDetail.Code -eq 200 -and $plSongs.Count -eq 1 -and $plSongs[0].songId -eq 3003) "songs=$($plSongs.Count)"

$plMine = Invoke-Api -Method GET -Path '/api/playlist/playlists/mine?pageNum=1&pageSize=50' -Token $userToken
$mineHit = @($plMine.Data.records) | Where-Object { $_.id -eq $playlistId }
Check 'PLA-04' '歌单' '我的歌单列表' ($plMine.Code -eq 200 -and $mineHit) "total=$($plMine.Data.total)"

$plRemove = Invoke-Api -Method DELETE -Path "/api/playlist/playlists/$playlistId/songs/3003" -Token $userToken
Check 'PLA-05' '歌单' '移除歌单歌曲' ($plRemove.Code -eq 200) "code=$($plRemove.Code)"

$plDelete = Invoke-Api -Method DELETE -Path "/api/playlist/playlists/$playlistId" -Token $userToken
Check 'PLA-06' '歌单' '删除歌单' ($plDelete.Code -eq 200) "code=$($plDelete.Code)"

$plPublic = Invoke-Api -Method GET -Path '/api/playlist/playlists/public?pageNum=1&pageSize=5'
Check 'PLA-07' '歌单' '公开歌单列表' ($plPublic.Code -eq 200 -and $plPublic.Data.total -ge 1) "total=$($plPublic.Data.total)"

# ------------------------------------------------------------
# 5. 点赞、评论、播放记录
# ------------------------------------------------------------
$like = Invoke-Api -Method POST -Path '/api/comment/likes/3003' -Token $userToken
$likeStatus = Invoke-Api -Method GET -Path '/api/comment/likes/3003/status' -Token $userToken
Check 'COM-01' '互动' '点赞歌曲' ($like.Code -eq 200 -and $likeStatus.Data -eq $true) "code=$($like.Code), liked=$($likeStatus.Data)"

$likeMine = Invoke-Api -Method GET -Path '/api/comment/likes/mine?pageNum=1&pageSize=20' -Token $userToken
$likeHit = @($likeMine.Data.records) | Where-Object { $_.songId -eq 3003 }
Check 'COM-02' '互动' '我喜欢的音乐' ($likeMine.Code -eq 200 -and $likeHit) "total=$($likeMine.Data.total)"

$commentText = "E2E 自动化评论 $Stamp"
$comment = Invoke-Api -Method POST -Path '/api/comment/comments' -Token $userToken -Body @{ songId = 3003; content = $commentText }
$commentId = $comment.Data
Check 'COM-03' '互动' '发表歌曲评论' ($comment.Code -eq 200 -and $commentId) "id=$commentId"

$commentList = Invoke-Api -Method GET -Path '/api/comment/comments/song/3003?pageNum=1&pageSize=50'
$commentHit = @($commentList.Data.records) | Where-Object { $_.id -eq $commentId }
Check 'COM-04' '互动' '评论列表回读' ($commentList.Code -eq 200 -and $commentHit) "total=$($commentList.Data.total)"

$commentDel = Invoke-Api -Method DELETE -Path "/api/comment/comments/$commentId" -Token $userToken
Check 'COM-05' '互动' '删除自己的评论' ($commentDel.Code -eq 200) "code=$($commentDel.Code)"

$unlike = Invoke-Api -Method DELETE -Path '/api/comment/likes/3003' -Token $userToken
Check 'COM-06' '互动' '取消点赞' ($unlike.Code -eq 200) "code=$($unlike.Code)"

$record = Invoke-Api -Method POST -Path '/api/comment/play-records' -Token $userToken -Body @{ songId = 3003; device = 'PC'; playedSeconds = 15 }
Start-Sleep -Seconds 2
$recordMine = Invoke-Api -Method GET -Path '/api/comment/play-records/mine?pageNum=1&pageSize=10' -Token $userToken
Check 'COM-07' '互动' '播放记录异步落库' ($record.Code -eq 200 -and $recordMine.Data.total -ge 1) "recordCode=$($record.Code), total=$($recordMine.Data.total)"

$recent = Invoke-Api -Method GET -Path '/api/comment/play-records/recent?limit=5' -Token $userToken
Check 'COM-08' '互动' '最近播放列表' ($recent.Code -eq 200 -and $recent.Data.Count -ge 1) "count=$($recent.Data.Count)"

# ------------------------------------------------------------
# 6. VIP 与模拟支付
# ------------------------------------------------------------
$packages = Invoke-Api -Method GET -Path '/api/vip/vip/packages'
Check 'VIP-01' '会员' '套餐列表免登录访问' ($packages.Code -eq 200 -and $packages.Data.Count -ge 3) "count=$($packages.Data.Count)"

$vipBefore = Invoke-Api -Method GET -Path '/api/vip/vip/status' -Token $userToken
Check 'VIP-02' '会员' '新用户初始非会员' ($vipBefore.Code -eq 200 -and $vipBefore.Data.vip -eq $false) "vip=$($vipBefore.Data.vip)"

$channels = Invoke-Api -Method GET -Path '/api/vip/vip/pay/channels' -Token $userToken
Check 'VIP-03' '支付' '支付渠道开关' ($channels.Code -eq 200 -and $channels.Data.mockEnabled -eq $true) "mock=$($channels.Data.mockEnabled)"

$packageId = $packages.Data[0].id
$payCreate = Invoke-Api -Method POST -Path '/api/vip/vip/pay/create' -Token $userToken -Body @{ packageId = $packageId; payType = 'MOCK' }
$orderNo = $payCreate.Data.orderNo
Check 'VIP-04' '支付' '创建模拟支付订单' ($payCreate.Code -eq 200 -and $orderNo) "orderNo=$orderNo, amount=$($payCreate.Data.amount)"

$payPending = Invoke-Api -Method GET -Path "/api/vip/vip/pay/status/$orderNo" -Token $userToken
Check 'VIP-05' '支付' '订单初始待支付' ($payPending.Code -eq 200 -and $payPending.Data.status -eq 'UNPAID') "status=$($payPending.Data.status)"

$mockPay = Invoke-Api -Method POST -Path "/api/vip/vip/pay/mock/$orderNo/success" -Token $userToken
Check 'VIP-06' '支付' '模拟支付成功' ($mockPay.Code -eq 200 -and $mockPay.Data.status -eq 'PAID') "status=$($mockPay.Data.status), transactionId=$($mockPay.Data.transactionId)"

$vipAfter = Invoke-Api -Method GET -Path '/api/vip/vip/status' -Token $userToken
Check 'VIP-07' '会员' '支付后会员生效' ($vipAfter.Code -eq 200 -and $vipAfter.Data.vip -eq $true) "vip=$($vipAfter.Data.vip), expire=$($vipAfter.Data.expireTime)"

$permission = Invoke-Api -Method GET -Path '/api/vip/vip/permission?type=SONG' -Token $userToken
Check 'VIP-08' '会员' '会员权限校验 SONG' ($permission.Code -eq 200 -and $permission.Data -eq $true) "permission=$($permission.Data)"

$mvAfterVip = Invoke-Api -Method GET -Path '/api/music/mv/4002/play' -Token $userToken
Check 'VIP-09' '会员' '开通后播放高清 MV' ($mvAfterVip.Code -eq 200 -and $mvAfterVip.Data.videoUrl) "code=$($mvAfterVip.Code)"

$orders = Invoke-Api -Method GET -Path '/api/vip/vip/orders?pageNum=1&pageSize=10' -Token $userToken
$orderHit = @($orders.Data.records) | Where-Object { $_.orderNo -eq $orderNo }
Check 'VIP-10' '会员' '我的开通记录' ($orders.Code -eq 200 -and $orderHit) "total=$($orders.Data.total)"

$expireBefore = (Invoke-Api -Method GET -Path '/api/vip/vip/status' -Token $userToken).Data.expireTime
$mockAgain = Invoke-Api -Method POST -Path "/api/vip/vip/pay/mock/$orderNo/success" -Token $userToken
$expireAfter = (Invoke-Api -Method GET -Path '/api/vip/vip/status' -Token $userToken).Data.expireTime
Check 'VIP-11' '支付' '重复支付幂等' ($mockAgain.Code -eq 200 -and $expireBefore -eq $expireAfter) "before=$expireBefore, after=$expireAfter"

$vipStats = Invoke-Api -Method GET -Path '/api/vip/vip/admin/statistics' -Token $adminToken
Check 'VIP-12' '后台' '会员统计' ($vipStats.Code -eq 200) "code=$($vipStats.Code)"

# ------------------------------------------------------------
# 7. 直播
# ------------------------------------------------------------
$square = Invoke-Api -Method GET -Path '/api/live/live/rooms/square?pageNum=1&pageSize=20'
Check 'LIVE-01' '直播' '直播广场' ($square.Code -eq 200 -and $square.Data.total -ge 2) "total=$($square.Data.total)"

$scenes = Invoke-Api -Method GET -Path '/api/live/live/rooms/scenes'
$outdoorScene = @($scenes.Data) | Where-Object { $_.value -eq 'OUTDOOR' }
Check 'LIVE-02' '直播' '场景枚举包含户外' ($scenes.Code -eq 200 -and $outdoorScene) "scenes=$($scenes.Data.value -join ',')"

$outdoorRoom = @($square.Data.records) | Where-Object { $_.scene -eq 'OUTDOOR' } | Select-Object -First 1
Check 'LIVE-03' '直播' '户外直播间出现在广场' ([bool]$outdoorRoom) "room=$($outdoorRoom.title), location=$($outdoorRoom.location)"

$normalRoom = Invoke-Api -Method POST -Path '/api/live/live/rooms' -Token $userToken -Body @{
    title = 'E2E 普通用户越权建房'; category = '弹唱'; scene = 'INDOOR'
}
Check 'LIVE-04' '直播' '普通用户创建直播间被拒绝' ($normalRoom.Code -eq 403 -or $normalRoom.Http -eq 403) "code=$($normalRoom.Code)"

$grant = Invoke-Api -Method PUT -Path "/api/users/admin/$userId/roles?role=ANCHOR" -Token $adminToken
Check 'LIVE-05' '权限' '管理员授予主播角色' ($grant.Code -eq 200) "code=$($grant.Code)"

$anchorLogin = Login $TestUser $TestPass
$anchorToken = $anchorLogin.Data.token
Check 'LIVE-06' '直播' '主播重新登录取得 ANCHOR 角色' ($anchorLogin.Code -eq 200 -and $anchorLogin.Data.roles -match 'ANCHOR') "roles=$($anchorLogin.Data.roles)"

$roomCreate = Invoke-Api -Method POST -Path '/api/live/live/rooms' -Token $anchorToken -Body @{
    title = "E2E 户外测试 $Stamp"; intro = '自动化测试'; category = '弹唱'; scene = 'OUTDOOR'; location = '测试公园'
}
$roomId = $roomCreate.Data
Check 'LIVE-07' '直播' '创建户外直播间' ($roomCreate.Code -eq 200 -and $roomId) "id=$roomId"

$roomDetail = Invoke-Api -Method GET -Path "/api/live/live/rooms/$roomId" -Token $anchorToken
Check 'LIVE-08' '直播' '户外场景与地点回读' ($roomDetail.Code -eq 200 -and $roomDetail.Data.scene -eq 'OUTDOOR' -and $roomDetail.Data.location -eq '测试公园') "scene=$($roomDetail.Data.scene), location=$($roomDetail.Data.location)"

$push = Invoke-Api -Method GET -Path "/api/live/live/rooms/$roomId/push-url" -Token $anchorToken
Check 'LIVE-09' '直播' '获取推流地址与户外二维码' ($push.Code -eq 200 -and $push.Data.pushUrl -match '^rtmp://' -and $push.Data.pushQr) "push=$($push.Data.pushUrl)"

$start = Invoke-Api -Method POST -Path "/api/live/live/rooms/$roomId/start" -Token $anchorToken
Check 'LIVE-10' '直播' '开播' ($start.Code -eq 200 -and $start.Data.status -eq 'LIVING') "status=$($start.Data.status)"

$online = Invoke-Api -Method GET -Path "/api/live/live/rooms/$roomId/online" -Token $anchorToken
Check 'LIVE-11' '直播' '在线人数查询' ($online.Code -eq 200) "online=$($online.Data)"

$stop = Invoke-Api -Method POST -Path "/api/live/live/rooms/$roomId/stop" -Token $anchorToken
Check 'LIVE-12' '直播' '下播并生成回放' ($stop.Code -eq 200 -and $stop.Data.status -eq 'ENDED' -and $stop.Data.replayUrl) "status=$($stop.Data.status), replay=$($stop.Data.replayUrl)"

$roomDelete = Invoke-Api -Method DELETE -Path "/api/live/live/rooms/$roomId" -Token $anchorToken
Check 'LIVE-13' '直播' '删除测试直播间' ($roomDelete.Code -eq 200) "code=$($roomDelete.Code)"

$dmCheck = Invoke-Api -Method GET -Path "/api/live/live/danmaku/check?content=$([uri]::EscapeDataString('测试弹幕'))" -Token $anchorToken
$dmSend = Invoke-Api -Method POST -Path '/api/live/live/danmaku/send' -Token $anchorToken -Body @{ roomId = 11001; content = "E2E 弹幕 $Stamp" }
$dmHistory = Invoke-Api -Method GET -Path '/api/live/live/danmaku/history/11001?pageNum=1&pageSize=20' -Token $anchorToken
Check 'LIVE-14' '直播' '弹幕检查/发送/历史' ($dmCheck.Code -eq 200 -and $dmSend.Code -eq 200 -and $dmHistory.Data.total -ge 1) "history=$($dmHistory.Data.total)"

$gifts = Invoke-Api -Method GET -Path '/api/live/live/gifts' -Token $anchorToken
$balance = Invoke-Api -Method GET -Path '/api/live/live/gifts/balance' -Token $anchorToken
$recharge = Invoke-Api -Method POST -Path '/api/live/live/gifts/recharge?amount=100' -Token $anchorToken
$giftSend = Invoke-Api -Method POST -Path '/api/live/live/gifts/send' -Token $anchorToken -Body @{ roomId = 11001; giftId = $gifts.Data[0].id; count = 1 }
Check 'LIVE-15' '直播' '礼物列表/充值/赠送' ($gifts.Code -eq 200 -and $balance.Code -eq 200 -and $recharge.Code -eq 200 -and $giftSend.Code -eq 200) "balance=$($balance.Data), gifts=$($gifts.Data.Count)"

$liveStats = Invoke-Api -Method GET -Path '/api/live/live/rooms/admin/statistics' -Token $adminToken
Check 'LIVE-16' '后台' '直播统计' ($liveStats.Code -eq 200) "code=$($liveStats.Code)"

# ------------------------------------------------------------
# 8. 音乐人 / 广告 / 文件
# ------------------------------------------------------------
$musicians = Invoke-Api -Method GET -Path '/api/musician/musician/page?pageNum=1&pageSize=10'
Check 'MUSICIAN-01' '音乐人' '音乐人列表' ($musicians.Code -eq 200 -and $musicians.Data.total -ge 1) "total=$($musicians.Data.total)"

if ($musicians.Data.total -ge 1) {
    $mid = $musicians.Data.records[0].id
    $musicianHome = Invoke-Api -Method GET -Path "/api/musician/musician/$mid/homepage"
    Check 'MUSICIAN-02' '音乐人' '音乐人主页作品' ($musicianHome.Code -eq 200 -and $musicianHome.Data.works.Count -ge 1) "works=$($musicianHome.Data.works.Count)"
} else {
    Check 'MUSICIAN-02' '音乐人' '音乐人主页作品' $false '无音乐人数据'
}

$ad = Invoke-Api -Method GET -Path '/api/ad/ad/active?slot=HOME_BANNER'
Check 'AD-01' '广告' '首页广告位投放' ($ad.Code -eq 200 -and $ad.Data.Count -ge 1) "count=$($ad.Data.Count)"
$adSlots = @('HOME_BANNER', 'LIVE', 'PLAYER') | ForEach-Object {
    $slotResult = Invoke-Api -Method GET -Path "/api/ad/ad/active?slot=$($_)"
    [pscustomobject]@{ slot = $_; result = $slotResult }
}
$activeSlots = @($adSlots | Where-Object { $_.result.Code -eq 200 -and $_.result.Data.Count -ge 1 })
Check 'AD-02' '广告' '多个广告位可查询有效投放' ($activeSlots.Count -ge 1) "activeSlots=$($activeSlots.slot -join ',')"
Check 'AD-03' '广告' '同一广告位返回可投放广告列表' ($ad.Code -eq 200 -and $null -ne $ad.Data -and (@($ad.Data | Select-Object -ExpandProperty planId -Unique).Count -eq @($ad.Data).Count)) "plans=$(@($ad.Data).Count)"

$constraints = Invoke-Api -Method GET -Path '/api/file/files/constraints' -Token $userToken
Check 'FILE-01' '文件' '上传约束' ($constraints.Code -eq 200 -and $constraints.Data.image) "image=$($constraints.Data.image -join ',')"

$png = 'D:\kumao\logs\run-20261106-e2e.png'
$b64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='
[IO.File]::WriteAllBytes($png, [Convert]::FromBase64String($b64))
$upload = Invoke-Api -Method POST -Path '/api/file/files/upload?bizType=IMAGE' -Token $userToken -Form @{ file = Get-Item $png }
$fileId = $upload.Data.id
$presigned = Invoke-Api -Method GET -Path "/api/file/files/$fileId/presigned" -Token $userToken
$fileDelete = Invoke-Api -Method DELETE -Path "/api/file/files/$fileId" -Token $userToken
Check 'FILE-02' '文件' '图片上传/预签名/删除' ($upload.Code -eq 200 -and $presigned.Code -eq 200 -and $fileDelete.Code -eq 200) "fileId=$fileId"

# ------------------------------------------------------------
# 9. 后台管理
# ------------------------------------------------------------
$usersPage = Invoke-Api -Method GET -Path '/api/users/admin/page?pageNum=1&pageSize=5' -Token $adminToken
Check 'ADMIN-01' '后台' '用户管理列表' ($usersPage.Code -eq 200) "total=$($usersPage.Data.total)"
$adminStats = Invoke-Api -Method GET -Path '/api/users/admin/statistics' -Token $adminToken
Check 'ADMIN-02' '后台' '用户统计' ($adminStats.Code -eq 200) "code=$($adminStats.Code)"

# ------------------------------------------------------------
# 输出结果
# ------------------------------------------------------------
$passed = @($script:Results | Where-Object { $_.result -eq 'PASS' }).Count
$failed = @($script:Results | Where-Object { $_.result -eq 'FAIL' }).Count
$total = $script:Results.Count
$duration = [math]::Round(((Get-Date) - $script:StartedAt).TotalSeconds, 1)

$outDir = 'D:\kumao\reports'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$jsonPath = Join-Path $outDir "e2e-api-result-$Stamp.json"
$csvPath = Join-Path $outDir "e2e-api-result-$Stamp.csv"
$script:Results | ConvertTo-Json -Depth 5 | Set-Content -Path $jsonPath -Encoding UTF8
$script:Results | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8

Write-Host ''
Write-Host "测试完成：$passed/$total 通过，$failed 失败，耗时 $duration 秒" -ForegroundColor $(if ($failed -eq 0) { 'Green' } else { 'Yellow' })
Write-Host "结果文件：$jsonPath"
Write-Host "结果文件：$csvPath"
if ($failed -gt 0) { exit 1 }
