from pathlib import Path
from datetime import datetime

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor


ROOT = Path(r"D:\kumao")
OUT = ROOT / "output" / "酷猫音乐_手机直播全链路测试报告.docx"
OUT.parent.mkdir(parents=True, exist_ok=True)

APK = ROOT / "kumao-web" / "public" / "downloads" / "KuMao-Android-v1.0.0.apk"
APK_URL = "http://192.168.20.103:5173/downloads/KuMao-Android-v1.0.0.apk"
APK_SHA256 = "0D27E22CC3539B509ED3B799D9624DC446A1E3F45ABF3EC121A434E5112DFFA1"
APK_SIZE = "310,754 bytes（约 303 KB）"

SHOTS = [
    (ROOT / "mumu-home3.png", "图 1  MuMu 模拟器中登录后进入 App 首页"),
    (ROOT / "mumu-loginpage2.png", "图 2  手机端登录页（演示账号可一键填充）"),
    (ROOT / "mumu-final-watch.png", "图 3  手机 App 观看直播：画面播放、真实在线人数 1、真实入场弹幕"),
    (ROOT / "mumu-anchor3.png", "图 4  主播手机开播页：选择直播间并获取 RTMP 推流地址"),
]


def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), fill)
    tc_pr.append(shd)


def set_cell_text(cell, text, bold=False, color=None, size=9.5):
    cell.text = ""
    p = cell.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    run = p.add_run(str(text))
    run.bold = bold
    run.font.size = Pt(size)
    if color:
        run.font.color.rgb = RGBColor(*color)
    cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER


def add_table(doc, headers, rows, widths=None):
    table = doc.add_table(rows=1, cols=len(headers))
    table.style = "Table Grid"
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    hdr = table.rows[0].cells
    for i, h in enumerate(headers):
        set_cell_text(hdr[i], h, bold=True, color=(255, 255, 255), size=9.5)
        set_cell_shading(hdr[i], "2F3542")
    trPr = table.rows[0]._tr.get_or_add_trPr()
    tblHeader = OxmlElement("w:tblHeader")
    tblHeader.set(qn("w:val"), "true")
    trPr.append(tblHeader)
    for row in rows:
        new_row = table.add_row()
        cells = new_row.cells
        for i, value in enumerate(row):
            set_cell_text(cells[i], value)
        trPr = new_row._tr.get_or_add_trPr()
        cant_split = OxmlElement("w:cantSplit")
        trPr.append(cant_split)
    if widths:
        for i, width in enumerate(widths):
            for row in table.rows:
                row.cells[i].width = Cm(width)
    return table


def add_bullet(doc, text, bold_prefix=None):
    p = doc.add_paragraph(style="List Bullet")
    if bold_prefix and text.startswith(bold_prefix):
        r = p.add_run(bold_prefix)
        r.bold = True
        p.add_run(text[len(bold_prefix):])
    else:
        p.add_run(text)
    return p


def add_code(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Cm(0.45)
    p.paragraph_format.space_before = Pt(3)
    p.paragraph_format.space_after = Pt(5)
    r = p.add_run(text)
    r.font.name = "Consolas"
    r._element.rPr.rFonts.set(qn("w:eastAsia"), "Consolas")
    r.font.size = Pt(8.5)
    r.font.color.rgb = RGBColor(0x9A, 0x4A, 0x12)
    pPr = p._p.get_or_add_pPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), "F4F5F7")
    pPr.append(shd)
    return p


def style_doc(doc):
    normal = doc.styles["Normal"]
    normal.font.name = "Microsoft YaHei"
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
    normal.font.size = Pt(10.5)
    normal.paragraph_format.line_spacing = 1.25
    normal.paragraph_format.space_after = Pt(5)

    for name, size, color in [
        ("Heading 1", 15, (31, 41, 55)),
        ("Heading 2", 12.5, (55, 65, 81)),
        ("Title", 24, (17, 24, 39)),
    ]:
        st = doc.styles[name]
        st.font.name = "Microsoft YaHei"
        st._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
        st.font.size = Pt(size)
        st.font.color.rgb = RGBColor(*color)
        st.font.bold = True

    section = doc.sections[0]
    section.top_margin = Cm(2.0)
    section.bottom_margin = Cm(1.8)
    section.left_margin = Cm(2.2)
    section.right_margin = Cm(2.2)


