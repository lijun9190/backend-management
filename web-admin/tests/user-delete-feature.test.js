const assert = require('assert')
const fs = require('fs')
const path = require('path')

const apiFile = path.resolve(__dirname, '../src/api/user.js')
const viewFile = path.resolve(__dirname, '../src/views/system/user/index.vue')

const apiSource = fs.readFileSync(apiFile, 'utf8')
const viewSource = fs.readFileSync(viewFile, 'utf8')

assert.ok(apiSource.includes('export function removeUser'), 'expected user api to expose removeUser')
assert.ok(viewSource.includes("v-permission=\"'system:user:delete'\""), 'expected user page to render delete action with delete permission')
assert.ok(viewSource.includes('handleDelete'), 'expected user page to define a delete handler')

console.log('user-delete-feature.test.js passed')
