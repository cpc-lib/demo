# System Tenant And User Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add platform tenant/user administration plus local password login and password change.

**Architecture:** Reuse the existing tenant context filter, admin role policy, MyBatis-Plus mappers, and tenant bypass helper. Add focused auth and system-admin services behind REST controllers, then update the React UI to use Bearer tokens and expose tenant/user operations.

**Tech Stack:** Java 21, Spring Boot 3.2.6, Spring Security, MyBatis-Plus, BCrypt, Nimbus JWT support already on classpath, React 18, TypeScript, Vite, Ant Design.

---

### Task 1: Schema And Domain Fields

**Files:**
- Modify: `src/main/resources/sql/all-in-one.sql`
- Modify: `src/main/java/cc/ivera/ragdemo/config/LocalSchemaInitializer.java`
- Modify: `src/main/java/cc/ivera/ragdemo/domain/tenant/SysUser.java`
- Test: `src/test/java/cc/ivera/ragdemo/config/LocalSchemaInitializerTest.java`

- [ ] **Step 1: Write schema repair test**

Add a test asserting local schema repair includes `sys_user.password_hash`, `sys_user.password_updated_at`, and `sys_user.must_change_password`.

- [ ] **Step 2: Run the focused test**

Run: `mvn -q -Dtest=LocalSchemaInitializerTest test`
Expected before implementation: fails because the new column repairs are absent.

- [ ] **Step 3: Implement schema/domain fields**

Add the three columns to `sys_user` in `all-in-one.sql`, add matching `ColumnRepair.after(...)` entries in `LocalSchemaInitializer`, and add fields to `SysUser`:

```java
@TableField("password_hash")
private String passwordHash;
@TableField("password_updated_at")
private LocalDateTime passwordUpdatedAt;
@TableField("must_change_password")
private Integer mustChangePassword;
```

- [ ] **Step 4: Verify task**

Run: `mvn -q -Dtest=LocalSchemaInitializerTest test`
Expected: PASS.

### Task 2: Local Auth Service

**Files:**
- Create: `src/main/java/cc/ivera/ragdemo/security/PasswordPolicy.java`
- Create: `src/main/java/cc/ivera/ragdemo/security/LocalJwtService.java`
- Create: `src/main/java/cc/ivera/ragdemo/security/LocalAuthService.java`
- Create: `src/main/java/cc/ivera/ragdemo/security/LoginRequest.java`
- Create: `src/main/java/cc/ivera/ragdemo/security/LoginResponse.java`
- Create: `src/main/java/cc/ivera/ragdemo/security/ChangePasswordRequest.java`
- Modify: `src/main/java/cc/ivera/ragdemo/config/SecurityConfig.java`
- Modify: `src/main/java/cc/ivera/ragdemo/tenant/ConfiguredIdentityProvider.java`
- Test: `src/test/java/cc/ivera/ragdemo/security/LocalAuthServiceTest.java`
- Test: `src/test/java/cc/ivera/ragdemo/security/LocalJwtServiceTest.java`

- [ ] **Step 1: Write auth tests**

Cover successful login, wrong password, disabled user, disabled tenant, and changing password with an incorrect current password.

- [ ] **Step 2: Run auth tests**

Run: `mvn -q -Dtest=LocalAuthServiceTest,LocalJwtServiceTest test`
Expected before implementation: fails because classes do not exist.

- [ ] **Step 3: Implement password and token services**

Use `BCryptPasswordEncoder`. Sign local tokens with `rag.security.jwt.hmac-secret` when present; outside production use a deterministic development fallback string with at least 32 bytes. Claims must align with existing JWT claim names.

- [ ] **Step 4: Allow auth public paths**

Permit `/api/auth/login`; keep `/api/auth/change-password` and `/api/auth/logout` authenticated.

- [ ] **Step 5: Verify task**

Run: `mvn -q -Dtest=LocalAuthServiceTest,LocalJwtServiceTest test`
Expected: PASS.

### Task 3: Auth Controller

**Files:**
- Create: `src/main/java/cc/ivera/ragdemo/controller/AuthController.java`
- Test: `src/test/java/cc/ivera/ragdemo/controller/AuthControllerTest.java`

- [ ] **Step 1: Write controller test**

Test that login returns a `RagApiResponse<LoginResponse>` and change-password delegates to `LocalAuthService`.

- [ ] **Step 2: Implement controller**

Expose:

```text
POST /api/auth/login
POST /api/auth/change-password
POST /api/auth/logout
```

- [ ] **Step 3: Verify task**

