import request from '../utils/request'

export function getLoginLogPage(params) {
  return request({ url: '/api/system/login-logs/page', method: 'get', params })
}

export function getOperationLogPage(params) {
  return request({ url: '/api/system/operation-logs/page', method: 'get', params })
}
