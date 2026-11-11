# 酷猫音乐平台 · 微服务版

依据《酷猫音乐平台_微服务模块需求分析.txt》实现：1 个网关 + 11 个业务微服务 + Vue3 前端（含 uniapp / Electron 壳）。

## 模块清单

| 模块 | 端口 | 说明 |
|---|---|---|
| kumao-gateway | 8080 | 统一入口、路由、JWT 鉴权、Sentinel 限流熔断、跨域、WebSocket 转发 |
| user-service | 8101 | 注册登录、账号管理、角色权限、主播申请、登录记录 |
| music-service | 8102 | 歌曲/歌手/榜单/MV/歌词 |
| playlist-service | 8103 | 歌单维护、歌单歌曲、收藏 |
| comment-service | 8104 | 评论、我喜欢、播放记录（RabbitMQ 异步） |
| file-service | 8105 | MinIO 统一上传、校验、删除 |
| live-service | 8106 | 直播间、弹幕 WebSocket、礼物、在线人数、回放 |
| search-service | 8107 | Elasticsearch 全文检索与索引同步 |
| vip-service | 8108 | 套餐、支付宝沙箱/微信支付、开通、付费权限校验 |
| musician-service | 8109 | 入驻、作品、数据、收益 |
| ad-service | 8110 | 入驻、素材、投放、数据、收益 |
| review-service | 8111 | AI 审核建议、自动审核调度、模型配置与审核日志 |

## 技术栈

Spring Boot 3.2.5 / Spring Cloud 2023.0.1 / Spring Cloud Alibaba 2023.0.1.0、
Nacos、MyBatis-Plus 3.5.6、MySQL 8、Redis 7、MinIO、RabbitMQ、Elasticsearch 8、
Sentinel、SRS 5、WebSocket；前端 Vue 3 + Vite + Element Plus + Pinia。

## 目录结构

```
kumao/
├── pom.xml                    父工程
├── kumao-common/              统一返回、异常、JWT、用户上下文、MP 配置
├── kumao-api/                 Feign 客户端与跨服务 DTO
├── kumao-gateway/
├── user-service/ … ad-service/ 10 个业务服务 + review-service AI 审核服务
├── kumao-web/                 管理后台 + 用户端 PC Web（Vue3）
├── kumao-uniapp/              APP 端（uniapp）
├── kumao-desktop/             Electron 桌面客户端
├── sql/                       建库建表 + 初始化数据
├── docker/                    docker-compose 中间件编排
└── docs/                      接口文档、架构说明
```

## 快速启动

见 `docs/部署运行说明.md`。

## 文档

- `docs/架构说明.md` — 总体架构、依赖关系、核心链路设计
- `docs/API接口清单.md` — 11 个模块全部接口
- `docs/部署运行说明.md` — 中间件、启动顺序、演示账号、链路验证
- `docs/支付接入说明.md` — 支付宝沙箱、微信支付配置与联调
- `docs/户外直播说明.md` — 户外直播创建、扫码推流与网络配置
