# WeChat Payment and Refund Reconciliation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Import real WeChat CSV/Tab/XLSX trade bills and reconcile PAYMENT rows against local payment flows and REFUND rows against local refund flows in both directions.

**Architecture:** Normalize every WeChat upload into text, parse it into typed `ChannelBillRecord` rows, then delegate WeChat matching to a pure `WxPaymentRefundMatcher`. `ReconciliationServiceImpl` remains the orchestration boundary: it loads imported bills and date-scoped local flows, filters them by payment application, persists matcher output, and leaves the existing Alipay path unchanged.

**Tech Stack:** Java 8, Spring Boot 2.3, MyBatis-Plus, JUnit 5, Mockito, AssertJ, Apache POI 5.5.1, React/Ant Design, Vue/Element UI.

---

## File map

- Create `payment-demo/src/main/java/cc/ivera/service/impl/reconciliation/WxPaymentRefundMatcher.java`: pure PAYMENT/REFUND matching and difference construction.
- Create `payment-demo/src/test/java/cc/ivera/service/impl/reconciliation/WxPaymentRefundMatcherTest.java`: matcher behavior and duplicate-key tests.
- Modify `WxBillParser.java`: official backtick/Tab/no-header parser plus XLSX normalization.
- Modify `ChannelBillRecord.java`: business type and merchant refund number.
- Modify `ChannelBillServiceImpl.java`: normalize uploaded bytes and accept XLSX/TXT.
- Modify `ReconciliationServiceImpl.java`: query local payment/refund flows by their own business time and invoke the matcher.
- Modify `ReconciliationDetail.java` and its mapper/SQL: persist business/refund identifiers.
- Modify React/Vue reconciliation pages: accept the supported extensions and show typed rows.
- Modify spec/README/project README: document the implemented compatibility contract.

Because the checkout already contains same-feature uncommitted user changes, this plan intentionally uses diff checkpoints instead of committing intermediate states. No existing dirty file is reset or included in an automatic commit.

### Task 1: Lock the WeChat input contract with failing parser tests

**Files:**
- Modify: `payment-demo/src/test/java/cc/ivera/service/impl/reconciliation/WxBillParserTest.java`

- [ ] **Step 1: Add a failing official ALL text test**

```java
@Test
void parsesOfficialAllRowsAsPaymentAndRefund() {
    String text = officialHeaders() + "\n"
            + officialPaymentRow() + "\n"
            + officialRefundRow() + "\n";
    List<ChannelBillRecord> records = parser.parse(text);
    assertThat(records).extracting(ChannelBillRecord::getBusinessType)
            .containsExactly("PAYMENT", "REFUND");
    assertThat(records.get(0).getAmount()).isEqualTo(100);
    assertThat(records.get(1).getRefundNo()).isEqualTo("REFUND_001");
    assertThat(records.get(1).getAmount()).isEqualTo(60);
}
```

- [ ] **Step 2: Add a failing headerless Tab test**

```java
@Test
void parsesHeaderlessOfficialAllTabRows() {
    List<ChannelBillRecord> records = parser.parse(officialPaymentRow().replace(',', '\t'));
    assertThat(records).hasSize(1);
    assertThat(records.get(0).getOrderNo()).isEqualTo("ORDER_001");
}
```

- [ ] **Step 3: Run the parser tests and observe RED**

Run: `mvn -Dtest=WxBillParserTest test` from `payment-demo`.

Expected: failures because official rows start with backticks, headerless rows are not recognized, and `businessType`/`refundNo` do not exist.

### Task 2: Implement typed text parsing

**Files:**
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/reconciliation/ChannelBillRecord.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/reconciliation/WxBillParser.java`

- [ ] **Step 1: Add typed fields**

```java
private String businessType;
private String refundNo;
```

- [ ] **Step 2: Normalize fields and infer the official 27-column ALL header**

```java
private String normalizeField(String value) {
    String normalized = value == null ? "" : value.trim();
    return normalized.startsWith("`") ? normalized.substring(1) : normalized;
}

private boolean isOfficialAllDataRow(String[] fields) {
    return fields.length == 27 && normalizeField(fields[0]).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
}
```

- [ ] **Step 3: Classify only PAYMENT and REFUND**

```java
boolean refund = "REFUND".equals(record.getStatus())
        || hasText(record.getRefundNo()) || hasText(record.getRefundId());
