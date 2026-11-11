<template>
  <div :class="['admin-layout', { collapsed }]">
    <button
      v-if="mobileOpen"
      class="admin-scrim"
      aria-label="关闭管理菜单"
      @click="mobileOpen = false"
    ></button>

    <aside id="admin-side-nav" :class="['side', { collapsed, 'mobile-open': mobileOpen }]">
      <div class="brand">
        <span class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 40 40">
            <defs>
              <linearGradient id="adminBrandGradient" x1="4" y1="2" x2="36" y2="38" gradientUnits="userSpaceOnUse">
                <stop stop-color="#a78bfa" /> <!-- taste-skill-ignore: brand mark -->
                <stop offset=".55" stop-color="#e879f9" />
                <stop offset="1" stop-color="#fda4af" />
              </linearGradient>
            </defs>
            <circle cx="20" cy="21" r="12.5" fill="none" stroke="url(#adminBrandGradient)" stroke-width="2" />
            <circle cx="20" cy="21" r="4.2" fill="url(#adminBrandGradient)" />
            <path d="M8.5 12.5 6 5.2l8 3.1M31.5 12.5 34 5.2l-8 3.1" fill="none" stroke="url(#adminBrandGradient)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            <circle cx="20" cy="21" r="16.5" fill="none" stroke="rgba(255,255,255,.10)" stroke-width="1" />
          </svg>
        </span>
        <div class="brand-text">
          <b>酷猫音乐</b>
          <span>运营后台</span>
        </div>
        <button class="mobile-close" aria-label="关闭菜单" @click="mobileOpen = false">
          <el-icon><Close /></el-icon>
        </button>
      </div>

      <button class="collapse-btn" :aria-label="collapsed ? '展开菜单' : '收起菜单'" @click="collapsed = !collapsed">
        <el-icon><component :is="collapsed ? 'Expand' : 'Fold'" /></el-icon>
      </button>

      <nav class="side-nav" aria-label="管理后台导航">
        <template v-for="group in menus" :key="group.title">
          <div class="nav-group">{{ group.title }}</div>
          <router-link
            v-for="m in group.items"
            :key="m.path"
            :to="m.path"
            :class="['nav-item', { active: $route.path === m.path }]"
            :aria-current="$route.path === m.path ? 'page' : undefined"
            :aria-label="m.label"
            :title="m.label"
            @click="mobileOpen = false"
          >
            <el-icon class="nav-ico"><component :is="m.icon" /></el-icon>
            <span class="nav-label">{{ m.label }}</span>
          </router-link>
        </template>
      </nav>

      <div class="side-foot">
        <router-link to="/" class="foot-link" aria-label="返回用户端">
          <el-icon><Back /></el-icon>
          <span>返回前台</span>
        </router-link>
        <div class="side-version">KUMAO OPS · v1.0</div>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <button class="menu-btn" aria-label="打开管理菜单" aria-controls="admin-side-nav" :aria-expanded="mobileOpen" @click="mobileOpen = true">
          <el-icon><Menu /></el-icon>
        </button>

        <div class="crumb">
          <span class="crumb-root">控制台</span>
          <el-icon class="crumb-sep"><ArrowRight /></el-icon>
          <span class="crumb-current">{{ $route.meta.title || '管理后台' }}</span>
        </div>

        <div class="right">
          <span class="ops-tag">运营环境</span>
          <span class="topbar-clock">{{ clock }}</span>
          <el-dropdown @command="onCommand" trigger="click">
            <span class="user-chip">
              <el-avatar :size="28" :src="userStore.user?.avatar">{{ (userStore.user?.nickname || 'A').slice(0, 1) }}</el-avatar>
              <span class="user-name">{{ userStore.user?.nickname || 'admin' }}</span>
              <el-icon class="chip-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>{{ userStore.user?.roles || 'ADMIN' }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <section class="content">
        <div v-if="$route.path !== '/admin/dashboard'" class="page-head">
          <div class="ph-left">
            <h1 class="ph-title">{{ $route.meta.title || '管理后台' }}</h1>
            <p class="ph-sub">{{ pageDesc }}</p>
          </div>
          <div class="ph-meta">
            <span class="ph-clock">{{ clock }}</span>
          </div>
        </div>

        <router-view v-slot="{ Component }">
          <transition name="km-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  DataLine, User, Microphone, Headset, Avatar, VideoCamera, Picture, Files,
  ChatDotRound, VideoPlay, ChatLineSquare, Present, Medal, Mic, Money,
  Search, FolderOpened, Setting, Back, ArrowRight, ArrowDown, Close, Menu
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)
const mobileOpen = ref(false)

