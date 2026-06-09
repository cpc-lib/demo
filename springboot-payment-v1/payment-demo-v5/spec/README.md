# Spec State Ledger

> Updated: 2026-06-09

`spec/` is the project state ledger for behavior, contracts, architecture boundaries, and governance. It is intentionally small: every entry should help future changes answer "what is the current contract, where is it implemented, and how do we prove it?"

## States

| State | Directory | Meaning |
|---|---|---|
| governance | `spec/governance/` | Always-on process rules, issue classification, PR checklists, and spec accounting rules. |
| planned | `spec/planned/` | Designed but not fully landed behavior, compatibility changes, or refactors that still need implementation/tests. |
| implemented | `spec/implemented/` | Current behavior that is already landed and linked to implementation/test anchors. |
| archived | `spec/archived/` | Deferred, deprecated, or abandoned decisions kept for history. |

## Current Ledger

| Spec | State | Purpose |
|---|---|---|
| `spec/governance/ISSUE_LEDGER.md` | governance | Current issue classification for the discovered legacy behavior. |
| `spec/governance/PR_SPEC_RECONCILIATION_CHECKLIST.md` | governance | Checklist to run before PR merge. |
| `spec/implemented/current-behavior/PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC.md` | implemented | Current behavior contract for public API, state effects, events, logs, and characterization tests. |

## Directory Layout

```text
spec/
|-- README.md
|-- governance/
|   |-- ISSUE_LEDGER.md
|   `-- PR_SPEC_RECONCILIATION_CHECKLIST.md
|-- planned/
|-- implemented/
|   `-- current-behavior/
|       `-- PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC.md
`-- archived/
    |-- deferred/
    `-- deprecated/
```

## Operating Rule

Before code changes, classify the issue and find or create the matching spec. After code changes, reconcile the spec state, implementation anchors, compatibility impact, and tests in the same PR.