Run: `mvn -q -Dtest=AuthControllerTest test`
Expected: PASS.

### Task 4: System Admin Backend

**Files:**
- Create: `src/main/java/cc/ivera/ragdemo/admin/SystemTenantUserAdminService.java`
- Create: `src/main/java/cc/ivera/ragdemo/admin/SystemTenantRequest.java`
- Create: `src/main/java/cc/ivera/ragdemo/admin/SystemUserRequest.java`
- Create: `src/main/java/cc/ivera/ragdemo/admin/UserPasswordResetRequest.java`
- Create: `src/main/java/cc/ivera/ragdemo/admin/UserRolesUpdateRequest.java`
- Create: `src/main/java/cc/ivera/ragdemo/controller/SystemAdminController.java`
- Modify: `src/main/java/cc/ivera/ragdemo/domain/tenant/SysUserRole.java`
- Test: `src/test/java/cc/ivera/ragdemo/admin/SystemTenantUserAdminServiceTest.java`

- [ ] **Step 1: Write service tests**

Cover platform-admin guard, tenant create/update/enable/disable, user create/update/enable/disable/reset-password, and role replacement.

- [ ] **Step 2: Run service tests**

Run: `mvn -q -Dtest=SystemTenantUserAdminServiceTest test`
Expected before implementation: fails because service does not exist.

- [ ] **Step 3: Implement admin service**

Use `AdminRolePolicy` and `TenantContextHolder.callWithBypass`. Normalize role codes to uppercase. Use audit operation names:

```text
SYSTEM_TENANT_CREATE
SYSTEM_TENANT_UPDATE
SYSTEM_TENANT_ENABLE
SYSTEM_TENANT_DISABLE
SYSTEM_USER_CREATE
SYSTEM_USER_UPDATE
SYSTEM_USER_ENABLE
SYSTEM_USER_DISABLE
SYSTEM_USER_RESET_PASSWORD
SYSTEM_USER_ROLES_UPDATE
```

- [ ] **Step 4: Implement controller**

Expose all `/api/admin/system/**` endpoints listed in the design document.

- [ ] **Step 5: Verify task**

Run: `mvn -q -Dtest=SystemTenantUserAdminServiceTest test`
Expected: PASS.

### Task 5: Frontend Auth

**Files:**
- Modify: `ui/src/api/http.ts`
- Modify: `ui/src/api/rag.ts`
- Modify: `ui/src/types/index.ts`
- Create: `ui/src/pages/LoginPage.tsx`
- Create: `ui/src/pages/ChangePasswordPage.tsx`
- Modify: `ui/src/App.tsx`
- Modify: `ui/src/layouts/AppLayout.tsx`

- [ ] **Step 1: Add API/types**

Add `LoginRequest`, `LoginResponse`, `ChangePasswordRequest`, and auth token storage helpers. Add `Authorization: Bearer ${token}` when present.

- [ ] **Step 2: Add pages/routes**

Add `/login` route outside the main layout and `/change-password` inside it. Login stores token and user context. Logout clears token and navigates to `/login`.

- [ ] **Step 3: Verify frontend build**

Run from `ui`: `cmd /c npm run build`
Expected: PASS.

### Task 6: Frontend System Management UI

**Files:**
- Modify: `ui/src/pages/TenantAccessPage.tsx`
- Modify: `ui/src/api/rag.ts`
- Modify: `ui/src/types/index.ts`
- Modify: `ui/src/layouts/AppLayout.tsx`

- [ ] **Step 1: Add system admin APIs**

Add client functions for tenants, users, roles, user status, tenant status, password reset, and role update.

- [ ] **Step 2: Replace page shell with tabs**

Use Ant Design tables and drawers/forms for:

```text
租户管理
用户管理
角色管理
模拟租户
配额与审计
```

- [ ] **Step 3: Preserve existing operations**

Keep existing impersonation, quota, deletion task, and audit log functionality available on the page.

- [ ] **Step 4: Verify frontend build**

Run from `ui`: `cmd /c npm run build`
Expected: PASS.

### Task 7: Full Verification

**Files:**
- No source changes unless verification finds defects.

- [ ] **Step 1: Run backend tests**

Run:

```powershell
$env:JAVA_HOME='D:\develop\java\jdk21.0.11_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -q test
```

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run:

```powershell
cd ui
cmd /c npm run build
```

Expected: PASS.

- [ ] **Step 3: Report result**

Summarize changed files, new endpoints, and any manual restart needed for the backend/frontend dev servers.
