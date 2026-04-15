# User Delete Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add logical user deletion with backend safeguards and a frontend delete action.

**Architecture:** Extend the existing user-management flow rather than introducing a new module. The backend will add a delete endpoint plus service logic; the frontend will add a delete API function, a table action, and a confirmation flow.

**Tech Stack:** Spring Boot, MyBatis-Plus, MySQL, Vue 2, Element UI, Node source tests, JUnit tests.

---

## File Structure

- Modify: `backend/backend-system/src/main/java/com/example/system/controller/UserController.java`
- Modify: `backend/backend-system/src/main/java/com/example/system/service/UserService.java`
- Modify: `backend/backend-system/src/main/java/com/example/system/service/impl/UserServiceImpl.java`
- Modify: `backend/backend-system/src/main/java/com/example/system/mapper/SysUserMapper.java`
- Modify: `backend/backend-system/src/main/java/com/example/system/mapper/SysRoleMapper.java`
- Modify: `backend/sql/data.sql`
- Create: `backend/backend-system/src/test/java/com/example/system/service/UserServiceDeleteTest.java`
- Modify: `web-admin/src/api/user.js`
- Modify: `web-admin/src/views/system/user/index.vue`
- Create: `web-admin/tests/user-delete-feature.test.js`

## Task 1: Lock the behavior with tests

- [ ] Add a failing backend test proving delete marks the user as deleted and clears roles.
- [ ] Run the backend test and verify it fails for the expected reason.
- [ ] Add a failing frontend source test proving the user page exposes a delete action and API call.
- [ ] Run the frontend test and verify it fails.

## Task 2: Implement backend delete support

- [ ] Add `removeUser(Long id)` to the user service contract.
- [ ] Add `DELETE /api/system/users/{id}` to the controller with `system:user:delete`.
- [ ] Implement service logic: existence checks, self-delete guard, super-admin guard, role cleanup, logical delete, audit update, operation log.
- [ ] Add mapper helper for role-code lookup if needed by the super-admin guard.
- [ ] Update seed SQL with a `system:user:delete` permission entry for future initialization.

## Task 3: Implement frontend delete support

- [ ] Add `removeUser(id)` API helper in `web-admin/src/api/user.js`.
- [ ] Add a delete button to the user list action column.
- [ ] Add a confirmation flow and success message.
- [ ] Refresh the table after deletion.

## Task 4: Verify end to end

- [ ] Run backend delete tests.
- [ ] Run frontend delete test plus existing frontend regressions.
- [ ] Run `npm run build` in `web-admin`.
