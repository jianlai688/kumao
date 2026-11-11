# 酷猫音乐 · 桌面客户端（Electron）

## 开发运行

需先启动 `kumao-web`：

```bash
# 终端 1
cd kumao-web && npm install && npm run dev

# 终端 2
cd kumao-desktop && npm install && npm run dev
```

## 打包

```bash
cd kumao-desktop
npm run dist
```

产物输出到 `release/`。

## 桌面端能力

- 自动写入 `km_device = DESKTOP`，网关据此放行桌面专属接口（`/api/music/desktop/**`）
- 系统托盘、单实例锁、外链用系统浏览器打开
- `preload.js` 通过 `contextBridge` 暴露 `window.kumao`，可用于桌面歌词、全局快捷键扩展