const pageDescs = {
  '/admin/dashboard': '平台整体运行指标与微服务健康状态一览',
  '/admin/ai-review': 'AI 辅助处理资质、内容与直播审核，管理员最终确认并写入审核日志',
  '/admin/model-settings': '为审核、文本生成与图片识别分别配置模型、接口与调用参数',
  '/admin/users': '管理用户账号、角色权限与登录设备记录',
  '/admin/anchor-applies': '审核主播入驻申请，通过后自动开通开播权限',
  '/admin/songs': '管理曲库歌曲的上架、审核与推荐状态',
  '/admin/singers': '维护歌手资料与归属歌曲',
  '/admin/mvs': '审核与维护平台 MV 视频内容',
  '/admin/banners': '配置首页轮播位与投放排序',
  '/admin/playlists': '管理公开歌单与推荐位',
  '/admin/comments': '审核与处理用户评论内容',
  '/admin/lives': '管理直播间状态、封禁与回放',
  '/admin/danmaku': '管理直播弹幕内容与敏感词',
  '/admin/gifts': '配置直播礼物与价格',
  '/admin/vip': '管理会员套餐与用户会员状态',
  '/admin/musicians': '审核音乐人认证与提现申请',
  '/admin/advertisers': '审核广告制作人资质与投放计划',
  '/admin/search': '查看检索热门词与索引同步状态',
  '/admin/files': '查看 MinIO 文件存储与上传记录'
}
const pageDesc = computed(() => pageDescs[route.path] || '酷猫音乐平台运营管理')

const clock = ref('')
let timer = null
function tick() {
  const d = new Date()
  clock.value = d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}
onMounted(() => {
  tick()
  timer = setInterval(tick, 1000)
})
onBeforeUnmount(() => timer && clearInterval(timer))

const menus = [
  {
    title: '运营总览',
    items: [
      { path: '/admin/dashboard', label: '数据看板', icon: DataLine },
      { path: '/admin/ai-review', label: 'AI审核工作台', icon: DataLine }
    ]
  },
  {
    title: '用户与创作者',
    items: [
      { path: '/admin/users', label: '用户管理', icon: User },
      { path: '/admin/anchor-applies', label: '主播审核', icon: Microphone },
      { path: '/admin/musicians', label: '音乐人管理', icon: Mic },
      { path: '/admin/advertisers', label: '广告管理', icon: Money }
    ]
  },
  {
    title: '内容管理',
    items: [
      { path: '/admin/songs', label: '歌曲管理', icon: Headset },
      { path: '/admin/singers', label: '歌手管理', icon: Avatar },
      { path: '/admin/mvs', label: 'MV 管理', icon: VideoCamera },
      { path: '/admin/banners', label: '轮播管理', icon: Picture },
      { path: '/admin/playlists', label: '歌单管理', icon: Files },
      { path: '/admin/comments', label: '评论管理', icon: ChatDotRound }
    ]
  },
  {
    title: '直播与会员',
    items: [
      { path: '/admin/lives', label: '直播管理', icon: VideoPlay },
      { path: '/admin/danmaku', label: '弹幕管理', icon: ChatLineSquare },
      { path: '/admin/gifts', label: '礼物配置', icon: Present },
      { path: '/admin/vip', label: '会员管理', icon: Medal }
    ]
  },
  {
    title: '系统',
    items: [
      { path: '/admin/model-settings', label: 'AI模型配置', icon: Setting },
      { path: '/admin/search', label: '检索监控', icon: Search },
      { path: '/admin/files', label: '文件管理', icon: FolderOpened }
    ]
  }
]

