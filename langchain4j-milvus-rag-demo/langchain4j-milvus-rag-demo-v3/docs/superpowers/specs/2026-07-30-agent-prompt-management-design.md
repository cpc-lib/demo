# Agent Prompt Management Design

## Goal

Add a tenant-scoped system prompt management page that uses the existing `rag_agent_prompt` table and `/api/admin/agent-prompts` backend APIs.

## Chosen Approach

Use the existing prompt service as the source of truth. The runtime already reads prompts through `PromptService.getActiveSystemPrompt()`, so the management page should call the existing admin prompt APIs instead of creating another configuration model.

## Behavior

- Show the current effective system prompt for the active tenant.
- If the tenant has no prompt, show the global default prompt as the effective prompt.
- Add a tenant-owned system prompt. It can be saved disabled or enabled immediately.
- Editing the current active tenant prompt creates a new active version and disables the previous active version.
- Editing a disabled tenant prompt updates that record in place and can optionally enable it.
- Editing the global fallback from a tenant context creates a tenant-owned active prompt and does not modify tenant `0`.
- Enabling one tenant prompt disables all other active prompts for the same tenant and prompt name.
- Disabling the current tenant prompt leaves no tenant active prompt, so runtime falls back to global default.
- The global default prompt is not modified by tenant edits.
- Show the current tenant's prompt version history.
- Allow viewing, copying, editing, enabling, disabling, and rolling back tenant-owned versions.

## Backend Changes

- Keep the existing `rag_agent_prompt` schema.
- Keep the existing admin endpoints.
- Change admin active-prompt lookup to follow runtime behavior: tenant prompt first, then global default.
- Preserve rollback behavior as tenant-only, because a tenant can only roll back versions it owns.
- Add admin endpoints for creating tenant prompts, updating prompt records, and enabling/disabling prompt records.

## Frontend Changes

- Add API helpers for `/api/admin/agent-prompts`.
- Add a "System Prompts" management page to the existing admin/navigation structure.
- Provide current prompt metadata, create/edit drawer, version table, version preview, copy action, rollback action, and enable/disable controls.

## Testing

- Add a backend unit test proving tenant admin lookup falls back to global default when no tenant prompt exists.
- Add backend unit tests for creating disabled prompts, enabling one prompt while disabling others, editing active prompts as new versions, and disabling active prompts.
- Add frontend helper tests for prompt editor metadata/state helpers where practical.
- Run backend Maven tests and frontend production build.
