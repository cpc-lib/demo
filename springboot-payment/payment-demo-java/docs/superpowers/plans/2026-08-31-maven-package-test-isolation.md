# Maven Package Test Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `mvn package` pass on the project's declared JDK 21 by removing unnecessary external-infrastructure startup from two legacy configuration tests while preserving their intended assertions.

**Architecture:** Keep production code and public APIs unchanged. Replace two full `@SpringBootTest` smoke tests with focused JUnit tests that load only the classpath resources they verify; the WeChat test then calls `WxPayConfig#getPrivateKey` directly. Treat the malformed XML SAX stack as expected output from a passing characterization test, not as the build failure.

**Tech Stack:** Java 21, Maven, Spring Boot 2.3.7, JUnit 5, AssertJ

---

### Task 1: Lock down the actual Maven failure

**Files:**
- Inspect: `payment-demo/target/surefire-reports/cc.ivera.AlipayTests.txt`
- Inspect: `payment-demo/target/surefire-reports/cc.ivera.PaymentDemoApplicationTests.txt`
- Inspect: `payment-demo/target/surefire-reports/cc.ivera.characterization.InfrastructureBehaviorCharacterizationTest.txt`

- [x] **Step 1: Run the package build on the declared JDK**

Run:

```powershell
mvn -f .\payment-demo\pom.xml package
```

Expected before the fix: exit code `1`; `AlipayTests` and `PaymentDemoApplicationTests` fail while creating Redisson/Netty, while `InfrastructureBehaviorCharacterizationTest` reports zero failures and zero errors.

- [x] **Step 2: Classify the issue and constrain scope**

Classify as `type: bug-local`. Do not change controller behavior, API responses, payment protocols, Maven failure-ignore settings, or production infrastructure configuration.

### Task 2: Isolate the Alipay configuration test

**Files:**
- Modify: `payment-demo/src/test/java/cc/ivera/AlipayTests.java`

- [x] **Step 1: Replace the full application bootstrap with a focused resource test**

Use this test body:

```java
@Test
void testAlipayConfig() throws IOException {
    Properties properties = PropertiesLoaderUtils.loadProperties(
            new ClassPathResource("alipay-sandbox.properties"));

    assertThat(properties.getProperty("alipay.appId")).isNotBlank();
    assertThat(properties.getProperty("alipay.gatewayUrl")).startsWith("https://");
    assertThat(properties.getProperty("alipay.merchantPrivateKey")).isNotBlank();
    assertThat(properties.getProperty("alipay.alipayPublicKey")).isNotBlank();
}
```

Remove `@SpringBootTest`, constructor injection, logging, and imports made unused by those removals.

- [x] **Step 2: Run the focused Alipay test**

Run:

```powershell
mvn -f .\payment-demo\pom.xml -Dtest=AlipayTests test
```

Expected: `Tests run: 1, Failures: 0, Errors: 0` and `BUILD SUCCESS`, without starting Redisson, Redis, RabbitMQ, or the web server.

### Task 3: Isolate the WeChat private-key test

**Files:**
- Modify: `payment-demo/src/test/java/cc/ivera/PaymentDemoApplicationTests.java`

- [x] **Step 1: Replace the full application bootstrap with a direct unit test**

Use this test body:

```java
@Test
void testGetPrivateKey() throws IOException {
    Properties properties = PropertiesLoaderUtils.loadProperties(
            new ClassPathResource("wxpay.properties"));
    String privateKeyPath = properties.getProperty("wxpay.private-key-path");
    WxPayConfig wxPayConfig = new WxPayConfig();

    assertThat(privateKeyPath).isNotBlank();
    assertThat(new ClassPathResource(privateKeyPath)).exists();

    PrivateKey privateKey = wxPayConfig.getPrivateKey(privateKeyPath);
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
    assertThat(privateKey.getEncoded()).isNotEmpty();
}
```

Remove `@SpringBootTest`, constructor injection, the unused HTTP client, console output, and imports made unused by those removals.

- [x] **Step 2: Run the focused WeChat test**

Run:

```powershell
mvn -f .\payment-demo\pom.xml -Dtest=PaymentDemoApplicationTests test
```

Expected: `Tests run: 1, Failures: 0, Errors: 0` and `BUILD SUCCESS`, without creating a Spring application context or external clients.

### Task 4: Verify the attached SAX trace is non-failing

**Files:**
- Verify: `payment-demo/src/test/java/cc/ivera/characterization/InfrastructureBehaviorCharacterizationTest.java`

- [x] **Step 1: Run only the malformed XML characterization test**

Run:

```powershell
mvn -f .\payment-demo\pom.xml '-Dtest=InfrastructureBehaviorCharacterizationTest#current_wxpay_v2_notify_bad_xml_returns_fail_without_touching_state' test
```

Expected: the controller returns the documented `FAIL` XML, no stateful collaborator is touched, and Maven reports one passing test. The SDK may print its caught SAX parser stack, but it must not be counted as a failure or error.

### Task 5: Complete build verification

**Files:**
- Verify: `payment-demo/pom.xml`
- Verify: all changed test files

- [x] **Step 1: Run the complete package lifecycle from a clean target directory**

Run:

```powershell
mvn -f .\payment-demo\pom.xml clean package
```

Expected: exit code `0`, all tests pass with zero failures and zero errors, and the packaged JAR is created under `payment-demo/target/`.

- [x] **Step 2: Review the final diff**

Run:

```powershell
git diff -- payment-demo/src/test/java/cc/ivera/AlipayTests.java payment-demo/src/test/java/cc/ivera/PaymentDemoApplicationTests.java
```

Expected: only test-isolation changes; no production API, configuration, dependency version, or unrelated user change is modified.
