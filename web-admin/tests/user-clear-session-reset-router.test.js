const assert = require('assert')
const fs = require('fs')
const path = require('path')
const vm = require('vm')

function loadUserModule(stubs) {
  const filePath = path.resolve(__dirname, '../src/store/modules/user.js')
  let source = fs.readFileSync(filePath, 'utf8')

  source = source
    .replace(
      "import { clearAuthTokens, getRefreshToken, getToken, setAuthTokens } from '../../utils/auth'",
      'const { clearAuthTokens, getRefreshToken, getToken, setAuthTokens } = stubs.auth'
    )
    .replace(
      "import { getProfile, login, logout, updateMyPassword } from '../../api/auth'",
      'const { getProfile, login, logout, updateMyPassword } = stubs.api'
    )
    .replace(
      "import { resetRouter } from '../../router'",
      'const { resetRouter } = stubs.router'
    )
    .replace('export default {', 'module.exports = {')

  const sandbox = {
    module: { exports: {} },
    exports: {},
    require,
    console,
    stubs
  }

  vm.runInNewContext(source, sandbox, { filename: filePath })
  return sandbox.module.exports
}

async function main() {
  let clearedTokens = false
  let resetRouterCount = 0
  let resetRoutesCount = 0
  let resetStateCommitted = false

  const user = loadUserModule({
    auth: {
      clearAuthTokens() {
        clearedTokens = true
      },
      getRefreshToken() {
        return ''
      },
      getToken() {
        return ''
      },
      setAuthTokens() {}
    },
    api: {
      getProfile() {},
      login() {},
      logout() {},
      updateMyPassword() {}
    },
    router: {
      resetRouter() {
        resetRouterCount += 1
      }
    }
  })

  await user.actions.clearSession({
    commit(type) {
      if (type === 'RESET_STATE') {
        resetStateCommitted = true
      }
    },
    dispatch(action) {
      if (action === 'permission/resetRoutes') {
        resetRoutesCount += 1
      }
      return Promise.resolve()
    }
  })

  assert.strictEqual(clearedTokens, true, 'expected auth tokens to be cleared')
  assert.strictEqual(resetStateCommitted, true, 'expected user state to be reset')
  assert.strictEqual(resetRoutesCount, 1, 'expected permission routes state to be reset')
  assert.strictEqual(resetRouterCount, 1, 'expected Vue Router dynamic routes to be reset')

  console.log('user-clear-session-reset-router.test.js passed')
}

main().catch(error => {
  console.error(error)
  process.exit(1)
})
