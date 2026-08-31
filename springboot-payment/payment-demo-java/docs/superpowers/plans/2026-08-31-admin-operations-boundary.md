# 管理员运营边界与主动状态核对 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 ADMIN 彻底不能购物，同时在 React/Vue 管理端提供全部订单、退款审核、支付查单和退款状态核对。

**Architecture:** 在 `AuthContext` 集中定义购买者权限，控制器/订单创建服务共同阻断 ADMIN 的购买路径；保留现有订单与退款服务，通过一个支付宝查单薄包装补齐双渠道状态同步。两套前端分别增加 ADMIN 路由与退款审核视图，并在订单页按角色渲染只读运维操作。

**Tech Stack:** Spring Boot MVC、JUnit 5/Mockito、MyBatis-Plus、React 18 + React Router + Ant Design、Vue 2 + Vue Router + Element UI。

---

### Task 1: Lock the ADMIN purchase boundary with failing backend tests

**Files:**
- Modify: `payment-demo/src/test/java/cc/ivera/controller/CartControllerTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/controller/OrderOwnershipTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/controller/RefundOwnershipTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/characterization/PublicApiCharacterizationTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/controller/AdminPurchaseBoundaryTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/AdminOrderCreationBoundaryTest.java`

- [ ] **Step 1: Write the failing controller tests**

  Add tests that set `AuthContext` to `new AuthUser(1L, "admin", UserRole.ADMIN)` and assert `ForbiddenException` for:

  ```java
  assertThrows(ForbiddenException.class, () -> controller.getCart());
  assertThrows(ForbiddenException.class, () -> controller.checkout(new CheckoutRequest(9L, "req")));
  assertThrows(ForbiddenException.class, () -> refundController.apply(refundRequest));
  ```

  Extend `AdminPurchaseBoundaryTest` with direct-controller cases for the three channel product/order payment methods using mocked facades/services; the assertion is the same 403-domain exception and the facade must have zero interactions.

- [ ] **Step 2: Run the tests to verify the red state**

  Run from `payment-demo` with JDK 11 (Maven startup scripts must be skipped on this machine):

  ```powershell
  $env:MAVEN_SKIP_RC='1'
  $env:JAVA_HOME='D:\develop\java\jdk11.0.25_9'
  $env:Path="$env:JAVA_HOME\bin;$env:Path"
  mvn '-Dtest=AdminPurchaseBoundaryTest,CartControllerTest,OrderOwnershipTest,RefundOwnershipTest,AdminOrderCreationBoundaryTest' test
  ```

  Expected: new ADMIN assertions fail because controllers still call services for ADMIN.

- [ ] **Step 3: Add the shared guard and wire every buyer entry point**

  Add `AuthContext.requireShoppingUser()`:

  ```java
  public static AuthUser requireShoppingUser() {
      AuthUser user = requireUser();
      if (user.getRole() == UserRole.ADMIN) {
          throw new ForbiddenException("管理员账号不参与购物");
      }
      return user;
  }
  ```

  Use it in `CartController.currentUserId()`, `OrderInfoController.checkout`, `RefundApplicationController` before ownership checks, and the product/order payment methods in `WxPayController`, `WxPayV2Controller`, and `AliPayController`. In `OrderInfoServiceImpl.createOrReuseOrder`, reject an ADMIN context before acquiring the order lock so service-level calls cannot bypass the controller guard.

  Update existing characterization tests that call refund/checkout controllers without a context to authenticate a USER first; keep their USER assertions unchanged.

- [ ] **Step 4: Run the focused tests to verify green**

  Run the same Maven command from Step 2. Expected: all selected tests pass, ADMIN calls throw `ForbiddenException`, USER calls still delegate to the existing services.