async function onCommand(cmd) {
  if (cmd === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: var(--km-bg);
}

.side {
  position: fixed;
  inset: 0 auto 0 0;
  width: 224px;
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
.side.collapsed { width: 76px; }

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 8px 18px;
  position: relative;
}
.brand-mark { width: 36px; height: 36px; flex-shrink: 0; }
.brand-mark svg { width: 100%; height: 100%; }
.brand-text { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.brand-text b { font-size: 15px; font-weight: 700; letter-spacing: -.02em; line-height: 1.2; color: var(--km-text); white-space: nowrap; }
.brand-text span { font-size: 11px; color: var(--km-text-faint); letter-spacing: .03em; }
.mobile-close { display: none; }

.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  margin: 0 0 8px auto;
  border: 1px solid var(--km-border);
  border-radius: 7px;
  background: transparent;
  color: var(--km-text-faint);
  cursor: pointer;
  transition: color .16s, border-color .16s;
}
.collapse-btn:hover { color: var(--km-text); border-color: var(--km-border-strong); }

.side-nav { display: flex; flex-direction: column; gap: 2px; }
.nav-group { padding: 16px 10px 6px; color: var(--km-text-faint); font-size: 11px; font-weight: 500; letter-spacing: .02em; }
.nav-group:first-child { padding-top: 4px; }
.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 36px;
  padding: 0 10px;
  border-radius: 8px;
  color: var(--km-text-dim);
  font-size: 12px;
  transition: color .16s, background .16s;
}
.nav-item:hover { color: var(--km-text); background: rgba(255, 255, 255, .04); }
.nav-item.active { color: var(--km-text); background: rgba(255, 255, 255, .06); font-weight: 600; }
.nav-item.active::before { content: ''; position: absolute; left: 0; top: 10px; bottom: 10px; width: 2px; border-radius: 2px; background: var(--km-grad); }
.nav-ico { font-size: 15px; color: var(--km-text-faint); flex-shrink: 0; }
.nav-item.active .nav-ico { color: var(--km-accent); }
.nav-label { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.side.collapsed .brand-text,
.side.collapsed .nav-label,
.side.collapsed .nav-group,
.side.collapsed .side-version { display: none; }
.side.collapsed .brand { justify-content: center; padding-left: 0; padding-right: 0; }
.side.collapsed .collapse-btn { margin-left: auto; margin-right: auto; }
.side.collapsed .nav-item { justify-content: center; padding: 0; }

.side-foot { margin-top: auto; padding: 20px 2px 0; }
.foot-link { display: flex; align-items: center; gap: 8px; min-height: 34px; padding: 0 9px; border-radius: 8px; color: var(--km-text-faint); font-size: 11px; transition: color .16s, background .16s; }
.foot-link:hover { color: var(--km-text); background: rgba(255, 255, 255, .04); }
.side-version { padding: 8px 9px 0; color: var(--km-text-faint); font-size: 11px; letter-spacing: .08em; }

.main { min-height: 100vh; margin-left: 224px; display: flex; flex-direction: column; transition: margin-left .2s ease; }
.admin-layout.collapsed .main { margin-left: 76px; }

.topbar {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 56px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 28px;
  background: rgba(8, 7, 11, .95);
  border-bottom: 1px solid var(--km-border);
}
.menu-btn { display: none; width: 34px; height: 34px; border: 1px solid var(--km-border); border-radius: 8px; background: transparent; color: var(--km-text); align-items: center; justify-content: center; cursor: pointer; }
.crumb { display: flex; align-items: center; gap: 7px; min-width: 0; font-size: 11px; color: var(--km-text-faint); }
.crumb-root { color: var(--km-text-faint); }
.crumb-sep { font-size: 11px; color: var(--km-text-faint); }
.crumb-current { color: var(--km-text-dim); font-weight: 500; }

.right { margin-left: auto; display: flex; align-items: center; gap: 14px; }
.ops-tag { padding: 3px 7px; border-radius: 4px; background: rgba(74, 222, 128, .07); color: #86efac; font-size: 11px; border: 1px solid rgba(74, 222, 128, .12); }
.topbar-clock { font-size: 11px; color: var(--km-text-faint); font-variant-numeric: tabular-nums; }
.user-chip { display: flex; align-items: center; gap: 7px; padding: 3px 8px 3px 3px; border-radius: 999px; border: 1px solid transparent; cursor: pointer; transition: background .16s, border-color .16s; }
.user-chip:hover { background: rgba(255, 255, 255, .04); border-color: var(--km-border); }
.user-name { font-size: 11px; max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chip-arrow { font-size: 11px; color: var(--km-text-faint); }

.content { flex: 1; min-height: 0; padding: 24px 28px 40px; }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 22px; }
.ph-title { margin: 0; font-size: 24px; line-height: 1.25; font-weight: 700; letter-spacing: -.04em; color: var(--km-text); }
.ph-sub { margin: 6px 0 0; font-size: 12px; color: var(--km-text-faint); }
.ph-meta { display: flex; align-items: center; gap: 10px; }
.ph-clock { font-size: 11px; color: var(--km-text-faint); font-variant-numeric: tabular-nums; }

.admin-scrim { display: none; }

@media (max-width: 1100px) {
  .side { width: 76px; }
  .side .brand-text, .side .nav-label, .side .nav-group, .side .side-version { display: none; }
  .side .brand { justify-content: center; padding-left: 0; padding-right: 0; }
  .side .collapse-btn { display: none; }
  .side .nav-item { justify-content: center; padding: 0; }
  .main { margin-left: 76px; }
  .admin-layout.collapsed .main { margin-left: 76px; }
  .content { padding: 22px 20px 32px; }
  .topbar { padding: 0 20px; }
}

@media (max-width: 800px) {
  .side {
    width: 248px;
    transform: translateX(-100%);
    transition: transform .22s ease;
    
  }
  .side.collapsed { width: 248px; }
  .side.mobile-open { transform: translateX(0); }
  .side .brand-text, .side .nav-label, .side .nav-group, .side .side-version { display: flex; }
  .side .nav-group { display: block; }
  .side .brand { justify-content: flex-start; padding-left: 8px; }
  .side .nav-item { justify-content: flex-start; padding: 0 10px; }
  .side .collapse-btn { display: none; }
  .mobile-close { display: flex; position: absolute; right: 2px; top: 8px; width: 30px; height: 30px; border: 1px solid var(--km-border); border-radius: 8px; background: transparent; color: var(--km-text-dim); align-items: center; justify-content: center; }
  .main { margin-left: 0; }
  .admin-layout.collapsed .main { margin-left: 0; }
  .menu-btn { display: flex; }
  .admin-scrim { display: block; position: fixed; inset: 0; z-index: 110; border: 0; background: rgba(0, 0, 0, .55); }
  .ops-tag { display: none; }
  .topbar-clock { display: none; }
  .topbar { padding: 0 14px; }
  .content { padding: 16px 16px 28px; }
}
</style>