def add_footer_pagenum(doc):
    section = doc.sections[0]
    footer = section.footer
    p = footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run("酷猫音乐 · 手机直播全链路测试报告    |    第 ")
    run.font.size = Pt(8.5)
    run.font.color.rgb = RGBColor(0x88, 0x88, 0x88)
    fld_begin = OxmlElement("w:fldChar")
    fld_begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = "PAGE"
    fld_end = OxmlElement("w:fldChar")
    fld_end.set(qn("w:fldCharType"), "end")
    run2 = p.add_run()
    run2._r.append(fld_begin)
    run2._r.append(instr)
    run2._r.append(fld_end)
    run2.font.size = Pt(8.5)
    run2.font.color.rgb = RGBColor(0x88, 0x88, 0x88)
    p.add_run(" 页").font.size = Pt(8.5)


doc = Document()
style_doc(doc)
add_footer_pagenum(doc)

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("酷猫音乐")
r.bold = True
r.font.size = Pt(12)
r.font.color.rgb = RGBColor(0xC2, 0x7A, 0x2C)

title = doc.add_paragraph(style="Title")
title.alignment = WD_ALIGN_PARAGRAPH.CENTER
title.add_run("手机直播全链路测试报告")
sub = doc.add_paragraph()
sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
sub.add_run("Android App × MuMu 模拟器 × RTMP / HLS 直播").font.size = Pt(11)
meta = doc.add_paragraph()
meta.alignment = WD_ALIGN_PARAGRAPH.CENTER
meta.add_run(f"测试日期：{datetime.now().strftime('%Y-%m-%d')}    版本：v1.0.0").font.size = Pt(9)
meta.runs[0].font.color.rgb = RGBColor(0x77, 0x77, 0x77)

doc.add_paragraph()
doc.add_heading("1. 测试结论", level=1)
conclusion = doc.add_paragraph()
conclusion.add_run("结论：手机端观看直播链路已打通，可安装 APK 登录观看；手机主播端已提供开播入口、直播间选择、推流地址生成与一键复制。").bold = True
doc.add_paragraph(
    "在 MuMu 模拟器中完成了 APK 安装、登录、直播广场进入、HLS 画面播放、登录用户在线人数实时 +1、"
    "入场弹幕、手机开播页推流地址获取等验证。Android 端产生的 H.264 画面也已通过 RTMP 成功推送到 SRS。"
)

doc.add_heading("2. 交付物", level=1)
add_table(
    doc,
    ["交付项", "内容"],
    [
        ["Android APK", "D:\\kumao\\kumao-web\\public\\downloads\\KuMao-Android-v1.0.0.apk"],
        ["手机下载地址", APK_URL],
        ["APK 大小", APK_SIZE],
        ["SHA-256", APK_SHA256],
        ["后端网关", "http://192.168.20.103:8080"],
        ["SRS 推流", "rtmp://192.168.20.103:1935/live"],
        ["SRS 播放", "http://192.168.20.103:8088/live/{stream}.m3u8"],
        ["模拟器", "MuMu 安卓设备（VM 0，Android 12，adb 127.0.0.1:16384）"],
    ],
    widths=[4.2, 12.6],
)

