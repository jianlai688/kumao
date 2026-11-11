# 酷猫音乐 · PC Web（用户端 + 管理后台）

Vue 3 + Vite + Element Plus + Pinia + ECharts。

## 启动

```bash
npm install
npm run dev          # http://localhost:5173
```

默认代理到网关 `http://127.0.0.1:8080`，可通过环境变量覆盖：

```bash
VITE_API_TARGET=http://192.168.1.10:8080 npm run dev
```

## 目录

```
src/
├── api/            接口封装（request.js 统一拦截 token / 错误码）
├── components/     PlayerBar 播放器、EChart 图表
├── layouts/        UserLayout（用户端）、AdminLayout（后台）
├── router/         路由与登录/管理员守卫
├── stores/         user（登录态）、player（播放器）
├── styles/         全局暗色主题
└── views/
    ├── user/       首页、排行榜、歌手、歌单、MV、直播、搜索、会员、音乐人
    ├── studio/     音乐人 / 主播 / 广告制作人工作台
    └── admin/      17 个管理页面
```

## 打包

```bash
npm run build        # 产物 dist/，可交给 Nginx 托管
```

Nginx 需将 `/api` 与 `/ws` 反向代理到网关 8080。

## 演示账号

| 账号 | 密码 | 入口 |
|---|---|---|
| admin | admin123 | `/admin` 管理后台 |
| zhangsan | 123456 | 用户端 |
| anchor01 | 123456 | 主播工作台 |
| musician01 | 123456 | 音乐人工作台 |
| adman01 | 123456 | 广告工作台 |
| vipuser | 123456 | VIP 会员 |
