<template>
  <div class="workbench">
    <!-- 头部 -->
    <div class="wb-hero">
      <div class="hero-glow"></div>
      <div class="wb-left">
        <span class="wb-emoji" aria-hidden="true"><el-icon><Microphone /></el-icon></span>
        <div class="wb-copy">
          <div class="wb-eyebrow">LIVE STUDIO</div>
          <h1 class="wb-title">主播工作台</h1>
          <button class="button button-quiet" style="margin-top:8px" @click="profileOpen = true">编辑主播资料</button>
          <div class="wb-tags">
            <span class="status-pill ok">已认证主播</span>
            <span class="km-faint">{{ rooms.length }} 个直播间 · {{ formatNum(summary.totalIncome || 0) }} 礼物收益</span>
          </div>
        </div>
      </div>
      <div class="hero-actions">
        <router-link to="/studio/earnings" class="btn-ghost">礼物收益</router-link>
        <button class="btn-accent" @click="roomDialog = true">
          <el-icon><Plus /></el-icon>创建直播间
        </button>
      </div>
    </div>

    <el-tabs v-model="tab" class="wb-tabs">
      <el-tab-pane label="我的直播间" name="rooms">
        <div class="room-grid">
          <div v-for="r in rooms" :key="r.id" class="room-card">
            <div class="cover-wrap">
              <LiveStreamPreview
                v-if="r.status === 'LIVING'"
                :src="r.pullUrl"
                :poster="r.coverUrl || fallbackCover"
                :active="!previewDialog || previewRoom?.id !== r.id"
                @expand="openPreview(r)"
              />
              <img v-else :src="r.coverUrl || fallbackCover" class="cover" @error="onImg" />
              <div class="cover-shade"></div>
              <div :class="['status-badge', r.status.toLowerCase()]">
                <i v-if="r.status === 'LIVING'" class="live-dot"></i>
                {{ statusText(r.status) }}
              </div>
              <div class="online-chip" v-if="r.status === 'LIVING'">
                <el-icon><View /></el-icon>{{ r.onlineCount || 0 }}
              </div>
            </div>
            <div class="room-body">
              <div class="room-title-row">
                <div class="room-title">{{ r.title }}</div>
                <button
                  v-if="r.status !== 'BANNED'"
                  type="button"
                  class="console-quick"
                  @click="$router.push(`/studio/live/${r.id}`)"
                >
                  控制台<el-icon><ArrowRight /></el-icon>
                </button>
              </div>
              <div class="room-meta">
                <span v-if="r.category" class="cat-mini">{{ r.category }}</span>
                <span v-if="r.scene === 'OUTDOOR'" class="scene-mini">📍 {{ r.location || '户外直播' }}</span>
                <span>实时在线 {{ r.onlineCount || 0 }}</span>
              </div>
              <div class="room-actions">
                <button v-if="r.status === 'LIVING'" class="mini-console" @click="$router.push(`/studio/live/${r.id}`)">
                  <el-icon><Monitor /></el-icon>控制台
                </button>
                <button v-else-if="r.status !== 'BANNED'" class="mini-ghost preview" @click="openPreview(r)">
                  <el-icon><VideoCamera /></el-icon>直播画面
                </button>
                <button v-if="r.status !== 'LIVING' && r.status !== 'BANNED'" class="mini-primary" @click="start(r)">开播</button>
                <button v-if="r.status === 'LIVING'" class="mini-warn" @click="stop(r)">一键下播</button>
                <button class="mini-ghost" @click="showPushUrl(r)">推流地址</button>
                <button class="mini-text danger" @click="removeRoom(r)">删除</button>
              </div>
            </div>
          </div>
        </div>
        <div v-if="!rooms.length" class="empty-box">
          <div class="empty-ico">📺</div>
          <p>还没有直播间，创建一个开始你的首播吧</p>
          <button class="btn-accent sm" @click="roomDialog = true">创建直播间</button>
        </div>
      </el-tab-pane>

      <el-tab-pane label="礼物收益" name="gifts">
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-ico">🎁</div>
            <div class="stat-body">
              <div class="stat-value accent">{{ formatNum(summary.totalIncome || 0) }}</div>
              <div class="stat-label">礼物收益（虚拟币）</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-ico">📦</div>
            <div class="stat-body">
              <div class="stat-value">{{ summary.count || 0 }}</div>
              <div class="stat-label">礼物记录数</div>
            </div>
          </div>
        </div>
        <el-table :data="giftRecords" style="width:100%">
          <el-table-column prop="giftIcon" label="礼物" width="80" />
          <el-table-column prop="giftName" label="名称" width="120" />
          <el-table-column prop="count" label="数量" width="80" />
          <el-table-column prop="totalPrice" label="价值" width="100" />
          <el-table-column prop="senderName" label="赠送人" width="140" />
          <el-table-column label="时间" width="180">
            <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
          </el-table-column>
        </el-table>
        <div v-if="!giftRecords.length" class="empty-box">
          <div class="empty-ico">🎁</div>
          <p>暂无礼物记录</p>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="roomDialog" title="创建直播间" width="520px">
      <el-form :model="roomForm" label-width="90px">
        <el-form-item label="标题"><el-input v-model="roomForm.title" /></el-form-item>
        <el-form-item label="分类">
          <el-radio-group v-model="roomForm.category">
            <el-radio-button value="弹唱">弹唱</el-radio-button>
            <el-radio-button value="电台">电台</el-radio-button>
            <el-radio-button value="翻唱">翻唱</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="直播场景">
          <el-radio-group v-model="roomForm.scene">
            <el-radio-button value="INDOOR">室内直播</el-radio-button>
            <el-radio-button value="OUTDOOR">户外直播</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="roomForm.scene === 'OUTDOOR'" label="直播地点">
          <el-input v-model="roomForm.location" placeholder="例如：西湖断桥 / 城市露营营地" maxlength="200" />
        </el-form-item>
        <el-form-item v-if="roomForm.scene === 'OUTDOOR'" label=" ">
          <el-alert type="success" :closable="false" show-icon
                    title="户外直播：创建后点「推流地址」获取手机扫码推流二维码，用手机推流 App 即可开播" />
        </el-form-item>
        <el-form-item label="封面">
          <el-input v-model="roomForm.coverUrl">
            <template #append>
              <el-upload :show-file-list="false" :http-request="uploadCover" accept="image/*">
                <el-button>上传</el-button>
              </el-upload>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="简介"><el-input v-model="roomForm.intro" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomDialog = false">取消</el-button>
        <el-button type="primary" @click="createRoom">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pushDialog" title="推流信息" width="620px">
      <el-alert :type="pushInfo.scene === 'OUTDOOR' ? 'success' : 'info'" :closable="false"
                :title="pushInfo.tip || '使用 OBS 推流到以下地址即可开播（SRS）'" style="margin-bottom:12px" />
      <div v-if="pushInfo.pushQr" class="push-qr">
        <img :src="pushInfo.pushQr" alt="手机扫码推流" />
        <div class="push-qr-copy">
          <b>手机扫码开播</b>
          <span>支持抖音/快手/腾讯会议等支持自定义 RTMP 的推流 App</span>
          <span class="push-loc" v-if="pushInfo.location">📍 {{ pushInfo.location }}</span>
        </div>
      </div>
      <el-form label-position="top">
        <el-form-item label="推流地址（RTMP）">
          <el-input :model-value="pushInfo.pushUrl" readonly>
            <template #append><el-button @click="copy(pushInfo.pushUrl)">复制</el-button></template>
          </el-input>
        </el-form-item>
        <el-form-item label="拉流播放地址（HLS）">
          <el-input :model-value="pushInfo.pullUrl" readonly>
            <template #append><el-button @click="copy(pushInfo.pullUrl)">复制</el-button></template>
          </el-input>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog
      v-model="previewDialog"
      width="min(900px, 92vw)"
      class="live-preview-dialog"
      destroy-on-close
      align-center
      @closed="previewRoom = null"
    >
      <template #header>
        <div class="preview-dialog-head">
          <div>
            <span class="preview-dialog-eyebrow">LIVE PREVIEW</span>
            <h3>{{ previewRoom?.title || '直播画面' }}</h3>
          </div>
          <span :class="['preview-dialog-meta', { idle: !previewIsLive }]">
            <i aria-hidden="true"></i>
            {{ previewIsLive ? `直播中 · ${previewRoom?.onlineCount || 0} 人在线` : '未开播 · 预览直播信号' }}
          </span>
        </div>
      </template>

      <div v-if="previewRoom" class="preview-dialog-stage">
        <LiveStreamPreview
          :src="previewRoom.pullUrl"
          :poster="previewRoom.coverUrl || fallbackCover"
          :active="previewDialog"
          controls
          @ready="previewReady = true"
          @offline="previewReady = false"
        />
      </div>
      <div class="preview-dialog-foot">
        <span v-if="!previewIsLive">先开始推流，确认画面正常后再开播。</span>
        <span v-else>主播端预览默认静音，避免声音回授；可在播放器中开启声音。</span>
        <div class="preview-dialog-actions">
          <el-button
            v-if="!previewIsLive"
            type="primary"
            :disabled="!previewReady"
            @click="startFromPreview"
          >
            {{ previewReady ? '画面已就绪，开始直播' : '等待直播信号…' }}
          </el-button>
          <router-link :to="`/live/${previewRoom?.id}`" target="_blank" class="preview-link">
            打开观众视角<el-icon><TopRight /></el-icon>
          </router-link>
          <el-button @click="previewDialog = false">关闭</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
    <RoleProfileDialog v-model="profileOpen" type="anchor" :record="anchorProfile" @saved="loadAnchorProfile" />
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Monitor, Plus, TopRight, VideoCamera, View } from '@element-plus/icons-vue'
import { liveApi, fileApi, userApi } from '@/api'
import RoleProfileDialog from '@/components/RoleProfileDialog.vue'
import LiveStreamPreview from '@/components/studio/LiveStreamPreview.vue'

