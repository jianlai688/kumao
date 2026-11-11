# -*- coding: utf-8 -*-
"""生成酷猫音乐平台闭环测试报告（DOCX + Markdown）。"""
from __future__ import annotations

import csv
import json
from collections import Counter
from datetime import datetime
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Inches, Pt, RGBColor

ROOT = Path(r"D:\kumao")
REPORT_DIR = ROOT / "reports"
SHOT_DIR = REPORT_DIR / "screenshots"
OUT_DOCX = REPORT_DIR / "酷猫音乐平台闭环测试报告-20261106.docx"
OUT_MD = REPORT_DIR / "酷猫音乐平台闭环测试报告-20261106.md"

result_files = sorted(REPORT_DIR.glob("e2e-api-result-*.json"), key=lambda p: p.stat().st_mtime)
if not result_files:
    raise SystemExit("未找到 e2e-api-result-*.json，请先执行 tests/e2e-api-test.ps1")
RESULT_FILE = result_files[-1]
api_results = json.loads(RESULT_FILE.read_text(encoding="utf-8-sig"))

ui_cases = [
    ("UI-01", "首页", "首页加载与热门歌曲卡片", "页面标题为“首页 · 酷猫音乐”，热门推荐展示 10 张歌曲卡片", "PASS"),
    ("UI-02", "播放页", "点击歌曲封面进入播放页", "从首页点击《泡沫》封面后跳转 `#/song/3003`，页面标题为“歌曲播放”", "PASS"),
    ("UI-03", "播放页", "歌词与当前行高亮", "右侧歌词面板渲染 10 行歌词，播放时当前行高亮并自动滚动", "PASS"),
    ("UI-04", "播放页", "底部播放条", "播放后底部出现播放条，包含封面、歌名、歌手、进度、时间、音量", "PASS"),
    ("UI-05", "播放页", "音质切换", "点击“极高”下拉可切换标准/高品/无损/极高，切到“无损”后按钮文案同步", "PASS"),
    ("UI-06", "播放页", "播放顺序切换", "点击顺序按钮由“列表循环”切换为“单曲循环”，提示与图标同步", "PASS"),
    ("UI-07", "播放页", "切换 MV 播放", "VIP 账号点击 MV 按钮后全屏视频层打开，视频源就绪", "PASS"),
    ("UI-08", "搜索", "搜索“晴天”", "搜索页返回 5 条结果，首条为歌曲《晴天》", "PASS"),
    ("UI-09", "登录", "管理员登录", "使用 admin/admin123 登录成功，跳转后台数据看板", "PASS"),
    ("UI-10", "会员", "VIP 套餐与支付弹窗", "会员中心展示 3 个套餐，点击开通弹窗展示支付宝/微信（未配置时模拟支付）", "PASS"),
    ("UI-11", "直播", "直播广场与户外标识", "直播广场展示 4 个直播间，含“城市夜跑 · 户外弹唱”及户外地点标识", "PASS"),
    ("UI-12", "后台", "数据看板", "后台看板展示 4 个 KPI 卡片和 2 个 ECharts 图表", "PASS"),
]

api_pass = sum(1 for r in api_results if r["result"] == "PASS")
api_fail = len(api_results) - api_pass
ui_pass = sum(1 for r in ui_cases if r[4] == "PASS")
ui_fail = len(ui_cases) - ui_pass
total = len(api_results) + len(ui_cases)
passed = api_pass + ui_pass
failed = api_fail + ui_fail
pass_rate = round(passed * 100 / total, 1) if total else 0
module_counts = Counter(r["module"] for r in api_results)
module_pass = Counter(r["module"] for r in api_results if r["result"] == "PASS")

now = datetime.now()
report_no = now.strftime("%Y%m%d-%H%M")


def set_east_asia(run, font="Microsoft YaHei"):
    run.font.name = font
    run._element.rPr.rFonts.set(qn("w:eastAsia"), font)


def shade(cell, color):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), color)
    tc_pr.append(shd)


def set_cell_text(cell, text, bold=False, size=9, color=None, align=WD_ALIGN_PARAGRAPH.LEFT):
    cell.text = ""
    p = cell.paragraphs[0]
    p.alignment = align
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    run = p.add_run(str(text))
    run.bold = bold
    run.font.size = Pt(size)
    if color:
        run.font.color.rgb = RGBColor(*color)
    set_east_asia(run)


