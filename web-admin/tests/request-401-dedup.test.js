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
    stubs
  }

  vm.runInNewContext(transformed, sandbox, { filename: filePath })
  return sandbox.module.exports
}

async function main() {
  let responseErrorHandler
  let clearSessionCount = 0
  let loginRedirectCount = 0
  let messageCount = 0

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
      error(message) {
        messageCount += 1
        return message
      }
    },
    store: {
      dispatch(action) {
        if (action === 'user/clearSession') {
          clearSessionCount += 1
        }
        return Promise.resolve()
      }
    },
    getToken: () => 'expired-token',
    getRefreshToken: () => '',
    setAuthTokens: () => {},
    router: {
      push(route) {
        if (route === '/login') {
          loginRedirectCount += 1
        }
      }
    }
  })

  assert.ok(responseErrorHandler, 'expected response interceptor error handler to be registered')

  responseErrorHandler({ response: { status: 401 } })
  responseErrorHandler({ response: { status: 401 } })
  responseErrorHandler({ response: { status: 401 } })

  await Promise.all([
    new Promise(resolve => setTimeout(resolve, 50))
  ])

  assert.strictEqual(messageCount, 1, 'expected 401 login-expired message to show only once')
  assert.strictEqual(clearSessionCount, 1, 'expected clear session to run only once for concurrent 401 responses')
  assert.strictEqual(loginRedirectCount, 1, 'expected redirect to login to run only once for concurrent 401 responses')

  console.log('request-401-dedup.test.js passed')
}

main().catch(error => {
  console.error(error)
  process.exit(1)
})
