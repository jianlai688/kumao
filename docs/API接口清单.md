# 酷猫音乐平台 · API 接口清单

> 统一前缀：网关 `http://127.0.0.1:8080`
> **网关访问路径 = `/api/{模块名}` + 下表路径**，例如 `GET /api/music/songs`、`POST /api/auth/login`、`GET /api/live/live/rooms/square`。
> 鉴权：除白名单外，所有接口需 `Authorization: Bearer {token}`
> 响应格式：`{"code":200,"message":"操作成功","data":...}`

## 1. gateway 网关

| 能力 | 说明 |
|---|---|
| 路由 | `/api/{module}/**` → 对应微服务（`StripPrefix=2`） |
| WebSocket | `/ws/**` → `lb://live-service`（弹幕） |
| 鉴权 | `AuthGlobalFilter`：JWT 解析、设备识别、专属接口隔离、后台 ADMIN 校验 |
| 限流 | Sentinel：全局限流 200 QPS/IP，写接口 30 QPS |
| 降级 | 统一返回 `{"code":429,"message":"访问过于频繁，请稍后再试"}` |

白名单：登录/注册/短信、歌曲/歌手/榜单/MV/歌词查询、公开歌单、搜索、直播广场、套餐、静态资源。

## 2. user-service（8101）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/auth/sms-code?phone=` | 发送短信验证码（演示返回 mockCode） |
| POST | `/auth/register` | 注册（`type=PHONE/PASSWORD`） |
| POST | `/auth/login` | 登录（`PASSWORD/SMS/OAUTH/ONE_CLICK`） |
| POST | `/auth/logout` | 退出登录 |
| POST | `/auth/kick-others` | 单点下线（踢掉其它端） |
| POST | `/auth/password/reset` | 找回密码 |
| POST | `/auth/password/change` | 修改密码 |
| GET | `/users/me` | 当前用户资料 |
| PUT | `/users/me` | 修改资料（昵称/头像/简介，三端同步） |
| GET | `/users/me/login-logs` | 我的登录设备记录 |
| POST | `/users/anchor/apply` | 提交主播认证，必须携带身份证号及正反面照片地址 |
| GET | `/users/anchor/my` | 我的主播申请记录（含收款账户绑定信息） |
| PUT | `/users/anchor/profile` | 修改主播资料；可绑定支付宝/微信收款账户（payType/payAccount/payName） |
| GET | `/users/admin/page` | 后台用户列表 |
| PUT | `/users/admin/{id}/status` | 启用/禁用账号 |
| GET | `/users/admin/login-logs` | 后台查询登录设备记录 |
| GET | `/users/admin/anchor-applies` | 主播申请列表 |
| POST | `/users/admin/anchor-applies/{id}/audit` | 主播审核 |
| PUT | `/users/admin/{id}/roles` | 授予角色 |
| GET | `/users/admin/statistics` | 用户统计 |
| GET | `/internal/users/{id}` | 内部：用户信息 |
| POST/DELETE | `/internal/users/{id}/roles` | 内部：角色授予/撤销 |

## 3. music-service（8102）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/songs` | 歌曲分页（region/language/style/singerId/status/keyword） |
| GET | `/songs/{id}` | 歌曲详情 |
| GET | `/songs/{id}/lyric` | 解析后的 LRC 歌词（时间轴） |
| GET | `/songs/{id}/lyric/raw` | 原始 LRC 文本 |
| GET | `/songs/{id}/mv` | 歌曲关联 MV（播放页切换 MV，可为空） |
| GET | `/songs/regions` | 地区分类（国内/国外/粤语） |
| POST/PUT/DELETE | `/songs/admin/**` | 后台歌曲维护、上下架 |
| GET | `/singers` `/singers/{id}` `/singers/{id}/songs` | 歌手查询 |
| POST/PUT/DELETE | `/singers/admin/**` | 歌手维护 |
| GET | `/rank/hot` `/rank/new` `/rank/rising` | 热门榜/新歌榜/飙升榜（Redis 缓存） |
| GET | `/mv` `/mv/{id}` | MV 列表与详情 |
| GET | `/mv/{id}/play` | MV 播放（VIP 高清校验） |
| POST/PUT/DELETE | `/mv/admin/**` | MV 管理 |
| GET | `/banners` | 轮播图 |
| GET | `/internal/music/songs/{id}` | 内部：歌曲信息 |
| GET | `/internal/music/songs/batch?ids=` | 内部：批量歌曲 |
| POST | `/internal/music/songs` | 内部：音乐人作品入库 |
| POST | `/internal/music/songs/{id}/play-count` | 内部：播放量+1 |
| POST | `/internal/music/songs/{id}/like-count` | 内部：红心数 |
| POST | `/internal/music/songs/{id}/comment-count` | 内部：评论数 |