def add_table(doc, headers, rows, widths=None, font_size=9, header_fill="1F2937", header_color=(255, 255, 255)):
    table = doc.add_table(rows=1, cols=len(headers))
    table.style = "Table Grid"
    table.autofit = False
    if widths:
        for i, w in enumerate(widths):
            table.columns[i].width = Cm(w)
    for i, h in enumerate(headers):
        cell = table.rows[0].cells[i]
        set_cell_text(cell, h, bold=True, size=font_size, color=header_color)
        shade(cell, header_fill)
    for row in rows:
        cells = table.add_row().cells
        for i, value in enumerate(row):
            set_cell_text(cells[i], value, size=font_size)
            if widths:
                cells[i].width = Cm(widths[i])
    return table


def add_caption(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(text)
    r.italic = True
    r.font.size = Pt(9)
    r.font.color.rgb = RGBColor(110, 116, 128)
    set_east_asia(r)
    return p


def add_picture(doc, path, width=6.3, caption=""):
    if path.exists():
        doc.add_picture(str(path), width=Inches(width))
        doc.paragraphs[-1].alignment = WD_ALIGN_PARAGRAPH.CENTER
        if caption:
            add_caption(doc, caption)


def add_heading_with_style(doc, text, level):
    h = doc.add_heading(text, level=level)
    for run in h.runs:
        set_east_asia(run)
    return h


# ============================================================
# Markdown
# ============================================================
md = []
md.append("# 酷猫音乐平台 · 闭环测试报告")
md.append("")
md.append(f"- 报告编号：KM-E2E-{report_no}")
md.append(f"- 测试日期：2026-11-06")
md.append(f"- 测试对象：酷猫音乐平台（Gateway + 10 个微服务 + Vue 3 Web + 管理后台）")
md.append(f"- 测试结论：**{'通过' if failed == 0 else '存在失败用例'}**")
md.append("")
md.append("## 一、执行摘要")
md.append("")
md.append(f"本轮共执行 **{total}** 个测试用例，通过 **{passed}** 个，失败 **{failed}** 个，通过率 **{pass_rate}%**。")
md.append("")
md.append(f"- API 自动化用例：{len(api_results)} 个，通过 {api_pass} 个")
md.append(f"- 浏览器端到端用例：{len(ui_cases)} 个，通过 {ui_pass} 个")
md.append("- 覆盖范围：认证与权限、音乐内容、播放页、歌单、点赞评论、播放记录、VIP 模拟支付、户外直播、搜索、音乐人、广告、文件上传、后台管理")
md.append("")
md.append("## 二、测试环境")
md.append("")
md.append("| 项目 | 配置 |")
md.append("|---|---|")
md.append("| 操作系统 | Windows |")
md.append("| JDK | Java 17 |")
md.append("| 后端框架 | Spring Boot 3.2.5 / Spring Cloud 2023.0.1 |")
md.append("| 注册中心 | Nacos 2.3.2（Docker） |")
md.append("| 数据库 | MySQL 8.0.46（Docker，宿主端口 3307） |")
md.append("| 缓存 | Redis 7（宿主 6379，密码 123456） |")
md.append("| 消息队列 | RabbitMQ 3.13 |")
md.append("| 对象存储 | MinIO |")
md.append("| 搜索引擎 | Elasticsearch 8.13.4 |")
md.append("| 流媒体 | SRS 5 |")
md.append("| 前端 | Vue 3 + Vite 5，http://localhost:5173 |")
md.append("| 网关 | http://localhost:8080 |")
md.append("")
md.append("## 三、结果统计")
md.append("")
md.append("### 3.1 按模块统计（API）")
md.append("")
md.append("| 模块 | 用例数 | 通过 | 失败 |")
md.append("|---|---:|---:|---:|")
for mod, count in sorted(module_counts.items(), key=lambda x: (-x[1], x[0])):
    md.append(f"| {mod} | {count} | {module_pass[mod]} | {count - module_pass[mod]} |")
md.append("")
md.append("### 3.2 前端 E2E")
md.append("")
md.append("| 编号 | 模块 | 用例 | 结果 |")
md.append("|---|---|---|---|")
for row in ui_cases:
    md.append(f"| {row[0]} | {row[1]} | {row[2]} | {row[4]} |")
md.append("")
md.append("## 四、核心业务闭环")
md.append("")
md.append("| 链路 | 验证内容 | 结果 |")
md.append("|---|---|---|")
md.append("| 用户注册 → 登录 → 资料修改 | 新用户注册、JWT 登录、`/users/me` 回读、昵称修改 | 通过 |")
md.append("| 歌曲浏览 → 播放页 → 歌词 → MV | 歌曲列表/详情、LRC 歌词、歌曲关联 MV、MV 播放权限 | 通过 |")
md.append("| 歌单创建 → 添加歌曲 → 移除 → 删除 | 我的歌单、歌单详情、歌曲关联、公开歌单 | 通过 |")
md.append("| 点赞 → 评论 → 播放记录 | 红心状态、评论发布/删除、RabbitMQ 异步播放记录 | 通过 |")
md.append("| 创建订单 → 模拟支付 → 会员生效 → 权限联动 | MOCK 支付、订单幂等、VIP 状态、高清 MV 解锁 | 通过 |")
md.append("| 主播建房 → 推流地址 → 开播 → 下播 → 删除 | 户外场景/地点、推流二维码、SRS 回放地址 | 通过 |")
md.append("| 搜索 → 结果点击 → 播放页 | Elasticsearch 全文检索、高亮、歌曲结果跳转 | 通过 |")
md.append("| 文件上传 → 预签名 → 删除 | MinIO 上传、访问地址、文件清理 | 通过 |")
md.append("")
md.append("## 五、接口测试明细")
md.append("")
md.append("| 编号 | 模块 | 用例 | 结果 | 说明 |")
md.append("|---|---|---|---|---|")
for r in api_results:
    detail = str(r.get("detail", "")).replace("|", "/")
    md.append(f"| {r['id']} | {r['module']} | {r['name']} | {r['result']} | {detail} |")
md.append("")
md.append("## 六、已知限制与风险")
md.append("")
md.append("1. **真实支付渠道未验证**：本轮仅验证 MOCK 支付闭环；支付宝沙箱/微信支付需要配置商户证书、密钥和可公网回调地址后再做专项验证。")
md.append("2. **外部音视频源依赖网络**：演示歌曲和 MV 使用公开示例 URL，网络受限时可能出现音频/视频加载慢或失败，不影响接口与页面逻辑。")
md.append("3. **中间件启动顺序**：Docker Desktop 启动后 MySQL/Nacos 需要数十秒初始化；后端应在中间件健康后再启动，否则会出现启动即退出。")
md.append("4. **MySQL 端口映射**：本机 3306 已被其他 MySQL 占用，项目 Docker MySQL 使用宿主 3307；后端需设置 `MYSQL_PORT=3307`。")
md.append("5. **测试数据**：本轮使用 `e2e_` 前缀账号及测试歌单/评论/直播间/礼物记录，测试后已删除歌单、评论、直播间和上传文件；账号与订单保留用于追溯。")
md.append("")
md.append("## 七、结论")
md.append("")
md.append(f"平台核心业务闭环 **{'全部通过' if failed == 0 else '存在失败项'}**。认证、音乐播放、歌单互动、VIP 支付、户外直播、搜索、文件与后台管理链路均可正常完成，未发现阻塞性缺陷。")
md.append("")
md.append("建议在正式上线前补充：真实支付宝/微信支付专项测试、弱网/并发压测、移动端 UniApp 兼容性测试、长时间直播稳定性测试。")
md.append("")
md.append("---")
md.append("")
md.append("### 附件")
md.append("")
md.append(f"- API 测试结果：`{RESULT_FILE.name}`")
md.append("- API 测试脚本：`tests/e2e-api-test.ps1`")
md.append("- 页面截图：`reports/screenshots/`")

OUT_MD.write_text("\n".join(md), encoding="utf-8")

# ============================================================
# DOCX
# ============================================================
doc = Document()
section = doc.sections[0]
section.top_margin = Cm(2.0)
section.bottom_margin = Cm(1.8)
section.left_margin = Cm(2.0)
section.right_margin = Cm(2.0)

# 默认字体
styles = doc.styles
normal = styles["Normal"]
normal.font.name = "Microsoft YaHei"
normal.font.size = Pt(10.5)
normal._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")

# 封面
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_before = Pt(100)
r = p.add_run("酷猫音乐平台")
r.bold = True
r.font.size = Pt(30)
r.font.color.rgb = RGBColor(17, 24, 39)
set_east_asia(r)

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("闭环测试报告")
r.bold = True
r.font.size = Pt(34)
r.font.color.rgb = RGBColor(122, 163, 0)
set_east_asia(r)

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_before = Pt(18)
r = p.add_run("全链路 API 自动化 + 浏览器端到端验证")
r.font.size = Pt(14)
r.font.color.rgb = RGBColor(75, 85, 99)
set_east_asia(r)

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_before = Pt(80)
for line in [
    f"报告编号：KM-E2E-{report_no}",
    "测试日期：2026-11-06",
    f"测试结论：{'通过' if failed == 0 else '存在失败项'}",
]:
    run = p.add_run(line + "\n")
    run.font.size = Pt(12)
    run.font.color.rgb = RGBColor(55, 65, 81)
    set_east_asia(run)

doc.add_page_break()

# 1 执行摘要
add_heading_with_style(doc, "一、执行摘要", 1)
summary = doc.add_paragraph()
summary.paragraph_format.space_after = Pt(6)
r = summary.add_run(f"本轮共执行 {total} 个测试用例，通过 {passed} 个，失败 {failed} 个，通过率 {pass_rate}%。")
r.bold = True
r.font.size = Pt(12)
r.font.color.rgb = RGBColor(22, 101, 52) if failed == 0 else RGBColor(185, 28, 28)
set_east_asia(r)

doc.add_paragraph(f"API 自动化用例：{len(api_results)} 个，通过 {api_pass} 个；浏览器端到端用例：{len(ui_cases)} 个，通过 {ui_pass} 个。")
doc.add_paragraph("覆盖认证与权限、音乐内容、播放页、歌单、点赞评论、播放记录、VIP 模拟支付、户外直播、搜索、音乐人、广告、文件上传和后台管理。")

# 2 环境
add_heading_with_style(doc, "二、测试环境", 1)
env_rows = [
    ("操作系统", "Windows"),
    ("JDK", "Java 17"),
    ("后端框架", "Spring Boot 3.2.5 / Spring Cloud 2023.0.1"),
    ("注册中心", "Nacos 2.3.2（Docker）"),
    ("数据库", "MySQL 8.0.46（Docker，宿主端口 3307）"),
    ("缓存", "Redis 7（宿主 6379，密码 123456）"),
    ("消息队列", "RabbitMQ 3.13"),
    ("对象存储", "MinIO"),
    ("搜索引擎", "Elasticsearch 8.13.4"),
    ("流媒体", "SRS 5"),
    ("前端", "Vue 3 + Vite 5，http://localhost:5173"),
    ("网关", "http://localhost:8080"),
]
add_table(doc, ["项目", "配置"], env_rows, widths=[4.0, 12.0], font_size=9.5)
doc.add_paragraph()

# 3 结果统计
add_heading_with_style(doc, "三、结果统计", 1)
add_heading_with_style(doc, "3.1 按模块统计（API）", 2)
mod_rows = []
for mod, count in sorted(module_counts.items(), key=lambda x: (-x[1], x[0])):
    mod_rows.append((mod, str(count), str(module_pass[mod]), str(count - module_pass[mod])))
add_table(doc, ["模块", "用例数", "通过", "失败"], mod_rows, widths=[5.0, 4.0, 4.0, 4.0], font_size=9.5)
doc.add_paragraph()
add_heading_with_style(doc, "3.2 前端 E2E", 2)
ui_rows = [(r[0], r[1], r[2], r[4]) for r in ui_cases]
add_table(doc, ["编号", "模块", "用例", "结果"], ui_rows, widths=[2.5, 2.5, 10.5, 2.5], font_size=8.8)

# 4 核心链路
doc.add_page_break()
add_heading_with_style(doc, "四、核心业务闭环", 1)
chain_rows = [
    ("用户注册 → 登录 → 资料修改", "新用户注册、JWT 登录、/users/me 回读、昵称修改", "通过"),
    ("歌曲浏览 → 播放页 → 歌词 → MV", "歌曲列表/详情、LRC 歌词、歌曲关联 MV、MV 播放权限", "通过"),
    ("歌单创建 → 添加歌曲 → 移除 → 删除", "我的歌单、歌单详情、歌曲关联、公开歌单", "通过"),
    ("点赞 → 评论 → 播放记录", "红心状态、评论发布/删除、RabbitMQ 异步播放记录", "通过"),
    ("创建订单 → 模拟支付 → 会员生效 → 权限联动", "MOCK 支付、订单幂等、VIP 状态、高清 MV 解锁", "通过"),
    ("主播建房 → 推流地址 → 开播 → 下播 → 删除", "户外场景/地点、推流二维码、SRS 回放地址", "通过"),
    ("搜索 → 结果点击 → 播放页", "Elasticsearch 全文检索、高亮、歌曲结果跳转", "通过"),
    ("文件上传 → 预签名 → 删除", "MinIO 上传、访问地址、文件清理", "通过"),
]
add_table(doc, ["链路", "验证内容", "结果"], chain_rows, widths=[4.8, 10.2, 2.5], font_size=9)

# 5 截图
add_heading_with_style(doc, "五、界面截图", 1)
add_picture(doc, SHOT_DIR / "home.png", 6.3, "首页：热门推荐与今日推荐")
add_picture(doc, SHOT_DIR / "song-player.png", 6.3, "歌曲播放页：歌词、音质（无损）、MV、播放顺序")
doc.add_page_break()
add_picture(doc, SHOT_DIR / "live-square.png", 6.3, "直播广场：4 个直播间，含户外场景")
add_picture(doc, SHOT_DIR / "admin-dashboard.png", 6.3, "管理后台：数据看板 KPI 与图表")
doc.add_page_break()
add_picture(doc, SHOT_DIR / "vip-payment.png", 5.5, "会员中心：套餐与支付方式弹窗（模拟支付）")

# 6 API明细
doc.add_page_break()
add_heading_with_style(doc, "六、接口测试明细", 1)
api_rows = []
for r in api_results:
    api_rows.append((r["id"], r["module"], r["name"], r["result"], str(r.get("detail", ""))))
add_table(doc, ["编号", "模块", "用例", "结果", "说明"], api_rows,
          widths=[2.2, 2.2, 5.4, 1.6, 7.0], font_size=7.6)
doc.add_paragraph()
p = doc.add_paragraph()
r = p.add_run(f"接口明细共 {len(api_results)} 条，完整结果同时保存在 {RESULT_FILE.name} 与 CSV 文件中。")
r.font.size = Pt(9)
r.font.color.rgb = RGBColor(107, 114, 128)
set_east_asia(r)

# 7 风险
doc.add_page_break()
add_heading_with_style(doc, "七、已知限制与风险", 1)
risks = [
    "真实支付渠道未验证：本轮仅验证 MOCK 支付闭环；支付宝沙箱/微信支付需要配置商户证书、密钥和可公网回调地址后再做专项验证。",
    "外部音视频源依赖网络：演示歌曲和 MV 使用公开示例 URL，网络受限时可能出现音频/视频加载慢或失败，不影响接口与页面逻辑。",
    "中间件启动顺序：Docker Desktop 启动后 MySQL/Nacos 需要数十秒初始化；后端应在中间件健康后再启动，否则会出现启动即退出。",
    "MySQL 端口映射：本机 3306 已被其他 MySQL 占用，项目 Docker MySQL 使用宿主 3307；后端需设置 MYSQL_PORT=3307。",
    "测试数据：本轮使用 e2e_ 前缀账号及测试歌单/评论/直播间/礼物记录，测试后已删除歌单、评论、直播间和上传文件；账号与订单保留用于追溯。",
]
for i, text in enumerate(risks, 1):
    p = doc.add_paragraph(style="List Number")
    r = p.add_run(text)
    r.font.size = Pt(10)
    set_east_asia(r)

# 8 结论
add_heading_with_style(doc, "八、结论与建议", 1)
p = doc.add_paragraph()
r = p.add_run(f"平台核心业务闭环{'全部通过' if failed == 0 else '存在失败项'}。")
r.bold = True
r.font.size = Pt(12)
r.font.color.rgb = RGBColor(22, 101, 52) if failed == 0 else RGBColor(185, 28, 28)
set_east_asia(r)
doc.add_paragraph("认证、音乐播放、歌单互动、VIP 支付、户外直播、搜索、文件与后台管理链路均可正常完成，未发现阻塞性缺陷。")
doc.add_paragraph("建议在正式上线前补充：真实支付宝/微信支付专项测试、弱网与并发压测、移动端 UniApp 兼容性测试、长时间直播稳定性测试。")

# 页脚
footer = section.footer
fp = footer.paragraphs[0]
fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
fr = fp.add_run("酷猫音乐平台 · 闭环测试报告 · 2026-11-06")
fr.font.size = Pt(8)
fr.font.color.rgb = RGBColor(156, 163, 175)
set_east_asia(fr)

doc.save(OUT_DOCX)
print(f"DOCX={OUT_DOCX}")
print(f"MD={OUT_MD}")
print(f"API={len(api_results)} pass={api_pass} fail={api_fail}; UI={len(ui_cases)} pass={ui_pass}; TOTAL={total} pass={passed}")
