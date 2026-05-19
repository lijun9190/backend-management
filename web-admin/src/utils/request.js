import axios from 'axios'
import { Message } from 'element-ui'
import store from '../store'
import { getToken, getRefreshToken, setAuthTokens } from './auth'
import router from '../router'

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 15000
})

const refreshClient = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 15000
})

let unauthorizedHandled = false
let activeToken = getToken() || ''
let refreshPromise = null
let redirectPendingPromise = null

function isRefreshRequest(config) {
  return config && config.url === '/api/auth/refresh'
}

function waitForRedirectTakeover() {
  if (!redirectPendingPromise) {
    redirectPendingPromise = new Promise(() => {})
  }
  return redirectPendingPromise
}

async function clearSessionAndRedirect(message) {
  if (unauthorizedHandled) {
    return
  }
  unauthorizedHandled = true
  Message.error(message)
  await store.dispatch('user/clearSession')
  if (!router.currentRoute || router.currentRoute.path !== '/login') {
    try {
      await router.push('/login')
    } catch (error) {
      // Ignore navigation races while we are already forcing the user back to login.
    }
  }
}

async function refreshAccessToken() {
  if (refreshPromise) {
    return refreshPromise
  }
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('NO_REFRESH_TOKEN')
  }
  refreshPromise = refreshClient.post('/api/auth/refresh', {
    refreshToken
  }).then(response => {
    const res = response.data
    if (!res || res.code !== 200 || !res.data) {
      throw new Error((res && res.message) || '刷新 access token 失败')
    }
    const tokens = {
      accessToken: res.data.accessToken,
      refreshToken: res.data.refreshToken
    }
    setAuthTokens(tokens)
    store.commit('user/SET_TOKENS', tokens)
    activeToken = tokens.accessToken
    unauthorizedHandled = false
    return tokens
  }).finally(() => {
    refreshPromise = null
  })
  return refreshPromise
}

service.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    if (token !== activeToken) {
      activeToken = token
      unauthorizedHandled = false
    }
    config.headers = config.headers || {}
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
    const originalConfig = error.config || {}
    let unauthorizedRedirected = false

    if (status === 401) {
//       如果当前请求 401，并且：
//       1. 这个请求还没重试过
//       2. 这个请求没有禁止自动 refresh (配置了skipAuthRefresh的请求如果失败，不应该再触发自动 refresh，否则会死循环)
//       3. 这个请求不是 refresh 接口本身
//       4. 本地还有 refresh token
//       那就先尝试 refresh token，而不是立刻退出登录。
        //!undefined == true

      if (!originalConfig._retry && !originalConfig.skipAuthRefresh && !isRefreshRequest(originalConfig) && getRefreshToken()) {
        try {
          const tokens = await refreshAccessToken()
          originalConfig._retry = true
          originalConfig.headers = originalConfig.headers || {}
          originalConfig.headers.Authorization = `Bearer ${tokens.accessToken}`
          return service.request(originalConfig)
        } catch (refreshError) {
          await clearSessionAndRedirect('登录状态已失效，请重新登录')
          unauthorizedRedirected = true
        }
      } else if (!originalConfig.skipUnauthorizedHandler) {
        await clearSessionAndRedirect('登录状态已失效，请重新登录')
        unauthorizedRedirected = true
      }
    } else if (status === 403) {
      Message.error('当前账号没有此操作权限')
      router.push('/403')
    } else {
      Message.error((response.data && response.data.message) || error.message || '请求失败')
    }

    if (unauthorizedRedirected) {
      return waitForRedirectTakeover()
    }

    return Promise.reject(error)
  }
)

export default service
