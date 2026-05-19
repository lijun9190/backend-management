import Cookies from 'js-cookie'

const ACCESS_TOKEN_KEY = 'ADMIN_DEMO_ACCESS_TOKEN'
const REFRESH_TOKEN_KEY = 'ADMIN_DEMO_REFRESH_TOKEN'
const ACCESS_TOKEN_EXPIRE_DAYS = 1 / 48
const REFRESH_TOKEN_EXPIRE_DAYS = 7

function isHttpsRuntime() {
  return typeof window === 'undefined' || (window.location && window.location.protocol === 'https:')
}

function tokenCookieOptions(expires) {
  return {
    expires,
    path: '/',
    sameSite: 'Strict',
    secure: isHttpsRuntime()
  }
}

export function getToken() {
  return Cookies.get(ACCESS_TOKEN_KEY)
}

export function getRefreshToken() {
  return Cookies.get(REFRESH_TOKEN_KEY)
}

export function setToken(token) {
  return Cookies.set(ACCESS_TOKEN_KEY, token, tokenCookieOptions(ACCESS_TOKEN_EXPIRE_DAYS))
}

export function setRefreshToken(token) {
  return Cookies.set(REFRESH_TOKEN_KEY, token, tokenCookieOptions(REFRESH_TOKEN_EXPIRE_DAYS))
}

export function setAuthTokens({ accessToken, refreshToken }) {
  setToken(accessToken)
  setRefreshToken(refreshToken)
}

export function removeToken() {
  return Cookies.remove(ACCESS_TOKEN_KEY)
}

export function removeRefreshToken() {
  return Cookies.remove(REFRESH_TOKEN_KEY)
}

export function clearAuthTokens() {
  removeToken()
  removeRefreshToken()
}
