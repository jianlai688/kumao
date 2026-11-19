<template>
  <div v-if="loading" class="console-page console-loading">
    <el-skeleton :rows="8" animated />
  </div>
  <div v-else-if="room" class="console-page">
    <!-- 顶部直播状态条 -->
    <header class="console-top">
      <div class="top-left">
        <router-link to="/studio/anchor" class="back-link">
          <el-icon><ArrowLeft /></el-icon>主播工作台
        </router-link>
        <span class="top-divider" aria-hidden="true"></span>
        <span :class="['live-badge', { offline: !isLive }]">
          <i aria-hidden="true"></i>{{ isLive ? 'LIVE' : 'OFFLINE' }}
        </span>
        <div class="top-title">
          <h1>直播控制台</h1>
          <span>{{ room.title }}</span>
        </div>
      </div>
      <div class="top-right">
        <div class="elapsed-box">
          <span>已播时长</span>
          <b class="tabular">{{ elapsedLabel }}</b>
        </div>
        <button v-if="!isLive && room.status !== 'BANNED'" class="start-live-btn" :disabled="ending" @click="startFromConsole">
          开始直播
        </button>
        <button v-else class="end-live-btn" :disabled="!isLive || ending" @click="openEndConfirm">
          结束直播
        </button>
      </div>
    </header>

    <!-- 数据概览 -->
    <section class="metric-strip" aria-label="直播数据概览">
      <div v-for="item in metricCards" :key="item.key" class="metric-card">
        <span class="metric-icon" :class="item.key">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <div class="metric-copy">
          <span>{{ item.label }}</span>
          <strong class="tabular">
            <AnimatedNumber :value="item.value" :formatter="item.formatter" />
          </strong>
        </div>
        <span v-if="item.hint" class="metric-hint">{{ item.hint }}</span>
      </div>
    </section>

    <div class="console-grid">
      <main class="console-main">
        <!-- 画面预览 -->
        <section class="preview-card km-card" aria-labelledby="preview-title">
          <div class="card-heading">
            <div>
              <span class="eyebrow">MONITOR / 16:9</span>
              <h2 id="preview-title">画面预览</h2>
            </div>
            <span
              :class="['preview-signal', { live: isLive && previewReady, connecting: isLive && !previewReady }]"
            >
              <i></i>{{ previewSignalLabel }}
            </span>
          </div>
          <div class="preview-stage">
            <LiveStreamPreview
              v-if="isLive && pullUrl"
              :src="pullUrl"
              :poster="room.coverUrl || fallbackCover"
              :active="isLive"
              controls
              @ready="previewReady = true"
              @offline="previewReady = false"
            />
            <template v-else>
              <img :src="room.coverUrl || fallbackCover" class="preview-bg" alt="" />
              <div class="preview-tint"></div>
              <div class="preview-center">
                <div class="waveform" aria-hidden="true">
                  <span v-for="(height, index) in waveform" :key="index" :style="{ height: `${height}%` }"></span>
                </div>
                <div class="preview-caption">
                  <span>WAITING FOR SIGNAL</span>
                  <strong>{{ room.title }}</strong>
                  <small>等待推流信号 · 开播后自动显示实时画面</small>
                </div>
              </div>
              <div class="preview-scanline" aria-hidden="true"></div>
              <span class="preview-watermark">等待推流信号</span>
            </template>
          </div>
        </section>

        <!-- 正在播放 -->
        <section class="now-card km-card" aria-labelledby="now-title">
          <div class="card-heading">
            <div>
              <span class="eyebrow">NOW PLAYING</span>
              <h2 id="now-title">正在播放</h2>
            </div>
            <span class="queue-position">{{ queue.length ? `${currentIndex + 1} / ${queue.length}` : '0 / 0' }} · 待唱队列</span>
          </div>

          <div v-if="currentTrack" class="now-body">
            <img :src="currentTrack.coverUrl || fallbackCover" class="now-cover" alt="" @error="onImg" />
            <div class="now-main">
              <div class="now-meta">
                <div class="now-title">{{ currentTrack.name }}</div>
                <div class="now-artist">{{ currentTrack.singerName || '未知歌手' }}</div>
              </div>
              <div class="now-controls">
                <button type="button" class="ctrl-btn" aria-label="上一首" @click="prevTrack">
                  <el-icon><DArrowLeft /></el-icon>
                </button>
                <button type="button" class="ctrl-btn play" :aria-label="isPlaying ? '暂停' : '播放'" @click="isPlaying = !isPlaying">
                  <el-icon><component :is="isPlaying ? 'VideoPause' : 'VideoPlay'" /></el-icon>
                </button>
                <button type="button" class="ctrl-btn" aria-label="下一首" @click="nextTrack">
                  <el-icon><DArrowRight /></el-icon>
                </button>
                <button type="button" class="mode-btn" :aria-label="`循环模式：${modeLabel}`" @click="cycleMode">
                  <el-icon><Refresh /></el-icon>{{ modeLabel }}
                </button>
              </div>
              <div class="track-progress-row">
                <span class="tabular">{{ formatSeconds(playProgress) }}</span>
                <input
                  v-model.number="playProgress"
                  class="track-range"
                  type="range"
                  min="0"
                  :max="currentTrack.duration || 240"
                  step="1"
                  aria-label="播放进度"
                  @input="onSeek"
                />
                <span class="tabular">{{ formatSeconds(currentTrack.duration || 240) }}</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-inline">暂无待唱歌曲</div>
        </section>

        <!-- 房间信息 -->
        <section class="room-info-card km-card" aria-labelledby="room-info-title">
          <div class="card-heading">
            <div>
              <span class="eyebrow">ROOM INFO</span>
              <h2 id="room-info-title">房间信息</h2>
            </div>
            <span class="room-code">ROOM {{ room.id }}</span>
          </div>
          <div class="room-info-body">
            <div class="room-info-copy">
              <h3>{{ room.title }}</h3>
              <p>{{ room.intro || '和来到直播间的朋友一起听歌、聊天，分享此刻的心情。' }}</p>
              <div class="topic-chips">
                <span v-for="chip in topicChips" :key="chip">#{{ chip }}</span>
              </div>
            </div>
            <div class="push-copy-box">
              <span>推流地址</span>
              <div class="push-copy-row">
                <code>{{ pushUrl || '加载中…' }}</code>
                <button type="button" class="copy-btn" :disabled="!pushUrl" @click="copyPushUrl">
                  <el-icon><CopyDocument /></el-icon>复制
                </button>
              </div>
              <small>点击开始直播会自动生成演示画面；也可用 OBS 或手机推流 App 推流到该地址</small>
              <div class="broadcast-hint">
                <button type="button" class="copy-btn" @click="copyBroadcastUrl">复制 OBS 画面地址</button>
                <button type="button" class="copy-btn" @click="openObsDialog">OBS 自动推流设置</button>
                <a :href="`#/studio/live/${room.id}/broadcast`" target="_blank" rel="noopener">预览画面</a>
              </div>
            </div>
          </div>
        </section>
      </main>

      <!-- 右侧互动栏 -->
      <aside class="interaction-panel" aria-label="直播互动">
        <div class="interaction-tabs" role="tablist" aria-label="互动类型">
          <button
            v-for="item in interactionTabs"
            :key="item.value"
            type="button"
            role="tab"
            :aria-selected="activeTab === item.value"
            :class="['interaction-tab', { active: activeTab === item.value }]"
            @click="activeTab = item.value"
          >
            {{ item.label }}<em v-if="item.count !== null">{{ item.count }}</em>
          </button>
        </div>

        <div v-if="activeTab === 'chat'" class="tab-panel chat-panel">
          <div ref="danmakuBox" class="danmaku-list" aria-live="polite">
            <div
              v-for="message in danmaku"
              :key="message.id"
              :class="['danmaku-item', { sensitive: message.sensitive, muted: message.muted, pinned: message.pinned, system: message.system }]"
            >
              <span class="danmaku-avatar" :style="{ background: message.color }">{{ message.nickname.slice(0, 1) }}</span>
              <div class="danmaku-copy">
                <div class="danmaku-meta">
                  <b>{{ message.nickname }}</b>
                  <span>{{ message.time }}</span>
                  <em v-if="message.pinned">置顶</em>
                </div>
                <p>{{ message.content }}</p>
                <div v-if="message.sensitive" class="fold-note">该消息已被折叠</div>
                <div v-if="message.muted" class="fold-note">已禁言该用户</div>
              </div>
              <div class="danmaku-actions">
                <button type="button" @click="muteMessage(message)">禁言</button>
                <button type="button" @click="pinMessage(message)">置顶</button>
              </div>
            </div>
          </div>
          <div class="danmaku-compose">
            <el-input
              v-model="danmakuText"
              placeholder="发一条弹幕，回车发送…"
              maxlength="120"
              clearable
              @keydown.enter.prevent="sendDanmaku"
            />
            <button type="button" class="send-btn" :disabled="!danmakuText.trim()" @click="sendDanmaku">发送</button>
          </div>
        </div>

        <div v-else-if="activeTab === 'gifts'" class="tab-panel gift-panel">
          <div class="panel-note"><span>礼物动态</span><small>按时间倒序</small></div>
          <div class="gift-list">
            <div v-for="gift in giftFeed" :key="gift.id" class="gift-row">
              <span class="gift-avatar" :style="{ background: gift.color }">{{ gift.nickname.slice(0, 1) }}</span>
              <div class="gift-copy">
                <p><b>{{ gift.nickname }}</b> 送出 <strong>×{{ gift.count }} {{ gift.giftName }}</strong></p>
                <span>{{ gift.time }}</span>
              </div>
              <div class="gift-value">
                <b>{{ formatNumber(gift.totalPrice) }}</b>
                <span>酷币</span>
              </div>
            </div>
          </div>
          <div v-if="!giftFeed.length" class="empty-inline">还没有收到礼物</div>
        </div>

        <div v-else-if="activeTab === 'rank'" class="tab-panel rank-panel">
          <div class="panel-note"><span>本场贡献榜</span><small>TOP 10</small></div>
          <div v-if="rankList.length" class="rank-list">
            <div v-for="(user, index) in rankList" :key="user.nickname" :class="['rank-row', `rank-${index + 1}`]">
              <span class="rank-badge">{{ index < 3 ? ['金', '银', '铜'][index] : String(index + 1).padStart(2, '0') }}</span>
              <span class="rank-avatar" :style="{ background: user.color }">{{ user.nickname.slice(0, 1) }}</span>
              <div class="rank-copy">
                <b>{{ user.nickname }}</b>
                <span>{{ user.giftCount }} 件礼物</span>
              </div>
              <strong>{{ formatNumber(user.totalPrice) }}<small> 酷币</small></strong>
            </div>
          </div>
          <div v-else class="empty-inline">还没有收到礼物</div>
        </div>

        <div v-else-if="activeTab === 'audience'" class="tab-panel audience-panel">
          <div class="panel-note">
            <span>实时观众画像</span>
            <small>{{ audience.sampleCount || 0 }} 位在线样本</small>
          </div>
          <div v-if="audience.sampleCount" class="audience-groups">
            <div v-for="group in audienceGroups" :key="group.label" class="audience-group">
              <h4>{{ group.label }}</h4>
              <div v-for="item in group.items" :key="item.label" class="audience-row">
                <span class="audience-name">{{ item.label }}</span>
                <span class="audience-track"><i :style="{ width: `${Math.min(100, item.value)}%` }"></i></span>
                <b>{{ item.value }}%</b>
              </div>
            </div>
          </div>
          <div v-else class="empty-inline">暂无在线登录观众</div>
        </div>

        <div v-else class="tab-panel queue-panel">
          <div class="panel-note">
            <span>待唱队列</span>
            <small>{{ queue.length }} 首</small>
            <button class="setlist-edit" type="button" @click="openSetlist">设置歌单</button>
          </div>
          <div class="setlist">
            <button
              v-for="(track, index) in queue"
              :key="track.id"
              type="button"
              :class="['setlist-row', { active: index === currentIndex }]"
              @click="selectTrack(index)"
            >
              <span class="setlist-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="setlist-copy">
                <b>{{ track.name }}</b>
                <small>{{ track.singerName }}</small>
              </span>
              <span class="setlist-status">{{ index === currentIndex ? '正在唱' : '切歌' }}</span>
            </button>
            <p v-if="!queue.length" class="empty-inline">还没有设置歌单，点击右上角「设置歌单」添加歌曲</p>
          </div>
        </div>
      </aside>
    </div>

    <!-- 设置直播歌单 -->
    <el-dialog v-model="setlistOpen" title="设置直播歌单" width="780px">
      <div class="setlist-dialog">
        <div class="sd-col">
          <el-radio-group v-model="setlistSource" size="small" @change="loadCandidates">
            <el-radio-button value="hot">热歌榜</el-radio-button>
            <el-radio-button value="mine">我的歌单</el-radio-button>
          </el-radio-group>
          <div v-if="setlistSource === 'mine' && myPlaylists.length" class="pl-list">
            <button v-for="pl in myPlaylists" :key="pl.id" type="button" class="pl-item" @click="loadPlaylistSongs(pl)">
              <span>{{ pl.name }}</span>
              <small>{{ pl.songCount }} 首</small>
            </button>
          </div>
          <div class="cand-list" v-loading="setlistLoading">
            <button v-for="t in candidates" :key="t.id" type="button" class="cand-row" @click="addTrack(t)">
              <span class="nm">{{ t.name }}</span>
              <small>{{ t.singerName }}</small>
              <b>＋</b>
            </button>
            <p v-if="!candidates.length" class="sd-empty">选择左侧歌单查看歌曲</p>
          </div>
        </div>
        <div class="sd-col">
          <div class="sd-head">已选 {{ picked.length }} 首</div>
          <div class="cand-list">
            <div v-for="(t, i) in picked" :key="t.id" class="pick-row">
              <span class="idx">{{ i + 1 }}</span>
              <span class="nm">{{ t.name }}</span>
              <small>{{ t.singerName }}</small>
              <button type="button" class="mini" :disabled="i === 0" @click="movePicked(i, -1)">↑</button>
              <button type="button" class="mini" :disabled="i === picked.length - 1" @click="movePicked(i, 1)">↓</button>
              <button type="button" class="mini danger" @click="removePicked(i)">×</button>
            </div>
            <p v-if="!picked.length" class="sd-empty">还没有选歌</p>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="setlistOpen = false">取消</el-button>
        <el-button type="primary" @click="saveSetlist">保存歌单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="obsDialog" title="OBS 自动推流设置" width="460px">
      <el-form label-position="top">
        <el-form-item label="OBS WebSocket 地址">
          <el-input v-model="obsForm.url" placeholder="ws://127.0.0.1:4455" />
        </el-form-item>
        <el-form-item label="OBS WebSocket 密码">
          <el-input v-model="obsForm.password" type="password" show-password placeholder="OBS → 工具 → WebSocket 服务器设置 中查看" />
        </el-form-item>
        <el-form-item label="直播画面来源">
          <el-radio-group v-model="obsForm.source">
            <el-radio value="screen">主播电脑屏幕</el-radio>
            <el-radio value="page">直播画面页</el-radio>
          </el-radio-group>
          <p class="obs-tip">选择「主播电脑屏幕」时，OBS 会自动切换到「酷猫屏幕直播」场景并采集显示器；请把 OBS 最小化，避免把 OBS 窗口也拍进去。</p>
        </el-form-item>
        <el-form-item v-if="obsForm.source === 'page'" label="选择直播页面">
          <el-select v-model="obsForm.page" style="width: 100%">
            <el-option v-for="page in obsPages" :key="page.value" :label="page.label" :value="page.value" />
          </el-select>
          <el-input
            v-if="obsForm.page === 'custom'"
            v-model="obsForm.customPage"
            style="margin-top: 8px"
            placeholder="https://example.com/page"
          />
          <p class="obs-tip">OBS 会用浏览器源打开所选页面；需要登录的页面无法直接作为直播源，建议使用公开页面。</p>
        </el-form-item>
        <el-form-item label="开播时">
          <el-switch v-model="obsForm.auto" active-text="自动写入推流地址并开始推流" />
        </el-form-item>
        <p class="obs-tip">开启后点击「开始直播」会把当前直播间推流地址写入 OBS，并自动开始 OBS 推流；OBS 推流成功后系统会跳过自动演示画面。</p>
      </el-form>
      <template #footer>
        <el-button @click="obsDialog = false">取消</el-button>
        <el-button type="primary" @click="saveObsConfig">保存设置</el-button>
      </template>
    </el-dialog>

    <!-- 结束直播确认层 -->
    <div v-if="showEndConfirm" class="end-backdrop" @click.self="showEndConfirm = false">
      <section class="end-modal" role="dialog" aria-modal="true" aria-labelledby="end-title">
        <button class="modal-close" type="button" aria-label="关闭" @click="showEndConfirm = false">×</button>
        <span class="end-mark">END SESSION</span>
        <h2 id="end-title">确定结束本场直播？</h2>
        <p class="end-desc">结束后观众将立即离开直播间，本场数据会整理成复盘报告。</p>
        <div class="end-stats">
          <div><span>本场时长</span><b class="tabular">{{ elapsedLabel }}</b></div>
          <div><span>峰值在线</span><b class="tabular">{{ formatNumber(peakOnline) }}</b></div>
          <div><span>礼物收益</span><b class="tabular">{{ formatNumber(metrics.gifts) }} <small>酷币</small></b></div>
        </div>
        <div class="end-hint">
          <el-icon><Document /></el-icon>
          <span>结束后将生成本场数据报告，可在主播工作台查看并导出。</span>
        </div>
        <div class="end-actions">
          <button type="button" class="cancel-btn" @click="showEndConfirm = false">取消</button>
          <button type="button" class="confirm-btn" @click="confirmEnd">确认下播</button>
        </div>
      </section>
    </div>

    <!-- 报告生成中 -->
    <div v-if="ending" class="ending-overlay" role="status" aria-live="polite">
      <div class="ending-card">
        <span class="ending-ring" aria-hidden="true"></span>
        <span class="eyebrow">SESSION REPORT</span>
        <h2>正在生成本场报告…</h2>
        <p>正在汇总在线人数、互动和礼物数据</p>
        <div class="ending-progress"><span :style="{ width: `${endingProgress}%` }"></span></div>
        <small>{{ Math.round(endingProgress) }}%</small>
      </div>
    </div>
  </div>
  <div v-else class="console-page"><el-empty description="直播间不存在" /></div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, nextTick, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  CopyDocument,
  DArrowLeft,
  DArrowRight,
  Document,
  Money,
  Refresh,
  Star,
  TrendCharts,
  User,
  View
} from '@element-plus/icons-vue'
import { liveApi, musicApi, playlistApi } from '@/api'
import { pushToObs, stopObsStream } from '@/utils/obs'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'
import AnimatedNumber from '@/components/studio/AnimatedNumber.vue'
import LiveStreamPreview from '@/components/studio/LiveStreamPreview.vue'
import {
  buildLiveReport,
  createSessionId,
  formatClockTime,
  saveLiveReport
} from '@/utils/liveSession'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const roomId = String(route.params.id || '')
const room = ref(null)
const loading = ref(true)
const isPlaying = ref(true)
const activeTab = ref('chat')
const danmakuText = ref('')
const danmakuBox = ref(null)
const showEndConfirm = ref(false)
const ending = ref(false)
const endingProgress = ref(0)
const pushUrl = ref('')
const pullUrl = ref('')
const previewReady = ref(false)
const startedAtMs = ref(Date.now())
const liveSeconds = ref(0)
const playProgress = ref(0)
const currentIndex = ref(0)
const loopMode = ref('loop')
const waveform = ref(Array.from({ length: 48 }, (_, i) => 20 + ((i * 13) % 56)))
const queue = ref([])
const metrics = reactive({
  online: 0,
  views: 0,
  likes: 0,
  gifts: 0,
  peak: 0
})