## 4. playlist-service（8103）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/playlists/public` | 公开歌单分页（sort=hot/new） |
| GET | `/playlists/public/{id}` | 公开歌单详情 |
| GET | `/playlists/mine` | 我的歌单 |
| GET | `/playlists/collected` | 我收藏的歌单 |
| GET | `/playlists/{id}` | 歌单详情（含歌曲） |
| POST/PUT/DELETE | `/playlists` `/playlists/{id}` | 创建/编辑/删除 |
| POST/DELETE | `/playlists/{id}/songs/{songId}` | 添加/移除歌曲 |
| POST/DELETE | `/playlists/{id}/collect` | 收藏/取消收藏 |
| GET | `/playlists/admin/page` | 后台歌单管理 |
| DELETE | `/playlists/admin/{id}` | 后台删除 |

## 5. comment-service（8104）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/comments/song/{songId}` | 歌曲评论分页 |
| GET | `/comments/{id}/replies` | 评论回复 |
| POST | `/comments` | 发表评论；AI 通过自动展示，否则进入待管理员复核 |
| DELETE | `/comments/{id}` | 删除自己的评论 |
| GET | `/comments/song/{songId}/count` | 评论数 |
| GET | `/comments/admin/page` | 后台评论管理，可按 `status=0/1/2` 筛选 |
| PUT | `/comments/admin/{id}/status` | 评论审核状态：0 通过 / 1 驳回屏蔽 / 2 待审核 |
| DELETE | `/comments/admin/{id}` | 删除评论 |
| POST/DELETE | `/likes/{songId}` | 添加/取消红心 |
| GET | `/likes/mine` | 我喜欢的列表 |
| GET | `/likes/{songId}/status` | 是否已红心 |
| POST | `/play-records` | 投递播放记录（RabbitMQ） |
| GET | `/play-records/mine` | 我的播放记录 |
| GET | `/play-records/recent` | 最近播放 |
| DELETE | `/play-records/mine` | 清空播放记录 |

## 6. file-service（8105）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/files/upload?bizType=` | 单文件上传（图片/音频/歌词/视频） |
| POST | `/files/upload/batch` | 多文件上传 |
| GET | `/files/{id}/presigned` | 预签名访问地址 |
| DELETE | `/files/{id}` | 删除文件 |
| GET | `/files/constraints` | 上传约束 |
| GET | `/files/admin/page` | 后台文件管理 |

