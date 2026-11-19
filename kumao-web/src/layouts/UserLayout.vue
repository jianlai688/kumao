<template>
  <div class="user-layout" :class="{ 'menu-open': menuOpen }" @keydown.esc="menuOpen = false">
    <button
      v-if="menuOpen"
      class="menu-scrim"
      aria-label="关闭导航菜单"
      @click="menuOpen = false"
    ></button>

    <!-- 左侧导航 -->
    <aside id="user-side-nav" class="side" :class="{ open: menuOpen }" aria-label="侧边导航">
      <router-link to="/" class="brand" aria-label="酷猫音乐首页">
        <span class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 36 36" role="img">
            <path d="M9.6 11.2 7.3 7.1l4.9 1.2M26.4 11.2l2.3-4.1-4.9 1.2" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M8.2 13.6a9.8 9.8 0 0 1 19.6 0v5.1a8.9 8.9 0 0 1-8.9 8.9h-1.8a8.9 8.9 0 0 1-8.9-8.9v-5.1Z" fill="#fff" stroke="currentColor" stroke-width="1.7"/>
            <path d="M12.3 16.2v4.6M8.6 14.3v6.1M23.7 16.2v4.6M27.4 14.3v6.1" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
            <path d="M14.8 22.8c1.6 1.2 4.8 1.2 6.4 0" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
            <circle cx="14.7" cy="19.7" r=".9" fill="currentColor"/>
            <circle cx="21.3" cy="19.7" r=".9" fill="currentColor"/>
          </svg>
        </span>
        <span class="brand-text">
          <b>酷猫音乐</b>
          <small>KUMAO MUSIC</small>
        </span>
      </router-link>

      <button class="side-close" aria-label="关闭导航" @click="menuOpen = false">
        <KmIcon name="back" :size="18" />
      </button>

      <nav class="side-nav" aria-label="主要导航">
        <router-link
          v-for="item in navMain"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          :aria-current="isActive(item.path) ? 'page' : undefined"
          :aria-label="item.label"
          @click="menuOpen = false"
        >
          <KmIcon class="ico" :name="item.icon" :size="18" />
          <span class="nav-label">{{ item.label }}</span>
          <span v-if="item.badge && liveCount > 0" class="nav-badge">{{ liveCount }}</span>
        </router-link>
      </nav>

      <div class="side-group-label">我的</div>
      <nav class="side-nav" aria-label="我的音乐">
        <router-link
          to="/mine"
          class="nav-item"
          :class="{ active: isActive('/mine') }"
          aria-label="我的音乐"
          @click="menuOpen = false"
        >
          <KmIcon class="ico" name="heart" :size="18" />
          <span class="nav-label">我的音乐</span>
        </router-link>
        <router-link
          to="/vip"
          class="nav-item"
          :class="{ active: isActive('/vip') }"
          aria-label="会员中心"
          @click="menuOpen = false"
        >
          <KmIcon class="ico" name="crown" :size="18" />
          <span class="nav-label">会员中心</span>
        </router-link>
      </nav>

      <div class="side-foot">
        <router-link to="/vip" class="vip-teaser" aria-label="开通酷猫 VIP">
          <span class="vip-label"><KmIcon name="crown" :size="14" /> 酷猫 VIP</span>
          <span class="vip-title">把现场留在耳边</span>
          <span class="vip-sub">Hi-Res 无损 · 专属弹幕</span>
          <span class="vip-action">查看会员权益 <KmIcon name="chevron-right" :size="12" /></span>
        </router-link>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main">
      <header class="topbar">
        <button class="menu-btn" aria-label="打开导航菜单" aria-controls="user-side-nav" :aria-expanded="menuOpen" @click="menuOpen = true">
          <KmIcon name="list" :size="20" />
        </button>

        <div class="topbar-left">
          <button class="nav-arrow" aria-label="返回上一页" @click="$router.back()">
            <KmIcon name="back" :size="20" />
          </button>
          <button class="nav-arrow" aria-label="前进下一页" @click="$router.forward()">
            <KmIcon name="forward" :size="20" />
          </button>
        </div>

        <div class="search-wrap">
          <el-input
            ref="searchRef"
            v-model="keyword"
            placeholder="搜索音乐、歌手、歌单、直播间"
            class="search-input"
            clearable
            aria-label="搜索音乐、歌手、歌单、直播间"
            @keyup.enter="doSearch"
          >
            <template #prefix><KmIcon name="search" :size="18" /></template>
            <template #suffix><span class="search-kbd">Ctrl K</span></template>
          </el-input>
        </div>

        <div class="topbar-right">
          <button class="app-entry-btn" type="button" title="下载手机 App" @click="downloadOpen = true">
            <KmIcon name="download" :size="16" />
            <span>下载 App</span>
          </button>

          <el-dropdown trigger="click" popper-class="km-user-pop" @command="onCreatorCommand">
            <button type="button" class="creator-entry" aria-label="打开创作者服务菜单">
              <KmIcon name="upload" :size="16" />
              <span>创作者中心</span>
              <KmIcon class="creator-caret" name="chevron-down" :size="13" />
            </button>
            <template #dropdown>
              <el-dropdown-menu class="creator-menu">
                <el-dropdown-item v-for="item in creatorNav" :key="item.path" :command="item.path">
                  <el-icon><component :is="item.icon" /></el-icon>
                  <span>{{ item.label }}</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <router-link v-if="userStore.isLogin" to="/live" class="topbar-live" aria-label="进入音乐直播">
            <span class="live-signal" aria-hidden="true"></span>
            直播
          </router-link>

          <template v-if="userStore.isLogin">
            <button class="icon-btn theme-btn" aria-label="主题设置" title="主题设置" @click="themeOpen = true">
              <KmIcon name="sun" :size="18" />
            </button>
            <MessageBell />
            <el-dropdown @command="onCommand" trigger="click" popper-class="km-user-pop">
              <span class="user-chip">
                <el-avatar :size="30" :src="userStore.user?.avatar" class="user-avatar">
                  {{ (userStore.user?.nickname || 'U').slice(0, 1) }}
                </el-avatar>
                <span class="user-name">{{ userStore.user?.nickname || userStore.user?.username }}</span>
                <KmIcon class="caret" name="chevron-down" :size="13" />
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="mine">我的音乐</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.hasRole('MUSICIAN')" command="musician">音乐人工作台</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.hasRole('ANCHOR')" command="anchor">主播工作台</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.hasRole('ADVERTISER')" command="advertiser">广告工作台</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>管理后台</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>

          <template v-else>
            <button class="text-action" @click="$router.push('/login')">登录</button>
            <button class="register-btn" @click="$router.push('/register')">免费注册</button>
          </template>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="km-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <PlayerBar />
    <ThemeDialog v-model="themeOpen" />
    <AppDownload v-model="downloadOpen" />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { liveApi } from '@/api'