let elapsedTimer = null
let waveTimer = null
let playTimer = null
let ws = null
let heartbeat = null

const fallbackCover = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="640" height="360"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#24142f"/><stop offset=".55" stop-color="#15121c"/><stop offset="1" stop-color="#0d0b12"/></linearGradient></defs><rect width="640" height="360" fill="url(#g)"/><circle cx="320" cy="180" r="76" fill="none" stroke="#c084fc" stroke-opacity=".2"/><path d="M286 192v-50l68-14v50" fill="none" stroke="#e9b8ff" stroke-width="5" stroke-linecap="round"/><circle cx="278" cy="194" r="12" fill="#e9b8ff"/><circle cx="346" cy="180" r="12" fill="#e9b8ff"/></svg>'
)

const isLive = computed(() => room.value?.status === 'LIVING' && !ending.value)
const previewSignalLabel = computed(() => {
  if (!isLive.value) return '等待推流'
  return previewReady.value ? '实时画面' : '连接中'
})
watch(isLive, (value) => {
  if (!value) previewReady.value = false
})
const elapsedLabel = computed(() => formatClockTime(liveSeconds.value))
const currentTrack = computed(() => queue.value[currentIndex.value] || null)
const peakOnline = computed(() => Math.max(Number(metrics.peak) || 0, metrics.online, 1))
const modeLabel = computed(() => ({ loop: '列表循环', single: '单曲循环', order: '顺序播放' }[loopMode.value]))
const topicChips = computed(() => {
  if (!room.value) return []
  const values = [room.value.category, room.value.scene === 'OUTDOOR' ? '户外现场' : '室内现场', '互动点歌']
  return [...new Set(values.filter(Boolean))]
})

