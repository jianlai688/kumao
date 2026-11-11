import { defineStore } from 'pinia'
import { authApi, userApi } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('km_token') || '',
    user: JSON.parse(localStorage.getItem('km_user') || 'null')
  }),
  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => (state.user?.roles || '').split(',').includes('ADMIN'),
    hasRole: (state) => (role) => (state.user?.roles || '').split(',').includes(role)
  },
  actions: {
    async login(payload) {
      const res = await authApi.login(payload)
      this.setSession(res.data)
      return res.data
    },
    setSession(data) {
      this.token = data.token
      this.user = {
        userId: data.userId,
        username: data.username,
        nickname: data.nickname,
        avatar: data.avatar,
        roles: data.roles,
        device: data.device
      }
      localStorage.setItem('km_token', data.token)
      localStorage.setItem('km_user', JSON.stringify(this.user))
      localStorage.setItem('km_device', data.device || 'PC')
    },
    async refresh() {
      if (!this.token) return
      try {
        const res = await userApi.me()
        this.user = { ...this.user, ...res.data }
        localStorage.setItem('km_user', JSON.stringify(this.user))
      } catch (e) {
        /* ignore */
      }
    },
    async logout() {
      try {
        await authApi.logout()
      } catch (e) {
        /* ignore */
      }
      this.token = ''
      this.user = null
      localStorage.removeItem('km_token')
      localStorage.removeItem('km_user')
    }
  }
})
