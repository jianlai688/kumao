import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/register',
    component: () => import('@/views/Register.vue'),
    meta: { public: true, title: '注册' }
  },
  {
    path: '/',
    component: () => import('@/layouts/UserLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/user/Home.vue'), meta: { title: '首页' } },
      { path: 'rank/:type', name: 'rank', component: () => import('@/views/user/Rank.vue'), meta: { title: '排行榜' } },
      { path: 'singers', name: 'singers', component: () => import('@/views/user/Singers.vue'), meta: { title: '歌手' } },
      { path: 'singer/:id', name: 'singerDetail', component: () => import('@/views/user/SingerDetail.vue') },
      { path: 'song/:id', name: 'songDetail', component: () => import('@/views/user/SongDetail.vue'), meta: { title: '歌曲播放' } },
      { path: 'playlists', name: 'playlists', component: () => import('@/views/user/Playlists.vue'), meta: { title: '歌单广场' } },
      { path: 'playlist/:id', name: 'playlistDetail', component: () => import('@/views/user/PlaylistDetail.vue') },
      { path: 'mine', name: 'mine', component: () => import('@/views/user/Mine.vue'), meta: { title: '我的音乐', auth: true } },
      { path: 'mv', name: 'mv', component: () => import('@/views/user/MvZone.vue'), meta: { title: 'MV 专区' } },
      { path: 'live', name: 'live', component: () => import('@/views/user/LiveSquare.vue'), meta: { title: '音乐直播' } },
      { path: 'live/:id', name: 'liveRoom', component: () => import('@/views/user/LiveRoom.vue') },
      { path: 'search', name: 'search', component: () => import('@/views/user/Search.vue'), meta: { title: '搜索' } },
      { path: 'vip', name: 'vip', component: () => import('@/views/user/Vip.vue'), meta: { title: '会员中心' } },
      { path: 'musicians', name: 'musicians', component: () => import('@/views/user/Musicians.vue'), meta: { title: '音乐人' } },
      { path: 'musician/:id', name: 'musicianHome', component: () => import('@/views/user/MusicianHome.vue') },
      { path: 'apply/anchor', name: 'applyAnchor', component: () => import('@/views/user/ApplyAnchor.vue'), meta: { title: '主播申请', auth: true } },
      { path: 'apply/musician', name: 'applyMusician', component: () => import('@/views/user/ApplyMusician.vue'), meta: { title: '音乐人认证', auth: true } },
      { path: 'apply/advertiser', name: 'applyAdvertiser', component: () => import('@/views/user/ApplyAdvertiser.vue'), meta: { title: '广告制作人认证', auth: true } }
    ]
  },
  {
    path: '/studio',
    component: () => import('@/views/studio/StudioLayout.vue'),
    meta: { auth: true },
    children: [
      { path: '', redirect: '/studio/musician' },
      { path: 'musician', name: 'musicianWorkbench', component: () => import('@/views/studio/MusicianWorkbench.vue'), meta: { title: '音乐人工作台', role: 'MUSICIAN' } },
      { path: 'advertiser', name: 'advertiserWorkbench', component: () => import('@/views/studio/AdvertiserWorkbench.vue'), meta: { title: '广告制作人工作台', role: 'ADVERTISER' } },
      { path: 'anchor', name: 'anchorWorkbench', component: () => import('@/views/studio/AnchorWorkbench.vue'), meta: { title: '主播工作台', role: 'ANCHOR' } },
      { path: 'earnings', name: 'anchorEarnings', component: () => import('@/views/studio/AnchorEarnings.vue'), meta: { title: '礼物收益', role: 'ANCHOR' } },
      { path: 'live/:id/prepare', name: 'livePrepare', redirect: (to) => ({ path: '/studio/anchor', query: { prepareRoom: to.params.id } }) },
      { path: 'live/:id', name: 'liveConsole', component: () => import('@/views/studio/LiveConsole.vue'), meta: { title: '直播控制台', role: 'ANCHOR' } },
      { path: 'report/:sessionId', name: 'liveReport', component: () => import('@/views/studio/LiveReport.vue'), meta: { title: '本场复盘', role: 'ANCHOR' } }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { auth: true, admin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'adminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'ai-review', name: 'adminAiReview', component: () => import('@/views/admin/AiReviewCenter.vue'), meta: { title: 'AI审核工作台' } },
      { path: 'model-settings', name: 'adminModelSettings', component: () => import('@/views/admin/AiModelSettings.vue'), meta: { title: 'AI模型配置' } },
      { path: 'users', name: 'adminUsers', component: () => import('@/views/admin/Users.vue'), meta: { title: '用户管理' } },
      { path: 'anchor-applies', name: 'adminAnchorApplies', component: () => import('@/views/admin/AnchorApplies.vue'), meta: { title: '主播审核' } },
      { path: 'songs', name: 'adminSongs', component: () => import('@/views/admin/Songs.vue'), meta: { title: '歌曲管理' } },
      { path: 'singers', name: 'adminSingers', component: () => import('@/views/admin/Singers.vue'), meta: { title: '歌手管理' } },
      { path: 'mvs', name: 'adminMvs', component: () => import('@/views/admin/Mvs.vue'), meta: { title: 'MV 管理' } },
      { path: 'banners', name: 'adminBanners', component: () => import('@/views/admin/Banners.vue'), meta: { title: '轮播管理' } },
      { path: 'playlists', name: 'adminPlaylists', component: () => import('@/views/admin/Playlists.vue'), meta: { title: '歌单管理' } },
      { path: 'comments', name: 'adminComments', component: () => import('@/views/admin/Comments.vue'), meta: { title: '评论管理' } },
      { path: 'lives', name: 'adminLives', component: () => import('@/views/admin/Lives.vue'), meta: { title: '直播管理' } },
      { path: 'danmaku', name: 'adminDanmaku', component: () => import('@/views/admin/Danmaku.vue'), meta: { title: '弹幕管理' } },
      { path: 'gifts', name: 'adminGifts', component: () => import('@/views/admin/Gifts.vue'), meta: { title: '礼物配置' } },
      { path: 'vip', name: 'adminVip', component: () => import('@/views/admin/VipManage.vue'), meta: { title: '会员管理' } },
      { path: 'musicians', name: 'adminMusicians', component: () => import('@/views/admin/Musicians.vue'), meta: { title: '音乐人管理' } },
      { path: 'advertisers', name: 'adminAdvertisers', component: () => import('@/views/admin/Advertisers.vue'), meta: { title: '广告管理' } },
      { path: 'search', name: 'adminSearch', component: () => import('@/views/admin/SearchMonitor.vue'), meta: { title: '检索监控' } },
      { path: 'files', name: 'adminFiles', component: () => import('@/views/admin/Files.vue'), meta: { title: '文件管理' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('km_token')
  const user = JSON.parse(localStorage.getItem('km_user') || 'null')
  if (to.meta.auth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.admin && !(user?.roles || '').split(',').includes('ADMIN')) {
    return { path: '/' }
  }
  if (to.meta.role && !(user?.roles || '').split(',').includes(to.meta.role)) {
    return { path: '/' }
  }
  document.title = to.meta.title ? `${to.meta.title} · 酷猫音乐` : '酷猫音乐'
  return true
})

export default router