if (refund) {
    record.setBusinessType("REFUND");
    record.setAmount(record.getRefundAmount());
    record.setStatus(hasText(refundStatus) ? refundStatus : "REFUND");
} else if ("SUCCESS".equals(record.getStatus())) {
    record.setBusinessType("PAYMENT");
} else {
    return null;
}
```

- [ ] **Step 4: Run parser tests and observe GREEN**

Run: `mvn -Dtest=WxBillParserTest test`.

Expected: all parser tests pass, including legacy CSV characterization tests.

### Task 3: Add XLSX upload compatibility with TDD

**Files:**
- Modify: `payment-demo/pom.xml`
- Modify: `payment-demo/src/test/java/cc/ivera/service/impl/reconciliation/ChannelBillServiceTest.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/reconciliation/WxBillParser.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/reconciliation/ChannelBillServiceImpl.java`
- Modify: `payment-demo/src/main/java/cc/ivera/service/reconciliation/ChannelBillService.java`

- [ ] **Step 1: Add a failing in-memory XLSX upload test**

```java
@Test
void uploadsMerchantPlatformXlsx() throws Exception {
    byte[] xlsx = createOfficialWorkbook();
    MockMultipartFile file = new MockMultipartFile(
            "file", "29082026_ALL.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx);
    ChannelBill bill = billService.uploadBill(file, yesterday(), "WXPAY", "ALL", false);
    assertThat(bill.getRecordCount()).isEqualTo(2);
    assertThat(bill.getBillContent()).contains("ORDER_001").contains("REFUND_001");
}
```

- [ ] **Step 2: Run the test and observe RED**

Run: `mvn -Dtest=ChannelBillServiceTest#uploadsMerchantPlatformXlsx test`.

Expected: test compilation first fails without POI, then the existing UTF-8 text reader fails to parse XLSX.

- [ ] **Step 3: Add Apache POI**

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.5.1</version>
</dependency>
```

- [ ] **Step 4: Normalize workbook rows to Tab text**

```java
public String normalize(byte[] bytes, String fileName) {
    if (isXlsx(bytes, fileName)) {
        return normalizeWorkbook(bytes);
    }
    return stripBom(new String(bytes, StandardCharsets.UTF_8));
}
```

- [ ] **Step 5: Make upload use normalized content**

```java
byte[] bytes = file.getBytes();
String billContent = wxBillParser.normalize(bytes, file.getOriginalFilename());
```

- [ ] **Step 6: Run upload and parser tests and observe GREEN**

Run: `mvn -Dtest=WxBillParserTest,ChannelBillServiceTest test`.

Expected: all tests pass; XLSX is stored as parseable normalized text.

### Task 4: Drive the pure PAYMENT/REFUND matcher from tests

**Files:**
- Create: `payment-demo/src/test/java/cc/ivera/service/impl/reconciliation/WxPaymentRefundMatcherTest.java`
- Create: `payment-demo/src/main/java/cc/ivera/service/impl/reconciliation/WxPaymentRefundMatcher.java`
- Modify: `payment-demo/src/main/java/cc/ivera/entity/ReconciliationDetail.java`

- [ ] **Step 1: Add failing exact-match tests**

```java
@Test
void matchesPaymentAndRefundIndependently() {
    List<ReconciliationDetail> details = matcher.match(
            Arrays.asList(channelPayment(), channelRefund()),
            Collections.singletonList(localPayment()),
            Collections.singletonList(localRefund()), 7L);
    assertThat(details).extracting(ReconciliationDetail::getBusinessType)
            .containsExactly("PAYMENT", "REFUND");
    assertThat(details).extracting(ReconciliationDetail::getDiffType)
            .containsOnly("MATCH");
}
```

- [ ] **Step 2: Add failing difference and duplicate-key tests**

Cover `MISSING_LOCAL`, `MISSING_CHANNEL`, `AMOUNT_MISMATCH`, `STATUS_MISMATCH`, cross-order refund identity, and two channel rows sharing a transaction ID but having different order numbers.

- [ ] **Step 3: Run and observe RED**

Run: `mvn -Dtest=WxPaymentRefundMatcherTest test`.

Expected: compilation fails because the matcher and detail fields do not exist.

- [ ] **Step 4: Implement primary-key-first, unique-fallback matching**

```java
public List<ReconciliationDetail> match(List<ChannelBillRecord> channelRecords,
                                        List<PaymentInfo> payments,
                                        List<RefundInfo> refunds,
                                        Long reconciliationId) {
    // PAYMENT: orderNo, then unique transactionId.
    // REFUND: refundNo, then unique refundId.
    // Track matched channel objects by identity so duplicate keys are not overwritten.
}
```

- [ ] **Step 5: Run and observe GREEN**

Run: `mvn -Dtest=WxPaymentRefundMatcherTest test`.

Expected: every matcher behavior passes without database mocks.

### Task 5: Integrate date-correct local flow queries

**Files:**
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/reconciliation/ReconciliationServiceImpl.java`
- Modify: `payment-demo/src/test/java/cc/ivera/service/impl/reconciliation/ReconciliationBillDependencyTest.java`

- [ ] **Step 1: Add a failing service test for payment and cross-day refund**

```java
@Test
void reconcilesPaymentInfoAndCrossDayRefundInfo() {
    when(paymentInfoMapper.selectList(any())).thenReturn(singletonList(localPaymentOnBillDate()));
    when(refundInfoMapper.selectList(any())).thenReturn(singletonList(localRefundApprovedOnBillDate()));
    when(orderInfoMapper.selectList(any())).thenReturn(singletonList(wxOrderCreatedEarlier()));
    Reconciliation result = service.executeReconciliation(request);
    assertThat(result.getDiffCount()).isZero();
    assertThat(result.getMatchCount()).isEqualTo(2);
}
```

- [ ] **Step 2: Run and observe RED**

Run: `mvn -Dtest=ReconciliationBillDependencyTest#reconcilesPaymentInfoAndCrossDayRefundInfo test`.

Expected: the constructor lacks `RefundInfoMapper`/matcher and the current implementation only queries order creation date.

- [ ] **Step 3: Query flow time instead of order time**

```java
QueryWrapper<PaymentInfo> payments = new QueryWrapper<>();
payments.ge("create_time", start).lt("create_time", end).eq("payment_type", PayType.WXPAY.getType());

QueryWrapper<RefundInfo> refunds = new QueryWrapper<>();
refunds.ge("approved_time", start).lt("approved_time", end)
       .eq("approval_status", RefundApprovalStatus.APPROVED.getType());
```

Load the referenced orders by `order_no`, then retain only orders with `payment_channel_code = WXPAY` and the requested `payment_app_id`.

- [ ] **Step 4: Route only WXPAY through the new matcher**

```java
if (CHANNEL_WXPAY.equals(channelCode)) {
    details = wxPaymentRefundMatcher.match(channelRecords, localPayments, localRefunds, reconciliation.getId());
} else {
    details = matchRecords(channelRecords, localOrders, paymentInfoMap, reconciliation.getId());
}
```

- [ ] **Step 5: Run reconciliation tests and observe GREEN**

Run: `mvn -Dtest=WxPaymentRefundMatcherTest,ReconciliationBillDependencyTest test`.

Expected: payment/refund match is complete and the existing imported-bill/idempotency tests remain green.

### Task 6: Persist and expose typed reconciliation details

**Files:**
- Modify: `payment-demo/src/main/resources/mapper/ReconciliationDetailMapper.xml`
- Modify: `payment-demo/sql/payment-demo.sql`
- Create: `payment-demo/sql/wxpay_reconciliation_v2_upgrade.sql`
- Modify: `payment-demo/src/main/java/cc/ivera/service/impl/reconciliation/ReconciliationServiceImpl.java`
- Modify: `payment-demo-react/src/pages/Reconciliation.jsx`
- Modify: `payment-demo-vue/src/views/Reconciliation.vue`

- [ ] **Step 1: Add nullable columns and mapper fields**

```sql
ALTER TABLE `t_reconciliation_detail`
  ADD COLUMN `business_type` varchar(16) NULL COMMENT '业务类型：PAYMENT/REFUND' AFTER `diff_type`,
  ADD COLUMN `refund_no` varchar(64) NULL COMMENT '商户退款单号' AFTER `transaction_id`,
  ADD COLUMN `refund_id` varchar(64) NULL COMMENT '渠道退款单号' AFTER `refund_no`;
```

- [ ] **Step 2: Add the same fields to batch insert/result mapping/export**

Export order: `业务类型,差异类型,商户订单号,渠道交易号,商户退款单号,渠道退款单号,...`.

- [ ] **Step 3: Update file selectors and typed columns**

React and Vue upload selectors accept `.csv,.txt,.xlsx`; button/help text says `CSV/TXT/XLSX`. Bill record and reconciliation detail tables show `businessType`, and refund identifiers when present.

- [ ] **Step 4: Run backend targeted tests**

Run: `mvn -Dtest=WxBillParserTest,ChannelBillServiceTest,WxPaymentRefundMatcherTest,ReconciliationBillDependencyTest test`.

Expected: all targeted tests pass.

### Task 7: Close the spec and verify the repository

**Files:**
- Move: `spec/planned/WXPAY_PAYMENT_REFUND_RECONCILIATION_COMPAT_SPEC.md` to `spec/implemented/WXPAY_PAYMENT_REFUND_RECONCILIATION_COMPAT_SPEC.md`
- Modify: `spec/README.md`
- Modify: `spec/implemented/CHANNEL_BILL_IMPORT_SPEC.md`
- Modify: `payment-demo/README.md`

- [ ] **Step 1: Update documentation and spec state**

Mark every verified SPEC-007 acceptance item complete, add implementation/test anchors, and record `planned -> implemented` dated 2026-08-29.

- [ ] **Step 2: Run L0 characterization tests**

Run: `mvn -Dtest=PublicApiCharacterizationTest,InfrastructureBehaviorCharacterizationTest test`.

Expected: 18 tests pass.

- [ ] **Step 3: Run the reconciliation regression set**

Run: `mvn -Dtest=WxBillParserTest,AliPayBillParserTest,ChannelBillServiceTest,WxPaymentRefundMatcherTest,ReconciliationBillDependencyTest test`.

Expected: all tests pass, including unchanged Alipay parser behavior.

- [ ] **Step 4: Run the Maven build**

Run: `mvn test`.

Expected: BUILD SUCCESS with no test failures.

- [ ] **Step 5: Inspect only scoped diffs**

Run: `git diff --check` and `git status --short`.

Expected: no whitespace errors; no private workbook or pasted attachment appears in Git status.
