const SESSION_ACTIVE_KEY = 'ADMIN_DEMO_SESSION_ACTIVE'

export function getToken() {
  return localStorage.getItem(SESSION_ACTIVE_KEY) === '1' ? 'cookie-session' : ''
}

export function getRefreshToken() {
  return getToken()
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(SESSION_ACTIVE_KEY, '1')
  }
}

export function setRefreshToken(token) {
  if (token) {
    localStorage.setItem(SESSION_ACTIVE_KEY, '1')
  }
}

export function setAuthTokens({ accessToken, refreshToken }) {
  if (accessToken !== false || refreshToken !== false) {
    localStorage.setItem(SESSION_ACTIVE_KEY, '1')
  }
}

export function removeToken() {
  localStorage.removeItem(SESSION_ACTIVE_KEY)
}

export function removeRefreshToken() {
  localStorage.removeItem(SESSION_ACTIVE_KEY)
}

export function clearAuthTokens() {
  removeToken()
  removeRefreshToken()
}
