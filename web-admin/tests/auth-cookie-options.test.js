const assert = require('assert')
const fs = require('fs')
const path = require('path')
const vm = require('vm')

function loadAuthModule(stubs) {
  const filePath = path.resolve(__dirname, '../src/utils/auth.js')
  const source = fs.readFileSync(filePath, 'utf8')
  const transformed = source
    .replace("import Cookies from 'js-cookie'", 'const Cookies = stubs.Cookies')
    .replace(/export function /g, 'function ')
    .concat('\nmodule.exports = { setToken, setRefreshToken, setAuthTokens }\n')

  const sandbox = {
    module: { exports: {} },
    exports: {},
    stubs
  }
  vm.runInNewContext(transformed, sandbox, { filename: filePath })
  return sandbox.module.exports
}

function main() {
  const writes = []
  const auth = loadAuthModule({
    Cookies: {
      set(key, value, options) {
        writes.push({ key, value, options })
      }
    }
  })

  auth.setAuthTokens({
    accessToken: 'access-token',
    refreshToken: 'refresh-token'
  })

  assert.strictEqual(writes.length, 2, 'expected both access and refresh token cookies to be written')
  for (const write of writes) {
    assert.strictEqual(write.options.sameSite, 'Strict')
    assert.strictEqual(write.options.secure, true)
    assert.strictEqual(write.options.path, '/')
  }
  assert.ok(writes[0].options.expires < writes[1].options.expires, 'expected refresh token cookie to live longer than access token cookie')

  console.log('auth-cookie-options.test.js passed')
}

try {
  main()
} catch (error) {
  console.error(error)
  process.exit(1)
}
