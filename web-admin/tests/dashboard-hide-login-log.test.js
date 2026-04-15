const assert = require('assert')
const fs = require('fs')
const path = require('path')

const filePath = path.resolve(__dirname, '../src/views/dashboard/index.vue')
const source = fs.readFileSync(filePath, 'utf8')

assert.ok(
  !source.includes('最近登录记录') && !source.includes('recentLoginLogs'),
  'expected dashboard to stop rendering recent login logs by default'
)

console.log('dashboard-hide-login-log.test.js passed')
