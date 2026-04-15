const assert = require('assert')
const fs = require('fs')
const path = require('path')
const vm = require('vm')

function loadRoleComponent(stubs = {}) {
  const filePath = path.resolve(__dirname, '../src/views/system/role/index.vue')
  const source = fs.readFileSync(filePath, 'utf8')
  const scriptMatch = source.match(/<script>([\s\S]*?)<\/script>/)

  if (!scriptMatch) {
    throw new Error('failed to locate role component script')
  }

  const roleApiStub = `
    const assignRoleMenus = stubs.assignRoleMenus || (() => Promise.resolve())
    const changeRoleStatus = stubs.changeRoleStatus || (() => Promise.resolve())
    const getRoleMenuIds = stubs.getRoleMenuIds || (() => Promise.resolve({ data: [] }))
    const getRolePage = stubs.getRolePage || (() => Promise.resolve({ data: { records: [], total: 0 } }))
    const saveRole = stubs.saveRole || (() => Promise.resolve())
    const updateRole = stubs.updateRole || (() => Promise.resolve())
  `
  const menuApiStub = `
    const getMenuTree = stubs.getMenuTree || (() => Promise.resolve({ data: [] }))
  `

  const script = scriptMatch[1]
    .replace(
      /import\s*\{\s*assignRoleMenus,\s*changeRoleStatus,\s*getRoleMenuIds,\s*getRolePage,\s*saveRole,\s*updateRole\s*\}\s*from\s*'..\/..\/..\/api\/role'/,
      roleApiStub
    )
    .replace(/import\s*\{\s*getMenuTree\s*\}\s*from\s*'..\/..\/..\/api\/menu'/, menuApiStub)
    .replace('export default', 'module.exports =')

  const sandbox = {
    module: { exports: {} },
    exports: {},
    require,
    console,
    stubs
  }

  vm.runInNewContext(script, sandbox, { filename: filePath })
  return sandbox.module.exports
}

async function main() {
  let menuTreeRequestCount = 0

  const component = loadRoleComponent({
    getMenuTree: async () => {
      menuTreeRequestCount += 1
      return { data: [{ id: 1, menuName: 'Dashboard', children: [] }] }
    },
    getRoleMenuIds: async () => ({ data: [1] })
  })

  const state = component.data()
  const vmInstance = {
    ...state,
    $store: {
      getters: {
        isSuperAdmin: true,
        permissions: []
      }
    },
    $nextTick(callback) {
      return Promise.resolve().then(callback)
    },
    $refs: {
      menuTreeRef: {
        setCheckedKeys() {}
      }
    }
  }

  Object.keys(component.methods).forEach(methodName => {
    vmInstance[methodName] = component.methods[methodName]
  })

  await component.created.call(vmInstance)
  assert.strictEqual(menuTreeRequestCount, 0, 'expected role page init to avoid loading menu tree eagerly')

  await component.methods.openAssign.call(vmInstance, { id: 9 })
  await Promise.resolve()
  await Promise.resolve()

  assert.strictEqual(menuTreeRequestCount, 1, 'expected menu tree to load lazily when opening assign dialog')
  assert.strictEqual(vmInstance.assignDialogVisible, true, 'expected assign dialog to open normally')

  console.log('role-menu-tree-lazy-load.test.js passed')
}

main().catch(error => {
  console.error(error)
  process.exit(1)
})
