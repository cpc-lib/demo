# 商品库存管理与按明细退款 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不超卖的前提下完成商品上下架、库存流水、订单预占/实扣/释放、后端签发 120 秒订单幂等键，以及按订单明细数量退款并在渠道确认成功后回补库存；React 与 Vue 保持同一契约。

**Architecture:** MySQL 是商品、库存、订单、退款和消息状态的唯一事实源。商品行、订单行、退款申请行分别在短事务中加锁，库存变化通过唯一 `business_key` 记录流水；订单创建使用后端预签发并绑定 USER 的幂等记录，`COMPLETED` 映射长期返回原订单。需要访问支付渠道的关单和退款动作由同库 Outbox 记录驱动 RabbitMQ，消费者用 Inbox 唯一键和业务键防重复；支付通知、主动查单和退款通知复用统一的本地状态转换服务。

**Tech Stack:** Spring Boot MVC 2.3.7、MyBatis-Plus 3.3.1、MySQL/InnoDB、Spring `TransactionTemplate`、Redisson、RabbitMQ、JUnit 5/Mockito；React 18 + Vite + Ant Design；Vue 2 + Vue CLI + Element UI。

**Issue classification:** `type: compatibility`。新增商品/库存/订单幂等/退款明细 API，删除旧按金额退款契约，扩展数据库字段和前端请求结构。

**Execution policy:** 当前工作区有与本功能无关的既有修改。执行时只能触碰每个任务列出的文件，不得 reset、clean、覆盖其他改动。任务末尾用 `git diff --check` 和任务范围 diff 复核；是否提交由用户另行确认，计划中的提交点不代表自动提交授权。

---

### Task 1: Lock the database contract and baseline behavior

**Files:**

- Create: `payment-demo/src/test/java/cc/ivera/schema/ProductInventoryRefundSchemaContractTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/handler/ConflictExceptionHandlerTest.java`
- Create: `payment-demo/src/main/java/cc/ivera/exception/ConflictException.java`
- Modify: `payment-demo/src/main/java/cc/ivera/handler/GlobalExceptionHandler.java`
- Modify: `payment-demo/sql/payment-demo.sql`
- Modify: `payment-demo/src/main/resources/application.yml`

- [x] **Step 1: Write failing schema and exception-handler tests**

  Add schema assertions that read `sql/payment-demo.sql` as UTF-8 and require the following exact names: `status`, `available_stock`, `locked_stock`, `sold_stock`, and `version` on `t_product`; `inventory_status` and `refunded_quantity` on `t_order_item`; `application_request_id` on `t_refund_info`; tables `t_order_idempotency`, `t_refund_item`, `t_inventory_operation`, `t_message_outbox`, and `t_message_consume_log`; unique keys `uk_order_idempotency_key`, `uk_order_idempotency_order`, `uk_refund_order_request`, `uk_inventory_business_key`, `uk_outbox_event_key`, and `uk_consume_event_consumer`; and the `ORDER_RESERVE`, `ORDER_COMMIT`, `ORDER_RELEASE`, and `REFUND_RESTORE` operation types. Add a test that asserts the initialization script uses `available_stock + locked_stock + sold_stock` rather than a fourth total column.

  Cart/checkout saleability is covered in Task 3 after the product model exists. Public/admin route protection is covered in Task 2 together with the new controller, so every completed task remains green.

  ```java
  @Test
  void consolidatedSqlContainsInventoryAndMessageSchema() throws Exception {
      String sql = new String(Files.readAllBytes(Paths.get("sql", "payment-demo.sql")), StandardCharsets.UTF_8).toLowerCase();
      assertTrue(sql.contains("`available_stock` int"));
      assertTrue(sql.contains("`locked_stock` int"));
      assertTrue(sql.contains("`sold_stock` int"));
      assertTrue(sql.contains("create table `t_order_idempotency`"));
      assertTrue(sql.contains("unique key `uk_inventory_business_key` (`business_key`)"));
      assertTrue(sql.contains("unique key `uk_outbox_event_key` (`event_key`)"));
      assertTrue(sql.contains("unique key `uk_consume_event_consumer` (`event_id`, `consumer_name`)"));
  }

  @Test
  void conflictExceptionUsesHttp409() {
      ResponseEntity<R<Map<String, Object>>> response =
              new GlobalExceptionHandler().handleConflict(new ConflictException("库存不足"));
      assertEquals(409, response.getStatusCodeValue());
      assertEquals("库存不足", response.getBody().getMessage());
  }
  ```

- [x] **Step 2: Run the tests to verify the red state**

  Run from `payment-demo`:

  ```powershell
  mvn '-Dtest=ProductInventoryRefundSchemaContractTest,ConflictExceptionHandlerTest' test
  ```

  Expected: the new schema assertions fail because the inventory and message tables/columns do not exist; the existing baseline assertions remain diagnostic and are not edited to hide failures.

- [x] **Step 3: Add the consolidated SQL definitions**

  Preserve the script’s single-file initialization style. Drop new child tables before their parents, then add these exact columns to the existing `CREATE TABLE` blocks:

  ```sql
  -- t_product
  `status` varchar(16) NOT NULL DEFAULT 'OFF_SHELF',
  `available_stock` int unsigned NOT NULL DEFAULT 0,
  `locked_stock` int unsigned NOT NULL DEFAULT 0,
  `sold_stock` int unsigned NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,

  -- t_order_info
  `checkout_request_id` varchar(64) NULL,

  -- t_order_item
  `inventory_status` varchar(16) NOT NULL DEFAULT 'RESERVED',
  `refunded_quantity` int unsigned NOT NULL DEFAULT 0,

  -- t_refund_info
  `application_request_id` varchar(64) NOT NULL,
  UNIQUE KEY `uk_refund_order_request` (`order_no`, `application_request_id`),
  ```

  Add the four new tables with InnoDB, foreign keys to the existing order/product/refund rows where the relationship is local, and the unique constraints named in the test. `t_order_idempotency.status` accepts `ISSUED`, `COMPLETED`, and `EXPIRED`; `expires_at` is issued-time plus 120 seconds and is ignored for `COMPLETED`. `t_inventory_operation.business_key` is globally unique and stores before/after values and operator/reason snapshots. `t_message_outbox` stores event JSON, retry/claim fields, and a unique `event_key`; `t_message_consume_log` stores `(event_id, consumer_name)`.

  Update the four demo products to `ON_SHELF` with `available_stock=100`, `locked_stock=0`, and `sold_stock=0`. Keep the script’s existing table order and `SET FOREIGN_KEY_CHECKS` behavior.

  `ConflictException` extends `BizException`; `GlobalExceptionHandler.handleConflict` returns the existing `R` envelope with HTTP 409. Use this exception for stock shortage, version conflict, expired/cross-user/reused order keys, refund quantity conflicts, and request-id payload conflicts; retain `BizException` for ordinary HTTP-200 business validation to avoid changing unrelated legacy behavior.

- [x] **Step 4: Add runtime configuration and compile the model contract**

  Add under `payment.order` in `application.yml`:

  ```yaml
  payment:
    order:
      close-delay-ms: 60000
      idempotency-key-ttl-seconds: 120
  ```

  Run:

  ```powershell
  mvn '-Dtest=ProductInventoryRefundSchemaContractTest,ConflictExceptionHandlerTest' test
  ```

  Expected: the schema contract is green and no existing authentication/cart characterization test regresses.

