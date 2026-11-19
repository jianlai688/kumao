# 手机 USB 调试 · 酷猫音乐

> 实测环境：Xiaomi `2410DPN6CC` / Android 16 / HyperOS V816，adb 37.0.0，应用 `com.kumao.music`
> 结论：**已经打通**——数据线连电脑后，手机不连 Wi-Fi、不用改防火墙，也能正常调后端。

---

## 一、为什么用 USB（当前手机的实际网络状况）

| 检查 | 结果 |
| --- | --- |
| 手机 Wi-Fi | 未连接（网卡里只有 `lo` 和移动数据 `rmnet_data2`，IP `10.37.63.7`） |
| 手机 ping 电脑 `192.168.20.103` | 100% 丢包 |
| 手机 curl `http://127.0.0.1:8080/api/music/rank/hot?limit=1`（经 adb reverse） | **200** |
| App 首页 | 数据正常渲染（榜单/热门推荐有内容） |
| WebView DevTools | 可连，页面 `http://127.0.0.1:8099/index.html#/` |

也就是说：上一篇《手机连接服务方案》里那套 Wi-Fi + 防火墙的做法，在这台手机上暂时用不了（它现在根本没连 Wi-Fi）。USB 反向端口映射绕开了局域网和防火墙，是最省事的调试通道。

## 二、原理

```
手机 App（WebView 载入本地资源 http://127.0.0.1:8099）
        │  fetch http://127.0.0.1:8080/api/**      ← 手机自己的 127.0.0.1
        ▼
   adb reverse tcp:8080 → tcp:8080（USB 线）
        ▼
电脑 127.0.0.1:8080  kumao-gateway → 8101~8111 各服务
```

- `adb reverse` 是「手机端口 → 电脑端口」的反向隧道，走 USB，不经过 Wi-Fi/路由器/防火墙。
- 同一个机制可以映射多个端口：`8080` 接口 + 静态资源、`8088` 直播 HLS、`5174` H5 热更新、`1935` 直播推流。
- 因此 **USB 版 APK 的接口基址必须是 `http://127.0.0.1:8080/api`**（手机上现装的 1.5.1 USB 版就是这种）；本地那个 `192.168.20.103` 的包是 Wi-Fi 版，装到这台手机上连不上。

## 三、三步用起来

### 1）建映射（每个新会话/重新插线都要做一次）

```powershell
cd D:\kumao
pwsh -File scripts\usb-phone-debug.ps1 -Launch
```

脚本会：找 adb → 检查设备 → 重置并建立 8080/8088/5174/1935 映射 → 用手机 curl 自检 → 拉起 App。

手工等价命令：

```powershell
adb reverse tcp:8080 tcp:8080
adb reverse tcp:8088 tcp:8088
adb reverse tcp:5174 tcp:5174
adb reverse tcp:1935 tcp:1935
adb reverse --list          # 查看当前映射
```

### 2）在手机上验证

手机浏览器打开 `http://127.0.0.1:8080/api/music/rank/hot?limit=1`，能看到 JSON 就通了（不需要 Wi-Fi，也不需要开数据）。

### 3）跑 App

- 已装：直接打开「酷猫音乐」，登录 `zhangsan/123456`；
- 重装：`pwsh -File scripts\usb-phone-debug.ps1 -Install`（或 `adb install -r <apk>`）。

## 四、调试三件套

### 1. WebView DevTools（断点、Network、Console）

`MainActivity` 里已经 `WebView.setWebContentsDebuggingEnabled(true)`，所以：

- **图形化**：电脑 Chrome 打开 `chrome://inspect` → 找到设备 `1f0d0052` 下的 `com.kumao.music` → `inspect`。
- **命令行/脚本化**：先拿到 WebView 的 devtools 套接字再转发：

```powershell
adb shell "cat /proc/net/unix | grep webview_devtools_remote"   # 形如 @webview_devtools_remote_9255
adb forward tcp:9222 localabstract:webview_devtools_remote_9255
curl.exe http://127.0.0.1:9222/json/list                        # 拿页面 ws 地址
```

- 仓库里已有现成的 CDP 脚本（走 9222 即可，实测跑通登录流程）：

```powershell
pwsh -File scripts\phone-flow.ps1 -Port 9222 -Account zhangsan -Password 123456
```

### 2. 日志

