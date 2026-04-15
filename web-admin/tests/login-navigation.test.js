const assert = require('assert')
const fs = require('fs')
const path = require('path')
const vm = require('vm')

function loadLoginComponent() {
  const filePath = path.resolve(__dirname, '../src/views/login/index.vue')
  const source = fs.readFileSync(filePath, 'utf8')
  const scriptMatch = source.match(/<script>([\s\S]*?)<\/script>/)

  if (!scriptMatch) {
    throw new Error('failed to locate login component script')
  }

  const script = scriptMatch[1].replace('export default', 'module.exports =')
  const sandbox = {
    module: { exports: {} },
    exports: {},
    require,
    console,
    setTimeout,
    clearTimeout
  }

  vm.runInNewContext(script, sandbox, { filename: filePath })
  return sandbox.module.exports
}

async function flushMicrotasks() {
  await new Promise(resolve => setImmediate(resolve))
}

async function main() {
  const component = loadLoginComponent()
  let unhandledError = null

  const onUnhandledRejection = error => {
    unhandledError = error
  }

  process.once('unhandledRejection', onUnhandledRejection)

  try {
    const vmInstance = {
      loading: false,
      form: {
        username: 'admin',
        password: 'Admin@123456'
      },
      $route: {
        query: {}
      },
      $refs: {
        formRef: {
          validate(callback) {
            callback(true)
          }
        }
      },
      $store: {
        dispatch() {
          return Promise.resolve()
        }
      },
      $router: {
        push() {
          const error = new Error('Redirected when going from "/login" to "/dashboard/index" via a navigation guard.')
          error.name = 'NavigationRedirected'
          return Promise.reject(error)
        }
      }
    }

    Object.keys(component.methods).forEach(methodName => {
      vmInstance[methodName] = component.methods[methodName]
    })

    component.methods.handleLogin.call(vmInstance)
    await flushMicrotasks()
    await flushMicrotasks()

    assert.strictEqual(unhandledError, null, `expected redirected navigation to be handled, but got: ${unhandledError && unhandledError.message}`)
  } finally {
    process.removeListener('unhandledRejection', onUnhandledRejection)
  }

  console.log('login-navigation.test.js passed')
}

main().catch(error => {
  console.error(error)
  process.exit(1)
})