import PlayerBar from '@/components/PlayerBar.vue'
import ThemeDialog from '@/components/ThemeDialog.vue'
import MessageBell from '@/components/MessageBell.vue'
import KmIcon from '@/components/KmIcon.vue'
import AppDownload from '@/components/AppDownload.vue'

if (typeof document !== 'undefined') document.body.classList.add('km-user-theme')

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeOpen = ref(false)
const downloadOpen = ref(false)
const keyword = ref('')
const searchRef = ref(null)
const menuOpen = ref(false)
const liveCount = ref(0)

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path === path || route.path.startsWith(`${path}/`)
}

function handleEscape(e) {
  if (e.key === 'Escape') menuOpen.value = false
}

function focusSearch(e) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    searchRef.value?.focus()
  }
}

async function loadLiveCount() {
  try {
    const res = await liveApi.square({ pageNum: 1, pageSize: 1 })
    liveCount.value = Number(res.data?.total) || 0
  } catch (e) {
    liveCount.value = 0
  }
}

onMounted(() => {
  window.addEventListener('keydown', focusSearch)
  window.addEventListener('keydown', handleEscape)
  loadLiveCount()
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', focusSearch)
  window.removeEventListener('keydown', handleEscape)
})

const navMain = [
  { path: '/', label: '发现音乐', icon: 'compass' },
  { path: '/rank/hot', label: '排行榜', icon: 'rank' },
  { path: '/singers', label: '歌手', icon: 'user' },
  { path: '/playlists', label: '歌单广场', icon: 'playlist' },
  { path: '/mv', label: 'MV 专区', icon: 'mv' },
  { path: '/live', label: '音乐直播', icon: 'live', badge: true },
  { path: '/musicians', label: '音乐人', icon: 'mic' }
]