doc.add_heading("3. 链路说明", level=1)
doc.add_paragraph("手机端直播分为主播推流和观众观看两段：")
add_bullet(doc, "主播手机开播：主播登录 App → 我的 → 主播中心 → 选择直播间 → 生成 RTMP 推流地址。")
add_bullet(doc, "手机摄像头推流：将 RTMP 地址粘贴到 Larix Broadcaster 等推流 App，手机摄像头采集 H.264/AAC 后推到 SRS 1935。")
add_bullet(doc, "观众手机观看：观众登录 App → 直播 → 进入直播间，App 通过 HLS 播放 SRS 8088 的直播流。")
add_bullet(doc, "实时数据：登录用户建立直播间 WebSocket 后，后端按 userId 去重统计在线人数，并广播在线人数、峰值、点赞、礼物收益。")

doc.add_heading("4. 测试环境", level=1)
add_table(
    doc,
    ["项目", "配置"],
    [
        ["宿主机", "Windows 11，192.168.20.103"],
        ["模拟器", "MuMu Player 12，Android 12，1080×1920 竖屏"],
        ["App 形态", "uni-app H5 构建 + Android WebView 容器（APK 内置静态资源）"],
        ["后端", "Spring Cloud Gateway + live-service，端口 8080 / 8106"],
        ["直播服务", "SRS 5，RTMP 1935，HTTP-FLV/HLS 8088"],
        ["测试账号", "观众：zhangsan / 123456；主播：anchor01 / 123456"],
        ["测试直播间", "11001（OBS 桌面推流，H.264 1280×720）；11002（Android 端 RTMP 链路验证）"],
    ],
    widths=[4.0, 12.8],
)

doc.add_page_break()
doc.add_heading("5. 测试用例与结果", level=1)
rows = [
    ["TC-01", "APK 安装", "adb install -r app-debug.apk，MuMu 启动应用", "安装成功、应用可启动", "通过"],
    ["TC-02", "手机登录", "输入 zhangsan / 123456，点击登录", "登录成功并回到首页", "通过"],
    ["TC-03", "登录态持久化", "强制停止 App 后重新打开", "仍显示张三已登录", "通过"],
    ["TC-04", "直播广场", "首页点击「深夜弹唱｜一起听老歌」", "进入房间 11001，状态为直播中", "通过"],
    ["TC-05", "HLS 播放", "打开直播间并等待 3 秒", "视频 readyState=4、currentTime 持续增长", "通过"],
    ["TC-06", "真实在线人数", "登录观众进入房间，观察 API/WSS", "在线人数 0→1，累计观看 0→1", "通过"],
    ["TC-07", "游客不计入", "无 token 建立 WebSocket", "在线人数不变，仅登录用户计入", "通过"],
    ["TC-08", "真实点赞", "登录用户点击点赞", "点赞数增加并广播到控制台", "通过"],
    ["TC-09", "主播手机开播页", "anchor01 登录 → 主播中心", "展示 3 个直播间，可选房并显示推流地址", "通过"],
    ["TC-10", "Android→RTMP→SRS", "模拟器屏幕采集 H.264 推流到 room_11002", "SRS publish.active=true，1080×1920", "通过（以屏幕采集代替摄像头）"],
    ["TC-11", "App 观看 Android 推流", "手机 App 进入 room_11002", "房间在线 1，WebSocket 入场弹幕正常", "通过"],
    ["TC-12", "OBS 桌面推流", "OBS 推流 room_11001", "SRS 约 2.5 Mbps，1280×720，0 丢帧", "通过"],
]
add_table(doc, ["编号", "场景", "步骤", "预期", "结果"], rows, widths=[1.4, 3.0, 6.5, 4.5, 2.0])

doc.add_heading("6. 手机主播开播操作", level=1)
steps = [
    "在手机安装酷猫音乐 APK，使用主播账号 anchor01 / 123456 登录。",
    "进入「我的」→「主播中心」→ 选择直播间。若未开播，点击「开始直播并获取推流地址」。",
    "复制页面上显示的 RTMP 推流地址（包含 token，请勿公开）。",
    "在手机安装 Larix Broadcaster 或任意支持 RTMP 的推流 App。",
    "新建 RTMP 连接，将完整推流地址粘贴到 URL；选择摄像头与麦克风，开始推流。",
    "观众用另一个账号登录 App，进入「直播」选择该房间即可观看。",
]
for i, s in enumerate(steps, 1):
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Cm(0.3)
    p.add_run(f"{i}. ").bold = True
    p.add_run(s)

