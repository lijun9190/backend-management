const assert = require('assert')
const fs = require('fs')
const path = require('path')
const vm = require('vm')

function loadAuthModule(localStorage) {
  const filePath = path.resolve(__dirname, '../src/utils/auth.js')
  const source = fs.readFileSync(filePath, 'utf8')
  const transformed = source
    .replace(/export function /g, 'function ')
    .concat('\nmodule.exports = { getToken, getRefreshToken, setToken, setRefreshToken, setAuthTokens, clearAuthTokens }\n')

  const sandbox = {
    module: { exports: {} },
    exports: {},
    localStorage
  }
  vm.runInNewContext(transformed, sandbox, { filename: filePath })
  return sandbox.module.exports
}

function main() {
  const store = {}
  const localStorage = {
    getItem(key) {
      return store[key]
    },
    setItem(key, value) {
      store[key] = value
    },
    removeItem(key) {
      delete store[key]
    }
  }
  const auth = loadAuthModule(localStorage)

  auth.setAuthTokens({ accessToken: 'cookie-session', refreshToken: 'cookie-session' })

  assert.strictEqual(auth.getToken(), 'cookie-session')
  assert.strictEqual(auth.getRefreshToken(), 'cookie-session')
  assert.ok(!Object.keys(store).some(key => key.includes('TOKEN')), 'expected local state to avoid storing token values')

  auth.clearAuthTokens()

  assert.strictEqual(auth.getToken(), '')
  assert.strictEqual(auth.getRefreshToken(), '')

  console.log('auth-cookie-options.test.js passed')
}

try {
  main()
} catch (error) {
  console.error(error)
  process.exit(1)
}
