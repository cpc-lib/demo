# Authenticated Cart And Multi-Item Order Implementation Plan

> **For Codex:** REQUIRED SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Apply superpowers:test-driven-development for every backend behavior change and superpowers:verification-before-completion before claiming completion.

**Goal:** Add USER/ADMIN authentication, a persistent per-user cart, multi-course checkout, and order-based payment while preserving the three existing payment channels and order-level refund/reconciliation.

**Architecture:** Spring MVC interceptors authenticate short-lived JWT access tokens and enforce USER/ADMIN routes; opaque rotating refresh tokens are stored as SHA-256 hashes. MySQL remains the source of truth for users, cart, cart items, order items, and checkout idempotency. Checkout locks per user, snapshots current product prices into one order, clears the cart in the same transaction, then the selected existing payment integration pays that order. React and Vue keep access tokens in memory and use the refresh cookie through a single-flight Axios retry queue.

**Tech Stack:** Spring Boot 2.3.7, Java 8 language level, MyBatis-Plus 3.3.1, JJWT, BCrypt, MySQL, Redisson, JUnit 5/Mockito, React 18/Ant Design/Vite, Vue 2/Element UI/Vue CLI.

**Issue classification:** `type: compatibility`. Existing purchase, order, refund, configuration, bill, and reconciliation endpoints gain authentication or role requirements. Existing callback protocols and response envelope fields remain compatible.

**Success criteria:**

- Passwords are stored only as BCrypt hashes; access tokens expire after 15 minutes.
- Refresh tokens rotate, are persisted only as SHA-256 hashes, and replay revokes the token family.
- USER can modify only their cart and orders; ADMIN-only operations return 403 to USER; missing/invalid access tokens return 401.
- A cart accepts 1-99 copies per course and at most 20 distinct courses.
- Checkout is idempotent by `(user_id, checkout_request_id)`, snapshots current prices, creates one order plus multiple items, and clears the cart atomically.
- WeChat V3, WeChat V2, and Alipay can initiate payment from an existing order number.
- Existing order-level refund and reconciliation behavior remains green.
- React and Vue expose equivalent login, cart, my-order, retry-payment, and role-aware navigation flows.

---

## Task 1: Lock The Authentication And Authorization Contract

**Files:**

- Modify: `payment-demo/pom.xml`
- Create: `payment-demo/src/test/java/cc/ivera/security/JwtTokenServiceTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/AuthServiceImplTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/security/AuthInterceptorTest.java`
- Create: `payment-demo/src/main/java/cc/ivera/config/AuthProperties.java`
- Create: `payment-demo/src/main/java/cc/ivera/enums/UserRole.java`
- Create: `payment-demo/src/main/java/cc/ivera/entity/User.java`
- Create: `payment-demo/src/main/java/cc/ivera/entity/RefreshToken.java`
- Create: `payment-demo/src/main/java/cc/ivera/mapper/UserMapper.java`
- Create: `payment-demo/src/main/java/cc/ivera/mapper/RefreshTokenMapper.java`
- Create: `payment-demo/src/main/java/cc/ivera/security/AuthUser.java`
- Create: `payment-demo/src/main/java/cc/ivera/security/AuthContext.java`
- Create: `payment-demo/src/main/java/cc/ivera/security/JwtTokenService.java`
- Create: `payment-demo/src/main/java/cc/ivera/security/AuthInterceptor.java`
- Create: `payment-demo/src/main/java/cc/ivera/security/AdminInterceptor.java`
- Create: `payment-demo/src/main/java/cc/ivera/config/WebMvcConfig.java`

**Steps:**

1. Add JJWT and Spring Security Crypto dependencies only; do not enable the full Spring Security filter chain.
2. Write tests for JWT subject/role/expiry, missing or malformed bearer tokens (401), USER on admin routes (403), and thread-local cleanup.
3. Run `mvn -Dtest=JwtTokenServiceTest,AuthInterceptorTest test` and verify the tests fail because the security classes do not exist.
4. Implement the minimum properties, token service, request context, and interceptors to pass.
5. Run the focused tests and `mvn -DskipTests package`.

## Task 2: Implement Registration, Login, Rotation, Logout, Me, And Password Change

**Files:**

