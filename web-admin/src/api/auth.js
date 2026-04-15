import request from '../utils/request'

export function login(data) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

export function logout() {
  return request({
    url: '/api/auth/logout',
    method: 'post'
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