const metricCards = computed(() => [
  { key: 'online', label: '在线人数', icon: User, value: metrics.online, formatter: formatNumber },
  { key: 'views', label: '累计观看', icon: View, value: metrics.views, formatter: formatNumber },
  { key: 'likes', label: '点赞', icon: Star, value: metrics.likes, formatter: formatNumber },
  { key: 'gifts', label: '礼物收益', icon: Money, value: metrics.gifts, formatter: formatNumber, hint: '酷币' },
  { key: 'peak', label: '峰值在线', icon: TrendCharts, value: metrics.peak, formatter: formatNumber }
])

const danmaku = ref([])
const giftFeed = ref([])
const audience = ref({ gender: [], age: [], region: [], sampleCount: 0, onlineCount: 0 })
let audienceTimer = null

const audienceGroups = computed(() => [
  { label: '性别分布', items: audience.value.gender || [] },
  { label: '年龄分布', items: audience.value.age || [] },
  { label: '地区分布', items: audience.value.region || [] }
])

const interactionTabs = computed(() => [
  { value: 'chat', label: '弹幕', count: danmaku.value.length },
  { value: 'gifts', label: '礼物', count: giftFeed.value.length },
  { value: 'rank', label: '榜单', count: null },
  { value: 'audience', label: '观众', count: audience.value.sampleCount || null },
  { value: 'queue', label: '歌单', count: queue.value.length }
])

const rankList = computed(() => {
  const grouped = new Map()
  for (const gift of giftFeed.value) {
    const key = gift.nickname
    const current = grouped.get(key) || { nickname: key, color: gift.color, totalPrice: 0, giftCount: 0 }
    current.totalPrice += Number(gift.totalPrice) || 0
    current.giftCount += Number(gift.count) || 0
    grouped.set(key, current)
  }
  return [...grouped.values()]
    .sort((a, b) => b.totalPrice - a.totalPrice)
    .slice(0, 10)
})

function formatNumber(value) {
  const number = Number(value) || 0
  if (number >= 100000000) return `${(number / 100000000).toFixed(1)}亿`
  if (number >= 10000) return `${(number / 10000).toFixed(1)}万`
  return String(Math.round(number))
}