const route = useRoute()

const fallbackCover = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="480" height="300"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#faf5ff"/><stop offset="1" stop-color="#f0e6f9"/></linearGradient></defs><rect width="480" height="300" fill="url(#g)"/><text x="240" y="176" font-size="64" text-anchor="middle" fill="#b78bd6">🎤</text></svg>'
)

function onImg(e) {
  e.target.src = fallbackCover
}
function formatNum(n) {
  n = Number(n) || 0
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  return n >= 10000 ? (n / 10000).toFixed(1) + '万' : String(n)
}

const tab = ref('rooms')
const profileOpen = ref(false)
const anchorProfile = ref({})
const rooms = ref([])
const giftRecords = ref([])
const summary = ref({})
const roomDialog = ref(false)
const pushDialog = ref(false)
const previewDialog = ref(false)
const previewRoom = ref(null)
const previewReady = ref(false)
const pushInfo = ref({ pushUrl: '', pullUrl: '', pushQr: '', scene: 'INDOOR', location: '', tip: '' })
const roomForm = reactive({ title: '', category: '弹唱', scene: 'INDOOR', location: '', coverUrl: '', intro: '' })

const statusText = (s) => ({ NOT_STARTED: '未开播', LIVING: '直播中', ENDED: '已下播', BANNED: '已封禁' }[s] || s)
const previewIsLive = computed(() => previewRoom.value?.status === 'LIVING')

