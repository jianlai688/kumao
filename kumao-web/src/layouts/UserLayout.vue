<template>
  <div class="user-layout" :class="{ 'menu-open': menuOpen }" @keydown.esc="menuOpen = false">
    <AdSplash />
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
          <svg viewBox="0 0 40 40" role="img">
            <defs>
              <linearGradient id="kmBrandGradient" x1="4" y1="2" x2="36" y2="38" gradientUnits="userSpaceOnUse">
                <stop stop-color="#a78bfa" /> <!-- taste-skill-ignore: brand mark -->
                <stop offset=".55" stop-color="#e879f9" />
                <stop offset="1" stop-color="#fda4af" />
              </linearGradient>
            </defs>
            <circle cx="20" cy="21" r="12.5" fill="none" stroke="url(#kmBrandGradient)" stroke-width="2" />
            <circle cx="20" cy="21" r="4.2" fill="url(#kmBrandGradient)" />
            <path d="M8.5 12.5 6 5.2l8 3.1M31.5 12.5 34 5.2l-8 3.1" fill="none" stroke="url(#kmBrandGradient)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            <circle cx="20" cy="21" r="16.5" fill="none" stroke="rgba(255,255,255,.10)" stroke-width="1" />
          </svg>
        </span>
        <span class="brand-text">
          <b>酷猫音乐</b>
          <small>KUMAO MUSIC</small>
        </span>
      </router-link>

      <button class="side-close" aria-label="关闭导航" @click="menuOpen = false">
        <el-icon><Close /></el-icon>
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
          :title="item.label"
          @click="menuOpen = false"
        >
          <el-icon class="ico"><component :is="item.icon" /></el-icon>
          <span class="nav-label">{{ item.label }}</span>
          <span v-if="item.dot" class="nav-dot" aria-hidden="true"></span>
        </router-link>
      </nav>

      <div class="side-group-label">我的</div>
      <nav class="side-nav" aria-label="我的音乐">
        <router-link
          to="/mine"
          class="nav-item"
          :class="{ active: isActive('/mine') }"
          aria-label="我的音乐"
          title="我的音乐"
          @click="menuOpen = false"
        >
          <el-icon class="ico"><Star /></el-icon><span class="nav-label">我的音乐</span>
        </router-link>
        <router-link
          to="/vip"
          class="nav-item"
          :class="{ active: isActive('/vip') }"
          aria-label="会员中心"
          title="会员中心"
          @click="menuOpen = false"
        >
          <el-icon class="ico"><Medal /></el-icon><span class="nav-label">会员中心</span>
        </router-link>
      </nav>

      <div class="side-foot">
        <router-link to="/vip" class="vip-teaser" aria-label="开通酷猫 VIP">
          <span class="vip-label">酷猫 VIP</span>
          <span class="vip-title">把现场留在耳边</span>
          <span class="vip-sub">高清 MV · 专属弹幕 · 入场特效</span>
          <span class="vip-action">查看会员权益 <el-icon><ArrowRight /></el-icon></span>
        </router-link>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main">
      <header class="topbar">
        <button class="menu-btn" aria-label="打开导航菜单" aria-controls="user-side-nav" :aria-expanded="menuOpen" @click="menuOpen = true">
          <el-icon><Menu /></el-icon>
        </button>

        <div class="topbar-left">
          <button class="nav-arrow" aria-label="返回上一页" @click="$router.back()">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <button class="nav-arrow" aria-label="前进下一页" @click="$router.forward()">
            <el-icon><ArrowRight /></el-icon>
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
            <template #prefix><el-icon><Search /></el-icon></template>
            <template #suffix><span class="search-kbd">Ctrl K</span></template>
          </el-input>
        </div>

        <div class="topbar-right">
          <el-dropdown trigger="click" @command="onCreatorCommand">
            <button type="button" class="creator-entry" aria-label="打开创作者服务菜单">
              <el-icon><Microphone /></el-icon>
              <span>创作者</span>
              <el-icon class="creator-caret"><ArrowDown /></el-icon>
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
            <button class="icon-btn" aria-label="我的音乐" @click="$router.push('/mine')">
              <el-icon><Bell /></el-icon>
            </button>
            <el-dropdown @command="onCommand" trigger="click">
              <span class="user-chip">
                <el-avatar :size="30" :src="userStore.user?.avatar">
                  {{ (userStore.user?.nickname || 'U').slice(0, 1) }}
                </el-avatar>
                <span class="user-name">{{ userStore.user?.nickname || userStore.user?.username }}</span>
                <el-icon class="caret"><ArrowDown /></el-icon>
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
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, ArrowRight, Bell, Close, Menu, Microphone, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import PlayerBar from '@/components/PlayerBar.vue'
import AdSplash from '@/components/AdSplash.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const keyword = ref('')
const searchRef = ref(null)
const menuOpen = ref(false)

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

onMounted(() => {
  window.addEventListener('keydown', focusSearch)
  window.addEventListener('keydown', handleEscape)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', focusSearch)
  window.removeEventListener('keydown', handleEscape)
})