`WebChromeClient` 已把 JS console 转到 logcat（tag `KuMaoWeb`）：

```powershell
adb logcat -s KuMaoWeb:* AndroidRuntime:E
```

### 3. 截图 / 录屏

```powershell
adb shell screencap -p /sdcard/s.png; adb pull /sdcard/s.png .\shot.png
adb shell screenrecord /sdcard/a.mp4     # 最长 180 秒，Ctrl+C 结束
```

## 五、日常热更新（改 UI 不用重打包）

```powershell
cd D:\kumao\kumao-uniapp
npm run dev:h5          # 0.0.0.0:5174，/api、/ws、/static 已代理到 127.0.0.1:8080
```

手机浏览器打开 **`http://127.0.0.1:5174`**（5174 已随脚本 reverse 好）。改代码 → 手机刷新即生效。
> H5 里接口用相对路径 `/api`，WS 用 `location.host`（即 127.0.0.1:5174）由 vite 的 `ws:true` 代理到网关，所以整条链路都走 USB。

## 六、两种 APK 别混（最常见的坑）

| 版本 | `VITE_API_BASE` | 适用 | 现状 |
| --- | --- | --- | --- |
| **USB 版** | `http://127.0.0.1:8080/api` | 数据线调试，无 Wi-Fi 也能用 | 手机上现装 1.5.1 USB 版属于这种，实测正常 |
| **Wi-Fi 版** | `http://192.168.20.103:8080/api` | 手机和电脑同一局域网 | `KuMao-Android-v1.5.1.apk` 是这种，适合手机与电脑同一 Wi-Fi |

USB 版重打包（推荐用一键脚本）：

```powershell
pwsh -File scripts\build-mobile-apk.ps1 -Mode usb -Install
```

手工等价步骤：

```powershell
$env:VITE_API_BASE = 'http://127.0.0.1:8080/api'
cd D:\kumao\kumao-uniapp
npm run build:h5
Remove-Item 'D:\kumao\kumao-mobile-apk\app\src\main\assets\www\*' -Recurse -Force
Copy-Item 'D:\kumao\kumao-uniapp\dist\build\h5\*' 'D:\kumao\kumao-mobile-apk\app\src\main\assets\www' -Recurse -Force
# 然后 Android Studio Build APK，或 gradle assembleDebug
```

## 七、映射会掉，掉了就重跑

`adb reverse` 在以下情况失效：**拔数据线、手机重启、adb server 重启（`adb kill-server`）、切换 USB 模式**。
判断方法：`adb reverse --list` 为空，或 App 里接口报「网络异常」。
处理：重跑 `scripts\usb-phone-debug.ps1`（10 秒）。

## 八、常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `adb devices` 空 / `unauthorized` | 数据线只能充电 / 没点授权弹窗 | 换数据线；手机弹窗点「允许」；`adb kill-server; adb start-server` |
| `adb install` 报 `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 新旧包签名不同 | `adb uninstall com.kumao.music` 后重装（会清登录态） |
| MIUI/HyperOS 拦截安装 | 未开「USB 安装」 | 开发者选项 → USB 安装 / USB 调试（安全设置） |
| App 白屏 | `assets/www` 没同步或 `base` 不是 `./` | 重新按第六节拷贝产物 |
| 接口全 401 | token 过期/换包清了数据 | 重新登录 |
| 封面灰、歌曲不播 | 演示数据是外链（picsum / soundhelix），手机只有 USB 没有外网 | 正常现象；用 `/static` 资源或给手机开数据 |
| 直播黑屏 | 8088 没映射 / SRS 没推流 | 脚本已含 8088；`curl -I http://127.0.0.1:8088/live/xxx.m3u8` 有流时应 200 |
| `chrome://inspect` 看不到设备 | WebView 调试被关 / 驱动 | 确认 `setWebContentsDebuggingEnabled(true)`；重插 USB 后刷新页面 |

## 九、和 Wi-Fi 方案的关系

- 两套是**互斥的构建**：USB 版指向 `127.0.0.1`，Wi-Fi 版指向 `192.168.20.103`，同一个 App 只能选一个基址。
- 想「一个包两种网络都能用」，需要在 App 里加一个可配置的服务器地址（登录页隐藏入口 / 长按 logo 弹出），把基址存到 localStorage。这个要改代码，需要的话我可以做。