function formatSeconds(value) {
  const seconds = Math.max(0, Math.round(Number(value) || 0))
  return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`
}

function onImg(event) {
  event.target.src = fallbackCover
}

function initialMetrics() {
  const living = room.value?.status === 'LIVING'
  metrics.online = living ? Number(room.value?.realtimeOnline ?? room.value?.onlineCount) || 0 : 0
  metrics.views = Number(room.value?.totalViewers) || 0
  metrics.likes = Number(room.value?.likeCount) || 0
  metrics.gifts = Number(room.value?.giftIncome) || 0
  metrics.peak = Number(room.value?.peakOnline) || 0
  const startAt = room.value?.liveStartedAt || room.value?.updateTime
  startedAtMs.value = living ? new Date(startAt || Date.now()).getTime() : Date.now()
  liveSeconds.value = living ? Math.max(0, Math.floor((Date.now() - startedAtMs.value) / 1000)) : 0
}

function animateWaveform() {
  waveform.value = waveform.value.map((height, index) => {
    const base = 22 + ((index * 17) % 30)
    return Math.max(12, Math.min(92, base + Math.round(Math.random() * 44)))
  })
}

async function loadRoom() {
  try {
    const [detail, push] = await Promise.all([
      liveApi.room(roomId),
      liveApi.pushUrl(roomId).catch(() => ({ data: null }))
    ])
    room.value = detail.data
    pushUrl.value = push.data?.pushUrl || room.value?.pushUrl || ''
    pullUrl.value = push.data?.pullUrl || room.value?.pullUrl || ''
    initialMetrics()
  } catch (e) {
    room.value = null
  } finally {
    loading.value = false
  }
}

async function loadQueue() {
  const ids = (room.value?.playlistIds || '').split(',').filter(Boolean)
  if (!ids.length) {
    // 未设置直播歌单时不使用热歌榜兜底，避免“没选歌却有歌单”
    queue.value = []
    currentIndex.value = 0
    return
  }
  try {
    const songs = await Promise.all(ids.map((id) => musicApi.song(id).then((r) => r.data).catch(() => null)))
    const tracks = songs.filter(Boolean).map(toTrack)
    if (tracks.length) {
      queue.value = tracks
      currentIndex.value = 0
      return
    }
  } catch (e) { /* empty queue */ }
  queue.value = []
  currentIndex.value = 0
}

function toTrack(song) {
  return {
    id: song.songId || song.id,
    name: song.songName || song.name,
    singerName: song.singerName,
    coverUrl: song.coverUrl,
    duration: song.duration || 240
  }
}

// ---------- 开播前设置直播歌单 ----------
const setlistOpen = ref(false)
const setlistSource = ref('hot')
const myPlaylists = ref([])
const candidates = ref([])
const picked = ref([])
const setlistLoading = ref(false)
const obsDialog = ref(false)
const obsForm = reactive({
  url: localStorage.getItem('km_obs_ws_url') || 'ws://127.0.0.1:4455',
  password: localStorage.getItem('km_obs_ws_password') || '',
  auto: localStorage.getItem('km_obs_auto') !== '0',
  source: localStorage.getItem('km_obs_source') || 'screen',
  page: localStorage.getItem('km_obs_page') || 'broadcast',
  customPage: localStorage.getItem('km_obs_custom_page') || ''
})

const obsPages = [
  { value: 'broadcast', label: '直播画面页（推荐）', path: '/studio/live/{id}/broadcast' },
  { value: 'home', label: '首页', path: '/' },
  { value: 'rank', label: '排行榜', path: '/rank/hot' },
  { value: 'playlists', label: '歌单广场', path: '/playlists' },
  { value: 'mv', label: 'MV 专区', path: '/mv' },
  { value: 'liveSquare', label: '直播广场', path: '/live' },
  { value: 'liveRoom', label: '本直播间', path: '/live/{id}' },
  { value: 'custom', label: '自定义页面', path: '' }
]

async function openSetlist() {
  setlistOpen.value = true
  const ids = (room.value?.playlistIds || '').split(',').filter(Boolean)
  picked.value = ids.map((id) => queue.value.find((t) => String(t.id) === String(id)) || { id, name: `歌曲 ${id}`, singerName: '' })
  await loadCandidates()
}

async function loadCandidates() {
  setlistLoading.value = true
  try {
    if (setlistSource.value === 'mine') {
      const res = await playlistApi.mine({ pageNum: 1, pageSize: 50 })
      myPlaylists.value = res.data?.records || []
      candidates.value = []
    } else {
      const res = await musicApi.hot(50)
      candidates.value = (res.data || []).map(toTrack)
    }
  } finally {
    setlistLoading.value = false
  }
}

async function loadPlaylistSongs(pl) {
  const res = await playlistApi.detail(pl.id)
  candidates.value = (res.data?.songs || []).map(toTrack)
}

function addTrack(track) {
  if (picked.value.length >= 50) {
    ElMessage.warning('最多 50 首')
    return
  }
  if (!picked.value.some((t) => String(t.id) === String(track.id))) picked.value.push({ ...track })
}

function removePicked(index) {
  picked.value.splice(index, 1)
}

function movePicked(index, delta) {
  const target = index + delta
  if (target < 0 || target >= picked.value.length) return
  const list = picked.value
  ;[list[index], list[target]] = [list[target], list[index]]
}

async function saveSetlist() {
  const songIds = picked.value.map((t) => t.id).join(',')
  await request.put(`/live/live/rooms/${roomId}/playlist`, { songIds })
  if (room.value) room.value.playlistIds = songIds
  setlistOpen.value = false
  await loadQueue()
  ElMessage.success('直播歌单已保存，开播后按此顺序播放')
}

function formatMessageTime(value) {
  const date = value ? new Date(value) : new Date()
  if (Number.isNaN(date.getTime())) return '--:--:--'
  return date.toLocaleTimeString('zh-CN', { hour12: false })
}

function toDanmakuMessage(message, index = 0) {
  const timestamp = Number(message?.timestamp) || (message?.createTime ? new Date(message.createTime).getTime() : Date.now())
  const type = message?.type || 'TEXT'
  return {
    id: message?.id || `${timestamp}-${index}`,
    nickname: message?.nickname || '系统',
    color: message?.color || (type === 'ENTRANCE' || type === 'SYSTEM' ? '#8a8497' : '#8b5cf6'),
    content: message?.content || '',
    time: formatMessageTime(timestamp),
    sensitive: false,
    muted: false,
    pinned: false,
    system: type === 'ENTRANCE' || type === 'SYSTEM'
  }
}

async function loadDanmaku() {
  try {
    const res = await liveApi.danmakuHistory(roomId, { pageNum: 1, pageSize: 50 })
    const records = (res.data?.records || [])
      .filter((item) => new Date(item.createTime).getTime() >= startedAtMs.value - 1000)
      .slice()
      .reverse()
    danmaku.value = records.map(toDanmakuMessage)
  } catch (e) {
    danmaku.value = []
  }
}

async function loadGifts() {
  try {
    const res = await liveApi.received({ pageNum: 1, pageSize: 60 })
    const records = (res.data?.records || [])
      .filter((item) => String(item.roomId) === roomId)
      .filter((item) => new Date(item.createTime).getTime() >= startedAtMs.value - 1000)
    giftFeed.value = records.slice(0, 12).map((item, index) => ({
      id: item.id,
      nickname: item.senderName || '匿名听众',
      color: ['#8b5cf6', '#ec4899', '#06b6d4', '#f59e0b'][index % 4],
      giftName: item.giftName || '礼物',
      count: item.count || 1,
      totalPrice: item.totalPrice || 0,
      time: (item.createTime || '').replace('T', ' ').slice(11, 16)
    }))
  } catch (e) {
    giftFeed.value = []
  }
}

async function loadAudience() {
  try {
    const res = await liveApi.audience(roomId)
    audience.value = {
      gender: res.data?.gender || [],
      age: res.data?.age || [],
      region: res.data?.region || [],
      sampleCount: Number(res.data?.sampleCount) || 0,
      onlineCount: Number(res.data?.onlineCount) || 0
    }
  } catch (e) {
    // 在线样本不足或接口暂不可用时保留上一次快照
  }
}

function startTimers() {
  elapsedTimer = window.setInterval(() => {
    if (isLive.value) liveSeconds.value += 1
  }, 1000)
  waveTimer = window.setInterval(animateWaveform, 180)
  playTimer = window.setInterval(() => {
    if (!isPlaying.value || !currentTrack.value || !isLive.value) return
    const duration = Number(currentTrack.value.duration) || 240
    if (playProgress.value >= duration) {
      nextTrack(true)
    } else {
      playProgress.value += 1
      pushNowPlaying()
    }
  }, 1000)
}

function clearTimers() {
  [elapsedTimer, waveTimer, playTimer].forEach((timer) => timer && window.clearInterval(timer))
}

function onSeek() {
  playProgress.value = Math.min(Number(playProgress.value) || 0, Number(currentTrack.value?.duration) || 240)
}

function nextTrack(auto = false) {
  if (!queue.value.length) return
  if (auto && loopMode.value === 'single') {
    playProgress.value = 0
    pushNowPlaying(true)
    return
  }
  if (loopMode.value === 'order' && !auto && currentIndex.value >= queue.value.length - 1) return
  currentIndex.value = (currentIndex.value + 1) % queue.value.length
  playProgress.value = 0
  pushNowPlaying(true)
}

function prevTrack() {
  if (!queue.value.length) return
  currentIndex.value = (currentIndex.value - 1 + queue.value.length) % queue.value.length
  playProgress.value = 0
  pushNowPlaying(true)
}

function cycleMode() {
  loopMode.value = ({ loop: 'single', single: 'order', order: 'loop' })[loopMode.value]
}

function selectTrack(index) {
  currentIndex.value = index
  playProgress.value = 0
  activeTab.value = 'queue'
  pushNowPlaying(true)
}

function scrollDanmaku() {
  nextTick(() => {
    if (!danmakuBox.value) return
    danmakuBox.value.scrollTo({ top: danmakuBox.value.scrollHeight, behavior: 'smooth' })
  })
}

function applyMetrics(message) {
  if (message.onlineCount != null) metrics.online = Number(message.onlineCount) || 0
  if (message.totalViewers != null) metrics.views = Number(message.totalViewers) || 0
  if (message.peakOnline != null) metrics.peak = Number(message.peakOnline) || 0
  if (message.likeCount != null) metrics.likes = Number(message.likeCount) || 0
  if (message.giftIncome != null) metrics.gifts = Number(message.giftIncome) || 0
  if (room.value) {
    room.value.realtimeOnline = metrics.online
    room.value.onlineCount = metrics.online
    room.value.totalViewers = metrics.views
    room.value.peakOnline = metrics.peak
    room.value.likeCount = metrics.likes
    room.value.giftIncome = metrics.gifts
  }
}

function appendDanmaku(message) {
  danmaku.value.push(toDanmakuMessage(message, danmaku.value.length))
  if (danmaku.value.length > 200) danmaku.value.shift()
  scrollDanmaku()
}

let wsRetryTimer = null
let wsStopped = false
let lastNowPlayingPush = 0

function pushNowPlaying(force = false) {
  if (!ws || ws.readyState !== WebSocket.OPEN || !currentTrack.value) return
  const now = Date.now()
  if (!force && now - lastNowPlayingPush < 2500) return
  lastNowPlayingPush = now
  const track = currentTrack.value
  const duration = Number(track.duration) || 240
  ws.send(JSON.stringify({
    type: 'NOW_PLAYING',
    songId: track.id,
    songName: track.name,
    singerName: track.singerName,
    coverUrl: track.coverUrl,
    duration,
    progress: Math.max(0, Math.min(100, Math.round((playProgress.value / duration) * 100))),
    queueIndex: currentIndex.value,
    queueLength: queue.value.length
  }))
}

function connectWs() {
  if (wsStopped || !roomId || !room.value) return
  if (ws) {
    try { ws.close() } catch (e) { /* ignore */ }
    ws = null
  }
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  const token = localStorage.getItem('km_token') || ''
  ws = new WebSocket(`${protocol}://${location.host}/ws/live?roomId=${roomId}&token=${token}&studio=1`)
  ws.onopen = () => {
    heartbeat = window.setInterval(() => {
      if (ws?.readyState === WebSocket.OPEN) ws.send('ping')
    }, 25000)
    window.setTimeout(() => pushNowPlaying(true), 400)
  }
  ws.onmessage = (event) => {
    if (event.data === 'pong') return
    try {
      const msg = JSON.parse(event.data)
      if (msg.type === 'ROOM_METRICS') {
        applyMetrics(msg)
        return
      }
      if (msg.type === 'STREAM_STATUS') {
        if (room.value) room.value.status = msg.content === 'ONLINE' ? 'LIVING' : 'ENDED'
        if (msg.content !== 'ONLINE') metrics.online = 0
        return
      }
      if (['TEXT', 'GIFT', 'ENTRANCE', 'SYSTEM'].includes(msg.type)) {
        appendDanmaku(msg)
        if (msg.type === 'GIFT') loadGifts()
      }
    } catch (e) { /* ignore malformed message */ }
  }
  ws.onclose = () => {
    if (heartbeat) {
      window.clearInterval(heartbeat)
      heartbeat = null
    }
    ws = null
    if (!wsStopped) wsRetryTimer = window.setTimeout(connectWs, 3000)
  }
  ws.onerror = () => {
    try { ws?.close() } catch (e) { /* ignore */ }
  }
}