- [x] **Step 5: Review only this task’s diff**

  ```powershell
  git diff --check
  git diff -- payment-demo/sql/payment-demo.sql payment-demo/src/main/resources/application.yml payment-demo/src/test/java/cc/ivera/schema/ProductInventoryRefundSchemaContractTest.java
  ```

  Confirm that no old table is dropped before its foreign-key children and that no plaintext credential is introduced.

### Task 2: Implement the product lifecycle and inventory operation ledger

**Files:**

- Create: `payment-demo/src/main/java/cc/ivera/enums/ProductStatus.java`
- Create: `payment-demo/src/main/java/cc/ivera/entity/InventoryOperation.java`
- Create: `payment-demo/src/main/java/cc/ivera/dto/ProductCreateRequest.java`
- Create: `payment-demo/src/main/java/cc/ivera/dto/ProductUpdateRequest.java`
- Create: `payment-demo/src/main/java/cc/ivera/dto/ProductStatusRequest.java`
- Create: `payment-demo/src/main/java/cc/ivera/dto/StockAdjustmentRequest.java`
- Create: `payment-demo/src/main/java/cc/ivera/vo/ProductAdminView.java`
- Create: `payment-demo/src/main/java/cc/ivera/mapper/InventoryOperationMapper.java`
- Create: `payment-demo/src/main/java/cc/ivera/service/ProductAdminService.java`
- Create: `payment-demo/src/main/java/cc/ivera/service/impl/ProductAdminServiceImpl.java`
- Create: `payment-demo/src/main/java/cc/ivera/controller/AdminProductController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/entity/Product.java`
- Modify: `payment-demo/src/main/java/cc/ivera/security/AuthContext.java`
- Modify: `payment-demo/src/main/java/cc/ivera/mapper/ProductMapper.java`
- Modify: `payment-demo/src/main/resources/mapper/ProductMapper.xml`
- Modify: `payment-demo/src/main/java/cc/ivera/service/ProductService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/ProductServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/ProductController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/config/WebMvcConfig.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/ProductAdminServiceTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/controller/AdminProductControllerTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/controller/ProductControllerTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/security/ApiAuthorizationMatrixTest.java`

- [x] **Step 1: Write failing service/controller tests**

  Cover: new products default to `OFF_SHELF`; name/price updates require the current `version`; status changes do not alter any stock bucket; positive/negative adjustments only change `available_stock`; a negative adjustment below zero returns 409-domain `BizException`; every adjustment inserts one `ADMIN_ADJUST:{requestId}:{productId}` operation; replaying the same request and payload is a no-op; replaying the same request with a different delta/reason is rejected; there is no delete method; public listing filters `ON_SHELF`, while the admin list includes both statuses and all three buckets.

  ```java
  @Test
  void duplicateStockAdjustmentWithSameBusinessKeyDoesNotChangeStockTwice() {
      Product locked = product(7L, ProductStatus.ON_SHELF, 10, 0, 0, 3);
      when(productMapper.selectByIdForUpdate(7L)).thenReturn(locked);
      when(operationMapper.selectByBusinessKey("ADMIN_ADJUST:req-1:7")).thenReturn(existingOperation(5));

      service.adjustStock(7L, new StockAdjustmentRequest("req-1", 5, "补货"), admin);

      verify(productMapper, never()).updateById(any(Product.class));
      verify(operationMapper, never()).insert(any(InventoryOperation.class));
  }
  ```

- [x] **Step 2: Run the focused tests to verify RED**

  ```powershell
  mvn '-Dtest=ProductAdminServiceTest,AdminProductControllerTest,ProductControllerTest,ApiAuthorizationMatrixTest' test
  ```

  Expected: compilation fails for the missing DTO/service/controller and the new `/api/admin/products/**` route.

- [x] **Step 3: Add the minimum domain and mapper contracts**

  Extend `Product` with `ProductStatus status`, `Integer availableStock`, `Integer lockedStock`, `Integer soldStock`, and `Integer version`. Use `ProductMapper.selectByIdForUpdate(Long)` for all stock mutations and add `selectOnShelf()` for the public catalog. Add `InventoryOperationMapper.selectByBusinessKey` and `selectByProductIdOrderByCreateTimeDesc`.

  Add the missing shared guard alongside the existing shopping guard:

  ```java
  public static AuthUser requireAdmin() {
      AuthUser user = requireUser();
      if (user.getRole() != UserRole.ADMIN) {
          throw new ForbiddenException("无权执行该操作");
      }
      return user;
  }
  ```

  Use these service signatures so later checkout/refund code can share the same stock-row rules:

  ```java
  public interface ProductAdminService {
      List<Product> listAdmin();
      Product create(ProductCreateRequest request, AuthUser operator);
      Product update(Long id, ProductUpdateRequest request, AuthUser operator);
      Product changeStatus(Long id, ProductStatusRequest request, AuthUser operator);
      InventoryOperation adjustStock(Long id, StockAdjustmentRequest request, AuthUser operator);
      List<InventoryOperation> listOperations(Long id);
  }

  public interface ProductService extends IService<Product> {
      List<Product> listPublicSaleable();
  }
  ```

  Validate product title as non-blank and at most 20 characters, price and initial stock as non-negative, update/status requests with a required `version`, and stock adjustment with a non-blank `requestId`, non-zero delta, and non-blank reason. The adjustment transaction locks the product row, checks the unique business key first, verifies `availableStock + delta >= 0`, updates only `available_stock` and `version`, then inserts the before/after operation. On a duplicate key, load the existing operation and compare product/delta/reason; equal input returns it and different input raises a 409 business error.

- [x] **Step 4: Add public/admin controllers and route protection**

  Keep `GET /api/product/list` public and return `productList` with `saleable = status == ON_SHELF && availableStock > 0`. Add an `AdminProductController` under `/api/admin/products` with:

  ```java
  @GetMapping
  public R<Map<String, Object>> list() { return R.ok().data("list", service.listAdmin()); }

  @PostMapping
  public R<Product> create(@Valid @RequestBody ProductCreateRequest request) {
      return R.ok(service.create(request, AuthContext.requireAdmin()));
  }

  @PostMapping("/{id}/stock-adjustments")
  public R<InventoryOperation> adjust(@PathVariable Long id,
                                      @Valid @RequestBody StockAdjustmentRequest request) {
      return R.ok(service.adjustStock(id, request, AuthContext.requireAdmin()));
  }
  ```

  Add the PUT status endpoints from SPEC-010 and include `/api/admin/products/**` and `/api/admin/outbox/**` in `AdminInterceptor`; narrow the auth public exclusion from `/api/product/**` to `/api/product/list`.

- [x] **Step 5: Run product tests and inspect the API matrix**

  ```powershell
  mvn '-Dtest=ProductAdminServiceTest,AdminProductControllerTest,ProductControllerTest,ApiAuthorizationMatrixTest,ProductInventoryRefundSchemaContractTest' test
  git diff --check
  ```

  Expected: ADMIN routes require ADMIN, USER receives 403, public listing returns only `ON_SHELF`, and every stock adjustment has one traceable business key.

### Task 3: Make the cart and catalog saleability-aware without reserving stock

**Files:**

- Modify: `payment-demo/src/main/java/cc/ivera/vo/CartItemView.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/CartServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/CartController.java`
- Modify: `payment-demo/src/test/java/cc/ivera/service/impl/CartServiceImplTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/controller/CartControllerTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/service/impl/CartCheckoutServiceTest.java`

