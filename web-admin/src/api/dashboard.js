import request from '../utils/request'

export function getDashboardOverview() {
  return request({
    url: '/api/system/dashboard/overview',
    method: 'get'
  })
}
