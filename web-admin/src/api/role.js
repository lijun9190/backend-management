import request from '../utils/request'

export function getRolePage(params) {
  return request({ url: '/api/system/roles/page', method: 'get', params })
}

export function getRoleOptions() {
  return request({ url: '/api/system/roles/options', method: 'get' })
}

export function getRoleMenuIds(id) {
  return request({ url: `/api/system/roles/${id}/menu-ids`, method: 'get' })
}

export function saveRole(data) {
  return request({ url: '/api/system/roles', method: 'post', data })
}

export function updateRole(id, data) {
  return request({ url: `/api/system/roles/${id}`, method: 'put', data })
}

export function changeRoleStatus(id, data) {
  return request({ url: `/api/system/roles/${id}/status`, method: 'put', data })
}

export function assignRoleMenus(id, data) {
  return request({ url: `/api/system/roles/${id}/menus`, method: 'put', data })
}
