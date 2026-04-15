import request from '../utils/request'

export function getUserPage(params) {
  return request({ url: '/api/system/users/page', method: 'get', params })
}

export function getUserDetail(id) {
  return request({ url: `/api/system/users/${id}`, method: 'get' })
}

export function saveUser(data) {
  return request({ url: '/api/system/users', method: 'post', data })
}

export function updateUser(id, data) {
  return request({ url: `/api/system/users/${id}`, method: 'put', data })
}

export function changeUserStatus(id, data) {
  return request({ url: `/api/system/users/${id}/status`, method: 'put', data })
}

export function resetUserPassword(id, data) {
  return request({ url: `/api/system/users/${id}/reset-password`, method: 'put', data })
}

export function assignUserRoles(id, data) {
  return request({ url: `/api/system/users/${id}/roles`, method: 'put', data })
}

export function removeUser(id) {
  return request({ url: `/api/system/users/${id}`, method: 'delete' })
}