## 7. live-service（8106）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/live/rooms/square` | 直播广场（分类/场景筛选、关键词） |
| GET | `/live/rooms/categories` | 直播分类 |
| GET | `/live/rooms/scenes` | 直播场景（室内/户外） |
| GET | `/live/rooms/{id}` | 直播间详情（含 VIP 特权信息、户外地点） |
| GET | `/live/rooms/{id}/online` | 实时在线人数（Redis） |
| POST | `/live/rooms` | 创建直播间（仅主播，支持 scene=INDOOR/OUTDOOR + location） |
| PUT | `/live/rooms/{id}` | 修改直播间 |
| GET | `/live/rooms/{id}/push-url` | 获取 SRS 推流/拉流地址；户外返回扫码推流二维码 |
| GET | `/live/rooms/obs-config` | 主播/管理员：读取本机 OBS WebSocket 地址和密码，用于控制台自动填充 |
| GET | `/live/rooms/{id}/audience` | 实时观众画像：当前在线登录用户样本的性别/年龄/地区分布 |
| POST | `/live/rooms/{id}/stop` | 一键下播（停止自动推流并生成回放） |
| DELETE | `/live/rooms/{id}` | 删除直播间 |
| GET | `/live/rooms/mine` | 我的直播间 |
| GET | `/live/rooms/admin/page` | 后台直播间管理 |
| POST | `/live/rooms/admin/{id}/ban` | 封禁/解封 |
| GET | `/live/rooms/admin/statistics` | 直播统计 |
| GET | `/live/danmaku/history/{roomId}` | 弹幕历史分页，仅返回已通过弹幕 |
| POST | `/live/danmaku/send` | HTTP 发送弹幕（WS 降级通道），返回是否自动通过或转人工 |
| GET | `/live/danmaku/check?content=` | 敏感词预检 |
| GET | `/live/danmaku/admin/page` | 后台弹幕管理，可按 `status=0/1/2` 筛选 |
| PUT | `/live/danmaku/admin/{id}/status` | 弹幕审核状态：0 通过 / 1 驳回屏蔽 / 2 待审核 |
| DELETE | `/live/danmaku/admin/{id}` | 删除弹幕 |
| GET | `/live/gifts` | 礼物列表 |
| GET | `/live/gifts/balance` | 我的虚拟币余额 |
| POST | `/live/gifts/recharge?amount=` | 模拟充值 |
| POST | `/live/gifts/send` | 赠送礼物 |
| GET | `/live/gifts/received` | 主播收到礼物清单 |
| GET | `/live/gifts/received/summary` | 礼物收益汇总 |
| GET/POST/PUT/DELETE | `/live/gifts/admin/**` | 后台礼物配置 |
| GET | `/internal/live/gift-income?anchorId=` | 内部：礼物收益 |
| GET | `/internal/live/rooms/{id}` | 内部：直播间信息（供 musician-service） |
| WS | `/ws/live?roomId=&token=` | 弹幕与实时状态 WebSocket：`STREAM_STATUS`、`ROOM_METRICS`、`NOW_PLAYING`（主播控制台实时同步当前歌曲/进度/队列） |
| POST | `/live/rooms/dvr-callback` | SRS 录制回调（生成回放） |
| POST | `/live/rooms/{id}/start` | 开播；`skipAutoPush=true` 时跳过 ffmpeg 演示推流（OBS 已接管）；SRS 无现成推流时自动启动演示推流 |

## 8. search-service（8107）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/search?keyword=&type=&pageNum=&pageSize=` | 五类对象全文检索（高亮，网关路径 `/api/search/search`） |
| GET | `/search/hot-keywords` | 热门搜索词 |
| GET | `/search/admin/monitor` | 检索数据监控 |
| POST | `/search/admin/sync` | 后台手动触发索引同步 |
| POST | `/internal/search/sync` | 内部：增量同步（新增/修改） |
| DELETE | `/internal/search/sync/{type}/{bizId}` | 内部：删除同步 |
| POST | `/internal/search/sync/batch` | 内部：批量同步 |

## 9. vip-service（8108）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/vip/packages` | 套餐列表（月/季/年卡） |
| GET | `/vip/status` | 我的会员状态 |
| POST | `/vip/open` | 兼容旧版下单（MOCK 直接支付，其它渠道返回支付参数） |
| GET | `/vip/orders` | 我的开通记录 |
| GET | `/vip/pay/channels` | 可用支付渠道（支付宝/微信/模拟） |
| POST | `/vip/pay/create` | 创建支付订单 |
| GET | `/vip/pay/status/{orderNo}` | 查询支付状态（含主动查单） |
| POST | `/vip/pay/mock/{orderNo}/success` | 本地模拟支付成功（仅 MOCK） |
| GET | `/vip/pay/alipay/page/{orderNo}` | 支付宝收银台表单（公开） |
| POST | `/vip/pay/alipay/notify` | 支付宝异步通知（验签） |
| POST | `/vip/pay/wechat/notify` | 微信支付异步通知（验签） |
| GET | `/vip/permission?type=` | 付费权限校验（SONG/MV_HD/DANMAKU/ENTRANCE） |
| GET | `/vip/admin/members` | 后台会员记录 |
| GET | `/vip/admin/orders` | 后台订单记录 |
| GET | `/vip/admin/statistics` | 会员数据统计 |
| POST/PUT/DELETE | `/vip/admin/packages/**` | 后台套餐配置 |
| GET | `/internal/vip/status?userId=` | 内部：会员状态 |
| GET | `/internal/vip/permission?userId=&type=` | 内部：权限校验 |

