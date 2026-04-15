# Dashboard Quick Cards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the Dashboard quick-entry area from plain buttons to card-style shortcuts with stronger hierarchy and hover feedback.

**Architecture:** Keep the existing Dashboard data flow and resolved route paths, but reshape the quick-entry rendering into custom shortcut cards. Enrich each quick menu item with icon and description metadata inside the component so the template can render a more polished layout without touching backend APIs.

**Tech Stack:** Vue 2 single-file component, Element UI icon classes, plain CSS in the scoped Dashboard stylesheet, Node-based source tests, Vue CLI build.

---

## File Structure

- Modify: `web-admin/src/views/dashboard/index.vue`
- Create: `web-admin/tests/dashboard-quick-card-style.test.js`
- Verify: `web-admin/tests/dashboard-quick-menu.test.js`
- Verify: `web-admin/tests/dashboard-hide-login-log.test.js`

### Task 1: Lock in the UI behavior with tests

**Files:**
- Create: `web-admin/tests/dashboard-quick-card-style.test.js`
- Verify: `web-admin/tests/dashboard-quick-menu.test.js`

- [ ] **Step 1: Write the failing style regression test**

```js
const source = fs.readFileSync(filePath, 'utf8')

assert.ok(source.includes('quick-card'), 'expected quick entries to render as cards')
assert.ok(source.includes('quick-card__desc'), 'expected quick cards to include descriptive copy')
assert.ok(!source.includes('class="quick-btn"'), 'expected legacy button shortcut style to be removed')
```

- [ ] **Step 2: Run the new test and verify it fails**

Run: `node web-admin/tests/dashboard-quick-card-style.test.js`
Expected: FAIL with a message about quick entries still using the legacy button layout

- [ ] **Step 3: Re-run the existing route-resolution test**

Run: `node web-admin/tests/dashboard-quick-menu.test.js`
Expected: PASS before any implementation change

### Task 2: Implement the Dashboard quick-card design

**Files:**
- Modify: `web-admin/src/views/dashboard/index.vue`

- [ ] **Step 1: Replace the quick-entry button list with clickable shortcut cards**

```vue
<button
  v-for="item in quickMenus"
  :key="item.path"
  type="button"
  class="quick-card"
  @click="$router.push(item.path)"
>
  <div class="quick-card__icon">
    <i :class="item.iconClass"></i>
  </div>
  <div class="quick-card__body">
    <div class="quick-card__title">{{ item.meta.title }}</div>
    <div class="quick-card__desc">{{ item.description }}</div>
  </div>
</button>
```

- [ ] **Step 2: Enrich each shortcut item with icon and description metadata**

```js
path: resolveRoutePath(child, route.path),
iconClass: (child.meta && child.meta.icon) || 'el-icon-data-analysis',
description: `进入${child.meta.title}，处理常用操作`
```

- [ ] **Step 3: Add polished card styling**

```css
.quick-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border-radius: 18px;
  border: 1px solid #dbe7f3;
  background: linear-gradient(135deg, #ffffff 0%, #f6f9fd 100%);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}
```

### Task 3: Verify the visual refactor safely

**Files:**
- Verify: `web-admin/tests/dashboard-quick-card-style.test.js`
- Verify: `web-admin/tests/dashboard-hide-login-log.test.js`
- Verify: `web-admin/tests/dashboard-quick-menu.test.js`
- Verify: `web-admin/tests/login-navigation.test.js`
- Verify: `web-admin/tests/router-order.test.js`
- Verify: `web-admin/src/views/dashboard/index.vue`

- [ ] **Step 1: Run the new style regression test**

Run: `node web-admin/tests/dashboard-quick-card-style.test.js`
Expected: PASS

- [ ] **Step 2: Run the existing dashboard regression tests**

Run: `node web-admin/tests/dashboard-hide-login-log.test.js`
Expected: PASS

Run: `node web-admin/tests/dashboard-quick-menu.test.js`
Expected: PASS

- [ ] **Step 3: Run route/login regression tests**

Run: `node web-admin/tests/login-navigation.test.js`
Expected: PASS

Run: `node web-admin/tests/router-order.test.js`
Expected: PASS

- [ ] **Step 4: Run the frontend production build**

Run: `npm run build`
Workdir: `web-admin`
Expected: PASS, with at most the existing bundle-size warnings

## Self-Review

### Spec coverage

- Card-style quick entries: Task 2
- Preserved shortcut navigation behavior: Task 1 and Task 3
- Visual polish without backend changes: Task 2

### Placeholder scan

- No `TODO`
- No `TBD`
- No unspecified “improve styling later” steps

### Type consistency

- Uses existing `quickMenus` computed property
- Keeps `item.path` as the click target
- Uses `meta.icon` with a fallback class so routes without icons still render consistently
