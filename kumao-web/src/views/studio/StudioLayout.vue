<template>
  <div class="studio-shell" :class="{ 'nav-open': menuOpen }">
    <button
      v-if="menuOpen"
      class="nav-scrim"
      aria-label="关闭创作者导航"
      @click="menuOpen = false"
    ></button>

    <aside id="studio-side-nav" class="studio-side" aria-label="创作者中心侧边导航">
      <div class="side-brand-row">
        <router-link to="/" class="studio-brand" aria-label="酷猫音乐首页">
          <span class="studio-mark" aria-hidden="true">
            <svg viewBox="0 0 40 40">
              <defs>
                <linearGradient id="studioBrand" x1="4" y1="2" x2="36" y2="38" gradientUnits="userSpaceOnUse">
                  <stop stop-color="#a78bfa" /> <!-- taste-skill-ignore: 酷猫品牌紫粉渐变 -->
                  <stop offset=".55" stop-color="#e879f9" />
                  <stop offset="1" stop-color="#fda4af" />
                </linearGradient>
              </defs>
              <circle cx="20" cy="21" r="12.5" fill="none" stroke="url(#studioBrand)" stroke-width="2" />
              <circle cx="20" cy="21" r="4.2" fill="url(#studioBrand)" />
              <path d="M8.5 12.5 6 5.2l8 3.1M31.5 12.5 34 5.2l-8 3.1" fill="none" stroke="url(#studioBrand)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </span>
          <span class="studio-brand-copy">
            <b>酷猫创作</b>
            <small>CREATOR STUDIO</small>
          </span>
        </router-link>
        <button class="side-close" aria-label="关闭导航" @click="menuOpen = false">
          <el-icon><Close /></el-icon>
        </button>
      </div>

      <div class="side-scope">
        <el-avatar :size="32" :src="userStore.user?.avatar">
          {{ (userStore.user?.nickname || '创作者').slice(0, 1) }}
        </el-avatar>
        <div class="scope-copy">
          <b>{{ userStore.user?.nickname || userStore.user?.username || '创作者' }}</b>
          <span>{{ roleSummary }}</span>
        </div>
      </div>

      <nav class="side-nav" aria-label="创作工作台">
        <span class="nav-heading">工作台</span>
        <router-link
          v-for="item in workspaceLinks"
          :key="item.path"
          :to="item.path"
          class="side-link"
          :class="{ active: isActive(item.path) }"
          :aria-current="isActive(item.path) ? 'page' : undefined"
        >
          <el-icon class="side-icon"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <nav v-if="userStore.hasRole('ANCHOR')" class="side-nav" aria-label="主播工具">
        <span class="nav-heading">主播工具</span>
        <router-link
          to="/studio/earnings"
          class="side-link"
          :class="{ active: isActive('/studio/earnings') }"
          :aria-current="isActive('/studio/earnings') ? 'page' : undefined"
        >
          <el-icon class="side-icon"><Wallet /></el-icon>
          <span>礼物收益</span>
        </router-link>
      </nav>

      <nav class="side-nav" aria-label="账号与创作服务">
        <span class="nav-heading">账号</span>
        <router-link to="/mine" class="side-link" :class="{ active: isActive('/mine') }" active-class="">
          <el-icon class="side-icon"><Star /></el-icon>
          <span>我的音乐</span>
        </router-link>
        <router-link v-if="userStore.hasRole('MUSICIAN')" to="/apply/musician" class="side-link" :class="{ active: isActive('/apply/musician') }">
          <el-icon class="side-icon"><Microphone /></el-icon>
          <span>音乐人资料</span>
        </router-link>
        <router-link v-if="userStore.hasRole('ANCHOR')" to="/apply/anchor" class="side-link" :class="{ active: isActive('/apply/anchor') }">
          <el-icon class="side-icon"><VideoCamera /></el-icon>
          <span>主播资料</span>
        </router-link>
        <router-link v-if="userStore.hasRole('ADVERTISER')" to="/apply/advertiser" class="side-link" :class="{ active: isActive('/apply/advertiser') }">
          <el-icon class="side-icon"><Money /></el-icon>
          <span>广告资料</span>
        </router-link>
      </nav>

      <div class="side-footer">
        <router-link to="/" class="back-link">
          <el-icon><Back /></el-icon>
          返回音乐前台
        </router-link>
      </div>
    </aside>

    <div class="studio-main">
      <header class="studio-bar">
        <button
          class="nav-toggle"
          aria-label="打开创作者导航"
          aria-controls="studio-side-nav"
          :aria-expanded="menuOpen"
          @click="menuOpen = true"
        >
          <el-icon><Menu /></el-icon>
        </button>

        <div class="top-context" aria-label="当前位置">
          <span>创作者中心</span>
          <el-icon aria-hidden="true"><ArrowRight /></el-icon>
          <strong>{{ pageTitle }}</strong>
        </div>

        <div class="bar-actions">
          <router-link to="/" class="front-link">前台</router-link>
          <el-dropdown trigger="click" @command="onCommand">
            <button class="account-button" aria-label="打开账号菜单">
              <el-avatar :size="26" :src="userStore.user?.avatar">
                {{ (userStore.user?.nickname || 'U').slice(0, 1) }}
              </el-avatar>
              <span>{{ userStore.user?.nickname || userStore.user?.username }}</span>
              <el-icon class="account-caret"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="mine">我的音乐</el-dropdown-item>
                <el-dropdown-item v-if="userStore.isAdmin" command="admin">管理后台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main id="studio-content" class="studio-content">
        <router-view v-slot="{ Component }">
          <transition name="studio-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
    <PlayerBar />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowDown,
  ArrowRight,
  Back,
  Close,
  Menu,
  Microphone,
  Money,
  Star,
  VideoCamera,
  VideoPlay,
  Wallet
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import PlayerBar from '@/components/PlayerBar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const menuOpen = ref(false)

