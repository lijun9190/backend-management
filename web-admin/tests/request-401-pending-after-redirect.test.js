const assert = require('assert')
const fs = require('fs')
const path = require('path')
const vm = require('vm')

function loadRequestModule(stubs) {
  const filePath = path.resolve(__dirname, '../src/utils/request.js')
  const source = fs.readFileSync(filePath, 'utf8')

  const transformed = source
    .replace("import axios from 'axios'", 'const axios = stubs.axios')
    .replace("import { Message } from 'element-ui'", 'const Message = stubs.Message')
    .replace("import store from '../store'", 'const store = stubs.store')
    .replace("import { getToken, getRefreshToken, setAuthTokens } from './auth'", 'const getToken = stubs.getToken; const getRefreshToken = stubs.getRefreshToken; const setAuthTokens = stubs.setAuthTokens')
    .replace("import router from '../router'", 'const router = stubs.router')
    .replace('export default service', 'module.exports = service')

  const sandbox = {
    module: { exports: {} },
    exports: {},
    process: { env: {} },
    stubs,
    setTimeout,
    clearTimeout
  }

  vm.runInNewContext(transformed, sandbox, { filename: filePath })
  return sandbox.module.exports
}

async function main() {
  let responseErrorHandler

  loadRequestModule({
    axios: {
      create() {
        return {
          interceptors: {
            request: { use() {} },
            response: {
              use(success, error) {
                responseErrorHandler = error
              }
            }
          }
        }
      }
    },
    Message: {
      error() {}
    },
    store: {
      dispatch() {
        return Promise.resolve()
      }
    },
    getToken: () => 'expired-token',
    getRefreshToken: () => '',
    setAuthTokens: () => {},
    router: {
      currentRoute: { path: '/system/user' },
      push() {
        return Promise.resolve()
      }
    }
  })

  assert.ok(responseErrorHandler, 'expected response interceptor error handler to be registered')

  const handledPromise = responseErrorHandler({
    config: {
      url: '/api/system/users/page',
      method: 'get'
    },
    response: { status: 401 }
  })

  const raceResult = await Promise.race([
    handledPromise.then(
      () => 'resolved',
      () => 'rejected'
    ),
    new Promise(resolve => setTimeout(() => resolve('timeout'), 50))
  ])

  assert.strictEqual(raceResult, 'timeout', 'expected handled 401 promise to stay pending while redirect takes over')

  console.log('request-401-pending-after-redirect.test.js passed')
}

main().catch(error => {
  console.error(error)
  process.exit(1)
})