doc.add_paragraph("推流地址示例（完整地址以 App 页面为准）：")
add_code(doc, "rtmp://192.168.20.103:1935/live/room_11001?token=<主播专属token>")
doc.add_paragraph("如果推流 App 要求拆分服务器和密钥：")
add_code(doc, "服务器：rtmp://192.168.20.103:1935/live\n密钥：room_11001?token=<主播专属token>")

doc.add_heading("7. 手机观众安装与观看", level=1)
for text in [
    "手机与电脑连接同一局域网（当前为 192.168.20.0/24）。",
    f"手机浏览器打开 {APK_URL} 下载 APK。",
    "允许浏览器安装未知来源应用，安装「酷猫音乐」。",
    "打开 App，使用观众账号 zhangsan / 123456 登录。",
    "首页点击「正在直播」卡片，或进入「直播」页选择直播间。",
    "进入直播间后即可看到直播画面、在线人数、点赞和弹幕。",
]:
    add_bullet(doc, text)

doc.add_heading("8. 关键实现说明", level=1)
add_bullet(doc, "APK 使用内置本地静态服务器（127.0.0.1:8099）+ WebView 加载 H5，固定 origin 保证登录态持久化。")
add_bullet(doc, "App 内 WebSocket 使用 live-service 的 /ws/live；H5 与 APP 均通过 live.js 将 127.0.0.1 媒体地址替换为局域网主机。")
add_bullet(doc, "在线人数只统计登录用户，按 userId 去重；同一账号多开只算 1 人，游客不计入。")
add_bullet(doc, "控制台已移除随机模拟数据，在线、累计观看、峰值、点赞、礼物收益均来自后端真实数据。")
add_bullet(doc, "新增 live_started_at 字段，保证本场时长和本场统计不因礼物/在线更新而漂移。")

doc.add_heading("9. 已知限制", level=1)
add_bullet(doc, "当前手机 App 不内置摄像头编码器；手机摄像头推流需配合 Larix Broadcaster 等 RTMP 推流 App。")
add_bullet(doc, "本次实际验证使用 MuMu 模拟器屏幕采集代替手机摄像头，验证了 Android→RTMP→SRS→App 的传输链路；真机摄像头需按第 6 节操作复测。")
add_bullet(doc, "APK 与后端/SRS 使用固定局域网地址 192.168.20.103，网络变化后需要重新构建或修改配置。")
add_bullet(doc, "手机必须与电脑同一局域网；首次安装需允许未知来源应用。")
add_bullet(doc, "本次未在实体手机上执行安装测试；APK 已提供下载地址，可按第 7 节在真机复测。")

doc.add_page_break()
doc.add_heading("10. 测试截图", level=1)
for path, caption in SHOTS:
    if path.exists():
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.add_run().add_picture(str(path), width=Cm(5.3))
        c = doc.add_paragraph(caption)
        c.alignment = WD_ALIGN_PARAGRAPH.CENTER
        c.runs[0].font.size = Pt(9)
        c.runs[0].font.color.rgb = RGBColor(0x66, 0x66, 0x66)

doc.add_heading("11. 附录：常用地址", level=1)
add_table(
    doc,
    ["用途", "地址"],
    [
        ["手机 APK 下载", APK_URL],
        ["Web 直播控制台", "http://172.27.176.1:5173/#/studio/live/11001"],
        ["Web 观众页", "http://172.27.176.1:5173/#/live/11001"],
        ["后端健康检查", "http://127.0.0.1:8106/actuator/health"],
        ["SRS 流列表", "http://127.0.0.1:1985/api/v1/streams/"],
    ],
    widths=[4.2, 12.6],
)

doc.save(OUT)
print(OUT)