const navMain = [
  { path: '/', label: '发现音乐', icon: 'Compass' },
  { path: '/rank/hot', label: '排行榜', icon: 'TrendCharts' },
  { path: '/singers', label: '歌手', icon: 'User' },
  { path: '/playlists', label: '歌单广场', icon: 'Files' },
  { path: '/mv', label: 'MV 专区', icon: 'VideoCamera' },
  { path: '/live', label: '音乐直播', icon: 'VideoPlay', dot: true },
  { path: '/musicians', label: '音乐人', icon: 'Microphone' }
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
}

/* ---------------- 侧边栏 ---------------- */
.side {
  width: var(--km-sidebar-w);
  flex-shrink: 0;
  position: fixed;
  inset: 0 auto 0 0;
  display: flex;
  flex-direction: column;
  padding: 18px 12px 14px;
  border-right: 1px solid var(--km-border);
  background: #0a080e;
  z-index: 120;
  overflow-y: auto;
  scrollbar-width: none;
}
.side::-webkit-scrollbar { display: none; }

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 8px 20px;
  font-weight: 700;
}
.brand-mark {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  display: block;
}
.brand-mark svg { width: 100%; height: 100%; display: block; }
.brand-text { display: flex; flex-direction: column; min-width: 0; gap: 2px; }
.brand-text b {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -.02em;
  color: var(--km-text);
  line-height: 1.15;
  white-space: nowrap;
}
.brand-text small {
  font-size: 11px;
  color: var(--km-text-faint);
  letter-spacing: .12em;
  line-height: 1.2;
  white-space: nowrap;
}

.side-close { display: none; }

.side-nav { display: flex; flex-direction: column; gap: 2px; }

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  height: 38px;
  padding: 0 10px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  color: var(--km-text-dim);
  transition: color .16s, background .16s;
}
.nav-item:hover { color: var(--km-text); background: rgba(255, 255, 255, .045); }
.nav-item.active {
  color: var(--km-text);
  background: rgba(255, 255, 255, .06);
  font-weight: 600;
}
.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 2px;
  border-radius: 2px;
  background: var(--km-grad);
}
.nav-item .ico {
  font-size: 16px;
  color: var(--km-text-faint);
  transition: color .16s;
}
.nav-item.active .ico { color: var(--km-accent); }
.nav-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #fb7185;
  margin-left: auto;
  flex-shrink: 0;
}

.side-group-label {
  padding: 0 10px;
  margin: 18px 0 6px;
  color: var(--km-text-faint);
  font-size: 11px;
  font-weight: 500;
  letter-spacing: .02em;
}
.side-group-label:first-of-type { margin-top: 16px; }

.side-foot { margin-top: auto; padding: 18px 0 2px; }

.vip-teaser {
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow: hidden;
  border-radius: 14px;
  padding: 13px 12px 12px;
  background: #100d16;
  border: 1px solid var(--km-border);
  position: relative;
  transition: border-color .2s, background .2s;
}
.vip-teaser::after {
  content: '';
  position: absolute;
  width: 110px;
  height: 110px;
  right: -48px;
  top: -54px;
  border-radius: 50%;
  border: 1px solid rgba(216, 180, 254, .18);
  pointer-events: none;
}
.vip-teaser:hover { border-color: var(--km-accent-line); background: #14101d; }
.vip-label { font-size: 11px; color: var(--km-accent); letter-spacing: .04em; }
.vip-title { font-size: 13px; font-weight: 600; color: var(--km-text); }
.vip-sub { font-size: 11px; color: var(--km-text-faint); line-height: 1.5; }
.vip-action {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  margin-top: 7px;
  font-size: 11px;
  color: var(--km-text-dim);
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
  gap: 18px;
  padding: 0 30px;
  background: rgba(8, 7, 11, .94);
  border-bottom: 1px solid var(--km-border);
}

.topbar-left { display: flex; gap: 4px; }
.nav-arrow {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--km-text-faint);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color .16s, background .16s;
}
.nav-arrow:hover { color: var(--km-text); background: rgba(255, 255, 255, .05); }

.menu-btn {
  display: none;
  width: 36px;
  height: 36px;
  border-radius: 9px;
  border: 1px solid var(--km-border);
  background: transparent;
  color: var(--km-text);
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.search-wrap { flex: 0 1 400px; min-width: 180px; }
.search-input :deep(.el-input__wrapper) {
  border-radius: 10px !important;
  height: 36px;
  padding: 0 10px 0 12px;
  background: rgba(255, 255, 255, .04) !important;
  box-shadow: 0 0 0 1px var(--km-border) inset !important;
}
.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--km-accent-line) inset !important;
  background: rgba(255, 255, 255, .055) !important;
}
.search-kbd {
  font-size: 11px;
  color: var(--km-text-faint);
  border: 1px solid var(--km-border);
  border-radius: 5px;
  padding: 2px 5px;
  line-height: 1.2;
  white-space: nowrap;
}

