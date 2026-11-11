<template>
  <div class="km-page">
    <!-- 个人头部 -->
    <div class="profile-hero">
      <div class="hero-blur"></div>
      <img :src="userStore.user?.avatar || fallbackAvatar" class="hero-avatar" @error="onImg" />
      <div class="hero-info">
        <h1 class="hero-name">{{ userStore.user?.nickname || userStore.user?.username }}</h1>
        <div class="hero-roles">
          <span v-for="r in roles" :key="r" class="role-tag" :class="r.toLowerCase()">{{ roleName(r) }}</span>
        </div>
        <p class="hero-meta">@{{ userStore.user?.username }}</p>
      </div>
      <button class="edit-btn" @click="openProfile">
        <el-icon><Edit /></el-icon> 编辑资料
      </button>
    </div>

    <!-- 标签页 -->
    <div class="tabs-row">
      <button
        v-for="t in tabs"
        :key="t.name"
        class="tab-btn"
        :class="{ active: tab === t.name }"
        @click="switchTab(t.name)"
      >
        <el-icon><component :is="t.icon" /></el-icon>
        {{ t.label }}
      </button>
    </div>

    <!-- 我喜欢 -->
    <div v-if="tab === 'likes'" class="panel">
      <div class="panel-head">
        <span class="panel-count">{{ likes.length }} 首歌曲</span>
        <button class="btn-mini" @click="playAllLikes" :disabled="!likes.length">
          <el-icon><VideoPlay /></el-icon> 播放全部
        </button>
      </div>
      <div class="track-list">
        <div v-for="(s, i) in likes" :key="s.id" class="track-row" @dblclick="playLike(s)">
          <span class="t-idx">{{ String(i + 1).padStart(2, '0') }}</span>
          <img :src="s.coverUrl || fallbackCover" class="t-cover" @error="onImg" @click="$router.push(`/song/${s.songId}`)" />
          <div class="t-main" @click="$router.push(`/song/${s.songId}`)">
            <div class="t-name">{{ s.songName }}</div>
            <div class="t-sub">{{ s.singerName }}</div>
          </div>
          <button class="t-btn" @click.stop="playLike(s)"><el-icon><VideoPlay /></el-icon></button>
          <button class="t-btn danger" @click.stop="unlike(s)"><el-icon><Delete /></el-icon></button>
        </div>
        <el-empty v-if="!likes.length" description="还没有喜欢的歌曲，去发现好音乐吧" />
      </div>
    </div>

    <!-- 我的歌单 -->
    <div v-if="tab === 'playlists'" class="panel">
      <div class="panel-head">
        <span class="panel-count">{{ myPlaylists.length }} 个歌单</span>
        <button class="btn-mini" @click="$router.push('/playlists')">去创建 <el-icon><Plus /></el-icon></button>
      </div>
      <div class="pl-grid">
        <div v-for="pl in myPlaylists" :key="pl.id" class="pl-card" @click="$router.push(`/playlist/${pl.id}`)">
          <div class="cover-wrap">
            <img :src="pl.coverUrl || fallbackCover" class="pl-cover" @error="onImg" />
            <div class="play-mask"><el-icon><VideoPlay /></el-icon></div>
          </div>
          <div class="pl-name">{{ pl.name }}</div>
          <div class="pl-sub">{{ pl.songCount }} 首 · {{ pl.isPublic ? '公开' : '私有' }}</div>
        </div>
      </div>
      <el-empty v-if="!myPlaylists.length" description="还没有创建歌单" />
    </div>

    <!-- 收藏歌单 -->
    <div v-if="tab === 'collected'" class="panel">
      <div class="panel-head"><span class="panel-count">{{ collected.length }} 个收藏</span></div>
      <div class="pl-grid">
        <div v-for="pl in collected" :key="pl.id" class="pl-card" @click="$router.push(`/playlist/${pl.id}`)">
          <div class="cover-wrap">
            <img :src="pl.coverUrl || fallbackCover" class="pl-cover" @error="onImg" />
            <div class="play-mask"><el-icon><VideoPlay /></el-icon></div>
          </div>
          <div class="pl-name">{{ pl.name }}</div>
          <div class="pl-sub">{{ pl.songCount }} 首 · {{ pl.creatorName }}</div>
        </div>
      </div>
      <el-empty v-if="!collected.length" description="还没有收藏歌单" />
    </div>

    <!-- 播放记录 -->
    <div v-if="tab === 'records'" class="panel">
      <div class="panel-head">
        <span class="panel-count">{{ records.length }} 条记录</span>
      </div>
      <div class="track-list">
        <div v-for="r in records" :key="r.id" class="track-row">
          <img :src="r.coverUrl || fallbackCover" class="t-cover" @error="onImg" @click="$router.push(`/song/${r.songId}`)" />
          <div class="t-main" @click="$router.push(`/song/${r.songId}`)">
            <div class="t-name">{{ r.songName }}</div>
            <div class="t-sub">{{ r.singerName }} · {{ r.device }} · {{ (r.createTime || '').replace('T', ' ').slice(5, 16) }}</div>
          </div>
          <button class="t-btn" @click="playRecord(r)"><el-icon><VideoPlay /></el-icon></button>
        </div>
        <el-empty v-if="!records.length" description="暂无播放记录" />
      </div>
    </div>

    <!-- 登录记录 -->
    <div v-if="tab === 'logins'" class="panel">
      <div class="km-card" style="padding: 8px 12px">
        <el-table :data="loginLogs" style="width: 100%">
          <el-table-column label="设备" width="120">
            <template #default="{ row }"><span class="pill">{{ row.device }}</span></template>
          </el-table-column>
          <el-table-column prop="ip" label="IP 地址" width="160" />
          <el-table-column prop="loginType" label="登录方式" width="130" />
          <el-table-column prop="userAgent" label="客户端" show-overflow-tooltip />
          <el-table-column label="登录时间" width="180">
            <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loginLogs.length" description="暂无登录记录" :image-size="80" />
      </div>
    </div>

    <!-- 账号安全 -->
    <div v-if="tab === 'security'" class="panel">
      <div class="security-card">
        <h3 class="sec-title">修改密码</h3>
        <p class="sec-desc">修改后需要使用新密码重新登录全部设备</p>
        <el-form label-position="top" style="max-width: 380px">
          <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password size="large" /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password size="large" /></el-form-item>
        </el-form>
        <div class="sec-actions">
          <el-button type="primary" round @click="changePwd">修改密码</el-button>
          <el-button round @click="kickOthers">踢出其他设备</el-button>
        </div>
      </div>
    </div>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="profileDialog" title="编辑资料" width="460px">
      <el-form label-position="top">
        <el-form-item label="昵称"><el-input v-model="profile.nickname" size="large" /></el-form-item>
        <el-form-item label="头像">
          <AvatarUploader v-model="profile.avatar" biz-type="avatar" fallback-text="我" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="profile.gender" placeholder="选择性别" style="width: 100%">
            <el-option :value="0" label="保密" />
            <el-option :value="1" label="男" />
            <el-option :value="2" label="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="profile.birthday" type="date" value-format="YYYY-MM-DD" placeholder="选择生日" style="width: 100%" />
        </el-form-item>
        <el-form-item label="地区"><el-input v-model="profile.region" placeholder="如 广东 深圳" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="profile.email" placeholder="name@example.com" /></el-form-item>
        <el-form-item label="个人简介"><el-input v-model="profile.intro" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialog = false">取消</el-button>
        <el-button type="primary" round @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { likeApi, playlistApi, recordApi, userApi, authApi } from '@/api'