const creatorNav = [
  { path: '/apply/musician', label: '音乐人认证', icon: 'Microphone' },
  { path: '/apply/anchor', label: '主播认证', icon: 'VideoCamera' },
  { path: '/apply/advertiser', label: '广告合作', icon: 'Money' }
]

function doSearch() {
  if (!keyword.value.trim()) return
  router.push({ path: '/search', query: { keyword: keyword.value.trim() } })
  menuOpen.value = false
}

function onCreatorCommand(path) {
  menuOpen.value = false
  router.push(path)
}

async function onCommand(cmd) {
  if (cmd === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } else if (cmd === 'mine') {
    router.push('/mine')
  } else if (cmd === 'admin') {
    router.push('/admin')
  } else {
    router.push(`/studio/${cmd}`)
  }
}
</script>

<style scoped>
.user-layout {
  display: flex;
  min-height: 100vh;
  background: var(--km-bg);
  color: var(--km-text);
}

/* ---------------- 侧边栏 ---------------- */
.side {
  width: var(--km-sidebar-w);
  flex-shrink: 0;
  position: fixed;
  inset: 0 auto 0 0;
  display: flex;
  flex-direction: column;
  padding: 20px 12px calc(var(--km-player-h) + 8px);
  border-right: 1px solid #eee9f3;
  background: #fff;
  z-index: 120;
  overflow-y: auto;
  scrollbar-width: none;
}
.side::-webkit-scrollbar { display: none; }

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 7px 14px;
  font-weight: 700;
}
.brand-mark {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  color: #d35fe4;
  background: #fff5fd;
  border: 1px solid #f3c8ee;
  border-radius: 11px;
}
.brand-mark svg { width: 30px; height: 30px; display: block; }
.brand-text { display: flex; flex-direction: column; min-width: 0; gap: 2px; }
.brand-text b {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -.02em;
  color: #1d1726;
  line-height: 1.15;
  white-space: nowrap;
}
.brand-text small {
  font-size: 10px;
  color: #776d83;
  letter-spacing: .15em;
  line-height: 1.2;
  white-space: nowrap;
  font-weight: 500;
}

.side-close { display: none; }