const allWorkspaces = [
  { path: '/studio/musician', label: '音乐人工作台', icon: Microphone, role: 'MUSICIAN' },
  { path: '/studio/anchor', label: '主播工作台', icon: VideoPlay, role: 'ANCHOR' },
  { path: '/studio/advertiser', label: '广告工作台', icon: Money, role: 'ADVERTISER' }
]

const workspaceLinks = computed(() => allWorkspaces.filter((item) => userStore.hasRole(item.role)))
const roleSummary = computed(() => {
  const labels = []
  if (userStore.hasRole('MUSICIAN')) labels.push('音乐人')
  if (userStore.hasRole('ANCHOR')) labels.push('主播')
  if (userStore.hasRole('ADVERTISER')) labels.push('广告制作人')
  return labels.length ? labels.join(' · ') : '创作者账号'
})
const pageTitle = computed(() => route.meta.title || '创作者工作台')

function isActive(path) {
  if (path === '/studio/anchor') {
    return route.path === path ||
      route.path.startsWith('/studio/anchor/') ||
      route.path.startsWith('/studio/live/') ||
      route.path.startsWith('/studio/report/')
  }
  return route.path === path || route.path.startsWith(`${path}/`)
}

function closeOnEscape(event) {
  if (event.key === 'Escape' && menuOpen.value) menuOpen.value = false
}

async function onCommand(command) {
  if (command === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
    return
  }
  if (command === 'admin') router.push('/admin')
  if (command === 'mine') router.push('/mine')
}

watch(() => route.fullPath, () => { menuOpen.value = false })
onMounted(() => window.addEventListener('keydown', closeOnEscape))
onBeforeUnmount(() => window.removeEventListener('keydown', closeOnEscape))
</script>

<style scoped>
.studio-shell {
  min-height: 100vh;
  background: #08070b;
  color: #f7f4fc;
}

.studio-side {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 120;
  width: 196px;
  display: flex;
  flex-direction: column;
  padding: 12px 10px 12px;
  overflow-y: auto;
  background: #0a090e;
  border-right: 1px solid #1d1a23;
  scrollbar-width: none;
}
.studio-side::-webkit-scrollbar { display: none; }