## 10. musician-service（8109）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/musician/apply` | 入驻认证申请，必须携带身份证号及正反面照片地址 |
| GET | `/musician/mine` | 我的音乐人信息 |
| GET | `/musician/page` | 音乐人列表 |
| GET | `/musician/{id}/homepage` | 音乐人主页（作品/专辑/粉丝） |
| POST/DELETE | `/musician/{id}/follow` | 关注/取关 |
| GET/POST/PUT/DELETE | `/musician/works/**` | 作品管理（上传/编辑/下架/删除） |
| GET/POST/PUT/DELETE | `/musician/albums/**` | 专辑管理 |
| GET | `/musician/statistics` | 数据统计（播放/收藏/评论/粉丝） |
| GET | `/musician/statistics/trend` | 播放量趋势（日/周/月） |
| GET | `/musician/statistics/rank` | 单曲排行榜 |
| GET | `/musician/statistics/region` | 粉丝地域分布 |
| GET | `/musician/income/summary` | 收益汇总 |
| GET | `/musician/income` | 收益明细 |
| POST | `/musician/income/settle` | 结算（播放+礼物） |
| GET/POST | `/musician/withdraws` | 提现记录/申请 |
| GET/POST | `/musician/admin/**` | 后台审核、作品审核、收益汇总、提现审核 |
| POST | `/internal/musician/works/{songId}/stats` | 内部：作品数据回写 |

## 11. ad-service（8110）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ad/active?slot=` | 按广告位获取在投广告 |
| POST | `/ad/report/{planId}?action=` | 曝光/点击上报 |
| POST | `/ad/apply` | 入驻认证申请，必须携带身份证号及正反面照片地址 |
| GET | `/ad/mine` | 我的广告制作人信息 |
| GET/POST/PUT/DELETE | `/ad/materials/**` | 素材管理 |
| GET/POST | `/ad/plans/**` | 投放计划（新建/暂停/续投/终止） |
| GET | `/ad/statistics` | 曝光/点击/CTR |
| GET | `/ad/statistics/rank` | 素材数据排行 |
| GET | `/ad/statistics/region` | 地域分布 |
| GET | `/ad/statistics/hourly` | 分时段趋势 |
| GET/POST | `/ad/admin/**` | 后台审核、投放审核、在投计划暂停/续投/终止、投放收入汇总 |

## 12. review-service（8111）

> 网关入口统一为 `/api/admin/ai/**`，网关要求 ADMIN 角色。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ai/status` | 审核模型配置/降级模式状态 |
| GET | `/ai/models` | 审核/文本/识别图片三类模型配置 |
| PUT | `/ai/models/{type}` | 保存模型配置（REVIEW/TEXT/VISION） |
| POST | `/ai/models/{type}/models` | 获取服务商模型列表，供页面下拉选择 |
| POST | `/ai/models/{type}/test` | 测试模型连接，支持未保存草稿 |
| GET | `/ai/auto/status` | 自动审核状态、统计与最近执行结果 |
| GET | `/ai/auto/tasks?pageNum=&pageSize=&status=&category=` | 自动审核任务分页 |
| POST | `/ai/auto/run` | 手动触发一轮自动审核 |
| DELETE | `/ai/auto/tasks/{category}/{itemId}` | 管理员确认后标记自动审核任务已处理 |
| POST | `/ai/review` | 生成 AI 审核建议（不会执行业务审核） |
| POST | `/ai/log` | 写入管理员最终审核日志 |
| GET | `/ai/logs?pageNum=&pageSize=&category=&decision=` | 审核日志分页 |
| GET | `/ai/stats` | 审核日志统计 |