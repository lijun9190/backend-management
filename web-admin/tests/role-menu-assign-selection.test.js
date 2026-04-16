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
  const filePath = path.resolve(__dirname, '../src/views/system/role/index.vue')
  const source = fs.readFileSync(filePath, 'utf8')

  assert.match(
    source,
    /<el-tree[\s\S]*check-strictly/,
    'expected role permission tree to enable check-strictly so menus and buttons can be selected independently'
  )

  const calls = []
  const component = loadRoleComponent({
    assignRoleMenus: async (roleId, payload) => {
      calls.push({ roleId, payload })
      return {}
    }
  })

  const state = component.data()
  const vmInstance = {
    ...state,
    currentRoleId: 4,
    $refs: {
      menuTreeRef: {
        getCheckedKeys() {
          return [210, 211]
        },
        getHalfCheckedKeys() {
          return [200]
        }
      }
    },
    $message: {
      success() {}
    }
  }

  Object.keys(component.methods).forEach(methodName => {
    vmInstance[methodName] = component.methods[methodName]
  })

  await component.methods.submitAssign.call(vmInstance)

  assert.strictEqual(calls.length, 1, 'expected role menu assignment to be submitted once')
  assert.strictEqual(calls[0].roleId, 4, 'expected role menu assignment to target the current role')
  assert.deepStrictEqual(
    Array.from(calls[0].payload.menuIds),
    [210, 211],
    'expected role menu assignment to persist only explicitly checked ids'
  )

  console.log('role-menu-assign-selection.test.js passed')
}

main().catch(error => {
  console.error(error)
  process.exit(1)
})