- [x] **Step 1: Write failing cart tests for status, zero stock, and quantity**

  Require cart responses to expose `productStatus`, `availableStock`, `purchasable`, and `unavailableReason`. Assert that adding or increasing a line for `OFF_SHELF`, zero-stock, or requested quantity greater than current available stock raises a 409 business error. Assert that reading an existing unavailable line keeps it in the cart with `purchasable=false`; removal remains allowed and does not write an inventory operation.

  ```java
  @Test
  void zeroStockProductCannotBeAddedButExistingLineCanBeRemoved() {
      when(productMapper.selectById(9L)).thenReturn(product(9L, ProductStatus.ON_SHELF, 0, 0, 2, 1));
      assertThrows(BizException.class, () -> service.addItem(7L, 9L, 1));
      service.removeItem(7L, 9L);
      verify(cartItemMapper).deleteByCartAndProduct(20L, 9L);
  }
  ```

- [x] **Step 2: Run focused cart tests to verify RED**

  ```powershell
  mvn '-Dtest=CartServiceImplTest,CartControllerTest,CartCheckoutServiceTest' test
  ```

  Expected: the current view has no saleability fields and the current add path accepts unavailable products.

- [x] **Step 3: Implement read-time flags and write-time checks**

  Extend `CartItemView` with the four fields and build each view from the current product row. Set `unavailableReason` to one of `OFF_SHELF`, `SOLD_OUT`, or `INSUFFICIENT_STOCK`. In `addItem` and `updateItem`, load the product and call one shared validator:

  ```java
  private void requirePurchasable(Product product, int quantity) {
      if (product == null) throw new BizException("商品不存在");
      if (!ProductStatus.ON_SHELF.equals(product.getStatus())) throw new BizException("商品已下架");
      if (product.getAvailableStock() == null || product.getAvailableStock() <= 0) throw new BizException("商品已售罄");
      if (quantity > product.getAvailableStock()) throw new BizException("商品可用库存不足");
  }
  ```

  Do not decrement or lock stock in cart methods. Keep the existing 1–99 line limit and 20 distinct-product limit.

- [x] **Step 4: Run cart and baseline tests**

  ```powershell
  mvn '-Dtest=CartServiceImplTest,CartControllerTest,CartCheckoutServiceTest,PublicApiCharacterizationTest' test
  ```

  Expected: USER cart behavior is green, ADMIN remains blocked by `requireShoppingUser`, and no cart operation creates inventory rows.

- [x] **Step 5: Review response compatibility**

  ```powershell
  git diff --check
  rg -n "refundAmount|availableStock|purchasable|unavailableReason" payment-demo/src/main/java/cc/ivera/vo payment-demo/src/main/java/cc/ivera/service/impl/CartServiceImpl.java
  ```

  Confirm that new fields are additive and that no frontend still assumes every cart item is purchasable.

### Task 4: Issue backend-owned 120-second order keys and unify order creation

**Files:**

- Create: `payment-demo/src/main/java/cc/ivera/entity/OrderIdempotency.java`
- Create: `payment-demo/src/main/java/cc/ivera/mapper/OrderIdempotencyMapper.java`
- Create: `payment-demo/src/main/resources/mapper/OrderIdempotencyMapper.xml`
- Create: `payment-demo/src/main/java/cc/ivera/service/OrderIdempotencyService.java`
- Create: `payment-demo/src/main/java/cc/ivera/service/impl/OrderIdempotencyServiceImpl.java`
- Create: `payment-demo/src/main/java/cc/ivera/dto/OrderIdempotencyKeyView.java`
- Create: `payment-demo/src/main/java/cc/ivera/controller/OrderIdempotencyController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/dto/CheckoutRequest.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/CheckoutService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/CheckoutServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/OrderInfoService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/OrderInfoServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/OrderInfoController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/WxPayController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/WxPayV2Controller.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/AliPayController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/wxpay/WxPayOrderFacade.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/wxpay/WxPayOrderService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/AliPayService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/AliPayServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/mapper/OrderInfoMapper.java`
- Modify: `payment-demo/src/main/resources/mapper/OrderInfoMapper.xml`
- Modify: `payment-demo/src/main/java/cc/ivera/vo/CheckoutResult.java`
- Modify: `payment-demo/src/main/java/cc/ivera/config/WebMvcConfig.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/OrderIdempotencyServiceTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/service/impl/CartCheckoutServiceTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/controller/OrderOwnershipTest.java`

- [x] **Step 1: Write failing idempotency and checkout tests**

  Cover: USER-only key issuance; key UUID and `expiresAt = issuedAt + 120s`; ADMIN issuance returns 403; cross-user key use returns 409; unused expired key returns 409; failed order transaction leaves `ISSUED`; same key/same canonical request returns the same order without touching cart/products; same key/different request returns 409; 20 concurrent identical requests create one order, one item set, one reserve set, and one close event.

  ```java
  @Test
  void completedKeyIgnoresItsOriginalExpiryAndReturnsTheSameOrder() {
      OrderIdempotency key = issuedFor(7L, "key-1", Instant.now().minusSeconds(180));
      key.setStatus("COMPLETED");
      key.setOrderId(88L);
      key.setRequestFingerprint("fp-1");
      when(mapper.selectByKeyForUpdate("key-1")).thenReturn(key);
      when(orderMapper.selectById(88L)).thenReturn(order("ORDER-88"));

      CheckoutResult result = service.checkout(7L, 9L, "key-1", "fp-1");

      assertEquals("ORDER-88", result.getOrderNo());
      verify(cartMapper, never()).selectByUserIdForUpdate(anyLong());
  }
  ```

- [x] **Step 2: Run the idempotency tests to verify RED**

  ```powershell
  mvn '-Dtest=OrderIdempotencyServiceTest,CartCheckoutServiceTest,OrderOwnershipTest' test
  ```

  Expected: compilation fails for the key table/service and current checkout still expects a client-generated `checkoutRequestId`.

- [x] **Step 3: Implement issuance and row-locked lookup**

  Add `POST /api/order-info/idempotency-keys`, guarded by `AuthContext.requireShoppingUser()`, returning `{ idempotencyKey, expiresAt }`. Generate the UUID only on the server and bind it to `userId`; use the configured `payment.order.idempotency-key-ttl-seconds` defaulting to 120. Store the request fingerprint only on first order use. Keep unused expired rows for seven days, then clean them; never clean `COMPLETED` rows by this job.

  Add a checkout method that receives `userId`, `paymentAppId`, `idempotencyKey`, and a server-calculated fingerprint. Lock `payment:order:create:{userId}:{idempotencyKey}` and then `select ... for update` the key row. Apply this exact state order:

  ```java
  public interface CheckoutService {
      CheckoutResult checkout(Long userId, Long paymentAppId,
                              String idempotencyKey, String requestFingerprint);
  }
  ```

  ```java
  if (record == null || !userId.equals(record.getUserId())) throw new BizException("订单幂等键无效");
  if ("COMPLETED".equals(record.getStatus())) {
      if (!fingerprint.equals(record.getRequestFingerprint())) throw new BizException("订单幂等键请求参数不一致");
      return loadCheckoutResult(record.getOrderId());
  }
  if ("EXPIRED".equals(record.getStatus()) || record.getExpiresAt().before(new Date())) {
      // Use a separate REQUIRES_NEW conditional update so returning 409 does not roll it back.
      markIssuedRecordExpiredInNewTransaction(record.getId());
      throw new BizException("订单幂等键已过期");
  }
  // Only this valid ISSUED branch reads the cart, locks products, and creates an order.
  ```

  Set `t_order_info.checkout_request_id` to the server key. Remove client generation from `CheckoutRequest`; during the transition accept a body `checkoutRequestId` only when it exactly equals the header value.

  A keyed direct-buy request must not search for or reuse an arbitrary earlier `NOTPAY` order by product/payment type. Its only reuse source is the locked `t_order_idempotency` row; the direct-buy fingerprint uses the fixed quantity `1` because the existing endpoint has no quantity field.

