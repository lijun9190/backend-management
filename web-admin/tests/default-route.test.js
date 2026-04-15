const assert = require('assert')
const fs = require('fs')
const path = require('path')
const vm = require('vm')

function loadRouteUtils() {
  const filePath = path.resolve(__dirname, '../src/utils/route.js')
  const source = fs.readFileSync(filePath, 'utf8')

  const transformed = source
    .replace("import Layout from '../layout/index.vue'", 'const Layout = { name: "Layout" }')
    .replace(/export function /g, 'function ')
    .concat('\nmodule.exports = { resolveRoutePath, buildAsyncRoutes, resolveDefaultRoutePath };')

  const sandbox = {
    module: { exports: {} },
    exports: {},
    require
  }

  vm.runInNewContext(transformed, sandbox, { filename: filePath })
  return sandbox.module.exports
}

function main() {
  const { resolveDefaultRoutePath } = loadRouteUtils()

  const menus = [
    {
      id: 200,
      menuType: 'CATALOG',
      path: '/system',
      visible: 1,
      status: 1,
      children: [
        {
          id: 210,
          menuType: 'MENU',
          path: 'user',
          visible: 1,
          status: 1,
          children: []
        }
      ]
    }
  ]

  const defaultPath = resolveDefaultRoutePath(menus)
  assert.strictEqual(defaultPath, '/system/user', 'expected default route to use the first accessible menu path')

  const emptyDefaultPath = resolveDefaultRoutePath([])
  assert.strictEqual(emptyDefaultPath, null, 'expected empty menus to produce no default route')

  console.log('default-route.test.js passed')
}

main()