- Create: `payment-demo/src/main/java/cc/ivera/dto/RegisterRequest.java`
- Create: `payment-demo/src/main/java/cc/ivera/dto/LoginRequest.java`
- Create: `payment-demo/src/main/java/cc/ivera/dto/PasswordChangeRequest.java`
- Create: `payment-demo/src/main/java/cc/ivera/vo/AuthSession.java`
- Create: `payment-demo/src/main/java/cc/ivera/service/AuthService.java`
- Create: `payment-demo/src/main/java/cc/ivera/service/impl/AuthServiceImpl.java`
- Create: `payment-demo/src/main/java/cc/ivera/controller/AuthController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/handler/GlobalExceptionHandler.java`
- Modify: `payment-demo/src/main/java/cc/ivera/config/WebMvcConfig.java`
- Test: `payment-demo/src/test/java/cc/ivera/service/impl/AuthServiceImplTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/controller/AuthControllerTest.java`

**Steps:**

1. Write failing tests for USER-only registration, BCrypt matching, duplicate username rejection, invalid login, refresh rotation, replay family revocation, logout current family, and password-change global revocation.
2. Run `mvn -Dtest=AuthServiceImplTest,AuthControllerTest test` and confirm RED for missing behavior.
3. Implement opaque 256-bit refresh tokens, SHA-256 persistence, 7-day expiry, rotation links, and access-token responses.
4. Set the refresh cookie with `HttpOnly`, `SameSite=Lax`, `Path=/api/auth`, configured `Secure`, and no raw token in JSON.
5. Keep `R` response fields for success and auth errors; add explicit 401/403 exception handling.
6. Run focused tests and verify no plaintext password or refresh-token logging with `rg`.

## Task 3: Implement The Persistent User Cart

**Files:**

- Create: `payment-demo/src/main/java/cc/ivera/entity/Cart.java`
- Create: `payment-demo/src/main/java/cc/ivera/entity/CartItem.java`
- Create: `payment-demo/src/main/java/cc/ivera/mapper/CartMapper.java`
- Create: `payment-demo/src/main/java/cc/ivera/mapper/CartItemMapper.java`
- Create: `payment-demo/src/main/java/cc/ivera/dto/CartItemRequest.java`
- Create: `payment-demo/src/main/java/cc/ivera/vo/CartItemView.java`
- Create: `payment-demo/src/main/java/cc/ivera/vo/CartView.java`
- Create: `payment-demo/src/main/java/cc/ivera/service/CartService.java`
- Create: `payment-demo/src/main/java/cc/ivera/service/impl/CartServiceImpl.java`
- Create: `payment-demo/src/main/java/cc/ivera/controller/CartController.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/CartServiceImplTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/controller/CartControllerTest.java`

**Steps:**

1. Write failing tests for add, aggregate quantity, update, remove, clear, missing product, quantity outside 1-99, aggregate above 99, and the 20-course limit.
2. Verify RED with `mvn -Dtest=CartServiceImplTest,CartControllerTest test`.
3. Implement one lazily created cart per user and price-enriched cart reads from the current product table.
4. Derive user identity only from `AuthContext`, never from a request body or query parameter.
5. Run focused tests and the existing 66-test characterization suite.

## Task 4: Implement Idempotent Multi-Item Checkout

**Files:**

- Create: `payment-demo/src/main/java/cc/ivera/entity/OrderItem.java`
- Create: `payment-demo/src/main/java/cc/ivera/mapper/OrderItemMapper.java`
- Create: `payment-demo/src/main/java/cc/ivera/dto/CheckoutRequest.java`
- Create: `payment-demo/src/main/java/cc/ivera/vo/CheckoutResult.java`
- Modify: `payment-demo/src/main/java/cc/ivera/entity/OrderInfo.java`
- Modify: `payment-demo/src/main/java/cc/ivera/mapper/OrderInfoMapper.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/OrderInfoService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/OrderInfoServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/OrderInfoController.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/CartCheckoutServiceTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/controller/OrderOwnershipTest.java`

**Steps:**

1. Write failing tests for empty cart, current-price totals, title snapshot, multiple quantities, same request ID reuse, different-user isolation, my-list, admin list, owner order-item access, and query-status ownership.
2. Verify RED with `mvn -Dtest=CartCheckoutServiceTest,OrderOwnershipTest test`.
3. Under a per-user distributed lock and one database transaction, re-read the cart, create the order and items, then clear cart items.
4. Preserve legacy nullable `product_id`; for a single cart item use its title as order title, otherwise use a concise course-count title.
5. Catch the idempotency unique-key race by loading the existing `(user_id, checkout_request_id)` order.
6. Run focused tests, the 66-test suite, and `mvn -DskipTests package`.

