# Current Issue Classification Ledger

> Updated: 2026-06-09

There are no external issue numbers in this workspace snapshot. The entries below classify the current behavior items discovered during code reading and characterization testing.

| ID | Current Item | Classification | Why It Is Classified This Way | Spec/Test Anchor |
|---|---|---|---|---|
| ISSUE-CB-001 | `query-order-status` returns `code=101,message=支付中......` for null, closed, or other non-success statuses. | Public API or compatibility impact | The behavior may look like a bug, but frontends may depend on the polling contract and message text. Any change must be planned. | `PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC`, `PublicApiCharacterizationTest` |
| ISSUE-CB-002 | `GET /api/payment-config/apps` returns runtime `PaymentAppConfig` objects. | Public API or compatibility impact | Response structure and exposed fields are part of the current API. Changing or redacting it affects callers. | `PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC`, `PublicApiCharacterizationTest` |
| ISSUE-CB-003 | Legacy refund route passes `refundAmount=null`. | Public API or compatibility impact | Legacy callers rely on the path shape; null amount currently means refunding remaining refundable amount at the service boundary. | `PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC`, `PublicApiCharacterizationTest` |
| ISSUE-CB-004 | Malformed WeChat V2 XML notification returns failure XML and does not touch Redis, lock, DB, or services. | Local bug candidate | This is a narrow rejection path. It is locked as current behavior before any future fix. | `PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC`, `InfrastructureBehaviorCharacterizationTest` |
| ISSUE-CB-005 | Duplicate payment flow insert swallows `DuplicateKeyException` and logs idempotent success. | Multiple issues, same root cause | Payment/refund duplicate handling appears in several services and should be treated as an idempotency rule, not patched one site at a time. | `PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC`, `InfrastructureBehaviorCharacterizationTest` |
| ISSUE-CB-006 | Refund summary status priority is full success, partial success, processing, abnormal, then success. | Design change | Changing priority changes business state transitions and must be planned with acceptance tests. | `PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC`, `InfrastructureBehaviorCharacterizationTest` |
| ISSUE-CB-007 | DB payment config and properties fallback coexist across WeChat/Alipay flows. | Multiple issues, same root cause | Several service paths resolve config differently. Any cleanup should be handled as one config-source spec. | `PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC` |
| ISSUE-CB-008 | WeChat V2 and V3 implementations coexist. | Design change | Unifying or retiring either version changes provider contracts, callbacks, config, and frontend entry points. | `PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC` |
| ISSUE-CB-009 | React and Vue frontends coexist against the same backend API. | Public API or compatibility impact | Backend route/response changes must account for both frontends unless a spec explicitly deprecates one. | `PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC` |
| ISSUE-CB-010 | `WxPayController` contains a TODO for user order cancellation. | Design change | Cancellation affects order state, provider close/refund behavior, frontend UX, and compatibility rules. | Future `spec/planned/order/...` |

## Classification Rule

If a future issue maps to more than one row above, prefer the broader classification: multiple issues same root cause, then public API/compatibility impact, then design change, then local bug.

