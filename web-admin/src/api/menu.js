import request from '../utils/request'

export function getMenuTree() {
  return request({ url: '/api/system/menus/tree', method: 'get' })
}

export function getMenuDetail(id) {
  return request({ url: `/api/system/menus/${id}`, method: 'get' })
}

export function saveMenu(data) {
  return request({ url: '/api/system/menus', method: 'post', data })
}

export function updateMenu(id, data) {
  return request({ url: `/api/system/menus/${id}`, method: 'put', data })
}

export function removeMenu(id) {
  return request({ url: `/api/system/menus/${id}`, method: 'delete' })
}
