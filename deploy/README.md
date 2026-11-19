# 酷猫音乐 · 公网发布说明（Nginx + cpolar）

本目录是这台机器上把酷猫音乐平台发布到公网的唯一入口配置。

## 架构

```
公网用户 → https://<随机子域>.r11.cpolar.top
        → cpolar Windows 服务（唯一一条隧道 cc-public，指向本机 80）
        → Nginx :80（本目录 nginx.conf）
            /        → D:/kumao/kumao-web/dist（已构建的 Vue3 单页，Hash 路由）
            /api/    → kumao-gateway :8080（网关自己 StripPrefix，代理不改写路径）
            /static/ → kumao-gateway :8080（图片/音频等 MinIO 资源经 file-service 输出）
            /ws/     → kumao-gateway :8080（弹幕/音乐人 WebSocket）
            /hls/    → SRS :8088（直播 HLS，/hls/live/x.m3u8 → /live/x.m3u8）
        → 11 个业务微服务 8101-8111
```

## 文件

| 文件 | 作用 |
|---|---|
| `nginx.conf` | 公网网关配置（唯一真源）。启动脚本会复制到 `D:\tools\nginx-1.30.4\conf\kumao.conf` |
| `start-public.bat` | 一键启动：11 个业务服务 + 网关 + nginx（端口被占用则跳过） |
| `stop-public.bat` | 一键停止：nginx + 全部后端 jar（不动 cpolar 服务） |
| `verify-public-home.png` | 公网首页渲染验收截图 |

> `.bat` 必须保持**纯 ASCII + CRLF**；改完请用
> `python <skills>\local-to-public\scripts\fix_bat.py <文件>` 重新规范化。

## 启停

```
双击 D:\kumao\deploy\start-public.bat    启动（第一次启动约 40 秒，日志见 D:\kumao\logs\public）
双击 D:\kumao\deploy\stop-public.bat     停止
```

本机自检（必须带 `--noproxy "*"`，否则会被会话代理污染成假 502）：

```
curl --noproxy "*" http://127.0.0.1/api/music/banners
```

## 公网地址

- 当前：`https://7c4869c1.r60.cpolar.top`（2026-11-17 15:5x 实测可用）
- 查看/变更：<https://dashboard.cpolar.com> → 「状态」页（云端权威）；
  本机也可看 `%USERPROFILE%\.cpolar\logs\` 最新日志里的 `Url` 字段
- cpolar 免费版子域名随机，**服务重启后可能变化**；长期使用请在 dashboard「预留」固定子域名，
  然后写进 `%USERPROFILE%\.cpolar\cpolar.yml` 的 `hostname`，再以管理员身份
  `net stop cpolar && net start cpolar`（服务不热重载配置）。

> **换域名后手机 App 怎么办**：v1.5.2 起 App 内「设置 → 服务器」可直接改接口地址并保存
> （存本地，立即生效），不用重新打包 APK。装的是旧版 APK 则必须重装新版。

## 手机 App（公网版）

| 项 | 值 |
|---|---|
| 安装包 | `kumao-web\public\downloads\KuMao-Android-v1.5.2-public.apk` |
| 公网下载 | `https://<当前域名>/downloads/KuMao-Android-v1.5.2-public.apk` |
| 内置接口地址 | `https://<当前域名>/api` |
| 版本 | 1.5.2（versionCode 152） |

重新打包（域名变了想做成新的默认值时才需要）：

```
pwsh -File D:\kumao\scripts\build-mobile-apk.ps1 -Mode wifi -ApiBase https://<当前域名>/api
```

打完把 `public\downloads\KuMao-Android-v<版本>.apk` 另存一份 `-public.apk`，
并同时复制到 `kumao-web\dist\downloads\`，否则公网下载页拿不到新包。

## 本机环境约定（踩坑记录）

1. **中间件端口**：kumao 的 docker 中间件把 MySQL 发布在 **3307**（3306 被本机 mysqld 占用）。
2. **Redis**：本机另有一个自带密码的 `redis-server` 占着 `127.0.0.1:6379`，docker 的
   kumao-redis 只能用 IPv6 回环访问，所以服务必须带 `REDIS_HOST=::1`，否则登录报
   `NOAUTH Authentication required`。
3. **nginx 配置路径**：nginx 读不了含中文的配置路径，配置一律先复制进它自己的 `conf\` 目录再加载。
4. **nginx 重启**：Windows 上 `nginx -s stop` 读 pid 文件经常打不中旧 master，脚本改用
   `taskkill /F /IM nginx.exe`。
5. **80 端口曾被占用**：原 `conf\creation-center.conf`（另一个项目）已被本配置替换，
   文件仍保留，改回去只需用该配置名启动 nginx。

## 已知遗留

- 部分种子数据里的封面地址缺少 `/static` 前缀（如 `/kumao/cover/...`），前端会自动回退到占位图，
  未改库。
- **cpolar 隧道现在靠用户级 agent 维持**（`cpolar start cc-public`），因为 Windows 服务
  读取 authtoken 失败（日志反复 `authToken is null`），服务不热重载配置。要彻底稳定，用**管理员**
  窗口执行 `net stop cpolar && net start cpolar`，之后由服务接管，agent 就可以不开。
- 后端异常现在会写到 `D:\kumao\logs\public\<服务名>.log`（start-public.bat 里用
  `-Dlogging.file.name` 指定），排查 500 直接看这个文件。
- 公网是**演示用途**：`admin/admin123` 等演示账号对外可见，演示结束请执行 `stop-public.bat`
  或删除 cpolar 隧道收口。