.topbar-right { margin-left: auto; display: flex; align-items: center; gap: 8px; }
.creator-entry { display: inline-flex; align-items: center; gap: 6px; height: 30px; padding: 0 9px; border: 1px solid transparent; border-radius: 8px; background: transparent; color: var(--km-text-dim); font-size: 12px; cursor: pointer; transition: color .16s, background .16s, border-color .16s; }
.creator-entry:hover { color: var(--km-text); background: rgba(255, 255, 255, .045); border-color: var(--km-border); }
.creator-entry .el-icon { font-size: 14px; color: var(--km-text-faint); }
.creator-entry:hover .el-icon { color: var(--km-accent); }
.creator-caret { font-size: 10px !important; margin-left: -1px; }
.creator-menu :deep(.el-dropdown-menu__item) { gap: 8px; font-size: 12.5px; }
.creator-menu :deep(.el-dropdown-menu__item .el-icon) { color: var(--km-text-faint); }

.topbar-live {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 10px;
  border-radius: 8px;
  color: var(--km-text-dim);
  font-size: 12px;
  border: 1px solid transparent;
  transition: color .16s, border-color .16s;
}
.topbar-live:hover { color: var(--km-text); border-color: var(--km-border); }
.live-signal {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #fb7185;
  box-shadow: 0 0 0 3px rgba(251, 113, 133, .12);
}

.icon-btn {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--km-text-faint);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color .16s, background .16s;
}
.icon-btn:hover { color: var(--km-text); background: rgba(255, 255, 255, .05); }

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 9px 3px 3px;
  border-radius: 999px;
  background: transparent;
  border: 1px solid transparent;
  cursor: pointer;
  transition: border-color .16s, background .16s;
}
.user-chip:hover { background: rgba(255, 255, 255, .04); border-color: var(--km-border); }
.user-name { font-size: 12px; max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.caret { font-size: 11px; color: var(--km-text-faint); }

.text-action {
  height: 34px;
  padding: 0 10px;
  border: 0;
  background: transparent;
  color: var(--km-text-dim);
  font-size: 13px;
  cursor: pointer;
}
.text-action:hover { color: var(--km-text); }
.register-btn {
  height: 34px;
  padding: 0 16px;
  border-radius: 10px;
  border: 1px solid var(--km-accent-line);
  background: rgba(216, 180, 254, .08);
  color: #f5eaff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background .16s, border-color .16s;
}
.register-btn:hover { background: rgba(216, 180, 254, .14); border-color: rgba(216, 180, 254, .4); }

.content { flex: 1; min-height: 0; padding-bottom: var(--km-player-h); }

.menu-scrim { display: none; }

/* 页面切换动画 */
.km-fade-enter-active, .km-fade-leave-active { transition: opacity .16s ease, transform .16s ease; }
.km-fade-enter-from { opacity: 0; transform: translateY(5px); }
.km-fade-leave-to { opacity: 0; }

@media (max-width: 1180px) {
  :root { --km-sidebar-w: 76px; }
  .side { width: 76px; padding: 16px 8px 12px; align-items: stretch; }
  .brand { justify-content: center; padding: 4px 0 18px; }
  .brand-text, .nav-label, .side-group-label, .side-foot, .nav-dot { display: none; }
  .nav-item { justify-content: center; padding: 0; height: 40px; }
  .nav-item.active::before { top: 12px; bottom: 12px; left: 0; }
  .main { margin-left: 76px; }
  .topbar { padding: 0 22px; }
}

@media (max-width: 860px) {
  .creator-entry span, .creator-caret { display: none; }
  .creator-entry { width: 32px; padding: 0; justify-content: center; }
  .side {
    width: 248px;
    padding: 18px 12px 14px;
    transform: translateX(-100%);
    transition: transform .22s ease;
    
  }
  .side.open { transform: translateX(0); }
  .brand { justify-content: flex-start; padding: 4px 8px 20px; }
  .brand-text, .nav-label, .side-group-label, .side-foot, .nav-dot { display: flex; }
  .side-group-label { display: block; }
  .side-foot { display: block; }
  .nav-item { justify-content: flex-start; padding: 0 10px; height: 42px; }
  .side-close {
    display: flex;
    position: absolute;
    top: 20px;
    right: 12px;
    width: 30px;
    height: 30px;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--km-border);
    border-radius: 8px;
    background: transparent;
    color: var(--km-text-dim);
  }
  .main { margin-left: 0; }
  .menu-scrim {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 110;
    border: 0;
    background: rgba(0, 0, 0, .55);
    cursor: pointer;
  }
  .menu-btn { display: flex; }
  .topbar { gap: 10px; padding: 0 14px; }
  .topbar-left { display: none; }
  .search-wrap { flex: 1; min-width: 0; }
  .search-kbd { display: none; }
  .topbar-live { display: none; }
  .user-name { display: none; }
  .content { padding-bottom: 112px; }
}

@media (max-width: 520px) {
  .topbar { height: 56px; }
  .icon-btn { display: none; }
  .user-chip { padding-right: 3px; }
  .text-action { display: none; }
  .register-btn { height: 32px; padding: 0 12px; font-size: 12px; }
  .search-input :deep(.el-input__wrapper) { height: 34px; }
}
</style>