async function loadAnchorProfile() {
  try {
    const res = await userApi.myAnchorApply()
    const list = res.data || []
    anchorProfile.value = list.find((x) => x.status === 'APPROVED') || list[0] || {}
  } catch {
    anchorProfile.value = {}
  }
}
async function loadRooms() {
  const res = await liveApi.mine({ pageNum: 1, pageSize: 50 })
  rooms.value = res.data?.records || []
}

async function loadGifts() {
  const [s, r] = await Promise.all([liveApi.giftSummary(), liveApi.received({ pageNum: 1, pageSize: 50 })])
  summary.value = s.data || {}
  giftRecords.value = r.data?.records || []
}

async function uploadCover(options) {
  const res = await fileApi.upload(options.file, 'live-cover')
  roomForm.coverUrl = res.data.url
}

async function createRoom() {
  if (!roomForm.title) return ElMessage.warning('请输入直播间标题')
  if (roomForm.scene === 'OUTDOOR' && !roomForm.location) {
    ElMessage.warning('建议填写户外直播地点，方便观众找到你')
  }
  await liveApi.create(roomForm)
  ElMessage.success('创建成功')
  roomDialog.value = false
  await loadRooms()
}

async function start(r) {
  await liveApi.start(r.id)
  ElMessage.success('已开播')
  await loadRooms()
  syncPreviewRoom(r.id)
}

async function startFromPreview() {
  if (!previewRoom.value) return
  await start(previewRoom.value)
}