- [ ] **Step 5: Commit the backend boundary**

  ```powershell
  git add payment-demo/src/main/java/cc/ivera/security/AuthContext.java payment-demo/src/main/java/cc/ivera/controller/CartController.java payment-demo/src/main/java/cc/ivera/controller/OrderInfoController.java payment-demo/src/main/java/cc/ivera/controller/RefundApplicationController.java payment-demo/src/main/java/cc/ivera/controller/WxPayController.java payment-demo/src/main/java/cc/ivera/controller/WxPayV2Controller.java payment-demo/src/main/java/cc/ivera/controller/AliPayController.java payment-demo/src/main/java/cc/ivera/service/impl/OrderInfoServiceImpl.java payment-demo/src/test/java/cc/ivera/controller/AdminPurchaseBoundaryTest.java payment-demo/src/test/java/cc/ivera/service/impl/AdminOrderCreationBoundaryTest.java payment-demo/src/test/java/cc/ivera/controller/CartControllerTest.java payment-demo/src/test/java/cc/ivera/controller/OrderOwnershipTest.java payment-demo/src/test/java/cc/ivera/controller/RefundOwnershipTest.java
  git commit -m "feat: block admin purchase operations"
  ```

### Task 2: Add and protect the missing payment-status query path

**Files:**
- Modify: `payment-demo/src/main/java/cc/ivera/controller/AliPayController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/config/WebMvcConfig.java`
- Create: `payment-demo/src/test/java/cc/ivera/controller/PaymentStatusControllerTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/security/ApiAuthorizationMatrixTest.java`

- [ ] **Step 1: Write the failing AliPay controller test**

  Construct `AliPayController` with mocks, set an ADMIN context, call `checkOrderStatus("ORDER-1")`, verify `aliPayService.checkOrderStatus("ORDER-1")`, and assert the response contains `orderNo=ORDER-1` and the local status returned by `orderInfoService.getOrderStatus("ORDER-1")`.

- [ ] **Step 2: Run the test to verify red**

  ```powershell
  mvn '-Dtest=PaymentStatusControllerTest' test
  ```

  Expected: compilation fails because `AliPayController.checkOrderStatus` does not exist.

- [ ] **Step 3: Implement the thin wrapper and route protection**

  Add to `AliPayController`:

  ```java
  @ApiOperation("主动查询支付宝支付状态")
  @GetMapping("/check-order-status/{orderNo}")
  public R<Map<String, Object>> checkOrderStatus(@PathVariable String orderNo) {
      orderInfoService.getOrderForUser(orderNo, AuthContext.requireUser());
      aliPayService.checkOrderStatus(orderNo);
      return R.ok().setMessage("查询成功")
              .data("orderNo", orderNo)
              .data("orderStatus", orderInfoService.getOrderStatus(orderNo));
  }
  ```

  Add `/api/wx-pay/check-order-status/**` and `/api/ali-pay/check-order-status/**` to `AdminInterceptor` patterns. Keep existing local polling `/api/order-info/query-order-status/**` unchanged.

- [ ] **Step 4: Run controller and route tests**

  ```powershell
  mvn '-Dtest=PaymentStatusControllerTest,ApiAuthorizationMatrixTest' test
  ```

  Expected: all pass, with both channel status routes matched by ADMIN protection.

- [ ] **Step 5: Commit the status-query API**

  ```powershell
  git add payment-demo/src/main/java/cc/ivera/controller/AliPayController.java payment-demo/src/main/java/cc/ivera/config/WebMvcConfig.java payment-demo/src/test/java/cc/ivera/controller/PaymentStatusControllerTest.java payment-demo/src/test/java/cc/ivera/security/ApiAuthorizationMatrixTest.java
  git commit -m "feat: expose admin payment status query"
  ```

### Task 3: Convert React to USER shopping and ADMIN operations

**Files:**
- Modify: `payment-demo-react/src/App.jsx`
- Modify: `payment-demo-react/src/components/AppHeader.jsx`
- Modify: `payment-demo-react/src/auth/AuthContext.jsx`
- Modify: `payment-demo-react/src/pages/Home.jsx`
- Modify: `payment-demo-react/src/pages/Orders.jsx`
- Modify: `payment-demo-react/src/api/orderInfo.js`
- Modify: `payment-demo-react/src/api/refundInfo.js`
- Create: `payment-demo-react/src/pages/Refunds.jsx`