## Task 5: Pay Existing Orders Through All Three Channels

**Files:**

- Modify: `payment-demo/src/main/java/cc/ivera/controller/WxPayController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/WxPayV2Controller.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/AliPayController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/wxpay/WxPayOrderFacade.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/wxpay/WxPayOrderService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/AliPayService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/AliPayServiceImpl.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/ExistingOrderPaymentTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/characterization/PublicApiCharacterizationTest.java`

**Steps:**

1. Write failing tests for owner-only order payment, NOTPAY-only initiation, payment-app/channel consistency, and request amount/title sourced from the order.
2. Verify RED with `mvn -Dtest=ExistingOrderPaymentTest,PublicApiCharacterizationTest test`.
3. Add the three `/order/{orderNo}` routes while retaining existing product-ID routes behind authentication.
4. Reuse channel request builders and callbacks; do not create a second order during retry payment.
5. Run focused tests and verify callbacks remain public in interceptor route tests.

## Task 6: Apply Role And Ownership Rules To Existing APIs

**Files:**

- Modify: `payment-demo/src/main/java/cc/ivera/config/WebMvcConfig.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/RefundApplicationController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/RefundInfoController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/PaymentAppController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/PaymentAppService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/PaymentAppServiceImpl.java`
- Create: `payment-demo/src/test/java/cc/ivera/security/ApiAuthorizationMatrixTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/RefundOwnershipTest.java`

**Steps:**

1. Write failing matrix tests for public products/auth/callbacks; authenticated cart/checkout/own order/refund; and ADMIN configuration/bills/reconciliation/refund approval/global orders.
2. Write failing ownership tests for refund apply/list and order status.
3. Add an authenticated read-only enabled-payment-app endpoint for checkout.
4. Enforce ownership at the service/controller boundary before refund or order operations.
5. Run focused tests and all existing refund/reconciliation tests.

## Task 7: Extend The Single SQL And Runtime Configuration

**Files:**

- Modify: `payment-demo/sql/payment-demo.sql`
- Modify: `payment-demo/src/main/resources/application.yml`
- Modify: `payment-demo/README.md`
- Create: `payment-demo/src/test/java/cc/ivera/schema/AuthCartSchemaContractTest.java`

**Steps:**

1. Write a schema contract test that reads the consolidated SQL and asserts the new tables, foreign keys/indexes, order columns, uniqueness rules, and BCrypt-shaped admin seed.
2. Verify RED before editing SQL.
3. Add `t_user`, `t_refresh_token`, `t_cart`, `t_cart_item`, `t_order_item`, order checkout columns/indexes, and an `admin` seed with a pre-generated BCrypt hash for `Admin@123456`.
4. Document `PAYMENT_AUTH_JWT_SECRET`, refresh cookie security, initial admin credential, and the mandatory first password change.
5. Run schema contract tests, `rg` for stale upgrade-script references, and `git diff --check`.

## Task 8: Implement React Authentication, Cart, Checkout, And My Orders

**Files:**

- Create: `payment-demo-react/src/api/auth.js`
- Create: `payment-demo-react/src/api/cart.js`
- Modify: `payment-demo-react/src/api/orderInfo.js`
- Modify: `payment-demo-react/src/api/wxPay.js`
- Modify: `payment-demo-react/src/api/aliPay.js`
- Modify: `payment-demo-react/src/utils/request.js`
- Create: `payment-demo-react/src/auth/AuthContext.jsx`
- Create: `payment-demo-react/src/components/ProtectedRoute.jsx`
- Modify: `payment-demo-react/src/components/AppHeader.jsx`
- Modify: `payment-demo-react/src/App.jsx`
- Modify: `payment-demo-react/src/pages/Home.jsx`
- Create: `payment-demo-react/src/pages/Login.jsx`
- Create: `payment-demo-react/src/pages/Cart.jsx`
- Modify: `payment-demo-react/src/pages/Orders.jsx`
- Modify: `payment-demo-react/src/assets/css/global.css`

**Steps:**

1. Load and follow `design-taste-frontend` and `build-web-apps:react-best-practices` before editing React.
2. Add an in-memory auth store; on startup call refresh with credentials and gate protected screens until bootstrap completes.
3. Implement one refresh promise shared by concurrent 401 responses; retry each original request once and redirect to login if refresh fails.
4. Convert the catalog to add-to-cart interactions, add the independent cart summary/payment-app picker, and create checkout before starting payment.
5. Show my orders with item snapshots and a continue-payment action for NOTPAY orders; show admin navigation only to ADMIN.
6. Verify with `npm run build`; treat existing chunk-size warning as a baseline warning, not a new failure.

