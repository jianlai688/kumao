# AI 审核工作台说明

## 1. 目标与边界

统一审核工作台覆盖：

- 主播资质：`ANCHOR_APPLY`
- 音乐人资质：`MUSICIAN`
- 广告制作人：`ADVERTISER`
- 广告素材：`AD_MATERIAL`
- 广告投放：`AD_PLAN`
- 歌曲：`SONG`
- MV：`MV`
- 评论：`COMMENT`
- 直播间：`LIVE_ROOM`
- 直播弹幕：`DANMAKU`
- 歌手认证：`SINGER`

AI 对资质、广告、歌曲/MV、歌手等业务项目返回 `PASS / REVIEW / REJECT` 建议、风险分和可解释依据，管理员在确认弹窗中最终确认后，前端调用各业务原有 admin API，再把结论写入统一 `review_log`。

评论与直播弹幕采用另一条同步链路：用户发送时立即调用审核模型，`PASS` 且低风险的内容自动发布；`REVIEW`、`REJECT` 或审核不可用的内容保存为待复核状态，不公开展示，由管理员在 AI 审核工作台或对应管理页处理。

## 2. 服务与数据流

```text
admin-web
  ├─ 读取各业务现有 admin API（主播、音乐人、广告、歌曲/MV、评论、直播、歌手）
  ├─ POST /api/admin/ai/review  -> review-service -> 审核模型 / 本地规则
  ├─ 管理员确认
  ├─ 调用原业务 admin 审核/封禁/删除 API
  └─ POST /api/admin/ai/log     -> review_log 审核日志

用户发送评论 / 弹幕
  ├─ comment-service / live-service 同步调用 POST /internal/ai/moderate
  ├─ PASS + 低风险 -> 评论公开 / 弹幕广播
  └─ REVIEW / REJECT / 超时 -> 保存为 status=2，交管理员复核

review-service 定时自动审核
  ├─ 定时通过网关读取资质、广告、歌曲/MV、歌手等待审队列
  ├─ 调用管理员配置的审核模型
  ├─ PASS + 低风险 + 自动通过分类 -> 调用原业务 admin API 自动处理
  └─ 其他结果 -> ai_review_task，等待管理员复核
```

`review-service` 不复制各业务状态机，只负责 AI 建议和审核日志。因此现有 admin API 的行为、权限和数据归属保持不变。

## 3. 启动

```bash
# 数据库迁移
mysql -h127.0.0.1 -P3307 -uroot -pkumao123456 kumao_music < sql/12-ai-review.sql
mysql -h127.0.0.1 -P3307 -uroot -pkumao123456 kumao_music < sql/13-ai-model-config.sql
mysql -h127.0.0.1 -P3307 -uroot -pkumao123456 kumao_music < sql/14-ai-auto-review.sql
mysql -h127.0.0.1 -P3307 -uroot -pkumao123456 kumao_music < sql/15-moderation.sql
mysql -h127.0.0.1 -P3307 -uroot -pkumao123456 kumao_music < sql/16-content-moderation-queue.sql

# 构建并启动
mvn -pl review-service -am -DskipTests package
MYSQL_PORT=3307 java -jar review-service/target/review-service.jar
```

服务端口：`8111`。网关路由：`/api/admin/ai/**`，网关按 `ADMIN` 角色校验。

## 4. 模型配置（管理员）

管理后台「系统 → AI模型配置」（`/admin/model-settings`）维护三类模型：

| 类型 | 用途 | 默认建议 |
| --- | --- | --- |
| `REVIEW` 审核模型 | AI 审核工作台的 PASS / REVIEW / REJECT 建议与风险分 | `deepseek-chat` |
| `TEXT` 文本模型 | 标题、简介、歌词与运营文案等文本生成 | `deepseek-chat` |
| `VISION` 识别图片模型 | 封面、广告素材与证件图片识别 | `qwen-vl-max` |

每类可配置服务商、OpenAI 兼容接口地址、模型 ID、API Key、温度、超时与重试次数。填写接口地址和 API Key 后，页面会自动请求 `{baseUrl}/models` 获取模型列表并填充“模型名称”下拉框，也可直接手动输入模型 ID。API Key 使用 AES-GCM 加密后写入 `ai_model_config`，接口只返回 `sk-****cdef` 形式掩码；保存时留空表示沿用原密钥，勾选“清除已保存的 API Key”才会删除。

生效顺序：**数据库启用配置 → 环境变量 `DEEPSEEK_*`（仅审核模型）→ 本地规则**。页面“测试连接”先请求 `{baseUrl}/models`，不支持时改为一次最小对话请求，不会保存草稿。