import AvatarUploader from '@/components/AvatarUploader.vue'
import { usePlayerStore } from '@/stores/player'
import { useUserStore } from '@/stores/user'

const player = usePlayerStore()
const userStore = useUserStore()

const tab = ref('likes')
const likes = ref([])
const myPlaylists = ref([])
const collected = ref([])
const records = ref([])
const loginLogs = ref([])
const profileDialog = ref(false)
const profile = reactive({ nickname: '', avatar: '', intro: '', gender: 0, birthday: '', region: '', email: '' })
const pwd = reactive({ oldPassword: '', newPassword: '' })

const roles = ref((userStore.user?.roles || 'USER').split(','))

const tabs = [
  { name: 'likes', label: '我喜欢的音乐', icon: 'Star' },
  { name: 'playlists', label: '我的歌单', icon: 'Files' },
  { name: 'collected', label: '收藏歌单', icon: 'Collection' },
  { name: 'records', label: '播放记录', icon: 'Clock' },
  { name: 'logins', label: '登录记录', icon: 'Monitor' },
  { name: 'security', label: '账号安全', icon: 'Lock' }
]

const fallbackAvatar = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120"><rect width="120" height="120" fill="#1a1c21"/><text x="60" y="76" font-size="42" text-anchor="middle" fill="#3a3d45">🐱</text></svg>'
)
const fallbackCover = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120"><rect width="120" height="120" fill="#1a1c21"/><text x="60" y="74" font-size="32" text-anchor="middle" fill="#33363e">♪</text></svg>'
)

