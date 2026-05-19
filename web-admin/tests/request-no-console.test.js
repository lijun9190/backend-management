const assert = require('assert')
const fs = require('fs')
const path = require('path')

function main() {
  const filePath = path.resolve(__dirname, '../src/utils/request.js')
  const source = fs.readFileSync(filePath, 'utf8')

  assert.ok(!source.includes('console.log'), 'request.js should not print full responses or error objects')
  assert.ok(!source.includes('console.error'), 'request.js should not print full responses or error objects')

  console.log('request-no-console.test.js passed')
}

try {
  main()
} catch (error) {
  console.error(error)
  process.exit(1)
}
