# Agent Prompt Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a tenant-scoped system prompt management UI backed by the existing prompt APIs.

**Architecture:** Reuse `PromptService` and `AgentPromptController` as the backend boundary. Add a small frontend API layer and one Ant Design page wired into the existing app navigation.

**Tech Stack:** Java 21, Spring Boot 3.2.6, MyBatis-Plus, JUnit 5, React 18, TypeScript, Vite, Ant Design.

---

### Task 1: Backend Fallback Behavior

**Files:**
- Modify: `src/main/java/cc/ivera/ragdemo/service/agent/PromptService.java`
- Test: `src/test/java/cc/ivera/ragdemo/service/agent/PromptServiceTest.java`

- [ ] Write a test where tenant `7` has no prompt and tenant `0` has an active default prompt.
- [ ] Run the test and verify it fails because `getActivePrompt(7L)` returns `null`.
- [ ] Update `getActivePrompt` to return tenant prompt first, then global prompt.
- [ ] Run the focused test and verify it passes.

### Task 2: Frontend Prompt API

**Files:**
- Modify: `ui/src/types/index.ts`
- Modify: `ui/src/api/rag.ts`
- Create: `ui/src/utils/agentPrompts.ts`
- Test: `ui/src/utils/agentPrompts.test.ts`

- [ ] Add `RagAgentPrompt` and request types.
- [ ] Add API methods for active prompt, update, versions, and rollback.
- [ ] Add small helper functions for prompt display state.
- [ ] Add and run helper tests with `tsx`.

### Task 3: Management Page And Navigation

**Files:**
- Create: `ui/src/pages/AgentPromptsPage.tsx`
- Modify: `ui/src/App.tsx`
- Modify: `ui/src/layouts/AppLayout.tsx`

- [ ] Add a page with current prompt summary, editor, save action, and version table.
- [ ] Add version preview, copy, and rollback actions.
- [ ] Add route and navigation entry.
- [ ] Run `npm run build`.

### Task 4: Verification

- [ ] Run `mvn -q test`.
- [ ] Run `cd ui; npm run build`.
- [ ] Report changed files and any runtime caveats.

### Task 5: Full Prompt Management Controls

**Files:**
- Modify: `src/main/java/cc/ivera/ragdemo/service/agent/PromptService.java`
- Modify: `src/main/java/cc/ivera/ragdemo/controller/AgentPromptController.java`
- Modify: `ui/src/types/index.ts`
- Modify: `ui/src/api/rag.ts`
- Modify: `ui/src/utils/agentPrompts.ts`
- Modify: `ui/src/pages/AgentPromptsPage.tsx`
- Test: `src/test/java/cc/ivera/ragdemo/service/agent/PromptServiceTest.java`
- Test: `ui/src/utils/agentPrompts.test.ts`

- [ ] Add failing backend tests for disabled creation, active edit creating a new version, enable-one-disable-others, and disable active prompt.
- [ ] Implement PromptService create/update/enable/disable methods.
- [ ] Add controller endpoints for POST create, PUT by id, PUT enable, and PUT disable.
- [ ] Add frontend types and API helpers for create, update by id, enable, disable, and list all tenant prompts.
- [ ] Replace the one-editor page with a current prompt panel plus create/edit drawer and table controls.
- [ ] Run focused backend tests, frontend helper tests, then full Maven tests and frontend build.