const roleMap = { USER: '普通用户', ANCHOR: '主播', MUSICIAN: '音乐人', VIP: 'VIP', ADVERTISER: '广告制作人', ADMIN: '管理员' }
const roleName = (r) => roleMap[r] || r

function onImg(e) {
  e.target.src = e.target.classList.contains('hero-avatar') ? fallbackAvatar : fallbackCover
}

function openProfile() {
  Object.assign(profile, {
    nickname: userStore.user?.nickname,
    avatar: userStore.user?.avatar,
    intro: userStore.user?.intro || '',
    gender: Number(userStore.user?.gender || 0),
    birthday: userStore.user?.birthday || '',
    region: userStore.user?.region || '',
    email: userStore.user?.email || ''
  })
  profileDialog.value = true
}

const toTrack = (s) => ({
  id: s.songId, name: s.songName, singerName: s.singerName,
  coverUrl: s.coverUrl, audioUrl: s.audioUrl, duration: s.duration
})

function playLike(s) {
  player.play(toTrack(s), likes.value.map(toTrack))
}

function playAllLikes() {
  if (!likes.value.length) return
  player.play(toTrack(likes.value[0]), likes.value.map(toTrack))
}

function playRecord(r) {
  player.play({ id: r.songId, name: r.songName, singerName: r.singerName, coverUrl: r.coverUrl, audioUrl: r.audioUrl, duration: r.duration })
}

async function unlike(s) {
  await likeApi.unlike(s.songId)
  likes.value = likes.value.filter((x) => x.id !== s.id)
  ElMessage.success('已取消喜欢')
}

async function loadTab(t) {
  if (t === 'likes') likes.value = (await likeApi.mine({ pageNum: 1, pageSize: 100 })).data?.records || []
  if (t === 'playlists') myPlaylists.value = (await playlistApi.mine({ pageNum: 1, pageSize: 100 })).data?.records || []
  if (t === 'collected') collected.value = (await playlistApi.collected({ pageNum: 1, pageSize: 100 })).data?.records || []
  if (t === 'records') records.value = (await recordApi.recent(50)).data || []
  if (t === 'logins') loginLogs.value = (await userApi.loginLogs({ pageNum: 1, pageSize: 20 })).data?.records || []
}

function switchTab(name) {
  tab.value = name
  loadTab(name)
}

async function saveProfile() {
  await userApi.updateMe({ ...profile })
  Object.assign(userStore.user, profile)
  localStorage.setItem('km_user', JSON.stringify(userStore.user))
  ElMessage.success('资料已更新')
  profileDialog.value = false
}

async function changePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) return ElMessage.warning('请填写完整')
  await authApi.changePassword(pwd)
  ElMessage.success('密码修改成功，请重新登录')
  await userStore.logout()
  location.hash = '#/login'
}

async function kickOthers() {
  const res = await authApi.kickOthers()
  ElMessage.success(`已踢出 ${res.data} 个设备`)
}

onMounted(async () => {
  const me = await userApi.me()
  userStore.user = { ...userStore.user, ...me.data }
  localStorage.setItem('km_user', JSON.stringify(userStore.user))
  roles.value = (userStore.user.roles || 'USER').split(',')
  await loadTab('likes')
})
</script>