- [x] **Step 4: Route direct-buy and cart checkout through the same creation contract**

  Add `Idempotency-Key` to every authenticated product direct-buy controller method (`WxPayController.nativePay`, `WxPayV2Controller.createNative`, and `AliPayController.tradePagePay`) and pass it through `WxPayOrderFacade`/`AliPayService` to `OrderInfoService.createOrReuseOrder(..., idempotencyKey)`. The no-key overloads may remain only as private/test adapters that reject an authenticated USER; no HTTP order creation path may bypass the key row. Calculate fingerprints from normalized entrance, product/quantity or cart snapshot, and payment application/channel.

  In cart checkout, lock cart and product rows in ascending product ID order. Create `RESERVED` order items and clear the cart in the same transaction. On any failure, roll back order, items, cart, key state, and outbox, leaving the key `ISSUED` while its 120-second window remains.

- [x] **Step 5: Run focused concurrency and authorization tests**

  ```powershell
  mvn '-Dtest=OrderIdempotencyServiceTest,CartCheckoutServiceTest,OrderOwnershipTest,AdminPurchaseBoundaryTest,AdminOrderCreationBoundaryTest' test
  ```

  Expected: duplicate key requests return one order; different fingerprints are 409; ADMIN cannot obtain or use a key; the existing USER order list/detail tests remain green.

### Task 5: Add the inventory state machine and wire payment/close transitions

**Files:**

- Create: `payment-demo/src/main/java/cc/ivera/service/InventoryService.java`
- Create: `payment-demo/src/main/java/cc/ivera/service/impl/InventoryServiceImpl.java`
- Create: `payment-demo/src/main/java/cc/ivera/enums/InventoryStatus.java`
- Modify: `payment-demo/src/main/java/cc/ivera/entity/OrderItem.java`
- Modify: `payment-demo/src/main/java/cc/ivera/mapper/OrderItemMapper.java`
- Modify: `payment-demo/src/main/java/cc/ivera/mapper/ProductMapper.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/CheckoutServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/wxpay/WxPayOrderService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/WxPayV2Controller.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/AliPayServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/mq/OrderCloseConsumer.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/OrderInfoServiceImpl.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/InventoryServiceTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/InventoryConcurrencyTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/controller/ExistingOrderPaymentTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/mq/RefundStatusSyncMessagingTest.java`

- [x] **Step 1: Write failing transition tests**

  Cover reserve success/insufficient stock, exactly-10-stock versus 50 concurrent checkout requests, `RESERVED -> SOLD` only once on payment success, `RESERVED -> RELEASED` only after confirmed unpaid/closed channel state, unknown state retaining locked stock, and payment/close race producing exactly one transition.

  ```java
  @Test
  void reserveUsesConditionalAvailableStockUpdateAndWritesOneOperation() {
      when(productMapper.reserve(7L, 3)).thenReturn(1);
      inventoryService.reserve(order("ORDER-1", item(7L, 3)));
      verify(operationMapper).insert(argThat(op -> "ORDER_RESERVE:ORDER-1:7".equals(op.getBusinessKey())));
  }
  ```

- [x] **Step 2: Run transition tests to verify RED**

  ```powershell
  mvn '-Dtest=InventoryServiceTest,InventoryConcurrencyTest,ExistingOrderPaymentTest' test
  ```

  Expected: the inventory service and order-item inventory fields do not yet exist.

- [x] **Step 3: Implement row-locked reserve/commit/release methods**

  Define the shared service contract:

  ```java
  public interface InventoryService {
      void reserve(OrderInfo order, List<OrderItem> items);
      boolean commitPayment(String orderNo);
      boolean releaseReservation(String orderNo);
  }
  ```

  `restoreRefund` is added in Task 8 together with trusted refund-item rows; Task 5 must not expose an empty restore contract.

  Each method locks order items and products in ascending product ID order, checks the expected `inventory_status`, and updates the three buckets plus an `InventoryOperation` in the same transaction. Use conditional SQL for the reserve (`status=ON_SHELF AND available_stock >= quantity`) and unique operation business keys as the final idempotency guard. Payment success first wins `NOTPAY -> SUCCESS`, then calls `commitPayment`; close first wins `NOTPAY -> CLOSED/CANCEL`, then calls `releaseReservation`. A repeated transition returns without changing buckets or inserting a second operation.

- [x] **Step 4: Wire all three payment success and close paths**

  In WeChat V3 `doSyncOrderStatusFromWxQuery` and notification handling, V2 `doProcessWxPayV2NotifyInTransaction`, and Alipay notification/query handling, call `inventoryService.commitPayment(orderNo)` immediately after the conditional order status change succeeds and before the transaction returns. In `OrderCloseConsumer` and user cancel paths, call `releaseReservation` only when the channel result is explicitly `NOTPAY` or `CLOSED`; network/unknown results leave the reservation locked and the message retryable. Keep the existing payment row idempotency and order lock.

- [x] **Step 5: Run payment, close, and concurrency tests**

  ```powershell
  mvn '-Dtest=InventoryServiceTest,InventoryConcurrencyTest,ExistingOrderPaymentTest,PaymentStatusControllerTest,RefundStatusSyncMessagingTest' test
  ```

  Expected: stock buckets never become negative, ten units permit exactly ten reserved units under 50 concurrent attempts, and repeated notifications/queries do not double-commit or release.

### Task 6: Replace direct Rabbit sends with transactional Outbox/Inbox delivery

**Files:**

