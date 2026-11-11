# 酷猫音乐 · APP 端（uniapp）

与 `kumao-web` 共用同一套后端（`http://127.0.0.1:8080/api`），登录态、歌曲、歌单、直播间、会员数据完全互通。

## 运行

```bash
npm install
npm run dev:h5      # http://127.0.0.1:5174
npm run dev:app     # HBuilderX / 自定义基座
npm run build:h5
```

H5 开发环境通过 `vite.config.js` 的 `/api` 代理访问网关；APP 端走 `src/api/index.js` 里的绝对地址（可用 `VITE_API_BASE` 覆盖）。

## 设计系统

| Token | 值 | 用途 |
| --- | --- | --- |
| `--km-accent` | `#f5b45b` | 主色（琥珀金） |
| `--km-accent-deep` | `#e08b3e` | 渐变暗端 |
| `--km-grad` | `140deg #fbd08a → #de823c` | 主按钮 / 选中态 |
| `--km-bg` | `#08070a` | 页面底色 |
| `--km-card` | `#121116` | 卡片 |
| `--km-text / dim / faint` | `#f7f1e8 / 58% / 34%` | 三级文字 |
| `--km-line` | `rgba(255,255,255,.075)` | 发丝分割线 |

全局样式与变量在 `src/App.vue`，通用类：`.km-screen`、`.km-card`、`.km-chip`、`.km-h1`、`.km-sub`、`.km-empty`、`.km-glass`。

## 页面

| 页面 | 说明 |
| --- | --- |
| `pages/index/index` | 发现音乐：今日主打 / 六宫格 / 正在直播 / 筛选 / 榜单 / 迷你播放器 |
| `pages/rank/rank` | 排行榜：热门 / 新歌 / 飙升，song-row 统一榜单行 |
| `pages/live/live` | 音乐直播：分类、精选直播间、双列直播卡 |
| `pages/live/room` | 直播间（弹幕、礼物） |
| `pages/song/song` | 播放页：沉浸封面、逐行歌词、进度、控制栏 |
| `pages/singer/singer` | 歌手（新增） |
| `pages/mv/mv` | MV 专区（新增） |
| `pages/mv/play` | MV 播放器（新增）：video 播放、喜欢/评论/分享、更多 MV |
| `pages/musician/musician` | 音乐人（新增） |
| `pages/playlist/*` | 歌单广场 / 详情 |
| `pages/search/search` | 搜索（支持 `?keyword=` 直达） |
| `pages/mine/mine` | 我的 |
| `pages/vip/vip` | 会员中心（tabBar 第 4 项） |
| `pages/login/login` | 登录 |

底部 tabBar：发现 / 排行 / 直播 / 会员 / 我的，图标在 `src/static/tabbar/`（脚本 `scripts/gen-tabbar-icons.py` 可重新生成）。

## 组件

| 组件 | 说明 |
| --- | --- |
| `km-icon` | 矢量图标：内联 SVG → data URI，任意颜色/尺寸，跨端可用 |
| `km-mini-player` | 悬浮迷你播放器（进度、播放/暂停、进入播放页） |
| `km-song-row` | 歌曲行：名次 / 封面 / VIP 标 / 播放量 / 收藏 |
| `km-live-card` | 直播卡：封面 / LIVE / 在线人数 / 主播 |
| `km-nav-bar` | 自定义导航条（状态栏适配 + 返回 + 右侧插槽） |
| `km-comment-sheet` | 评论面板：分页加载、发评论、登录拦截 |
| `km-queue-sheet` | 播放列表抽屉：当前曲高亮、点击切歌、清空 |

## 播放内核

`src/utils/player.js` 维护全局单例 `InnerAudioContext`：`state`（当前歌曲、队列、进度、播放态）+ `play/toggle/seek/next/prev`。
首页迷你播放器、榜单、播放页共用同一个实例，切换页面不断音。播放记录通过 `api.playRecord` 上报（静默失败，未登录不打断）。

## 评论区 / 播放列表 / MV

- **评论区** `km-comment-sheet`：`GET /comment/comments/song/{id}` 分页拉取，`POST /comment/comments` 发布；未登录点发送会跳登录；播放页评论图标、MV 页评论按钮共用同一组件。
- **播放列表抽屉** `km-queue-sheet`：读取 `usePlayer().state.queue`，点击切歌（`playAt`）、清空（`clear`），当前曲有小均衡器动效；播放页和迷你播放器都能拉起。
- **MV 播放器** `pages/mv/play`：`GET /music/mv/{id}` 取详情、`POST /music/mv/{id}/play` 取播放地址（相对路径经 `fileUrl()` 补全到网关），`<video>` 播放；下方为信息、操作与相关 MV。
- H5 开发服务器已代理 `/static`（视频/封面资源），APP 端由 `fileUrl()` 拼 `http://127.0.0.1:8080`。

## 与 Web 的接口对齐

- 榜单：`/music/rank/hot|new|rising`
- 直播：`/live/live/rooms/square`
- 歌曲：`/music/songs/{id}`、`/lyric`
- 互动：`/comment/likes/{id}`、`/comment/comments/song/{id}/count`（静默）
- MV：`/music/mv`、歌手：`/music/singers`
- 会员：`/vip/vip/*`

`src/api/index.js` 的 `request` 支持 `silent: true`：401 时不跳登录、不弹 toast，用于可选请求（点赞状态、评论数、播放记录）。