模型配置接口（网关 `/api/admin/ai/**`，要求 `ADMIN`）：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/ai/models` | 三类配置、服务商预设与审核链路状态 |
| PUT | `/ai/models/{type}` | 保存配置，`type=REVIEW/TEXT/VISION` |
| POST | `/ai/models/{type}/models` | 读取服务商模型列表，供下拉选择 |
| POST | `/ai/models/{type}/test` | 测试连接，支持传入未保存草稿 |

文本模型与识别图片模型存放在共享库 `ai_model_config`，业务服务按 `model_type` 读取，避免把模型名写死在代码里。

## 5. 环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `AI_CONFIG_SECRET` | `kumao-ai-config-dev-secret-change-me` | 模型 API Key 落库加密口令，生产必须覆盖；更换后已存密钥需重新录入 |
| `AI_AUTO_REVIEW_ENABLED` | `true` | 是否开启定时自动审核 |
| `AI_AUTO_REVIEW_INTERVAL_MS` | `60000` | 自动审核间隔（毫秒） |
| `AI_AUTO_REVIEW_PASS_RISK` | `35` | 自动通过的最高风险分 |
| `AI_AUTO_REVIEW_CATEGORIES` | `ANCHOR_APPLY,MUSICIAN,ADVERTISER,AD_MATERIAL,AD_PLAN,SONG,MV,SINGER` | 允许定时任务自动通过的业务分类；评论/弹幕在发送时同步审核，不进入定时扫描 |
| `AI_AUTO_REVIEW_MAX_ITEMS` | `60` | 单轮最多处理条数 |
| `KUMAO_GATEWAY_URL` | `http://127.0.0.1:8080` | review-service 读取待审队列使用的内部网关地址 |
| `DEEPSEEK_ENABLED` | `false` | 审核模型的环境变量兜底开关，页面配置优先生效 |
| `DEEPSEEK_API_KEY` | 空 | DeepSeek API Key，禁止提交到仓库 |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | OpenAI 兼容接口根地址 |
| `DEEPSEEK_MODEL` | `deepseek-chat` | 模型名 |
| `DEEPSEEK_TIMEOUT_SECONDS` | `20` | 连接/读取超时秒数 |
| `DEEPSEEK_MAX_RETRIES` | `2` | 失败后的最大重试次数（总请求数 = 1 + retries） |
| `DEEPSEEK_TEMPERATURE` | `0.1` | 低温度，减少建议漂移 |
| `MYSQL_HOST` / `MYSQL_PORT` | `127.0.0.1` / `3306` | 本地项目通常设置 `MYSQL_PORT=3307` |

`DEEPSEEK_*` 只作为审核模型的环境变量兜底；文本模型与识别图片模型请直接在管理后台配置。

启用示例：

```bash
DEEPSEEK_ENABLED=true \
DEEPSEEK_API_KEY=sk-xxxx \
DEEPSEEK_MODEL=deepseek-chat \
MYSQL_PORT=3307 \
java -jar review-service/target/review-service.jar
```

没有配置 Key、页面与环境变量均未启用、超时、429/5xx 或响应不是合法 JSON 时，服务自动返回 `source=LOCAL_RULE` 的本地规则结果，并在 `fallbackReason` 中说明原因。401/403 不重试，直接降级。

## 6. 接口契约

### `GET /api/admin/ai/status`

管理员权限。审核链路的当前生效配置。

```json
{
  "enabled": false,
  "configured": false,
  "mode": "LOCAL_RULE",
  "provider": "none",
  "providerLabel": "未配置",
  "model": "deepseek-chat",
  "source": "NONE",
  "timeoutSeconds": 30,
  "maxRetries": 2,
  "environmentConfigured": false,
  "fallbackReason": "审核模型未启用，请前往「模型配置」启用"
}
```

### `POST /api/admin/ai/review`

管理员权限。请求：

```json
{
  "category": "COMMENT",
  "itemId": "8001",
  "title": "张三",
  "content": "评论正文",
  "metadata": { "songId": 3001, "status": 0 }
}
```

响应 `data`：

```json
{
  "decision": "REVIEW",
  "riskScore": 42,
  "reasons": "本地规则建议复核：疑似站外导流或联系方式。",
  "signals": ["疑似站外导流或联系方式"],
  "source": "LOCAL_RULE",
  "provider": null,
  "model": "local-rule-v1",
  "fallbackReason": "审核模型未启用，请前往「模型配置」启用",
  "latencyMs": 2
}
```