.side-brand-row { display: flex; align-items: center; justify-content: space-between; padding: 2px 2px 12px; }
.studio-brand { display: flex; align-items: center; gap: 9px; min-width: 0; color: #f7f4fc; }
.studio-mark { width: 30px; height: 30px; flex: none; }
.studio-mark svg { width: 100%; height: 100%; display: block; }
.studio-brand-copy { display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.studio-brand-copy b { font-size: 14px; font-weight: 650; letter-spacing: -.02em; }
.studio-brand-copy small { font-size: 11px; color: #8f8799; letter-spacing: .12em; white-space: nowrap; }
.side-close { display: none; }

.side-scope {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 8px;
  margin-bottom: 12px;
  border-radius: 9px;
  background: #121016;
  border: 1px solid #211d28;
}
.scope-copy { min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.scope-copy b { max-width: 126px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; font-weight: 600; }
.scope-copy span { font-size: 10.5px; color: #948c9f; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.side-nav { display: flex; flex-direction: column; gap: 2px; }
.nav-heading {
  padding: 7px 9px 5px;
  color: #81788b;
  font-size: 10.5px;
  font-weight: 500;
}
.side-link {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 34px;
  padding: 0 9px;
  border-radius: 7px;
  color: #a9a1b4;
  font-size: 12.5px;
  font-weight: 500;
  transition: color .14s, background-color .14s;
}
.side-link:hover { color: #f7f4fc; background: #141119; }
.side-link.active { color: #fff; background: #1b1721; font-weight: 600; }
.side-icon { font-size: 15px; color: #8e8599; }
.side-link.active .side-icon { color: #e9b8ff; }

.side-footer { margin-top: auto; padding-top: 18px; }
.back-link {
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 34px;
  padding: 0 9px;
  border-radius: 7px;
  color: #948c9f;
  font-size: 12px;
  transition: color .14s, background-color .14s;
}
.back-link:hover { color: #f7f4fc; background: #141119; }

.studio-main { min-width: 0; margin-left: 196px; }
.studio-bar {
  position: sticky;
  top: 0;
  z-index: 90;
  height: 54px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 22px;
  background: #08070b;
  border-bottom: 1px solid #1d1a23;
}
.nav-toggle {
  display: none;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  border: 1px solid #302a39;
  border-radius: 7px;
  background: transparent;
  color: #f7f4fc;
  cursor: pointer;
}
.top-context { display: flex; align-items: center; gap: 7px; min-width: 0; color: #8f8799; font-size: 12px; }
.top-context .el-icon { font-size: 11px; }
.top-context strong { color: #f7f4fc; font-weight: 600; font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.bar-actions { margin-left: auto; display: flex; align-items: center; gap: 12px; }
.front-link {
  height: 30px;
  display: inline-flex;
  align-items: center;
  padding: 0 9px;
  border-radius: 7px;
  color: #a9a1b4;
  font-size: 12px;
  transition: color .14s, background-color .14s;
}
.front-link:hover { color: #f7f4fc; background: #141119; }
.account-button {
  height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 4px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #c9c2d0;
  font-size: 12px;
  cursor: pointer;
}
.account-button:hover { background: #141119; color: #f7f4fc; }
.account-caret { color: #8f8799; font-size: 11px; }

.studio-content {
  width: 100%;
  max-width: 1680px;
  margin: 0 auto;
  padding: 18px 24px 108px;
}
.nav-scrim { display: none; }

.studio-fade-enter-active, .studio-fade-leave-active { transition: opacity .14s ease; }
.studio-fade-enter-from, .studio-fade-leave-to { opacity: 0; }

@media (max-width: 1100px) {
  .studio-side { width: 184px; }
  .studio-main { margin-left: 184px; }
  .studio-content { padding: 16px 18px 36px; }
}

@media (max-width: 900px) {
  .studio-side {
    width: 248px;
    padding: 14px 12px;
    transform: translateX(-100%);
    transition: transform .2s ease;
  }
  .studio-shell.nav-open .studio-side { transform: translateX(0); }
  .side-close {
    display: inline-flex;
    width: 30px;
    height: 30px;
    align-items: center;
    justify-content: center;
    border: 1px solid #302a39;
    border-radius: 7px;
    background: transparent;
    color: #c9c2d0;
  }
  .studio-main { margin-left: 0; }
  .nav-toggle { display: inline-flex; }
  .nav-scrim {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 110;
    border: 0;
    background: rgba(0, 0, 0, .62);
  }
  .studio-bar { height: 52px; padding: 0 14px; gap: 10px; }
  .account-button span { max-width: 92px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}

@media (max-width: 620px) {
  .top-context span, .top-context .el-icon, .front-link { display: none; }
  .studio-content { padding: 12px 16px 100px; }
  .account-button span { display: none; }
}
</style>

<style>
.studio-shell .player-wrap { left: 196px !important; }
@media (max-width: 1100px) {
  .studio-shell .player-wrap { left: 184px !important; }
}
@media (max-width: 900px) {
  .studio-shell .player-wrap { left: 0 !important; }
}
</style>
