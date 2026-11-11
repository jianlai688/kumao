<template>
  <div>
    <!-- 状态卡 -->
    <div class="mon-grid">
      <div class="stat-card">
        <div class="stat-ico">📚</div>
        <div>
          <div class="value">{{ monitor.docCount ?? 0 }}</div>
          <div class="label">索引文档数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-ico">⚡</div>
        <div>
          <div class="value" :class="monitor.esAvailable ? 'ok' : 'bad'">
            {{ monitor.esAvailable ? '正常' : '不可用' }}
          </div>
          <div class="label">Elasticsearch 状态</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-ico">🗂️</div>
        <div>
          <div class="value small">{{ monitor.index || 'kumao_search' }}</div>
          <div class="label">索引名称（ES 不可用时降级 MySQL）</div>
        </div>
      </div>
    </div>

    <!-- 同步 -->
    <div class="km-card">
      <div class="km-between" style="margin-bottom:14px">
        <div>
          <b style="font-size:14px">手动触发索引同步</b>
          <span class="km-faint" style="margin-left:10px;font-size:11.5px">从数据库全量重建 ES 索引</span>
        </div>
        <div class="km-flex">
          <el-button @click="loadMonitor">刷新监控</el-button>
          <el-button type="primary" :loading="syncing" @click="syncAll">全量同步</el-button>
        </div>
      </div>
      <div class="sync-note">
        同步说明：从 music-service / playlist-service / live-service / musician-service 拉取数据写入 ES 索引
        <code>kumao_search</code>。
      </div>
      <el-table :data="syncLog" style="margin-top:14px" max-height="300">
        <el-table-column prop="time" label="时间" width="190" />
        <el-table-column prop="message" label="内容" />
      </el-table>
    </div>

    <!-- 监控明细 -->
    <div class="km-card">
      <b style="font-size:14px">检索数据监控</b>
      <div class="kv"><span>索引健康</span><b :class="monitor.esAvailable ? 'ok' : 'bad'">{{ monitor.esAvailable ? 'GREEN' : 'RED（降级中）' }}</b></div>
      <div class="kv"><span>索引名称</span><b>{{ monitor.index }}</b></div>
      <div class="kv"><span>文档总数</span><b>{{ monitor.docCount ?? 0 }}</b></div>
      <div class="kv"><span>最近检查</span><b>{{ lastCheck }}</b></div>
      <div v-if="monitor.message" class="kv"><span>错误信息</span><b class="bad">{{ monitor.message }}</b></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { searchApi, musicApi, playlistApi, liveApi, musicianApi } from '@/api'

const monitor = ref({})
const syncing = ref(false)
const syncLog = ref([])
const lastCheck = ref('')

async function loadMonitor() {
  const res = await searchApi.monitor()
  monitor.value = res.data || {}
  lastCheck.value = new Date().toLocaleString()
}

async function syncAll() {
  syncing.value = true
  const docs = []
  try {
    const [songs, singers, playlists, rooms, musicians] = await Promise.all([
      musicApi.songs({ pageNum: 1, pageSize: 500, status: 1 }),
      musicApi.singers({ pageNum: 1, pageSize: 500 }),
      playlistApi.publicPage({ pageNum: 1, pageSize: 500 }),
      liveApi.square({ pageNum: 1, pageSize: 500 }),
      musicianApi.page({ pageNum: 1, pageSize: 500 })
    ])
    ;(songs.data?.records || []).forEach((s) => docs.push({
      bizId: s.id, type: 'SONG', title: s.name, subtitle: s.singerName, coverUrl: s.coverUrl,
      tags: `${s.style || ''},${s.language || ''}`, description: s.albumName, heat: s.playCount
    }))
    ;(singers.data?.records || []).forEach((s) => docs.push({
      bizId: s.id, type: 'SINGER', title: s.name, subtitle: s.region, coverUrl: s.avatar,
      tags: s.region, description: s.intro, heat: s.fansCount
    }))
    ;(playlists.data?.records || []).forEach((p) => docs.push({
      bizId: p.id, type: 'PLAYLIST', title: p.name, subtitle: p.creatorName, coverUrl: p.coverUrl,
      tags: '歌单', description: p.description, heat: p.collectCount
    }))
    ;(rooms.data?.records || []).forEach((r) => docs.push({
      bizId: r.id, type: 'LIVE_ROOM', title: r.title, subtitle: r.anchorName, coverUrl: r.coverUrl,
      tags: r.category, description: r.intro, heat: r.realtimeOnline
    }))
    ;(musicians.data?.records || []).forEach((m) => docs.push({
      bizId: m.id, type: 'MUSICIAN', title: m.stageName, subtitle: m.style, coverUrl: m.avatar,
      tags: m.style, description: m.intro, heat: m.fansCount
    }))
    await searchApi.sync(docs)
    syncLog.value.unshift({ time: new Date().toLocaleString(), message: `同步成功，共 ${docs.length} 条文档` })
    ElMessage.success(`同步成功，共 ${docs.length} 条`)
    await loadMonitor()
  } catch (e) {
    syncLog.value.unshift({ time: new Date().toLocaleString(), message: '同步失败：' + (e.message || '未知错误') })
    ElMessage.error('同步失败')
  } finally {
    syncing.value = false
  }
}

onMounted(loadMonitor)
</script>

<style scoped>
.mon-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; margin-bottom: 16px; }
.stat-card {
  display: flex;
  align-items: center;
  gap: 15px;
  background: linear-gradient(160deg, #16171c, #111216);
  border: 1px solid var(--km-border);
  border-radius: 18px;
  padding: 20px 22px;
  transition: all .2s;
}
.stat-card:hover { border-color: var(--km-border-strong); }
.stat-ico {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: rgba(192, 132, 252, .08);
  border: 1px solid rgba(192, 132, 252, .16);
  font-size: 17px;
  flex-shrink: 0;
}
.value { font-size: 26px; font-weight: 900; letter-spacing: -1px; line-height: 1.15; }
.value.small { font-size: 17px; letter-spacing: -.3px; }
.value.ok { color: var(--km-accent); }
.value.bad { color: #ff8095; }
.label { font-size: 11.5px; color: var(--km-text-faint); margin-top: 4px; }

.sync-note {
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(122, 184, 255, .07);
  border: 1px solid rgba(122, 184, 255, .18);
  font-size: 12px;
  line-height: 1.7;
  color: rgba(243, 244, 246, .7);
}
.sync-note code {
  font-family: inherit;
  padding: 1px 6px;
  border-radius: 5px;
  background: rgba(255, 255, 255, .08);
  color: var(--km-accent);
  font-size: 11.5px;
}

.kv { display: flex; justify-content: space-between; align-items: center; padding: 11px 0; border-bottom: 1px solid rgba(255, 255, 255, .05); font-size: 12.5px; }
.kv:last-child { border-bottom: none; }
.kv span { color: var(--km-text-dim); }
.kv b { font-weight: 700; font-size: 13px; }
.kv b.ok { color: var(--km-accent); }
.kv b.bad { color: #ff8095; }
</style>
