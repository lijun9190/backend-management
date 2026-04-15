import axios from 'axios'
import { Message } from 'element-ui'
import store from '../store'
import { getToken } from './auth'
import router from '../router'

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 15000
})

let unauthorizedHandled = false
let activeToken = getToken() || ''

service.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    if (token !== activeToken) {
      activeToken = token
      unauthorizedHandled = false
    }
    config.headers.Authorization = `Bearer ${token}`
  } else {
    activeToken = ''
  }
  return config
})

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      Message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  async error => {
    const response = error.response || {}
    const status = response.status
    if (status === 401) {
      if (!unauthorizedHandled) {
        unauthorizedHandled = true
        Message.error('登录状态已失效，请重新登录')
        await store.dispatch('user/logout')
        if (!router.currentRoute || router.currentRoute.path !== '/login') {
          router.push('/login')
        }
      }
    } else if (status === 403) {
      Message.error('当前账号没有此操作权限')
      router.push('/403')
    } else {
      Message.error((response.data && response.data.message) || error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default service
