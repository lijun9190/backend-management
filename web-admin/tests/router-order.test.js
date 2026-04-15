const assert = require('assert')
const fs = require('fs')
const path = require('path')
const vm = require('vm')
const Vue = require('vue')
const VueRouter = require('vue-router')

Vue.use(VueRouter)

function loadRouterModule() {
  const filePath = path.resolve(__dirname, '../src/router/index.js')
  let source = fs.readFileSync(filePath, 'utf8')

  source = source
    .replace("import Vue from 'vue'", "const Vue = require('vue')")
    .replace("import VueRouter from 'vue-router'", "const VueRouter = require('vue-router')")
    .replace("import Layout from '../layout/index.vue'", "const Layout = { name: 'Layout' }")
    .replace('export const constantRoutes =', 'const constantRoutes =')
    .replace('export default router', 'module.exports = { router, constantRoutes }')

  const sandbox = {
    require,
    module: { exports: {} },
    exports: {},
    console,
    __dirname: path.dirname(filePath),
    __filename: filePath
  }

  vm.runInNewContext(source, sandbox, { filename: filePath })
  return sandbox.module.exports
}

function createDashboardRoute() {
  return {
    path: '/dashboard',
    component: { name: 'Layout' },
    children: [
      {
        path: 'index',
        name: 'Dashboard',
        component: { name: 'DashboardIndex' }
      }
    ]
  }
}

const { router } = loadRouterModule()

const initialMatch = router.match('/dashboard/index')

assert.notStrictEqual(
  initialMatch.meta && initialMatch.meta.publicPage,
  true,
  'expected protected dashboard path to avoid resolving to a public 404 route before dynamic routes load'
)

router.addRoute(createDashboardRoute())

const matched = router.match('/dashboard/index')
const matchedPaths = matched.matched.map(route => route.path)

assert.ok(
  matchedPaths.includes('/dashboard') && matchedPaths.includes('/dashboard/index'),
  `expected /dashboard/index to match dashboard route, but got ${matchedPaths.join(' -> ')}`
)

assert.notStrictEqual(
  matchedPaths[matchedPaths.length - 1],
  '*',
  `expected dynamic dashboard route to win before wildcard route, but got ${matchedPaths.join(' -> ')}`
)

console.log('router-order.test.js passed')