.side-nav { display: flex; flex-direction: column; gap: 3px; }

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 38px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 9px;
  font-size: 13.5px;
  font-weight: 500;
  color: #4f4759;
  transition: color .16s, background .16s, border-color .16s;
}
.nav-item:hover { color: #1d1726; background: #faf7fd; }
.nav-item.active {
  color: #241b2e;
  background: #f8f0ff;
  border-color: #efdcff;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, .6);
}
.nav-item.active::before {
  content: '';
  position: absolute;
  left: -13px;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 0 4px 4px 0;
  background: #cf5ce5;
}
.nav-item .ico {
  color: #776d83;
  transition: color .16s;
}
.nav-item.active .ico { color: #9d3dd8; }
.nav-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nav-badge {
  margin-left: auto;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fce7f3;
  color: #c0266b;
  font-size: 10px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.side-group-label {
  padding: 0 10px;
  margin: 14px 0 7px;
  color: #776d83;
  font-size: 11.5px;
  font-weight: 500;
}

.side-foot { margin-top: auto; padding: 22px 0 2px; }

.vip-teaser {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 5px;
  overflow: hidden;
  border-radius: 14px;
  padding: 14px 13px 12px;
  background: linear-gradient(140deg, #fff6fd 0%, #f9efff 56%, #f4ebff 100%);
  border: 1px solid #f1dded;
  transition: border-color .2s, transform .2s;
}
.vip-teaser::after {
  content: '';
  position: absolute;
  width: 120px;
  height: 120px;
  right: -56px;
  top: -58px;
  border-radius: 50%;
  border: 1px solid rgba(206, 132, 244, .22);
  pointer-events: none;
}
.vip-teaser:hover { border-color: #e8c6ea; transform: translateY(-1px); }
.vip-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 600;
  color: #a54ad0;
  letter-spacing: .02em;
}
.vip-title { font-size: 13.5px; font-weight: 600; color: #2c2235; line-height: 1.35; }
.vip-sub { font-size: 11px; color: #756b81; line-height: 1.5; white-space: nowrap; }
.vip-action {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-top: 4px;
  font-size: 11px;
  color: #766083;
}

/* ---------------- 主区域 ---------------- */
.main {
  flex: 1;
  min-width: 0;
  margin-left: var(--km-sidebar-w);
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 100;
  height: var(--km-topbar-h);
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 34px;
  background: #fff;
  border-bottom: 1px solid #f0ebf5;
}

.topbar-left { display: flex; gap: 2px; }
.nav-arrow {
  width: 28px;
  height: 32px;
  border-radius: 7px;
  border: 0;
  background: transparent;
  color: #5c5364;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color .16s, background .16s;
}
.nav-arrow:hover { color: #1d1726; background: #f8f4fb; }

.menu-btn {
  display: none;
  width: 36px;
  height: 36px;
  border-radius: 9px;
  border: 1px solid var(--km-border);
  background: #fff;
  color: var(--km-text);
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.search-wrap { flex: 0 1 434px; width: 434px; min-width: 200px; margin-left: 10px; }
.search-input :deep(.el-input__wrapper) {
  border-radius: 9px !important;
  height: 38px;
  padding: 0 10px 0 13px;
  background: #f7f4fa !important;
  box-shadow: none !important;
  transition: background .16s, box-shadow .16s;
}
.search-input :deep(.el-input__wrapper:hover) { background: #f4f0f8 !important; }
.search-input :deep(.el-input__wrapper.is-focus) {
  background: #fff !important;
  box-shadow: 0 0 0 1px #d9c3ef inset !important;
}
.search-input :deep(.el-input__inner) { font-size: 13px; color: #2c2435; }
.search-input :deep(.el-input__inner::placeholder) { color: #6f657b; }
.search-input :deep(.el-input__prefix) { color: #655c6f; margin-right: 2px; }
.search-kbd {
  font-size: 10.5px;
  color: #6f657b;
  background: #fff;
  border: 1px solid #e9e2f0;
  border-radius: 5px;
  padding: 2px 6px;
  line-height: 1.35;
  white-space: nowrap;
}

.topbar-right { margin-left: auto; display: flex; align-items: center; gap: 8px; }
.creator-entry {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 7px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #4e4659;
  font-size: 12.5px;
  cursor: pointer;
  transition: color .16s, background .16s;
}
.creator-entry:hover { color: #1d1726; background: #f8f4fb; }
.creator-entry .km-icon { color: #5f5669; }
.creator-caret { color: #776d83 !important; }
.creator-menu :deep(.el-dropdown-menu__item) { gap: 8px; font-size: 12.5px; }
.creator-menu :deep(.el-dropdown-menu__item .el-icon) { color: var(--km-text-faint); }

.topbar-live {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 13px 0 8px;
  border-right: 1px solid #f0ebf5;
  color: #514858;
  font-size: 12.5px;
  white-space: nowrap;
  transition: color .16s;
}
.topbar-live:hover { color: #1d1726; }
.live-signal {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ef3d6f;
  box-shadow: 0 0 0 3px #fde8ef;
}

.icon-btn {
  position: relative;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 0;
  background: transparent;
  color: #514858;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color .16s, background .16s;
}
.icon-btn:hover { color: #1d1726; background: #f8f4fb; }

.app-entry-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 10px;
  border-radius: 8px;
  border: 1px solid #ecdff8;
  background: #faf5ff;
  color: #7a3fae;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background .16s, border-color .16s;
}
.app-entry-btn:hover { background: #f3e8ff; border-color: #d9bdf0; }
.bell-dot {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ef3d6f;
  border: 1.5px solid #fff;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 5px 3px 3px;
  border-radius: 999px;
  background: transparent;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background .16s, border-color .16s;
}
.user-chip:hover { background: #faf7fd; border-color: #efe9f5; }
.user-avatar {
  background: linear-gradient(135deg, #9b33d1 0%, #d538be 58%, #f35a9f 100%);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}
.user-name {
  font-size: 12.5px;
  color: #3d3446;
  max-width: 86px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.caret { color: #776d83; }

.text-action {
  height: 34px;
  padding: 0 9px;
  border: 0;
  background: transparent;
  color: #514858;
  font-size: 13px;
  cursor: pointer;
}
.text-action:hover { color: #1d1726; }
.register-btn {
  height: 34px;
  padding: 0 15px;
  border-radius: 9px;
  border: 1px solid #e3cff4;
  background: #f8efff;
  color: #7626ab;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background .16s, border-color .16s;
}
.register-btn:hover { background: #f1e2ff; border-color: #d4b2eb; }

.content { flex: 1; min-height: 0; padding-bottom: var(--km-player-h); }

.menu-scrim { display: none; }

/* 页面切换动画 */
.km-fade-enter-active, .km-fade-leave-active { transition: opacity .16s ease, transform .16s ease; }
.km-fade-enter-from { opacity: 0; transform: translateY(5px); }
.km-fade-leave-to { opacity: 0; }

@media (max-width: 1180px) {
  .user-layout { --km-sidebar-w: 76px; }
  .side { width: 76px; padding: 18px 8px 14px; align-items: stretch; }
  .brand { justify-content: center; padding: 2px 0 14px; }
  .brand-text, .nav-label, .side-group-label, .side-foot, .nav-badge { display: none; }
  .nav-item { justify-content: center; padding: 0; height: 42px; }
  .nav-item.active::before { left: -9px; top: 10px; bottom: 10px; }
  .nav-item .ico { width: 19px; height: 19px; }
  .main { margin-left: 76px; }
  .topbar { padding: 0 24px; }
}

@media (max-width: 860px) {
  .creator-entry span, .creator-caret { display: none; }
  .creator-entry { width: 32px; padding: 0; justify-content: center; }
  .side {
    width: 244px;
    padding: 18px 12px 14px;
    transform: translateX(-100%);
    transition: transform .22s ease;
  }
  .side.open { transform: translateX(0); }
  .brand { justify-content: flex-start; padding: 2px 7px 14px; }
  .brand-text, .nav-label, .side-group-label, .side-foot, .nav-badge { display: flex; }
  .side-group-label { display: block; }
  .side-foot { display: block; }
  .nav-item { justify-content: flex-start; padding: 0 10px; height: 42px; }
  .nav-item.active::before { left: -13px; top: 8px; bottom: 8px; }
  .side-close {
    display: flex;
    position: absolute;
    top: 22px;
    right: 12px;
    width: 30px;
    height: 30px;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--km-border);
    border-radius: 8px;
    background: #fff;
    color: var(--km-text-dim);
  }
  .main { margin-left: 0; }
  .menu-scrim {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 110;
    border: 0;
    background: rgba(31, 20, 42, .38);
    cursor: pointer;
  }
  .menu-btn { display: flex; }
  .topbar { gap: 10px; padding: 0 16px; }
  .topbar-left { display: none; }
  .search-wrap { flex: 1; width: auto; min-width: 0; margin-left: 0; }
  .search-kbd { display: none; }
  .topbar-live { display: none; }
  .user-name { display: none; }
  .content { padding-bottom: var(--km-player-h); }
}

@media (max-width: 620px) {
  .topbar { height: 58px; }
  .theme-btn, .creator-entry { display: none; }
  .app-entry-btn span { display: none; }
  .app-entry-btn { padding: 0 8px; }
  .user-chip { padding-right: 2px; }
  .text-action { display: none; }
  .register-btn { height: 32px; padding: 0 11px; font-size: 12px; }
  .search-input :deep(.el-input__wrapper) { height: 36px; }
  .search-input :deep(.el-input__inner) { font-size: 12.5px; }
  .topbar-right { gap: 3px; }
}
</style>
