const assert = require('assert')
const fs = require('fs')
const path = require('path')
const vm = require('vm')

function loadDashboardComponent() {
  const filePath = path.resolve(__dirname, '../src/views/dashboard/index.vue')
  const source = fs.readFileSync(filePath, 'utf8')
  const scriptMatch = source.match(/<script>([\s\S]*?)<\/script>/)

  if (!scriptMatch) {
    throw new Error('failed to locate dashboard component script')
  }

  const script = scriptMatch[1]
    .replace("import { getDashboardOverview } from '../../api/dashboard'", "const getDashboardOverview = () => Promise.resolve({ data: {} })")
    .replace(
      "import { resolveRoutePath } from '../../utils/route'",
      "const resolveRoutePath = (route, basePath = '') => { if (!route || !route.path) { return basePath || '/' } if (route.path.startsWith('/')) { return route.path } return `${basePath}/${route.path}`.replace(/\\/+/g, '/') }"
    )
    .replace('export default', 'module.exports =')

  const sandbox = {
    module: { exports: {} },
    exports: {},
    require,
    console
  }

  vm.runInNewContext(script, sandbox, { filename: filePath })
  return sandbox.module.exports
}

const component = loadDashboardComponent()

const vmInstance = {
  $store: {
    getters: {
      routes: [
        {
          path: '/system',
          children: [
            { path: 'user', meta: { title: '用户管理' } },
            { path: 'role', meta: { title: '角色管理' } }
          ]
        },
        {
          path: '/log',
          children: [
            { path: 'login', meta: { title: '登录日志' } }
          ]
        }
      ]
    }
  }
}

const quickMenus = component.computed.quickMenus.call(vmInstance)

assert.deepStrictEqual(
  quickMenus.map(item => item.path),
  ['/system/user', '/system/role', '/log/login'],
  `expected quick menu paths to be fully resolved, but got ${quickMenus.map(item => item.path).join(', ')}`
)

console.log('dashboard-quick-menu.test.js passed')
