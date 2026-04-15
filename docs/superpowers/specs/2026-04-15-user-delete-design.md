# User Delete Design

## Goal

Add a proper user deletion flow so operators can remove created users from the admin UI without touching the database manually.

## Recommended Approach

Use logical deletion instead of physical deletion.

This matches the existing `deleted` field pattern already used by users, departments, and menus. The backend will expose `DELETE /api/system/users/{id}`. The service will mark the user as deleted, update audit fields, and clear `sys_user_role` associations. The frontend user list will add a delete button with a confirmation dialog and refresh the table after success.

## Alternatives

1. Logical delete with role cleanup. Recommended.
   - Keeps auditability and aligns with current schema.
   - Safer for a management system demo.
2. Physical delete.
   - Simpler to understand at first glance.
   - Harder to recover from mistakes and less consistent with current model.
3. Disable-only workflow.
   - Lowest risk.
   - Does not solve the current problem because stale users still pollute the list.

## Safety Rules

- Do not allow deleting the current logged-in user.
- Do not allow deleting super administrator accounts.
- Keep the delete action permission-gated with `system:user:delete`.

## Data Flow

1. User clicks delete on the user list.
2. Frontend shows a confirmation dialog.
3. Frontend calls `DELETE /api/system/users/{id}`.
4. Backend verifies the user exists and passes safety checks.
5. Backend deletes user-role relations, marks `deleted = 1`, updates audit fields, and writes an operation log.
6. Frontend refreshes the list and the deleted user disappears because list queries already filter `deleted = 0`.

## Testing

- Backend regression test for service-level delete behavior.
- Frontend regression test for delete API/button wiring.
- Existing frontend regression tests should still pass.
