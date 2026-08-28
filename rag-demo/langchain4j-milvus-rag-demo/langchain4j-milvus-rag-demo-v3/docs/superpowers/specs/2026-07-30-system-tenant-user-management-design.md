# System Tenant And User Management Design

## Goal

Add a platform administration console for managing tenants and users, and add local password login for the web UI.

The implementation must preserve tenant isolation for ordinary users. Platform administrators can view and operate data across tenants through explicit admin APIs.

## Existing Context

- Tenant context is resolved by `TenantContextFilter` and `ConfiguredIdentityProvider`.
- Admin APIs under `/api/admin/**` are protected by configured admin roles through Spring Security.
- Cross-tenant administrative reads and writes must use `TenantContextHolder.callWithBypass`.
- Tenant and user base tables already exist: `sys_tenant`, `sys_user`, `sys_role`, `sys_user_role`.
- Existing admin functions include impersonation, quota, audit logs, and tenant deletion tasks.
- The current UI uses development tenant headers stored in local storage.

## Authentication

Add local account password login:

- `POST /api/auth/login`
  - Request: tenant code or tenant id, username or email, password.
  - Response: access token, token expiry, and current user context.
- `POST /api/auth/change-password`
  - Requires authentication.
  - Request: current password, new password.
  - Updates only the current user's password in the current tenant.
- `POST /api/auth/logout`
  - Requires authentication.
  - Records audit intent when possible. The browser clears the local token.

The token should be a signed JWT using the existing `rag.security.jwt.hmac-secret` when configured, with a development fallback only outside production. It carries tenant id, user id, display name, roles, knowledge base ids, and permission tags.

`ConfiguredIdentityProvider` should accept the local token via `Authorization: Bearer ...` and keep existing `dev`, `jwt`, and `gateway` behavior intact.

## Schema Changes

Extend `sys_user`:

- `password_hash VARCHAR(255) DEFAULT NULL`
- `password_updated_at DATETIME(3) DEFAULT NULL`
- `must_change_password TINYINT NOT NULL DEFAULT 0`

Update `src/main/resources/sql/all-in-one.sql` and `LocalSchemaInitializer` repairs so existing local databases are upgraded without adding new application profile files.

Passwords are stored only as BCrypt hashes. Plaintext passwords must never be logged.

## Platform Admin APIs

Add platform-only APIs under `/api/admin/system`.

Tenants:

- `GET /api/admin/system/tenants`
- `POST /api/admin/system/tenants`
- `PUT /api/admin/system/tenants/{tenantId}`
- `PUT /api/admin/system/tenants/{tenantId}/enable`
- `PUT /api/admin/system/tenants/{tenantId}/disable`

Users:

- `GET /api/admin/system/users?tenantId=&keyword=`
- `POST /api/admin/system/users`
- `PUT /api/admin/system/users/{id}`
- `PUT /api/admin/system/users/{id}/enable`
- `PUT /api/admin/system/users/{id}/disable`
- `PUT /api/admin/system/users/{id}/reset-password`

Roles:

- `GET /api/admin/system/roles?tenantId=`
- `PUT /api/admin/system/users/{id}/roles`

All system admin write operations record `SysOperationAuditLog`.

## Authorization Rules

- Platform admin roles come from `rag.security.admin-roles`.
- Non-platform users receive 403 for `/api/admin/system/**`.
- Platform admins can operate all tenants and users through bypass-scoped mapper calls.
- Tenant id `0` remains the platform/default tenant.
- Disabling a tenant prevents password login for users in that tenant.
- Disabling a user prevents password login for that user.

## Frontend

Add:

- Login page at `/login`.
- Auth token storage and `Authorization` header support in `ui/src/api/http.ts`.
- Password change entry in the app header.
- A system management page that replaces the current "Tenant Access" label with Chinese system management wording while preserving existing impersonation, quota, deletion, and audit functions.
- Tenant and user management tables with drawers/forms for create and edit.
- User actions for enable, disable, reset password, and role assignment.

The development header context can remain available for local debugging, but logged-in token identity should be the normal UI path.

## Error Handling

- Invalid credentials return 401 with a stable API error response.
- Disabled tenant or disabled user returns 403.
- Duplicate tenant code or duplicate user within a tenant returns 400.
- Password changes require the current password to match.
- Admin reset password sets `must_change_password=1`.

## Verification

Backend:

- Unit tests for password hashing/authentication.
- Unit tests for platform-admin guard and tenant/user CRUD service behavior.
- `mvn -q test`.

Frontend:

- Build verifies routes, API typing, and pages.
- `cmd /c npm run build` from `ui/`.
