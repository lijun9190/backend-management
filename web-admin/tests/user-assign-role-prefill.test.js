const assert = require('assert')
const fs = require('fs')
const path = require('path')
const vm = require('vm')

function loadUserComponent(stubs = {}) {
  const filePath = path.resolve(__dirname, '../src/views/system/user/index.vue')
  const source = fs.readFileSync(filePath, 'utf8')
  const scriptMatch = source.match(/<script>([\s\S]*?)<\/script>/)

  if (!scriptMatch) {
    throw new Error('failed to locate user component script')
  }

  const userApiStub = `
    const assignUserRoles = stubs.assignUserRoles || (() => Promise.resolve())
    const changeUserStatus = stubs.changeUserStatus || (() => Promise.resolve())
    const getUserDetail = stubs.getUserDetail || (() => Promise.resolve({ data: {} }))
    const getUserPage = stubs.getUserPage || (() => Promise.resolve({ data: {} }))
    const kickoutUser = stubs.kickoutUser || (() => Promise.resolve())
    const removeUser = stubs.removeUser || (() => Promise.resolve())
    const resetUserPassword = stubs.resetUserPassword || (() => Promise.resolve())
    const saveUser = stubs.saveUser || (() => Promise.resolve())
    const updateUser = stubs.updateUser || (() => Promise.resolve())
  `
  const roleApiStub = `
    const getRoleOptions = stubs.getRoleOptions || (() => Promise.resolve({ data: [] }))
  `

  const script = scriptMatch[1]
    .replace(
      /import\s*\{\s*assignUserRoles,\s*changeUserStatus,\s*getUserDetail,\s*getUserPage,\s*kickoutUser,\s*resetUserPassword,\s*saveUser,\s*updateUser\s*\}\s*from\s*'..\/..\/..\/api\/user'/,
      userApiStub
    )
    .replace(/import\s*\{\s*removeUser\s*\}\s*from\s*'..\/..\/..\/api\/user'/, '')
    .replace(/import\s*\{\s*getRoleOptions\s*\}\s*from\s*'..\/..\/..\/api\/role'/, roleApiStub)
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
  const component = loadUserComponent({
    getUserDetail: async id => ({ data: { id, roleIds: [2, 4] } })
  })

  const state = component.data()
  const vmInstance = {
    ...state,
    $store: {
      getters: {
        isSuperAdmin: true,
        permissions: []
      }
    }
  }

  await component.methods.openAssignRole.call(vmInstance, { id: 9 })

  assert.strictEqual(vmInstance.currentUserId, 9, 'expected current user id to be set for role assignment')
  assert.strictEqual(vmInstance.roleDialogVisible, true, 'expected role dialog to open')
  assert.deepStrictEqual(vmInstance.roleForm.roleIds, [2, 4], 'expected assigned role ids to prefill from user detail')

  console.log('user-assign-role-prefill.test.js passed')
}

main().catch(error => {
  console.error(error)
  process.exit(1)
})
