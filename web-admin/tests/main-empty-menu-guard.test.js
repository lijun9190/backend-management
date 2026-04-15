const assert = require('assert')
const fs = require('fs')
const path = require('path')
const vm = require('vm')

function loadMainModule(stubs) {
  const filePath = path.resolve(__dirname, '../src/main.js')
  const source = fs.readFileSync(filePath, 'utf8')

  const transformed = source
    .replace("import Vue from 'vue'", 'const Vue = stubs.Vue')
    .replace(/import\s+ElementUI(?:,\s*\{\s*Message\s*\})?\s+from 'element-ui'/, 'const ElementUI = stubs.ElementUI\nconst Message = stubs.Message')
    .replace("import 'element-ui/lib/theme-chalk/index.css'", '')
    .replace("import App from './App.vue'", 'const App = stubs.App')
    .replace("import router from './router'", 'const router = stubs.router')
    .replace("import store from './store'", 'const store = stubs.store')
    .replace("import permission from './directive/permission'", 'const permission = stubs.permission')
    .replace("import './styles/index.css'", '')
    .replace("import { resolveDefaultRoutePath } from './utils/route'", 'const resolveDefaultRoutePath = stubs.resolveDefaultRoutePath')

  const sandbox = {
    module: { exports: {} },
    exports: {},
    require,
    console,
    stubs
  }

  vm.runInNewContext(transformed, sandbox, { filename: filePath })
}

async function main() {
  let guard
  let logoutCount = 0
  let messageText = ''
  let nextArg = null

  function FakeVue() {
    return {
      $mount() {}
    }
  }
  FakeVue.use = function () {}
  FakeVue.directive = function () {}
  FakeVue.config = {}

  loadMainModule({
    Vue: FakeVue,
    ElementUI: {
      install() {}
    },
    Message: {
      error(message) {
        messageText = message
      }
    },
    App: {},
    permission: {},
    resolveDefaultRoutePath: () => null,
    router: {
      beforeEach(fn) {
        guard = fn
      },
      addRoute() {},
      push() {},
      currentRoute: { path: '/' }
    },
    store: {
      getters: {
        token: 'token',
        routeLoaded: false,
        menus: []
      },
      dispatch(action) {
        if (action === 'user/fetchProfile') {
          return Promise.resolve()
        }
        if (action === 'permission/generateRoutes') {
          return Promise.resolve([])
        }
        if (action === 'user/logout') {
          logoutCount += 1
          return Promise.resolve()
        }
        return Promise.resolve()
      }
    }
  })

  assert.ok(guard, 'expected router guard to be registered')

  await guard({ path: '/', query: {} }, { path: '/login' }, arg => {
    nextArg = arg
  })

  assert.strictEqual(messageText, '当前账号未分配角色，请联系管理员', 'expected empty-menu user to see a clear message')
  assert.strictEqual(logoutCount, 1, 'expected empty-menu user to be logged out')
  assert.strictEqual(nextArg, '/login?redirect=/', 'expected empty-menu user to be redirected back to login')

  console.log('main-empty-menu-guard.test.js passed')
}

main().catch(error => {
  console.error(error)
  process.exit(1)
})
