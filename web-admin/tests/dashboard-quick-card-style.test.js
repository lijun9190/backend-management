const assert = require('assert')
const fs = require('fs')
const path = require('path')

const filePath = path.resolve(__dirname, '../src/views/dashboard/index.vue')
const source = fs.readFileSync(filePath, 'utf8')

assert.ok(source.includes('quick-card'), 'expected quick entries to render as cards')
assert.ok(source.includes('quick-card__desc'), 'expected quick cards to include descriptive copy')
assert.ok(!source.includes('class="quick-btn"'), 'expected legacy button shortcut style to be removed')

console.log('dashboard-quick-card-style.test.js passed')