function disconnectWs() {
  wsStopped = true
  if (wsRetryTimer) {
    window.clearTimeout(wsRetryTimer)
    wsRetryTimer = null
  }
  if (heartbeat) {
    window.clearInterval(heartbeat)
    heartbeat = null
  }
  if (ws) {
    try { ws.close() } catch (e) { /* ignore */ }
    ws = null
  }
}

function sendDanmaku() {
  const content = danmakuText.value.trim()
  if (!content) return
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    ElMessage.warning('弹幕连接未就绪，请稍后重试')
    return
  }
  ws.send(JSON.stringify({ content }))
  danmakuText.value = ''
}

function muteMessage(message) {
  message.muted = true
  ElMessage.success(`已禁言 ${message.nickname}`)
}

function pinMessage(message) {
  message.pinned = true
  danmaku.value = [message, ...danmaku.value.filter((item) => item.id !== message.id)]
  ElMessage.success('已置顶该弹幕')
}

async function copyPushUrl() {
  if (!pushUrl.value) return
  try {
    await navigator.clipboard.writeText(pushUrl.value)
    ElMessage.success('推流地址已复制')
  } catch (e) {
    ElMessage.warning('复制失败，请手动选择地址')
  }
}

async function copyBroadcastUrl() {
  if (!room.value) return
  const url = `${location.origin}${location.pathname}#/studio/live/${room.value.id}/broadcast`
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('直播画面地址已复制，粘贴到 OBS「浏览器源」即可')
  } catch (e) {
    ElMessage.warning('复制失败，请手动打开预览画面地址')
  }
}

function openEndConfirm() {
  if (!isLive.value) return
  showEndConfirm.value = true
}

async function loadObsConfig() {
  try {
    const res = await liveApi.obsConfig()
    const data = res.data || {}
    if (!localStorage.getItem('km_obs_ws_url') && data.websocketUrl) obsForm.url = data.websocketUrl
    if (!obsForm.password && data.password) obsForm.password = data.password
  } catch (e) {
    // 非主播或 OBS 配置不可读时保持手动输入
  }
}

async function openObsDialog() {
  await loadObsConfig()
  obsDialog.value = true
}

function saveObsConfig() {
  localStorage.setItem('km_obs_ws_url', obsForm.url.trim())
  localStorage.setItem('km_obs_ws_password', obsForm.password)
  localStorage.setItem('km_obs_auto', obsForm.auto ? '1' : '0')
  localStorage.setItem('km_obs_source', obsForm.source)
  localStorage.setItem('km_obs_page', obsForm.page)
  localStorage.setItem('km_obs_custom_page', obsForm.customPage.trim())
  obsDialog.value = false
  ElMessage.success('OBS 自动推流设置已保存')
}