- Create: `payment-demo/src/main/java/cc/ivera/entity/MessageOutbox.java`
- Create: `payment-demo/src/main/java/cc/ivera/entity/MessageConsumeLog.java`
- Create: `payment-demo/src/main/java/cc/ivera/mapper/MessageOutboxMapper.java`
- Create: `payment-demo/src/main/java/cc/ivera/mapper/MessageConsumeLogMapper.java`
- Create: `payment-demo/src/main/resources/mapper/MessageOutboxMapper.xml`
- Create: `payment-demo/src/main/java/cc/ivera/service/MessageOutboxService.java`
- Create: `payment-demo/src/main/java/cc/ivera/service/impl/MessageOutboxServiceImpl.java`
- Create: `payment-demo/src/main/java/cc/ivera/mq/RefundSubmitMessage.java`
- Create: `payment-demo/src/main/java/cc/ivera/mq/RefundSubmitConsumer.java`
- Create: `payment-demo/src/main/java/cc/ivera/config/RefundSubmitRabbitConfig.java`
- Create: `payment-demo/src/main/java/cc/ivera/task/OutboxPublisher.java`
- Create: `payment-demo/src/main/java/cc/ivera/service/MessageConsumeLogService.java`
- Create: `payment-demo/src/main/java/cc/ivera/service/impl/MessageConsumeLogServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/CheckoutServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/RefundApplicationServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/RefundStatusSyncMessageServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/mq/OrderCloseConsumer.java`
- Modify: `payment-demo/src/main/java/cc/ivera/mq/RefundStatusSyncConsumer.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/wxpay/WxPayOrderService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/WxPayV2Controller.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/AliPayServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/wxpay/WxPayRefundService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/config/OrderCloseRabbitConfig.java`
- Modify: `payment-demo/src/main/java/cc/ivera/config/RefundStatusSyncRabbitConfig.java`
- Modify: `payment-demo/src/main/resources/application.yml`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/AdminProductController.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/MessageOutboxServiceTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/mq/MessageIdempotencyTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/service/impl/CartCheckoutServiceTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/service/impl/RefundApplicationRabbitSyncTest.java`

- [x] **Step 1: Write failing Outbox/Inbox tests**

  Assert that order creation writes one `ORDER_CLOSE_SCHEDULED:{orderNo}` row in the same transaction; approval writes one `REFUND_SUBMIT_REQUESTED:{refundNo}` row and does not call a channel client; publisher retries NEW/FAILED rows with bounded backoff; a crash after publish and before `SENT` permits duplicate delivery; duplicate `(eventId, consumerName)` is acknowledged without repeating local work; ADMIN retry changes only FAILED rows to NEW.

  ```java
  @Test
  void duplicateConsumerDeliveryDoesNotExecuteRefundTwice() {
      when(consumeLogMapper.insertIgnore("event-1", "refund-submit")).thenReturn(0);
      consumer.handle(new RefundSubmitMessage("event-1", "REFUND-1"));
      verify(channelFacade, never()).executeRefund(any());
  }
  ```

- [x] **Step 2: Run messaging tests to verify RED**

  ```powershell
  mvn '-Dtest=MessageOutboxServiceTest,MessageIdempotencyTest,RefundApplicationRabbitSyncTest,CartCheckoutServiceTest' test
  ```

  Expected: current services call RabbitTemplate directly and no message tables/consumers exist.

- [ ] **Step 3: Implement transactional event persistence and publisher**

  `MessageOutboxService.insertOnce(eventKey, aggregateType, aggregateId, eventType, payload)` inserts the row with `NEW`; a duplicate `eventKey` loads and compares payload, returning the existing row for equal content and raising a conflict for different content. `MessageConsumeLogService.tryStart(eventId, consumerName, eventType, businessKey)` first performs insert-ignore and otherwise conditionally reclaims only `FAILED` or expired `PROCESSING` rows; it returns `CLAIMED` with a per-attempt token, `CONSUMED`, or `BUSY`, while mismatched event metadata raises a conflict. `BUSY` must remain retryable rather than being acknowledged as success. Consumers execute remote calls outside the database transaction, then use the same token to atomically commit local changes and `CONSUMED`; failures mark `FAILED`, and a crash is recovered after the lease expires. `OutboxPublisher` likewise uses a per-claim token, claims rows with a status/lease conditional update, publishes with Rabbit publisher confirms, marks `SENT` only after confirmation, and returns failed rows to `FAILED` with retry count and backoff. Expose `POST /api/admin/outbox/{eventId}/retry` to reset a FAILED row to NEW through `AuthContext.requireAdmin()`.

- [ ] **Step 4: Wire order close, refund submission, and status sync**

  Replace `sendCloseMessageAfterCommit` with an outbox insert inside checkout. In refund approval, transactionally mark approval `APPROVED` and insert `REFUND_SUBMIT_REQUESTED:{refundNo}`; `RefundSubmitConsumer` claims the Inbox row, invokes the existing channel facade outside the local transaction, and schedules the existing status-sync event. Add `MessageConsumeLogService.tryStart` to order payment notification handlers using WeChat notification ID/transaction ID or Alipay notification ID/transaction ID, and to refund notification/query synchronization using channel + refund number + channel status. Keep channel callbacks/queries as independent idempotent consumers. Preserve the current queue names where possible so existing RabbitMQ configuration remains usable.

  Enable publisher confirms in `application.yml` with `spring.rabbitmq.publisher-confirm-type: correlated`, `publisher-returns: true`, and `template.mandatory: true`; do not treat a Rabbit publish call without confirmation as delivered.

- [ ] **Step 5: Run messaging and route tests**

  ```powershell
  mvn '-Dtest=MessageOutboxServiceTest,MessageIdempotencyTest,RefundApplicationRabbitSyncTest,RefundStatusSyncMessagingTest,ApiAuthorizationMatrixTest' test
  ```

  Expected: duplicate Rabbit deliveries produce one local action; approval no longer makes a remote call inside the request transaction; only ADMIN can retry FAILED outbox events.

### Task 7: Convert refund application to order-item quantities and remove amount-based contracts

**Files:**

- Create: `payment-demo/src/main/java/cc/ivera/entity/RefundItem.java`
- Create: `payment-demo/src/main/java/cc/ivera/dto/RefundItemRequest.java`
- Create: `payment-demo/src/main/java/cc/ivera/dto/RefundApplyRequest.java`
- Create: `payment-demo/src/main/java/cc/ivera/mapper/RefundItemMapper.java`
- Create: `payment-demo/src/main/resources/mapper/RefundItemMapper.xml`
- Modify: `payment-demo/src/main/java/cc/ivera/entity/RefundInfo.java`
- Modify: `payment-demo/src/main/java/cc/ivera/entity/OrderItem.java`
- Modify: `payment-demo/src/main/java/cc/ivera/mapper/OrderItemMapper.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/RefundApplicationService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/RefundApplicationServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/RefundInfoServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/refund/OrderRefundStatusService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/RefundApplicationController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/RefundInfoController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/WxPayController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/AliPayController.java`
- Modify: `payment-demo/src/main/java/cc/ivera/dto/RefundRequest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/controller/RefundOwnershipTest.java`
- Modify: `payment-demo/src/test/java/cc/ivera/service/impl/RefundApplicationRabbitSyncTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/ItemizedRefundServiceTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/ItemizedRefundConcurrencyTest.java`

- [ ] **Step 1: Write failing itemized refund tests**

  Cover USER ownership, paid/partially-refunded order states, missing/duplicate item IDs, quantity > 0, quantity not exceeding `quantity - processingQuantity - refundedQuantity`, exact integer amount calculation from `unit_price`, same `requestId` and same items returning the original refund, same request ID with a different item set/reason returning 409, and at least 20 concurrent requests whose total accepted quantity never exceeds the purchased quantity.

  ```java
  @Test
  void refundAmountIsCalculatedFromSelectedOrderItems() {
      when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-1")).thenReturn(paidOrder("ORDER-1"));
      when(orderItemMapper.selectByOrderIdForUpdate(88L)).thenReturn(Arrays.asList(orderItem(10L, 7L, 3, 1250, 1)));
      when(refundItemMapper.sumReservedQuantityByOrderItemIds(anyList())).thenReturn(Collections.singletonMap(10L, 0));

      RefundInfo refund = service.createApplication(new RefundApplyRequest(
              "req-1", "ORDER-1", "不需要了", Collections.singletonList(new RefundItemRequest(10L, 2))));

      assertEquals(2500, refund.getRefund());
      verify(refundItemMapper).insert(argThat(item -> item.getQuantity() == 2 && item.getRefundAmount() == 2500));
  }
  ```

- [ ] **Step 2: Run refund tests to verify RED**

  ```powershell
  mvn '-Dtest=ItemizedRefundServiceTest,ItemizedRefundConcurrencyTest,RefundOwnershipTest,RefundApplicationRabbitSyncTest' test
  ```

  Expected: the current DTO/service only accepts `refundAmount`, and the old path mappings still compile.

- [ ] **Step 3: Implement locked quantity calculation and idempotent insert**

  Add `RefundItem` with `refundId`, `orderItemId`, product/title/unit-price snapshots, `quantity`, and `refundAmount`. Add persisted `refundedQuantity` plus non-persisted response fields `processingRefundQuantity` and `refundableQuantity` to `OrderItem`; the order-item mapper must populate those fields for `GET /api/order-info/{orderNo}/items`. Change `RefundInfoServiceImpl.createRefundApplication` to receive `RefundApplyRequest`; lock the order and all selected order items in ascending item ID order. Query all pending/approved/processing/success refund-item quantities for those items, subtract `processingQuantity` and `refundedQuantity`, and reject any excess. Sum `unitPrice * quantity` using `long`, reject values above `Integer.MAX_VALUE`, insert `t_refund_info` and all `t_refund_item` rows in one transaction, and set `application_request_id`.

  On `(order_no, application_request_id)` duplicate, load the stored refund and its items. Return it only when reason and normalized item/quantity pairs match; otherwise throw 409. Do not infer quantities from an amount and do not allow an empty item list.

- [ ] **Step 4: Remove old amount-based mappings and update status aggregation**

  Replace `RefundRequest` use with `RefundApplyRequest` in `/api/refund-info/apply`. Delete `applyLegacy` and the `/api/wx-pay/refunds` and `/api/ali-pay/trade/refund` application mappings. Remove `refundAmount` from the frontend-facing request type; retain `RefundInfo.refund` only as the backend-calculated amount sent to payment channels. Add a transient `List<RefundItem> items` to the refund response or a dedicated `RefundDetailView`, and make list/detail methods populate it. Update `OrderRefundStatusService` to sum persisted calculated refund amounts while inventory restoration uses item quantities.

  Keep channel-specific refund execution/query methods for the approved refund record, but they must never accept a client-supplied amount or create an application.

- [ ] **Step 5: Run itemized refund and compatibility tests**

  ```powershell
  mvn '-Dtest=ItemizedRefundServiceTest,ItemizedRefundConcurrencyTest,RefundOwnershipTest,RefundApplicationRabbitSyncTest,PublicApiCharacterizationTest' test
  ```

  Expected: the new body succeeds, old URL mappings return 404, old amount-shaped bodies return 400, and all accepted refund amounts equal backend item-price arithmetic.

### Task 8: Restore stock only after channel-confirmed refund success

**Files:**

- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/RefundInfoServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/refund/OrderRefundStatusService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/AliPayServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/wxpay/WxPayRefundService.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/RefundStatusSyncMessageServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/mq/RefundStatusSyncConsumer.java`
- Modify: `payment-demo/src/main/java/cc/ivera/controller/RefundInfoController.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/RefundStatusSyncServiceTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/RefundInventoryRestoreTest.java`
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/RefundChannelStateTest.java`

- [ ] **Step 1: Write failing channel-state and restore tests**

  Assert that approval changes only approval state and emits `REFUND_SUBMIT_REQUESTED`; `PROCESSING`, `FAILED`, `ABNORMAL`, and `CLOSED` never change stock; the first confirmed `SUCCESS` changes each selected item’s sold quantity back to available and writes `REFUND_RESTORE:{refundNo}:{orderItemId}`; duplicate success notifications, active queries, and Inbox redeliveries do not restore twice; a channel-repaired refund without trusted `t_refund_item` rows is marked for manual review and never infers stock from money.

  ```java
  @Test
  void secondSuccessNotificationDoesNotRestoreAgain() {
      when(refundMapper.updateStatusIfCurrent(anyString(), eq("SUCCESS"), anyList())).thenReturn(false);
      assertFalse(refundInfoService.syncRefundStatus(successResult("REFUND-1")));
      verify(inventoryService, never()).restoreRefund("REFUND-1");
  }
  ```

- [ ] **Step 2: Run the tests to verify RED**

  ```powershell
  mvn '-Dtest=RefundStatusSyncServiceTest,RefundInventoryRestoreTest,RefundChannelStateTest' test
  ```

  Expected: current success synchronization changes only refund/order amounts and does not call inventory restoration.

- [ ] **Step 3: Make success synchronization the single restore gate**

  In `syncRefundStatus`, lock the refund row, validate channel order/amount, perform the conditional transition to `SUCCESS`, and only when that update changes the row call `inventoryService.restoreRefund(refundNo)` in the same transaction. The inventory service locks each `t_refund_item`, `t_order_item`, and product row in deterministic order, checks `refunded_quantity` was not already applied for the same refund, moves the selected quantity from `sold_stock` to `available_stock`, increments `refunded_quantity`, and writes one `REFUND_RESTORE` operation per order item.

  `approve` must no longer invoke `executeRefund` directly. The Outbox consumer invokes the existing Alipay/WeChat channel refund facade with the backend-calculated `RefundInfo.refund`; channel immediate success and asynchronous notification both enter the same `syncRefundStatus` gate. Active refund query and reconciliation reuse that gate.

- [ ] **Step 4: Preserve unknown-channel safety and order status refresh**

  Keep `PROCESSING`/unknown responses locked and enqueue the existing status-sync retry. Refresh aggregate order status after each accepted refund transition. For external channel records created without local item mapping, keep the existing reconciliation record for financial visibility but set an abnormal/manual-review reason and skip stock changes; never derive item quantities from `refund`.

- [ ] **Step 5: Run refund, inventory, and message tests**

  ```powershell
  mvn '-Dtest=RefundStatusSyncServiceTest,RefundInventoryRestoreTest,RefundChannelStateTest,ItemizedRefundServiceTest,MessageIdempotencyTest' test
  ```

  Expected: approval has no inventory side effect, only channel-confirmed success restores the exact selected quantity, and duplicate success paths remain no-ops.

### Task 9: Align React USER flows with server-issued keys, stock flags, and itemized refunds

**Files:**

- Modify: `payment-demo-react/src/api/orderInfo.js`
- Modify: `payment-demo-react/src/api/refundInfo.js`
- Modify: `payment-demo-react/src/api/product.js`
- Modify: `payment-demo-react/src/api/wxPay.js`
- Modify: `payment-demo-react/src/api/aliPay.js`
- Modify: `payment-demo-react/src/pages/Home.jsx`
- Modify: `payment-demo-react/src/pages/Cart.jsx`
- Modify: `payment-demo-react/src/pages/Orders.jsx`
- Modify: `payment-demo-react/src/auth/AuthContext.jsx`
- Create: `payment-demo-react/src/components/RefundItemSelector.jsx`

- [ ] **Step 1: Add API methods and write the UI contract tests/checklist**

  Add:

  ```js
  // orderInfo.js
  issueIdempotencyKey() { return request.post('/api/order-info/idempotency-keys') }
  checkout(paymentAppId, idempotencyKey) {
    return request.post('/api/order-info/checkout',
      { paymentAppId, checkoutRequestId: idempotencyKey },
      { headers: { 'Idempotency-Key': idempotencyKey } })
  }

  // refundInfo.js
  apply(data) { return request.post('/api/refund-info/apply', data) }
  ```

  Remove `refunds(data)` from `wxPay.js` and `aliPay.js`. Add a checklist/test fixture for: zero-stock add button disabled; off-shelf cart line shows reason and can be removed; checkout requests a key only on the final click; retries reuse the same key; refund modal lists `refundableQuantity` and submits `{requestId, orderNo, reason, items}` without `refundAmount`.

- [ ] **Step 2: Run the React build to capture the current red contract**

  ```powershell
  Push-Location payment-demo-react
  npm run build
  Pop-Location
  ```

  Expected: build remains green before UI wiring; the contract checklist identifies current client-generated request IDs and amount refund calls.

- [ ] **Step 3: Implement just-in-time key issuance and retry behavior**

  In `Cart.jsx`, call `issueIdempotencyKey()` immediately before the order POST, take `idempotencyKey` from the response, and send it in both header and transition body. Hold the key in the current checkout attempt. On timeout/network failure retry with that same key even if the local 120-second clock elapsed; only a server response explicitly saying the unused key expired may trigger a new key. Never generate an order key with `crypto.randomUUID()`.

  In `Home.jsx`, disable add-to-cart when `saleable=false` and display `unavailableReason`; in `Cart.jsx`, render the four backend flags and disable checkout if any line is not purchasable. Refresh cart/product data after a 409.

- [ ] **Step 4: Implement itemized USER refund selection**

  Load order items before opening the refund modal. `RefundItemSelector.jsx` renders each item’s title, unit price, `refundableQuantity`, and an integer quantity selector. `Orders.jsx` computes a display-only preview from unit prices, generates a stable `requestId` for the submit/retry, and calls `refundInfoApi.apply` with:

  ```js
  {
    requestId,
    orderNo: row.orderNo,
    reason,
    items: selectedItems
      .filter(item => item.quantity > 0)
      .map(item => ({ orderItemId: item.id, quantity: item.quantity }))
  }
  ```

  Remove `refundAmountYuan`, payment-channel refund calls, and any amount input. Refresh order/refund records after success and show the server-calculated amount.

- [ ] **Step 5: Build and static-review React**

  ```powershell
  Push-Location payment-demo-react
  npm run build
  Pop-Location
  rg -n "createRequestId|refundAmount|/api/wx-pay/refunds|/api/ali-pay/trade/refund" payment-demo-react/src
  ```

  Expected: build exits 0; remaining `createRequestId` uses are only non-order request IDs such as stock/refund adjustment where explicitly allowed, and no old amount refund endpoint remains.

### Task 10: Add the React ADMIN product and refund operations

**Files:**

- Create: `payment-demo-react/src/api/adminProduct.js`
- Create: `payment-demo-react/src/pages/Products.jsx`
- Modify: `payment-demo-react/src/App.jsx`
- Modify: `payment-demo-react/src/components/AppHeader.jsx`
- Modify: `payment-demo-react/src/pages/Refunds.jsx`
- Modify: `payment-demo-react/src/pages/Orders.jsx`
- Modify: `payment-demo-react/src/auth/AuthContext.jsx`

- [ ] **Step 1: Add the admin API wrapper**

  Use one module for all admin product calls:

  ```js
  export default {
    list: () => request.get('/api/admin/products'),
    create: data => request.post('/api/admin/products', data),
    update: (id, data) => request.put(`/api/admin/products/${id}`, data),
    status: (id, status, version) => request.patch(`/api/admin/products/${id}/status`, { status, version }),
    adjustStock: (id, data) => request.post(`/api/admin/products/${id}/stock-adjustments`, data),
    operations: id => request.get(`/api/admin/products/${id}/stock-operations`)
  }
  ```

- [ ] **Step 2: Create the Products page**

  Render title, price, status, available/locked/sold stock, total (sum of the three buckets), version, and update time. Provide create/edit forms, status toggle, stock delta + reason form, and operation drawer. Generate `requestId` once per adjustment attempt and reuse it on retry. Do not render a delete action; on 409 reload the row and show the backend message.

- [ ] **Step 3: Wire route/navigation and role behavior**

  Add `/products` under `ProtectedRoute role="ADMIN"`; add the Header link only for ADMIN. Keep course/cart links hidden for ADMIN and keep `refreshCartCount` from calling `/api/cart` for ADMIN. Redirect an ADMIN who opens `/` to `/orders`; do not load the public catalog for that role.

- [ ] **Step 4: Show itemized refund state to ADMIN**

  Extend `Refunds.jsx` and the ADMIN branch of `Orders.jsx` to show refund item lines (title, quantity, amount), calculated total, approval status, channel refund status, and “查询状态/订单退款对账”. Approval buttons call only the approval endpoint; there is no manual inventory-restock action.

- [ ] **Step 5: Build and inspect route/API parity**

  ```powershell
  Push-Location payment-demo-react
  npm run build
  Pop-Location
  rg -n "products|stock-adjustments|refundableQuantity|refundAmount|refunds\(" payment-demo-react/src
  ```

  Expected: ADMIN sees product/refund operations, USER sees only shopping/refund selection, and no ADMIN component calls cart/checkout/payment/refund-apply endpoints.

### Task 11: Align Vue with the same API and state machine

**Files:**

- Create: `payment-demo-vue/src/api/adminProduct.js`
- Create: `payment-demo-vue/src/views/Products.vue`
- Create: `payment-demo-vue/src/components/RefundItemSelector.vue`
- Modify: `payment-demo-vue/src/api/orderInfo.js`
- Modify: `payment-demo-vue/src/api/refundInfo.js`
- Modify: `payment-demo-vue/src/api/product.js`
- Modify: `payment-demo-vue/src/api/wxPay.js`
- Modify: `payment-demo-vue/src/api/aliPay.js`
- Modify: `payment-demo-vue/src/views/index.vue`
- Modify: `payment-demo-vue/src/views/Cart.vue`
- Modify: `payment-demo-vue/src/views/Orders.vue`
- Modify: `payment-demo-vue/src/views/Refunds.vue`
- Modify: `payment-demo-vue/src/router/index.js`
- Modify: `payment-demo-vue/src/components/AppHeader.vue`
- Modify: `payment-demo-vue/src/auth/session.js`

- [ ] **Step 1: Add Vue API wrappers**

  Mirror React paths and payloads, including the Axios header:

  ```js
  issueIdempotencyKey() { return request.post('/api/order-info/idempotency-keys') },
  checkout(paymentAppId, idempotencyKey) {
    return request.post('/api/order-info/checkout',
      { paymentAppId, checkoutRequestId: idempotencyKey },
      { headers: { 'Idempotency-Key': idempotencyKey } })
  },
  applyRefund(data) { return request.post('/api/refund-info/apply', data) }
  ```

  Remove channel `refunds` helpers and client-generated order IDs.

- [ ] **Step 2: Implement USER catalog/cart/checkout parity**

  Match React behavior exactly: `saleable=false` disables add; cart rows show status/available stock/reason; checkout requests the backend key at the final click and reuses it for retries; a server-expired unused key is the only condition that permits issuing a new key. Keep cart removal available for stale off-shelf lines and never make cart operations reserve inventory.

- [ ] **Step 3: Implement Vue itemized refund selector**

  Replace the amount input in `Orders.vue` with `RefundItemSelector.vue` using `refundableQuantity`. Submit one stable `requestId` with order number, reason, and selected item quantities. Display the server-calculated refund amount and itemized records in the existing refund-record dialog. Update `Refunds.vue` to show item lines and channel status for ADMIN.

- [ ] **Step 4: Create the Vue Products page and route**

  Use Element UI `el-table`, `el-dialog`, `el-input-number`, and `el-drawer` for the same fields/actions as React. Add `/products` with `meta: { requiresAuth: true, role: 'ADMIN' }`, hide catalog/cart for ADMIN, and keep ADMIN out of payment/refund application flows.

- [ ] **Step 5: Run Vue lint/build and stale-contract scan**

  ```powershell
  Push-Location payment-demo-vue
  npm run lint
  npm run build
  Pop-Location
  rg -n "createRequestId|refundAmount|/api/wx-pay/refunds|/api/ali-pay/trade/refund" payment-demo-vue/src
  ```

  Expected: lint/build exit 0 and no old amount-refund call remains.

### Task 12: Add real-MySQL race tests and complete verification

**Files:**

- Create: `payment-demo/src/test/java/cc/ivera/integration/ProductInventoryConcurrencyIT.java`
- Create: `payment-demo/src/test/java/cc/ivera/integration/OrderIdempotencyConcurrencyIT.java`
- Create: `payment-demo/src/test/java/cc/ivera/integration/RefundQuantityConcurrencyIT.java`
- Modify: `payment-demo/src/test/java/cc/ivera/characterization/CorsPreflightCharacterizationTest.java`
- Modify: `spec/README.md`
- Modify: `payment-demo/README.md`
- Modify: `payment-demo/docs/RABBITMQ_OPERATIONS.md`
- Move after implementation: `spec/planned/PRODUCT_INVENTORY_MANAGEMENT_COMPAT_SPEC.md` to `spec/implemented/PRODUCT_INVENTORY_MANAGEMENT_COMPAT_SPEC.md`

- [ ] **Step 1: Write the integration tests before enabling them**

  Use an isolated MySQL schema and deterministic cleanup of product/order/refund/inventory/outbox/inbox rows. Test: 10 stock units versus 50 concurrent checkouts; 20 identical requests with the same user/key; payment notification versus close; duplicate Rabbit delivery; 20 concurrent partial refund requests; and CORS `OPTIONS /api/cart/items` returning HTTP 200 with the configured origin/method/header response.

  ```java
  @Test
  void fiftyConcurrentCheckoutsCannotReserveMoreThanTenUnits() throws Exception {
      seedProduct(7L, 10);
      List<Future<Integer>> results = submitFiftyCheckouts("key-per-request");
      assertEquals(10, countSuccessful(results));
      assertEquals(10, productMapper.selectById(7L).getLockedStock());
      assertEquals(0, productMapper.selectById(7L).getAvailableStock());
  }
  ```

- [ ] **Step 2: Run unit/characterization tests and verify the baseline first**

  ```powershell
  Set-Location payment-demo
  mvn '-Dtest=ProductInventoryRefundSchemaContractTest,ProductAdminServiceTest,AdminProductControllerTest,CartServiceImplTest,CartCheckoutServiceTest,OrderIdempotencyServiceTest,InventoryServiceTest,ItemizedRefundServiceTest,RefundInventoryRestoreTest,MessageOutboxServiceTest,MessageIdempotencyTest,AdminPurchaseBoundaryTest,AdminOrderCreationBoundaryTest,RefundOwnershipTest,PublicApiCharacterizationTest,CorsPreflightCharacterizationTest' test
  ```

  Expected: all database-independent tests pass. If MySQL/RabbitMQ is unavailable, keep the integration tests separately reported as environment-blocked; never convert them into ignored tests.

- [ ] **Step 3: Run the real-MySQL integration tests**

  ```powershell
  mvn '-Dtest=ProductInventoryConcurrencyIT,OrderIdempotencyConcurrencyIT,RefundQuantityConcurrencyIT' test
  ```

  Expected: exact stock and quantity invariants from the spec, one order per key, one inventory operation per business key, and no negative bucket.

- [ ] **Step 4: Build both frontends and package the backend**

  ```powershell
  Set-Location payment-demo
  mvn clean package
  Set-Location ..\payment-demo-react
  npm run build
  Set-Location ..\payment-demo-vue
  npm run lint
  npm run build
  Set-Location ..
  ```

  Expected: Maven package, React build, Vue lint, and Vue build all exit 0. Preserve the known non-failing XML parser output from the characterization test.

- [ ] **Step 5: Perform live API/browser and spec reconciliation**

  With backend `http://localhost:8080` and either frontend running, verify:

  1. USER sees only on-shelf products; zero stock disables add and direct API returns 409.
  2. ADMIN is redirected away from catalog and receives 403 for cart, checkout, product payment, order payment, and refund application.
  3. USER checkout first obtains a backend key, sends it in the header, and a repeated request returns the same order.
  4. USER refund selects item quantities; ADMIN approval only schedules channel refund; inventory changes only after channel success/query confirmation.
  5. ADMIN `/products`, `/refunds`, payment status query, refund query/reconcile, and outbox retry are available; React and Vue field labels/actions match.

  Update `payment-demo/README.md` and `docs/RABBITMQ_OPERATIONS.md` with the 120-second key configuration, outbox/inbox retry procedure, and itemized refund payload. Move SPEC-010 to `spec/implemented`, add implementation anchors and final test counts to `spec/README.md`, and append the planned→implemented transition only after all checks pass.