## Task 9: Implement Vue Feature Parity

**Files:**

- Create: `payment-demo-vue/src/api/auth.js`
- Create: `payment-demo-vue/src/api/cart.js`
- Modify: `payment-demo-vue/src/api/orderInfo.js`
- Modify: `payment-demo-vue/src/api/wxPay.js`
- Modify: `payment-demo-vue/src/api/aliPay.js`
- Modify: `payment-demo-vue/src/utils/request.js`
- Create: `payment-demo-vue/src/auth/store.js`
- Modify: `payment-demo-vue/src/router/index.js`
- Modify: `payment-demo-vue/src/components/AppHeader.vue`
- Modify: `payment-demo-vue/src/App.vue`
- Modify: `payment-demo-vue/src/views/index.vue`
- Create: `payment-demo-vue/src/views/Login.vue`
- Create: `payment-demo-vue/src/views/Cart.vue`
- Modify: `payment-demo-vue/src/views/Orders.vue`
- Modify: `payment-demo-vue/src/assets/css/global.css`

**Steps:**

1. Implement the same refresh bootstrap, one-retry single-flight queue, route guards, role-aware navigation, and logout behavior as React.
2. Implement the same catalog, cart, checkout, payment selection, order-item display, and continue-payment behavior.
3. Verify with `npm run lint` and `npm run build`.

## Task 10: Reconcile Spec, Documentation, And Full Verification

**Files:**

- Move: `spec/planned/AUTH_CART_MULTI_ITEM_ORDER_COMPAT_SPEC.md` to `spec/implemented/AUTH_CART_MULTI_ITEM_ORDER_COMPAT_SPEC.md`
- Modify: `spec/README.md`

**Steps:**

1. Update the test map and API documentation with new authentication, cart, checkout, ownership, and payment-by-order coverage.
2. Move SPEC-008 to implemented only after all relevant code and tests are complete.
3. Run all new backend tests plus the 66 existing characterization/refund/reconciliation tests.
4. Run `mvn clean package` if infrastructure-independent tests allow; otherwise report the exact MySQL-dependent failures separately and keep the proven isolated suite.
5. Run React build, Vue lint/build, SQL contract tests, `git diff --check`, and a scoped `git status --short -- springboot-payment/payment-demo-java` from the repository root.
6. Inspect the final diff for plaintext secrets, accidental public admin routes, unrelated edits, and spec/implementation drift.
7. Use `superpowers:requesting-code-review`, address findings, then use `superpowers:verification-before-completion` and `superpowers:finishing-a-development-branch`.

---

## Verification Command Set

```powershell
Set-Location D:\code\demo\springboot-payment\payment-demo-java\payment-demo
mvn '-Dtest=JwtTokenServiceTest,AuthInterceptorTest,AuthServiceImplTest,AuthControllerTest,CartServiceImplTest,CartControllerTest,CartCheckoutServiceTest,OrderOwnershipTest,ExistingOrderPaymentTest,ApiAuthorizationMatrixTest,RefundOwnershipTest,AuthCartSchemaContractTest' test
mvn '-Dtest=InfrastructureBehaviorCharacterizationTest,PublicApiCharacterizationTest,RefundStatusSyncMessagingTest,RefundApplicationRabbitSyncTest,AliPayBillParserTest,ChannelBillServiceTest,ReconciliationBillDependencyTest,WxBillParserTest,WxPaymentRefundMatcherTest,WxPayBillServiceTest' test
mvn -DskipTests package

Set-Location D:\code\demo\springboot-payment\payment-demo-java\payment-demo-react
npm run build

Set-Location D:\code\demo\springboot-payment\payment-demo-java\payment-demo-vue
npm run lint
npm run build

Set-Location D:\code\demo\springboot-payment\payment-demo-java
git diff --check
rg -n "Admin@123456|password\s*[:=]|refreshToken" payment-demo/src payment-demo/sql payment-demo-react/src payment-demo-vue/src
```

**Known environment constraint:** The configured remote MySQL endpoint was unreachable during the baseline. Database-free unit/characterization tests and static SQL contract checks are authoritative in this workspace; any live integration test limitation must be reported explicitly and must not be described as passed.