function selectedPageUrl() {
  if (obsForm.page === 'custom') {
    const value = obsForm.customPage.trim()
    if (/^https?:\/\//i.test(value)) return value
    if (!value) return `${location.origin}${location.pathname}#/studio/live/${room.value?.id || ''}/broadcast`
    return `${location.origin}${location.pathname}#${value.startsWith('/') ? value : `/${value}`}`
  }
  const selected = obsPages.find((page) => page.value === obsForm.page)
  const path = (selected?.path || '/').replace('{id}', room.value?.id || '')
  return `${location.origin}${location.pathname}#${path}`
}

async function startFromConsole() {
  if (!room.value || isLive.value) return
  if (!obsForm.password) await loadObsConfig()
  let obsStarted = false
  if (obsForm.auto) {
    try {
      const result = await pushToObs(
        { url: obsForm.url.trim(), password: obsForm.password },
        pushUrl.value || room.value.pushUrl,
        obsForm.source === 'page' ? selectedPageUrl() : '',
        obsForm.source
      )
      obsStarted = true
      ElMessage.success(result.alreadyStreaming ? 'OBS 已在推流' : '推流地址已写入 OBS，正在开始推流')
    } catch (e) {
      ElMessage.warning(`OBS 自动推流失败：${e?.message || '请检查 OBS WebSocket 设置'}，已切换自动演示画面`)
    }
  }
  try {
    const res = await liveApi.start(room.value.id, { skipAutoPush: obsStarted })
    room.value = { ...room.value, ...(res.data || {}), status: res.data?.status || 'LIVING' }
    startedAtMs.value = Date.now()
    liveSeconds.value = 0
    metrics.online = 0
    metrics.views = 0
    metrics.likes = 0
    metrics.gifts = 0
    metrics.peak = 0
    ElMessage.success(obsStarted ? '已开播，OBS 正在推流' : '已开播')
  } catch (e) {
    ElMessage.error(e?.message || '开播失败，请稍后重试')
  }
}

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function confirmEnd() {
  if (!room.value) return
  showEndConfirm.value = false
  ending.value = true
  endingProgress.value = 0
  window.setTimeout(() => { endingProgress.value = 100 }, 40)
  const endTime = new Date()
  let obsStopResult = null
  try {
    // 先停 OBS，避免后端下播后 OBS 仍在推流把房间重新标记为直播中
    if (obsForm.url) {
      try {
        obsStopResult = await Promise.race([
          stopObsStream({ url: obsForm.url.trim(), password: obsForm.password }),
          wait(2500)
        ])
      } catch (e) {
        // OBS 未启动/未推流时静默忽略，不阻塞下播
      }
    }
    try {
      await Promise.all([liveApi.stop(room.value.id), wait(1800)])
    } catch (e) {
      // OBS 已停止后，状态监控可能先把房间置为 ENDED，后端再次 stop 会提示已结束
      if (!String(e?.message || '').includes('已结束')) throw e
    }
    const sessionId = createSessionId(room.value.id)
    const report = buildLiveReport({
      room: room.value,
      sessionId,
      startTime: new Date(startedAtMs.value),
      endTime,
      metrics: { peakOnline: peakOnline.value, online: metrics.online, giftIncome: metrics.gifts },
      audience: audience.value.sampleCount ? audience.value : null
    })
    saveLiveReport(report)
    ElMessage.success(obsStopResult?.stopped ? '已下播，OBS 推流已停止' : '已下播')
    router.push(`/studio/report/${sessionId}`)
  } catch (e) {
    ending.value = false
    endingProgress.value = 0
    ElMessage.error(e?.message || '结束直播失败，请稍后重试')
  }
}

onMounted(async () => {
  await loadRoom()
  await loadQueue()
  await Promise.all([loadDanmaku(), loadGifts()])
  loadObsConfig()
  loadAudience()
  audienceTimer = window.setInterval(loadAudience, 6000)
  scrollDanmaku()
  startTimers()
  connectWs()
})

onBeforeUnmount(() => {
  clearTimers()
  if (audienceTimer) window.clearInterval(audienceTimer)
  disconnectWs()
})
</script>

<style scoped>
.console-page {
  width: 100%;
  max-width: 1880px;
  margin: 0 auto;
  padding: 0 0 42px;
  color: var(--km-text);
}
.console-loading { padding-top: 24px; }

/* 顶部状态条 */
.console-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 58px;
  padding: 0 2px 16px;
  border-bottom: 1px solid var(--km-border);
  margin-bottom: 18px;
}
.top-left, .top-right { display: flex; align-items: center; gap: 14px; min-width: 0; }
.back-link { display: inline-flex; align-items: center; gap: 5px; color: var(--km-text-faint); font-size: 12px; white-space: nowrap; }
.back-link:hover { color: var(--km-text); }
.top-divider { width: 1px; height: 22px; background: var(--km-border); }
.live-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 25px;
  padding: 0 9px;
  border-radius: 6px;
  background: #ef2d56;
  color: #fff;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: .12em;
  animation: pulse 1.6s ease-in-out infinite;
}
.live-badge i { width: 6px; height: 6px; border-radius: 50%; background: #fff; }
.live-badge.offline { background: #2a2531; color: #918a9d; animation: none; }
.live-badge.offline i { background: #918a9d; }
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 45, 86, .28); }
  50% { box-shadow: 0 0 0 7px rgba(239, 45, 86, 0); }
}
.top-title { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.top-title h1 { margin: 0; font-size: 16px; font-weight: 600; letter-spacing: -.03em; }
.top-title span { max-width: 380px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--km-text-faint); font-size: 11.5px; }
.elapsed-box { display: flex; align-items: baseline; gap: 8px; color: var(--km-text-faint); font-size: 11px; }
.elapsed-box b { color: #f7d774; font-size: 18px; letter-spacing: .03em; }
.end-live-btn {
  height: 34px;
  padding: 0 15px;
  border: 1px solid rgba(251, 113, 133, .32);
  border-radius: 8px;
  background: rgba(251, 113, 133, .08);
  color: #ff8fa2;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.end-live-btn:hover { background: rgba(251, 113, 133, .16); }
.end-live-btn:disabled { opacity: .35; cursor: not-allowed; }
.start-live-btn {
  height: 34px;
  padding: 0 15px;
  border: 0;
  border-radius: 8px;
  background: var(--km-accent);
  color: #120c19;
  font: inherit;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}
.start-live-btn:hover { background: #e9d5ff; }
.start-live-btn:disabled { opacity: .35; cursor: not-allowed; }

/* 指标条 */
.metric-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}
.metric-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 70px;
  padding: 12px 13px;
  border: 1px solid var(--km-border);
  border-radius: 12px;
  background: linear-gradient(145deg, rgba(27, 22, 34, .82), rgba(15, 13, 20, .92));
  overflow: hidden;
}
.metric-card::after { content: ''; position: absolute; right: -18px; bottom: -32px; width: 82px; height: 82px; border-radius: 50%; background: radial-gradient(circle, rgba(216, 180, 254, .08), transparent 68%); pointer-events: none; }
.metric-icon { width: 30px; height: 30px; display: grid; place-items: center; flex: none; border-radius: 8px; background: rgba(216, 180, 254, .09); color: var(--km-accent); font-size: 14px; }
.metric-icon.online { color: #86efac; background: rgba(74, 222, 128, .08); }
.metric-icon.views { color: #93c5fd; background: rgba(96, 165, 250, .08); }
.metric-icon.likes { color: #f9a8d4; background: rgba(249, 168, 212, .08); }
.metric-icon.gifts { color: #f7d774; background: rgba(247, 215, 116, .08); }
.metric-icon.peak { color: #c4b5fd; background: rgba(196, 181, 253, .08); }
.metric-copy { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.metric-copy > span { color: var(--km-text-faint); font-size: 10.5px; white-space: nowrap; }
.metric-copy strong { font-size: 19px; line-height: 1.15; font-weight: 850; }
.metric-hint { position: absolute; top: 12px; right: 10px; color: var(--km-text-faint); font-size: 9px; }

/* 主布局 */
.console-grid { display: flex; align-items: flex-start; gap: 18px; }
.console-main { display: flex; flex-direction: column; gap: 14px; flex: 1; min-width: 0; }
.interaction-panel {
  display: flex;
  flex-direction: column;
  width: 380px;
  flex: 0 0 380px;
  min-height: 680px;
  max-height: calc(100vh - 190px);
  position: sticky;
  top: 16px;
  border: 1px solid var(--km-border);
  border-radius: 16px;
  background: #0e0b13;
  overflow: hidden;
}

/* 卡片基本 */
.km-card.preview-card, .km-card.now-card, .km-card.room-info-card { padding: 16px 17px; }
.card-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 13px; }
.eyebrow { display: block; color: var(--km-accent); font-size: 9px; font-weight: 800; letter-spacing: .17em; margin-bottom: 3px; }
.card-heading h2 { margin: 0; font-size: 14px; font-weight: 800; letter-spacing: -.02em; }
.preview-signal { display: inline-flex; align-items: center; gap: 5px; color: var(--km-text-faint); font-size: 10.5px; }
.preview-signal i { width: 5px; height: 5px; border-radius: 50%; background: #f7d774; }
.preview-signal.live i { background: #fb7185; box-shadow: 0 0 0 3px rgba(251, 113, 133, .14); animation: preview-signal-pulse 1.6s ease-in-out infinite; }
.preview-signal.connecting i { background: #f7d774; animation: preview-signal-pulse 1.2s ease-in-out infinite; }
@keyframes preview-signal-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .35; }
}

/* 预览 */
.preview-stage {
  position: relative;
  aspect-ratio: 16 / 9;
  min-height: 250px;
  overflow: hidden;
  border-radius: 11px;
  background: #09070c;
  isolation: isolate;
}
.preview-bg { position: absolute; inset: -20px; width: calc(100% + 40px); height: calc(100% + 40px); object-fit: cover; filter: blur(20px) saturate(.7) brightness(.38); transform: scale(1.06); }
.preview-tint { position: absolute; inset: 0; background: radial-gradient(ellipse at center, rgba(17, 12, 24, .18), rgba(7, 6, 10, .8)), linear-gradient(180deg, rgba(6, 5, 9, .3), transparent 35%, rgba(6, 5, 9, .76)); }
.preview-center { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 15px; padding: 22px; }
.waveform { width: min(78%, 520px); height: 112px; display: flex; align-items: center; justify-content: center; gap: 3px; }
.waveform span { width: 4px; min-height: 7px; border-radius: 999px; background: linear-gradient(180deg, #f0abfc, #a855f7 60%, #6d28d9); box-shadow: 0 0 9px rgba(216, 180, 254, .28); transition: height .16s ease; }
.preview-caption { display: flex; flex-direction: column; align-items: center; gap: 3px; text-align: center; }
.preview-caption span { color: rgba(240, 171, 252, .72); font-size: 8.5px; letter-spacing: .2em; font-weight: 800; }
.preview-caption strong { max-width: 80vw; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 18px; letter-spacing: -.03em; text-shadow: 0 2px 14px rgba(0,0,0,.5); }
.preview-caption small { color: rgba(255, 255, 255, .55); font-size: 10.5px; }
.preview-scanline { position: absolute; inset: 0; opacity: .18; background: repeating-linear-gradient(180deg, rgba(255,255,255,.07) 0 1px, transparent 1px 4px); pointer-events: none; mix-blend-mode: overlay; }
.preview-watermark { position: absolute; right: 12px; bottom: 11px; padding: 5px 8px; border: 1px solid rgba(255,255,255,.14); border-radius: 5px; background: rgba(7,6,10,.55); color: rgba(255,255,255,.65); font-size: 9.5px; letter-spacing: .04em; backdrop-filter: blur(8px); }
.preview-channels { position: absolute; left: 12px; bottom: 12px; color: rgba(255,255,255,.35); font-size: 8.5px; letter-spacing: .12em; }
.preview-channels::before { content: '●'; color: #fb7185; margin-right: 5px; font-size: 7px; }

/* 正在播放 */
.queue-position { color: var(--km-text-faint); font-size: 10.5px; padding-top: 5px; }
.setlist-edit { margin-left: auto; height: 24px; padding: 0 10px; border-radius: 999px; border: 1px solid var(--km-border); background: transparent; color: var(--km-text-dim); font-size: 11px; cursor: pointer; }
.setlist-edit:hover { color: var(--km-accent); border-color: rgba(245,180,91,.5); }
.setlist-dialog { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.sd-col { display: flex; flex-direction: column; gap: 10px; min-width: 0; }
.sd-head { font-size: 12px; color: var(--km-text-dim); }
.sd-empty { font-size: 12px; color: var(--km-text-faint); padding: 12px 0; }
.pl-list { display: flex; flex-direction: column; gap: 4px; max-height: 140px; overflow: auto; }
.pl-item { display: flex; align-items: center; justify-content: space-between; padding: 6px 10px; border-radius: 8px; border: 1px solid transparent; background: rgba(255,255,255,.03); color: var(--km-text); font-size: 12px; cursor: pointer; }
.pl-item:hover { border-color: var(--km-border); }
.pl-item small { color: var(--km-text-faint); }
.cand-list { display: flex; flex-direction: column; gap: 4px; max-height: 300px; overflow: auto; }
.cand-row { display: flex; align-items: center; gap: 8px; padding: 6px 10px; border-radius: 8px; border: 0; background: transparent; color: var(--km-text); font-size: 12px; cursor: pointer; text-align: left; }
.cand-row:hover { background: rgba(255,255,255,.05); }
.cand-row .nm { flex: 1; }
.cand-row small { color: var(--km-text-faint); }
.cand-row b { color: var(--km-accent); }
.pick-row { display: flex; align-items: center; gap: 6px; padding: 5px 8px; border: 1px solid var(--km-border); border-radius: 8px; font-size: 12px; }
.pick-row .idx { color: var(--km-text-faint); width: 18px; }
.pick-row .nm { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pick-row small { color: var(--km-text-faint); }
.pick-row .mini { width: 22px; height: 22px; border-radius: 6px; border: 1px solid var(--km-border); background: transparent; color: var(--km-text-dim); cursor: pointer; }
.pick-row .mini:disabled { opacity: .35; cursor: not-allowed; }
.pick-row .mini.danger { color: #ff9fb0; }
.now-body { display: flex; align-items: stretch; gap: 15px; }
.now-cover { width: 104px; height: 104px; flex: none; border-radius: 10px; object-fit: cover; border: 1px solid rgba(255,255,255,.08); }
.now-main { display: flex; flex: 1; min-width: 0; flex-direction: column; justify-content: space-between; gap: 8px; }
.now-meta { min-width: 0; }
.now-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 17px; font-weight: 800; letter-spacing: -.03em; }
.now-artist { margin-top: 3px; color: var(--km-text-faint); font-size: 11.5px; }
.now-controls { display: flex; align-items: center; gap: 7px; }
.ctrl-btn { width: 27px; height: 27px; display: grid; place-items: center; border: 1px solid var(--km-border); border-radius: 50%; background: transparent; color: var(--km-text-dim); cursor: pointer; font-size: 12px; }
.ctrl-btn:hover { color: var(--km-text); border-color: var(--km-border-strong); }
.ctrl-btn.play { width: 32px; height: 32px; border: 0; background: var(--km-accent); color: #120c19; }
.ctrl-btn.play:hover { background: #e9d5ff; }
.mode-btn { display: inline-flex; align-items: center; gap: 4px; margin-left: 4px; border: 0; background: transparent; color: var(--km-text-faint); font: inherit; font-size: 10.5px; cursor: pointer; }
.mode-btn:hover { color: var(--km-accent); }
.mode-btn .el-icon { font-size: 12px; }
.track-progress-row { display: flex; align-items: center; gap: 8px; color: var(--km-text-faint); font-size: 10px; }
.track-range { flex: 1; height: 3px; accent-color: var(--km-accent); cursor: pointer; }
.empty-inline { padding: 26px 0; color: var(--km-text-faint); font-size: 12px; text-align: center; }
.audience-groups { display: flex; flex-direction: column; gap: 14px; padding: 4px 2px 14px; }
.audience-group h4 { margin: 0 0 8px; color: var(--km-text-dim); font-size: 10.5px; font-weight: 650; }
.audience-row { display: flex; align-items: center; gap: 7px; margin-bottom: 7px; }
.audience-name { width: 38px; flex: none; color: var(--km-text-faint); font-size: 9.5px; }
.audience-track { flex: 1; height: 5px; overflow: hidden; border-radius: 999px; background: rgba(255,255,255,.07); }
.audience-track i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #8b5cf6, #f0abfc); }
.audience-row b { width: 30px; flex: none; color: var(--km-text-dim); text-align: right; font-size: 9.5px; font-weight: 500; }

/* 房间信息 */
.room-code { color: var(--km-text-faint); font-size: 9.5px; letter-spacing: .12em; padding-top: 5px; }
.room-info-body { display: grid; grid-template-columns: minmax(0, 1fr) minmax(280px, .85fr); gap: 20px; align-items: center; }
.room-info-copy h3 { margin: 0 0 5px; font-size: 16px; letter-spacing: -.02em; }
.room-info-copy p { max-width: 540px; margin: 0 0 10px; color: var(--km-text-faint); font-size: 11.5px; line-height: 1.6; }
.topic-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.topic-chips span { padding: 4px 7px; border: 1px solid rgba(216,180,254,.18); border-radius: 5px; color: var(--km-accent); background: rgba(216,180,254,.05); font-size: 10px; }
.push-copy-box { padding: 11px 12px; border: 1px solid var(--km-border); border-radius: 9px; background: rgba(0,0,0,.14); }
.push-copy-box > span { display: block; margin-bottom: 6px; color: var(--km-text-faint); font-size: 10px; }
.push-copy-row { display: flex; gap: 7px; }
.push-copy-row code { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; padding: 6px 8px; border-radius: 5px; background: rgba(255,255,255,.04); color: #cdb8e2; font-size: 10px; }
.copy-btn { display: inline-flex; align-items: center; gap: 4px; flex: none; padding: 0 9px; border: 1px solid rgba(216,180,254,.22); border-radius: 5px; background: transparent; color: var(--km-accent); font: inherit; font-size: 10px; cursor: pointer; }
.copy-btn:hover { background: rgba(216,180,254,.08); }
.copy-btn:disabled { opacity: .4; cursor: not-allowed; }
.push-copy-box small { display: block; margin-top: 6px; color: var(--km-text-faint); font-size: 9.5px; }
.broadcast-hint { display: flex; align-items: center; gap: 10px; margin-top: 8px; }
.broadcast-hint a { color: var(--km-accent); font-size: 10px; text-decoration: none; }
.broadcast-hint a:hover { text-decoration: underline; }
.obs-tip { margin: 4px 0 0; color: var(--km-text-faint); font-size: 11px; line-height: 1.6; }

/* 互动栏 */
.interaction-tabs { display: flex; flex: none; padding: 9px 10px 0; border-bottom: 1px solid var(--km-border); gap: 2px; }
.interaction-tab { position: relative; display: inline-flex; align-items: center; gap: 4px; padding: 9px 8px 11px; border: 0; background: transparent; color: var(--km-text-faint); font: inherit; font-size: 11.5px; cursor: pointer; white-space: nowrap; }
.interaction-tab:hover { color: var(--km-text); }
.interaction-tab.active { color: var(--km-text); font-weight: 700; }
.interaction-tab.active::after { content: ''; position: absolute; left: 8px; right: 8px; bottom: -1px; height: 2px; border-radius: 2px; background: var(--km-accent); }
.interaction-tab em { min-width: 15px; height: 15px; padding: 0 4px; display: inline-flex; align-items: center; justify-content: center; border-radius: 999px; background: rgba(255,255,255,.07); color: var(--km-text-faint); font-size: 9px; font-style: normal; }
.interaction-tab.active em { background: rgba(216,180,254,.14); color: var(--km-accent); }
.tab-panel { min-height: 0; flex: 1; display: flex; flex-direction: column; }
.chat-panel { overflow: hidden; }
.danmaku-list { flex: 1; min-height: 0; overflow-y: auto; padding: 12px 12px 6px; scrollbar-width: thin; }
.danmaku-item { position: relative; display: flex; gap: 8px; padding: 8px 2px; border-radius: 7px; transition: background .15s; }
.danmaku-item:hover { background: rgba(255,255,255,.025); }
.danmaku-item.sensitive { margin: 3px 0; padding: 9px 8px; background: rgba(247,215,116,.07); border: 1px solid rgba(247,215,116,.15); }
.danmaku-item.system { padding-top: 5px; padding-bottom: 5px; opacity: .68; }
.danmaku-item.system .danmaku-avatar { width: 22px; height: 22px; font-size: 9px; }
.danmaku-item.system .danmaku-copy p { color: var(--km-text-faint); font-size: 10.5px; }
.danmaku-item.muted { opacity: .56; }
.danmaku-avatar, .gift-avatar, .rank-avatar { display: grid; place-items: center; flex: none; border-radius: 50%; color: #fff; font-size: 10px; font-weight: 800; }
.danmaku-avatar { width: 28px; height: 28px; }
.danmaku-copy { min-width: 0; flex: 1; }
.danmaku-meta { display: flex; align-items: center; gap: 6px; margin-bottom: 3px; }
.danmaku-meta b { max-width: 110px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--km-text-dim); font-size: 10.5px; font-weight: 650; }
.danmaku-meta > span { color: var(--km-text-faint); font-size: 9px; }
.danmaku-meta em { padding: 1px 4px; border-radius: 3px; background: rgba(216,180,254,.12); color: var(--km-accent); font-size: 8.5px; font-style: normal; }
.danmaku-copy p { margin: 0; color: #ded8e6; font-size: 11.5px; line-height: 1.5; word-break: break-word; }
.danmaku-item.sensitive .danmaku-copy p { color: #d9c58d; }
.fold-note { display: inline-flex; margin-top: 5px; padding: 2px 5px; border-radius: 3px; background: rgba(247,215,116,.11); color: #e8ca70; font-size: 9px; }
.danmaku-actions { position: absolute; right: 3px; top: 5px; display: flex; gap: 2px; opacity: 0; transition: opacity .15s; }
.danmaku-item:hover .danmaku-actions { opacity: 1; }
.danmaku-actions button { padding: 2px 5px; border: 1px solid var(--km-border); border-radius: 4px; background: #17131e; color: var(--km-text-faint); font: inherit; font-size: 9px; cursor: pointer; }
.danmaku-actions button:hover { color: var(--km-text); border-color: var(--km-border-strong); }
.danmaku-compose { display: flex; gap: 7px; padding: 10px 12px 12px; border-top: 1px solid var(--km-border); background: #0b0910; }
.danmaku-compose .el-input { --el-input-bg-color: #15111c; --el-input-border-color: #2c2538; --el-input-text-color: #f7f4fc; }
.send-btn { flex: none; padding: 0 12px; border: 0; border-radius: 7px; background: var(--km-accent); color: #120c19; font: inherit; font-size: 11px; font-weight: 800; cursor: pointer; }
.send-btn:hover { background: #e9d5ff; }
.send-btn:disabled { opacity: .35; cursor: not-allowed; }
.panel-note { display: flex; justify-content: space-between; padding: 13px 13px 9px; color: var(--km-text-dim); font-size: 11px; }
.panel-note small { color: var(--km-text-faint); font-size: 10px; }
.gift-list { flex: 1; overflow-y: auto; padding: 0 11px 12px; }
.gift-row { display: flex; align-items: center; gap: 9px; padding: 10px 3px; border-bottom: 1px solid rgba(255,255,255,.04); }
.gift-avatar { width: 26px; height: 26px; }
.gift-copy { flex: 1; min-width: 0; }
.gift-copy p { margin: 0; color: var(--km-text-dim); font-size: 10.5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.gift-copy p b { color: var(--km-text); font-weight: 650; }
.gift-copy p strong { color: var(--km-accent); font-weight: 700; }
.gift-copy > span { display: block; margin-top: 2px; color: var(--km-text-faint); font-size: 9px; }
.gift-value { flex: none; text-align: right; }
.gift-value b { color: #f7d774; font-size: 12px; }
.gift-value span { display: block; color: var(--km-text-faint); font-size: 8.5px; }
.rank-list { flex: 1; overflow-y: auto; padding: 0 10px 12px; }
.rank-row { display: flex; align-items: center; gap: 8px; min-height: 42px; padding: 7px 8px; border-radius: 7px; }
.rank-row.rank-1 { background: linear-gradient(90deg, rgba(247,215,116,.16), rgba(247,215,116,.03)); }
.rank-row.rank-2 { background: linear-gradient(90deg, rgba(203,213,225,.12), rgba(203,213,225,.02)); }
.rank-row.rank-3 { background: linear-gradient(90deg, rgba(217,119,6,.13), rgba(217,119,6,.02)); }
.rank-badge { width: 22px; text-align: center; color: var(--km-text-faint); font-size: 9px; font-weight: 800; }
.rank-1 .rank-badge { color: #f7d774; }
.rank-2 .rank-badge { color: #cbd5e1; }
.rank-3 .rank-badge { color: #d97706; }
.rank-avatar { width: 24px; height: 24px; }
.rank-copy { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 2px; }
.rank-copy b { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--km-text-dim); font-size: 10.5px; font-weight: 650; }
.rank-copy span { color: var(--km-text-faint); font-size: 9px; }
.rank-row > strong { color: #f7d774; font-size: 11px; font-weight: 750; }
.rank-row > strong small { color: var(--km-text-faint); font-size: 8.5px; font-weight: 400; }
.setlist { flex: 1; overflow-y: auto; padding: 0 10px 12px; }
.setlist-row { display: flex; align-items: center; gap: 9px; width: 100%; padding: 10px 7px; border: 0; border-bottom: 1px solid rgba(255,255,255,.04); background: transparent; color: inherit; text-align: left; cursor: pointer; }
.setlist-row:hover { background: rgba(255,255,255,.025); }
.setlist-row.active { background: rgba(216,180,254,.07); }
.setlist-index { width: 19px; color: var(--km-text-faint); font-size: 9px; }
.setlist-copy { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.setlist-copy b { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--km-text-dim); font-size: 11px; font-weight: 650; }
.setlist-copy small { color: var(--km-text-faint); font-size: 9.5px; }
.setlist-status { flex: none; color: var(--km-text-faint); font-size: 9px; }
.setlist-row.active .setlist-status { color: var(--km-accent); }

/* 结束确认 */
.end-backdrop, .ending-overlay { position: fixed; inset: 0; z-index: 1200; display: grid; place-items: center; padding: 22px; background: rgba(2, 1, 5, .74); backdrop-filter: blur(10px); }
.end-modal { position: relative; width: min(460px, 94vw); padding: 28px; border: 1px solid #ece7f2; border-radius: 18px; background: radial-gradient(600px 220px at 50% -20%, rgba(216,180,254,.18), transparent 70%), #fff; box-shadow: 0 28px 80px rgba(40,22,63,.16); }
.modal-close { position: absolute; top: 12px; right: 14px; border: 0; background: transparent; color: #8b8195; font-size: 22px; line-height: 1; cursor: pointer; }
.modal-close:hover { color: #4f465a; }
.end-mark { color: #ff8fa2; font-size: 9px; font-weight: 800; letter-spacing: .18em; }
.end-modal h2 { margin: 8px 0 8px; font-size: 21px; letter-spacing: -.04em; }
.end-desc { margin: 0; color: var(--km-text-faint); font-size: 11.5px; line-height: 1.7; }
.end-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin: 20px 0 14px; }
.end-stats > div { padding: 10px 11px; border: 1px solid #ece7f2; border-radius: 8px; background: #faf7fd; }
.end-stats span { display: block; color: #776d83; font-size: 9.5px; }
.end-stats b { display: block; margin-top: 4px; color: #1d1726; font-size: 15px; }
.end-stats small { color: #776d83; font-size: 8.5px; font-weight: 400; }
.end-hint { display: flex; align-items: flex-start; gap: 8px; padding: 10px 11px; border-radius: 8px; background: #faf4ff; color: #5f5769; font-size: 10.5px; line-height: 1.5; }
.end-hint .el-icon { color: #8b3fce; margin-top: 1px; }
.end-actions { display: flex; justify-content: flex-end; gap: 9px; margin-top: 22px; }
.cancel-btn, .confirm-btn { height: 36px; padding: 0 16px; border-radius: 8px; font: inherit; font-size: 12px; font-weight: 700; cursor: pointer; }
.cancel-btn { border: 1px solid #e1d9e9; background: #fff; color: #5f5769; }
.cancel-btn:hover { color: #1d1726; border-color: #d3bce6; background: #faf6fe; }
.confirm-btn { border: 1px solid #f0c6cc; background: #fff5f6; color: #b4233a; }
.confirm-btn:hover { background: #ffecef; }
.ending-card { width: min(390px, 92vw); display: flex; flex-direction: column; align-items: center; padding: 30px 26px; border: 1px solid #ece7f2; border-radius: 17px; background: #fff; box-shadow: 0 24px 70px rgba(40,22,63,.14); text-align: center; }
.ending-ring { width: 42px; height: 42px; margin-bottom: 14px; border: 2px solid rgba(216,180,254,.18); border-top-color: var(--km-accent); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.ending-card h2 { margin: 5px 0 5px; font-size: 17px; }
.ending-card p { margin: 0; color: var(--km-text-faint); font-size: 10.5px; }
.ending-progress { width: 100%; height: 5px; margin: 20px 0 8px; overflow: hidden; border-radius: 999px; background: rgba(255,255,255,.08); }
.ending-progress span { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #a855f7, #f0abfc); transition: width 1.8s cubic-bezier(.2,.8,.2,1); }
.ending-card > small { color: var(--km-accent); font-size: 10px; }

@media (max-width: 1180px) {
  .metric-strip { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .console-grid { flex-direction: column; }
  .interaction-panel { width: 100%; flex-basis: auto; min-height: 520px; max-height: none; position: static; }
  .room-info-body { grid-template-columns: 1fr; }
}
@media (max-width: 720px) {
  .console-top { align-items: flex-start; flex-direction: column; }
  .top-right { width: 100%; justify-content: space-between; }
  .metric-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .preview-stage { min-height: 180px; }
  .waveform { height: 74px; gap: 2px; }
  .waveform span { width: 3px; }
  .now-body { flex-direction: column; }
  .now-cover { width: 100%; height: 160px; }
}
</style>