function syncPreviewRoom(roomId) {
  if (!previewRoom.value || String(previewRoom.value.id) !== String(roomId)) return
  previewRoom.value = rooms.value.find((item) => String(item.id) === String(roomId)) || previewRoom.value
}

async function stop(r) {
  try {
    await ElMessageBox.confirm('确定一键下播吗？将自动生成回放', '提示', { type: 'warning' })
    await liveApi.stop(r.id)
    ElMessage.success('已下播')
    await loadRooms()
    syncPreviewRoom(r.id)
  } catch (e) { /* cancel */ }
}

async function showPushUrl(r) {
  const res = await liveApi.pushUrl(r.id)
  pushInfo.value = res.data
  pushDialog.value = true
}

function openPreview(r) {
  previewReady.value = false
  previewRoom.value = r
  previewDialog.value = true
}

function prefillPrepareRoom() {
  const source = rooms.value.find((item) => String(item.id) === String(route.query.prepareRoom))
  if (!source) return
  roomForm.title = `${source.title} · 返场`
  roomForm.category = source.category || '弹唱'
  roomForm.scene = source.scene || 'INDOOR'
  roomForm.location = source.location || ''
  roomForm.coverUrl = source.coverUrl || ''
  roomForm.intro = source.intro || ''
  roomDialog.value = true
}

async function removeRoom(r) {
  try {
    await ElMessageBox.confirm('确定删除该直播间吗？', '提示', { type: 'warning' })
    await liveApi.remove(r.id)
    ElMessage.success('已删除')
    await loadRooms()
  } catch (e) { /* cancel */ }
}

function copy(text) {
  navigator.clipboard?.writeText(text)
  ElMessage.success('已复制')
}

watch(tab, (v) => {
  if (v === 'gifts') loadGifts()
})

onMounted(async () => {
  loadAnchorProfile()
  await loadRooms()
  await loadGifts()
  prefillPrepareRoom()
})
</script>

<style scoped>
.workbench { padding-bottom: 40px; }

