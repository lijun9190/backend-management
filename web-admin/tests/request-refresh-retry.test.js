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
  let refreshCallCount = 0
  let retryRequestConfig = null
  let storedTokens = null
  let createCount = 0

  const mainService = {
    interceptors: {
      request: { use() {} },
      response: {
        use(success, error) {
          responseErrorHandler = error
        }
      }
    },
    request(config) {
      retryRequestConfig = config
      return Promise.resolve({ code: 200, data: { ok: true } })
    }
  }

  loadRequestModule({
    axios: {
      create() {
        createCount += 1
        if (createCount === 1) {
          return mainService
        }
        return {
          post(url, data) {
            refreshCallCount += 1
            assert.strictEqual(url, '/api/auth/refresh')
            assert.strictEqual(data.refreshToken, 'refresh-token-1')
            return Promise.resolve({
              data: {
                code: 200,
                data: {
                  accessToken: 'access-token-2',
                  refreshToken: 'refresh-token-2'
                }
              }
            })
          }
        }
      }
    },
    Message: {
      error(message) {
        throw new Error(`did not expect error message during refresh flow: ${message}`)
      }
    },
    store: {
      commit(action, payload) {
        if (action === 'user/SET_TOKENS') {
          storedTokens = payload
        }
      },
      dispatch() {
        throw new Error('did not expect clear session during successful refresh flow')
      }
    },
    getToken: () => 'access-token-1',
    getRefreshToken: () => 'refresh-token-1',
    setAuthTokens: () => {},
    router: {
      push() {
        throw new Error('did not expect redirect during successful refresh flow')
      }
    }
  })

  assert.ok(responseErrorHandler, 'expected response interceptor error handler to be registered')

  const result = await responseErrorHandler({
    config: {
      url: '/api/system/users/page',
      method: 'get',
      headers: {}
    },
    response: { status: 401 }
  })

  assert.strictEqual(refreshCallCount, 1, 'expected refresh endpoint to be called once')
  assert.ok(storedTokens, 'expected refreshed tokens to be stored')
  assert.strictEqual(storedTokens.accessToken, 'access-token-2')
  assert.strictEqual(storedTokens.refreshToken, 'refresh-token-2')
  assert.ok(retryRequestConfig, 'expected original request to be retried')
  assert.strictEqual(retryRequestConfig._retry, true, 'expected retried request to be marked')
  assert.strictEqual(retryRequestConfig.headers.Authorization, 'Bearer access-token-2')
  assert.deepStrictEqual(result, { code: 200, data: { ok: true } })

  console.log('request-refresh-retry.test.js passed')
}

main().catch(error => {
  console.error(error)
  process.exit(1)
})