- [ ] **Step 1: Add API methods before UI wiring**

  Add these client methods without duplicating URL literals in page components:

  ```js
  // orderInfo.js
  checkPaymentStatus(orderNo, channelCode) {
    return channelCode === 'ALIPAY'
      ? request.get(`/api/ali-pay/check-order-status/${orderNo}`)
      : request.get(`/api/wx-pay/check-order-status/${orderNo}`)
  }

  // refundInfo.js
  query(refundNo) { return request.post(`/api/refund-info/query/${refundNo}`) }
  reconcile(orderNo) { return request.post(`/api/refund-info/reconcile/${orderNo}`) }
  ```

- [ ] **Step 2: Update React navigation and route guards**

  In `App.jsx`, import `Refunds`, add `<Route path="/refunds" element={<ProtectedRoute role="ADMIN"><Refunds /></ProtectedRoute>} />`, set `role="USER"` on `/cart` and `/success`, and keep `/orders` available to both roles.

  In `AppHeader.jsx`, render the course link/cart badge only for non-admin users; label `/orders` as “我的订单” for USER and “全部订单” for ADMIN; add `/refunds` for ADMIN. In `AuthContext.refreshCartCount`, return zero without calling `cartApi.get()` when the session role is ADMIN.

- [ ] **Step 3: Hide the catalog for ADMIN and make Orders read-only in ADMIN mode**

  In `Home.jsx`, when `auth.user?.role === 'ADMIN'`, navigate to `/orders` with `replace: true`, skip the product request, and render no catalog. Keep the USER add-to-cart code unchanged.

  In `Orders.jsx`, branch only the action column and page title:

  - ADMIN actions: `checkPaymentStatus`, `reconcileRefunds`, `showRefundRecords`;
  - USER actions: existing retry/cancel/refund application/records;
  - never render payment dialogs or refund-application modals in ADMIN mode.

- [ ] **Step 4: Create the ADMIN refund page**

  `Refunds.jsx` loads `refundInfoApi.list()` on mount and renders an Ant Design table with `refundNo`, `orderNo`, `refund`, `approvalStatus`, `refundStatus`, `reason`, `approveRemark`, and `createTime`. Pending rows show approve/reject buttons that open a small remark modal; approved rows show “查询状态” calling `refundInfoApi.query`. After each mutation reload the list and display the API message.

- [ ] **Step 5: Build React**

  ```powershell
  npm run build
  ```

  Expected: Vite exits 0 and includes the new `/refunds` route/page.

- [ ] **Step 6: Commit React changes**

  ```powershell
  git add payment-demo-react/src/App.jsx payment-demo-react/src/components/AppHeader.jsx payment-demo-react/src/auth/AuthContext.jsx payment-demo-react/src/pages/Home.jsx payment-demo-react/src/pages/Orders.jsx payment-demo-react/src/api/orderInfo.js payment-demo-react/src/api/refundInfo.js payment-demo-react/src/pages/Refunds.jsx
  git commit -m "feat: add React admin refund operations"
  ```

### Task 4: Convert Vue to the same role behavior and operations

**Files:**
- Modify: `payment-demo-vue/src/router/index.js`
- Modify: `payment-demo-vue/src/components/AppHeader.vue`
- Modify: `payment-demo-vue/src/views/index.vue`
- Modify: `payment-demo-vue/src/views/Orders.vue`
- Modify: `payment-demo-vue/src/api/orderInfo.js`
- Modify: `payment-demo-vue/src/api/refundInfo.js`
- Create: `payment-demo-vue/src/views/Refunds.vue`

- [ ] **Step 1: Add Vue API methods**

  Add `orderInfoApi.checkPaymentStatus(orderNo, channelCode)` selecting `/api/ali-pay/check-order-status/` for `ALIPAY` and `/api/wx-pay/check-order-status/` otherwise; add `refundInfoApi.query(refundNo)` and `refundInfoApi.reconcile(orderNo)` with the same paths as React.

- [ ] **Step 2: Protect Vue routes and navigation**

  Add `/refunds` with `meta: { requiresAuth: true, role: 'ADMIN' }`; set `role: 'USER'` on `/cart` and `/success`. In `AppHeader.vue`, hide the course link/cart badge for ADMIN, label the orders link by role, add the refund link, and skip `cartApi.get()` for ADMIN.