.wb-hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 28px;
  margin-bottom: 20px;
  border-radius: 24px;
  overflow: hidden;
  background:
    radial-gradient(600px 300px at 5% 0%, rgba(192, 132, 252, .14), transparent 62%),
    radial-gradient(500px 300px at 95% 100%, rgba(255, 154, 171, .07), transparent 60%),
    linear-gradient(140deg, #14161a, #0a0b0d 72%);
  border: 1px solid var(--km-border);
  flex-wrap: wrap;
}
.hero-glow {
  position: absolute;
  width: 320px;
  height: 320px;
  left: -80px;
  top: -160px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(192, 132, 252, .16), transparent 65%);
  filter: blur(28px);
  pointer-events: none;
}
.wb-left { display: flex; align-items: center; gap: 18px; position: relative; }
.wb-emoji { font-size: 54px; filter: drop-shadow(0 10px 24px rgba(192, 132, 252, .22)); }
.wb-eyebrow { font-size: 10px; letter-spacing: 2px; color: var(--km-accent); font-weight: 800; margin-bottom: 3px; }
.wb-title { margin: 0 0 7px; font-size: 24px; font-weight: 900; letter-spacing: -.8px; }
.button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  padding: 0 14px;
  border: 1px solid #e4d9ee;
  border-radius: 8px;
  background: #fff;
  color: #5f5769;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background .16s, border-color .16s, color .16s;
}
.button:hover:not(:disabled) { background: #faf6fe; border-color: #d6c3e5; color: #2b2334; }
.wb-tags { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; font-size: 12px; }
.status-pill {
  height: 20px;
  padding: 0 9px;
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 700;
}
.status-pill.ok { background: rgba(192, 132, 252, .14); color: var(--km-accent); }

.hero-actions { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; }
.btn-ghost {
  display: inline-flex;
  align-items: center;
  height: 38px;
  padding: 0 15px;
  border: 1px solid var(--km-border);
  border-radius: 999px;
  background: transparent;
  color: var(--km-text-dim);
  font-size: 12px;
  font-weight: 650;
  transition: all .18s;
}
.btn-ghost:hover { color: var(--km-text); border-color: var(--km-border-strong); background: rgba(255,255,255,.03); }

.btn-accent {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 18px;
  border: none;
  border-radius: 999px;
  background: var(--km-accent);
  color: #07080a;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all .2s;
  box-shadow: 0 8px 22px rgba(192, 132, 252, .22);
  position: relative;
}
.btn-accent:hover { background: #d8b4fe; transform: translateY(-1px); }
.btn-accent.sm { height: 34px; padding: 0 15px; font-size: 12px; }

.wb-tabs :deep(.el-tabs__header) { margin-bottom: 20px; }
.wb-tabs :deep(.el-tabs__item) { font-size: 14px; padding: 0 20px; }
.wb-tabs :deep(.el-table) {
  --el-table-bg-color: var(--km-card);
  --el-table-tr-bg-color: var(--km-card);
  border: 1px solid var(--km-border);
  border-radius: 16px;
  overflow: hidden;
  padding: 4px 8px;
}
.wb-tabs :deep(.el-table th.el-table__cell) {
  background: rgba(255, 255, 255, .03) !important;
  font-size: 12px;
  color: var(--km-text-dim) !important;
}

/* 直播间卡片 */
.room-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(290px, 1fr)); gap: 18px; }
.room-card {
  background: var(--km-card);
  border: 1px solid var(--km-border);
  border-radius: 18px;
  overflow: hidden;
  transition: all .22s;
}
.room-card:hover { border-color: var(--km-border-strong); transform: translateY(-3px); box-shadow: 0 20px 44px rgba(0, 0, 0, .5); }
.cover-wrap { position: relative; overflow: hidden; aspect-ratio: 16/10; }
.cover { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .3s; }
.room-card:hover .cover { transform: scale(1.04); }
.cover-shade { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0, 0, 0, .34), transparent 45%, rgba(0, 0, 0, .5)); pointer-events: none; }
.status-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10.5px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, .9);
  border: 1px solid rgba(255, 255, 255, .65);
  backdrop-filter: blur(10px);
  color: #4f465a;
}
.status-badge.living { background: var(--km-accent); border-color: var(--km-accent); color: #fff; }
.status-badge.ended { color: #5f5769; }
.status-badge.banned { background: #fff1f3; color: #c4242a; border-color: #f3d3d8; }
.status-badge.living .live-dot { background: #fff; }
.live-dot { width: 5px; height: 5px; border-radius: 50%; background: #8b3fce; animation: blink 1.2s infinite; }
@keyframes blink { 50% { opacity: .3; } }
.online-chip {
  position: absolute;
  right: 10px;
  top: 10px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 24px;
  padding: 0 9px;
  border-radius: 999px;
  background: rgba(10, 11, 13, .72);
  border: 1px solid rgba(255, 255, 255, .14);
  backdrop-filter: blur(10px);
  font-size: 11px;
  font-weight: 600;
}

.room-body { padding: 14px 16px 16px; }
.room-title-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.room-title { flex: 1; min-width: 0; font-weight: 700; font-size: 14.5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.console-quick {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex: none;
  padding: 2px 0;
  border: 0;
  background: transparent;
  color: var(--km-accent);
  font: inherit;
  font-size: 10.5px;
  font-weight: 650;
  cursor: pointer;
  opacity: .8;
  transition: opacity .15s, color .15s;
}
.console-quick:hover { opacity: 1; color: #f0abfc; }
.console-quick .el-icon { font-size: 11px; }
.room-meta { display: flex; align-items: center; gap: 9px; font-size: 11.5px; color: var(--km-text-faint); margin-top: 6px; }
.cat-mini {
  height: 18px;
  padding: 0 7px;
  display: inline-flex;
  align-items: center;
  border-radius: 5px;
  background: rgba(255, 255, 255, .06);
  color: var(--km-text-dim);
  font-size: 10.5px;
}
.scene-mini {
  height: 18px;
  padding: 0 7px;
  display: inline-flex;
  align-items: center;
  border-radius: 5px;
  background: rgba(192, 132, 252, .12);
  color: var(--km-accent);
  font-size: 10.5px;
  font-weight: 600;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.push-qr {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 14px;
  margin-bottom: 16px;
  border-radius: 16px;
  background: rgba(192, 132, 252, .05);
  border: 1px solid rgba(192, 132, 252, .18);
}
.push-qr img { width: 116px; height: 116px; border-radius: 10px; background: #fff; padding: 6px; }
.push-qr-copy { display: flex; flex-direction: column; gap: 6px; }
.push-qr-copy b { font-size: 14px; color: var(--km-accent); }
.push-qr-copy span { font-size: 12px; color: var(--km-text-dim); line-height: 1.5; }
.push-qr-copy .push-loc { color: var(--km-text); }
.room-actions { display: flex; gap: 7px; flex-wrap: wrap; margin-top: 14px; }
.mini-primary, .mini-warn, .mini-ghost {
  height: 30px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all .18s;
}
.mini-primary { border: none; background: var(--km-accent); color: #07080a; }
.mini-primary:hover { background: #d8b4fe; }
.mini-warn { border: none; background: rgba(247, 215, 116, .16); color: #f7d774; }
.mini-warn:hover { background: rgba(247, 215, 116, .28); }
.mini-ghost { border: 1px solid var(--km-border); background: transparent; color: var(--km-text-dim); }
.mini-ghost:hover { color: var(--km-text); border-color: var(--km-border-strong); }
.mini-ghost.preview {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 0 11px;
  color: var(--km-accent);
  border-color: rgba(192, 132, 252, .32);
}
.mini-ghost.preview:hover { color: #e9d5ff; border-color: var(--km-accent); background: rgba(192, 132, 252, .08); }
.mini-ghost.preview .el-icon { font-size: 12px; }
.mini-console {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 30px;
  padding: 0 11px;
  border: 1px solid rgba(216, 180, 254, .38);
  border-radius: 999px;
  background: rgba(216, 180, 254, .12);
  color: var(--km-accent);
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all .18s;
}
.mini-console:hover { background: var(--km-accent); color: #120c19; border-color: var(--km-accent); }
.mini-console .el-icon { font-size: 12px; }
.mini-text { border: none; background: transparent; color: var(--km-text-faint); height: 30px; padding: 0 8px; font-size: 11.5px; cursor: pointer; }
.mini-text.danger:hover { color: #ff8095; }

/* 直播画面预览 */
.preview-dialog-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; flex-wrap: wrap; padding-right: 30px; }
.preview-dialog-eyebrow { display: block; color: var(--km-accent); font-size: 9.5px; font-weight: 800; letter-spacing: 2px; margin-bottom: 4px; }
.preview-dialog-head h3 { margin: 0; color: var(--km-text); font-size: 17px; font-weight: 800; letter-spacing: -.3px; }
.preview-dialog-meta { display: inline-flex; align-items: center; gap: 6px; color: var(--km-text-dim); font-size: 11.5px; white-space: nowrap; }
.preview-dialog-meta i { width: 6px; height: 6px; border-radius: 50%; background: #fb7185; box-shadow: 0 0 0 4px rgba(251, 113, 133, .12); }
.preview-dialog-meta.idle i { background: #f7d774; box-shadow: 0 0 0 4px rgba(247, 215, 116, .12); }
.preview-dialog-stage {
  position: relative;
  width: 100%;
  aspect-ratio: 16/9;
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid var(--km-border);
  background: #000;
}
.preview-dialog-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;
  margin-top: 14px;
  color: var(--km-text-faint);
  font-size: 11.5px;
}
.preview-dialog-actions { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.preview-link { display: inline-flex; align-items: center; gap: 4px; color: var(--km-accent); font-size: 12px; font-weight: 700; text-decoration: none; }
.preview-link:hover { color: #e9d5ff; }
.preview-link .el-icon { font-size: 12px; }

/* 统计 */
.stats-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px; margin-bottom: 18px; }
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: linear-gradient(160deg, #16171c, #111216);
  border: 1px solid var(--km-border);
  border-radius: 16px;
  padding: 18px;
}
.stat-ico {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: rgba(192, 132, 252, .08);
  border: 1px solid rgba(192, 132, 252, .16);
  font-size: 16px;
  flex-shrink: 0;
}
.stat-body { min-width: 0; }
.stat-value { font-size: 22px; font-weight: 900; letter-spacing: -.8px; line-height: 1.2; }
.stat-value.accent { color: var(--km-accent); }
.stat-label { font-size: 11.5px; color: var(--km-text-faint); margin-top: 3px; }

.empty-box { padding: 60px 20px; text-align: center; }
.empty-ico { font-size: 34px; opacity: .5; margin-bottom: 10px; }
.empty-box p { margin: 0 0 16px; color: var(--km-text-dim); font-size: 13px; }
</style>
