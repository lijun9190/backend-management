import request from '../utils/request'

export function getDeptTree() {
  return request({ url: '/api/system/depts/tree', method: 'get' })
}

export function saveDept(data) {
  return request({ url: '/api/system/depts', method: 'post', data })
}

export function updateDept(id, data) {
  return request({ url: `/api/system/depts/${id}`, method: 'put', data })
}

export function removeDept(id) {
  return request({ url: `/api/system/depts/${id}`, method: 'delete' })
}
