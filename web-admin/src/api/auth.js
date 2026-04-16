import request from '../utils/request'

export function login(data) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

export function refreshToken(data) {
  return request({
    url: '/api/auth/refresh',
    method: 'post',
    data,
    skipAuthRefresh: true,
    skipUnauthorizedHandler: true
  })
}

export function logout(config = {}) {
  return request({
    url: '/api/auth/logout',
    method: 'post',
    ...config
  })
}

export function getProfile() {
  return request({
    url: '/api/auth/user/profile',
    method: 'get'
  })
}

export function updateMyPassword(data) {
  return request({
    url: '/api/auth/user/password',
    method: 'put',
    data
  })
}
