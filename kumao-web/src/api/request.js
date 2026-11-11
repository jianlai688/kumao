import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 20000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('km_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Device-Type'] = localStorage.getItem('km_device') || 'PC'
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 200) {
      if (!response.config?.silent) ElMessage.error(body.message || '请求失败')
      if (body.code === 401) {
        localStorage.removeItem('km_token')
        localStorage.removeItem('km_user')
        if (!location.hash.includes('/login')) {
          location.hash = '#/login'
        }
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const config = error.config || {}
    const method = (config.method || 'get').toLowerCase()
    // 微服务重启/注册中心短暂摘除实例时，GET 请求自动重试一次，避免页面直接报 503。
    if ([502, 503, 504].includes(status) && method === 'get' && !config.__serviceRetry) {
      config.__serviceRetry = true
      return new Promise((resolve) => setTimeout(resolve, 900)).then(() => request(config))
    }
    const message = status === 503
      ? '服务正在启动或重启，请稍后重试'
      : error.response?.data?.message || error.message || '网络异常'
    if (status === 401) {
      localStorage.removeItem('km_token')
      localStorage.removeItem('km_user')
      if (!location.hash.includes('/login')) {
        location.hash = '#/login'
      }
    }
    if (!config.silent) ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