- [ ] **Step 3: Hide the catalog and make Orders read-only for ADMIN**

  In `index.vue`, if `authState.user.role === 'ADMIN'`, replace the route with `/orders` before loading products. In `Orders.vue`, leave USER behavior intact and render ADMIN-only actions for payment status, refund reconciliation, and refund records; remove ADMIN retry/cancel/refund-application actions and dialogs.

- [ ] **Step 4: Create `Refunds.vue`**

  Use Element UI `el-table` and `el-dialog` to mirror React fields and actions. Pending rows open a remark prompt before `refundInfoApi.approve`/`reject`; approved rows call `refundInfoApi.query`; reload after every mutation.

- [ ] **Step 5: Build Vue**

  ```powershell
  npm run build
  ```

  Expected: Vue CLI exits 0.

- [ ] **Step 6: Commit Vue changes**

  ```powershell
  git add payment-demo-vue/src/router/index.js payment-demo-vue/src/components/AppHeader.vue payment-demo-vue/src/views/index.vue payment-demo-vue/src/views/Orders.vue payment-demo-vue/src/api/orderInfo.js payment-demo-vue/src/api/refundInfo.js payment-demo-vue/src/views/Refunds.vue
  git commit -m "feat: add Vue admin refund operations"
  ```

### Task 5: Update spec, run full verification, and migrate SPEC-009

**Files:**
- Modify: `spec/planned/ADMIN_OPERATIONS_AND_PURCHASE_BOUNDARY_SPEC.md`
- Modify: `spec/README.md`
- Modify: `payment-demo/src/test/java/cc/ivera/controller/AdminPurchaseBoundaryTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/service/impl/AdminOrderCreationBoundaryTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/controller/PaymentStatusControllerTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/security/ApiAuthorizationMatrixTest.java`

- [ ] **Step 1: Run the complete backend suite with the known working JDK 11 setup**

  ```powershell
  $env:MAVEN_SKIP_RC='1'
  $env:JAVA_HOME='D:\develop\java\jdk11.0.25_9'
  $env:Path="$env:JAVA_HOME\bin;$env:Path"
  mvn test
  ```

  Expected: `BUILD SUCCESS`, zero failures/errors.

- [ ] **Step 2: Run both frontend builds**

  ```powershell
  Push-Location payment-demo-react
  npm run build
  Pop-Location
  Push-Location payment-demo-vue
  npm run build
  Pop-Location
  ```

  Expected: both commands exit 0.

- [ ] **Step 3: Verify live role behavior**

  With backend on `http://localhost:8080`, authenticate as ADMIN and use the browser/network panel or curl with the access token to verify 403 for `GET /api/cart`, `POST /api/order-info/checkout`, a product payment endpoint, an order payment endpoint, and `/api/refund-info/apply`. Verify `OPTIONS` preflight remains 200.

  Verify ADMIN `GET /api/order-info/list`, `GET /api/refund-info/list`, `POST /api/refund-info/query/{refundNo}`, and both payment status routes reach the controller (channel credentials determine whether the external call succeeds; authorization must not return 403).

- [ ] **Step 4: Migrate SPEC-009 to implemented**

  Move `spec/planned/ADMIN_OPERATIONS_AND_PURCHASE_BOUNDARY_SPEC.md` to `spec/implemented/ADMIN_OPERATIONS_AND_PURCHASE_BOUNDARY_SPEC.md`, change status to `implemented`, add implementation anchors and the final backend test count, update the SPEC-009 row and detail in `spec/README.md`, and append the planned→implemented transition record.

- [ ] **Step 5: Run final repository checks**

  ```powershell
  git diff --check
  git status --short --branch
  ```

  Confirm only task files are changed by this work; preserve all unrelated pre-existing modifications.

- [ ] **Step 6: Commit the completed spec migration**

  ```powershell
  git add spec/implemented/ADMIN_OPERATIONS_AND_PURCHASE_BOUNDARY_SPEC.md spec/README.md
  git commit -m "docs: mark admin operations spec implemented"
  ```