<style scoped>
/* 个人头图 */
.profile-hero {
  position: relative;
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 30px 34px;
  border-radius: 28px;
  margin-bottom: 28px;
  overflow: hidden;
  background: linear-gradient(130deg, rgba(192, 132, 252, .13), rgba(255, 255, 255, .02) 55%, transparent);
  border: 1px solid var(--km-border);
}
.hero-blur {
  position: absolute;
  left: -60px;
  bottom: -100px;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(192, 132, 252, .16), transparent 65%);
}
.hero-avatar {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(255, 255, 255, .1);
  position: relative;
  z-index: 1;
}
.hero-info { flex: 1; position: relative; z-index: 1; }
.hero-name { margin: 0 0 10px; font-size: 26px; font-weight: 900; letter-spacing: -.6px; }
.hero-roles { display: flex; gap: 7px; flex-wrap: wrap; }
.role-tag {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, .07);
  color: rgba(243, 244, 246, .7);
}
.role-tag.vip { background: linear-gradient(90deg, #c9971f, #f7d774); color: #2a1f00; }
.role-tag.admin { background: rgba(255, 77, 109, .18); color: #ff8095; }
.role-tag.musician { background: rgba(192, 132, 252, .16); color: var(--km-accent); }
.role-tag.anchor { background: rgba(96, 190, 255, .16); color: #7cc7ff; }
.hero-meta { margin: 10px 0 0; font-size: 12px; color: var(--km-text-faint); }

.edit-btn {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid var(--km-border-strong);
  background: rgba(255, 255, 255, .04);
  color: var(--km-text);
  font-size: 13px;
  cursor: pointer;
  transition: all .2s;
}
.edit-btn:hover { background: rgba(255, 255, 255, .1); border-color: rgba(255, 255, 255, .3); }

/* 标签页 */
.tabs-row {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  overflow-x: auto;
  scrollbar-width: none;
  padding-bottom: 2px;
}
.tabs-row::-webkit-scrollbar { display: none; }

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--km-text-dim);
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all .2s;
}
.tab-btn:hover { color: var(--km-text); background: rgba(255, 255, 255, .05); }
.tab-btn.active {
  background: var(--km-accent);
  color: #07080a;
  border-color: var(--km-accent);
  font-weight: 800;
  box-shadow: 0 8px 22px rgba(192, 132, 252, .2);
}

/* 面板 */
.panel {
  border-radius: 24px;
  border: 1px solid var(--km-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, .03), rgba(255, 255, 255, .006));
  padding: 18px;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px 14px;
}
.panel-count { font-size: 13px; color: var(--km-text-dim); }

.btn-mini {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--km-border-strong);
  background: transparent;
  color: var(--km-text);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all .18s;
}
.btn-mini:hover { background: var(--km-accent); color: #07080a; border-color: var(--km-accent); }
.btn-mini:disabled { opacity: .4; cursor: not-allowed; }

/* 歌曲行 */
.track-list { display: flex; flex-direction: column; gap: 2px; }

.track-row {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 9px 12px;
  border-radius: 13px;
  transition: background .18s;
}
.track-row:hover { background: rgba(255, 255, 255, .05); }
.t-idx {
  width: 26px;
  text-align: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--km-text-faint);
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}
.t-cover { width: 46px; height: 46px; border-radius: 11px; object-fit: cover; flex-shrink: 0; }
.t-main { flex: 1; min-width: 0; }
.t-name { font-size: 14px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.t-sub { font-size: 12px; color: var(--km-text-dim); margin-top: 3px; }

.t-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--km-border);
  background: transparent;
  color: var(--km-text-dim);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all .18s;
  flex-shrink: 0;
}
.t-btn:hover { color: #07080a; background: var(--km-accent); border-color: var(--km-accent); }
.t-btn.danger:hover { background: #ff4d6d; border-color: #ff4d6d; color: #fff; }

/* 歌单网格 */
.pl-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 18px; padding: 4px; }
.pl-card { cursor: pointer; }
.pl-cover { width: 100%; aspect-ratio: 1; object-fit: cover; display: block; }
.pl-name { margin-top: 10px; font-size: 13.5px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.pl-sub { font-size: 12px; color: var(--km-text-dim); margin-top: 3px; }

/* 安全 */
.security-card { padding: 10px 12px 20px; }
.sec-title { margin: 0 0 6px; font-size: 17px; font-weight: 800; }
.sec-desc { margin: 0 0 20px; font-size: 12.5px; color: var(--km-text-dim); }
.sec-actions { display: flex; gap: 10px; margin-top: 6px; }

.pill {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, .07);
  color: var(--km-text-dim);
}

@media (max-width: 900px) {
  .profile-hero { flex-wrap: wrap; }
  .pl-grid { grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); }
}
</style>