- [ ] **Step 6: Final scoped diff review**

  ```powershell
  git diff --check
  git status --short --branch
  rg -n "refundAmount|/api/wx-pay/refunds|/api/ali-pay/trade/refund|createRequestId\(\).*checkout|total_stock" payment-demo payment-demo-react/src payment-demo-vue/src spec
  ```

  Confirm stale amount-refund contracts are absent, completed idempotency records are not expired by the 120-second cleanup, all changed files map to SPEC-010, and unrelated existing worktree edits are preserved.

---

## Verification command set

```powershell
Set-Location D:\code\demo\springboot-payment\payment-demo-java\payment-demo
mvn '-Dtest=ProductInventoryRefundSchemaContractTest,ProductAdminServiceTest,AdminProductControllerTest,CartServiceImplTest,CartCheckoutServiceTest,OrderIdempotencyServiceTest,InventoryServiceTest,ItemizedRefundServiceTest,RefundInventoryRestoreTest,MessageOutboxServiceTest,MessageIdempotencyTest,AdminPurchaseBoundaryTest,AdminOrderCreationBoundaryTest,RefundOwnershipTest,PublicApiCharacterizationTest,CorsPreflightCharacterizationTest' test
mvn '-Dtest=ProductInventoryConcurrencyIT,OrderIdempotencyConcurrencyIT,RefundQuantityConcurrencyIT' test
mvn clean package

Set-Location ..\payment-demo-react
npm run build

Set-Location ..\payment-demo-vue
npm run lint
npm run build

Set-Location ..
git diff --check
```

The package and frontend commands are authoritative for build health. Real-MySQL/RabbitMQ tests are authoritative for concurrency/message guarantees; if infrastructure is unavailable, report the exact blocked command and retain the passing unit/characterization evidence.