`source` 为 `AI_MODEL`、`AI_MODEL_TEXT` 或 `LOCAL_RULE`；使用外部模型时 `provider` / `model` 分别返回服务商与模型 ID。对资质、广告等后台审核，`decision` 是管理员参考建议；评论/弹幕在发送时由业务服务按 `PASS + 风险阈值` 自动放行，其余转人工复核。

### `POST /api/admin/ai/log`

管理员权限。审核动作成功后调用。请求：

```json
{
  "category": "COMMENT",
  "itemId": "8001",
  "itemTitle": "张三",
  "decision": "APPROVED",
  "adminReason": "人工确认评论正常",
  "aiDecision": "PASS",
  "aiRiskScore": 8,
  "aiSource": "LOCAL_RULE",
  "aiReasons": "未命中本地风险词"
}
```

响应 `data` 为日志 ID。服务端从登录身份写入 `admin_id`、`admin_name`，不信任前端传入管理员身份。

### `GET /api/admin/ai/logs`

管理员权限，参数：`pageNum`、`pageSize`、`category`、`decision`、`keyword`。

返回统一分页结构，记录包含审核类型、对象标题/ID、结论、人工原因、AI 来源/风险/理由、管理员和时间。

### `GET /api/admin/ai/stats`

管理员权限。返回累计/今日审核数、通过/驳回数、按分类和 AI 来源统计，以及最近日志。

## 7. 本地规则与可解释性

本地规则不是“假 AI”，而是明确标注的降级策略：

- 风险词权重：诈骗、赌博、色情、毒品、枪支、刷单、加微信、返利等；
- 评论/弹幕：站外链接、手机号、重复刷屏、超长文本；
- 广告素材：绝对化用语、医疗/金融效果承诺、外链；
- 歌曲/MV：缺少音频/视频或封面；
- 投放计划：未完成付费；
- 直播间：已封禁、简介为空；
- 资质类：缺少简介/资料，或主播、音乐人、广告商缺少身份证号及正反面照片时，提示人工核验。

规则结果会写入 `signals` 和 `reasons`，管理员能看出为什么被标为高风险。本地规则本身只返回判定；评论/弹幕业务服务会在 `PASS` 且风险低于阈值时自动发布，其余情况转管理员复核。

## 8. 管理员确认与日志

前端确认弹窗明确提示：

> AI 建议仅供参考，最终审核结论由你确认；确认后将调用原业务 admin 接口，并写入统一审核日志。

执行顺序：

1. 调用原业务 admin API；
2. 业务接口成功后调用 `POST /api/admin/ai/log`；
3. 若业务成功但日志失败，提示“审核已执行，但审核日志写入失败”，不伪装成完整成功。

## 9. AI 自动审核

review-service 启动后按 `AI_AUTO_REVIEW_INTERVAL_MS`（默认 60 秒）执行一轮自动审核：

1. 使用内部服务账号（ADMIN）通过网关读取资质、广告、歌曲/MV、歌手等待审队列；
2. 调用管理员配置的审核模型；模型未配置时整轮跳过，不修改业务数据；
3. **自动通过条件**：`source=AI_MODEL`、`decision=PASS`、`riskScore < AI_AUTO_REVIEW_PASS_RISK`（默认 35），且分类在 `AI_AUTO_REVIEW_CATEGORIES`；主播、音乐人、广告商还要求身份证号及正反面照片齐全。命中后调用原业务 admin API，并写入 `review_log`；
4. 其余结果写入 `ai_review_task`，状态为 `PENDING_ADMIN`，在管理端 AI 审核工作台展示 AI 建议，由管理员最终确认；确认后任务标记为 `RESOLVED`；
5. 自动处理失败写入 `APPROVAL_FAILED` 和失败原因，仍进入人工复核，不伪装成成功。

评论和直播弹幕不在此定时任务的扫描范围内：它们在用户发送时同步审核，避免内容先公开、后补审；审核未通过的内容直接进入管理端待复核队列。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/ai/auto/status` | 开关、模型状态、统计和最近一次执行摘要 |
| GET | `/ai/auto/tasks` | 自动审核任务分页，可按 status/category 过滤 |
| POST | `/ai/auto/run` | 手动触发一轮，异步执行 |
| DELETE | `/ai/auto/tasks/{category}/{itemId}` | 管理员确认后标记任务为已处理 |

## 10. 测试

```bash
mvn -pl review-service -am test
```

测试覆盖：

- 本地规则高风险/正常/资料缺失判断；
- 证件号、手机号脱敏；
- 审核模型未配置时降级本地规则；
- 模拟模型服务 500 重试后解析；
- 401 不重试并降级；
- 模型密钥 AES-GCM 加解密与掩码展示；
- 自动审核任务状态与管理员确认清理。
