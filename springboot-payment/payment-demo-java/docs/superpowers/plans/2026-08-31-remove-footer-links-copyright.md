# Remove Footer Links and Copyright Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the complete friend-links block and copyright statement from both public frontends while preserving all other footer content.

**Architecture:** Make two surgical template-only edits, one in the React footer and one in the Vue footer. Do not add CSS hiding, feature flags, shared abstractions, dependencies, or backend changes. Validate absence and retained content with deterministic source assertions, then compile and visually inspect both apps.

**Tech Stack:** React 18 + Vite, Vue 2 + Vue CLI, PowerShell source assertions, Browser-based UI QA

**Design:** `docs/superpowers/specs/2026-08-31-remove-footer-links-copyright-design.md`

---

### Task 1: Remove the content from the React footer

**Files:**
- Modify: `payment-demo-react/src/components/AppFooter.jsx`
- Verify: `payment-demo-react/package.json`

- [x] **Step 1: Run the React source assertion to establish RED**

Run:

```powershell
$target = '.\payment-demo-react\src\components\AppFooter.jsx'
$forbidden = Select-String -Path $target -Pattern '友情链接|www\.atguigu\.com|尚硅谷|课程版权均归谷粒学院所有|京ICP备17055252号'
if ($forbidden) {
    $forbidden
    throw 'React footer still contains content that must be removed.'
}
```

Expected before implementation: command fails because the friend-links block and copyright line are still present.

- [x] **Step 2: Delete only the approved React nodes**

Leave `AppFooter.jsx` with this complete component:

```jsx
export default function AppFooter() {
  return (
    <footer id="footer">
      <section className="container">
        <div className="b-foot">
          <section className="fl col-7">
            <section className="mr20">
              <section className="b-f-link">
                <a href="#" title="关于我们" target="_blank" rel="noreferrer">关于我们</a>|
                <a href="#" title="联系我们" target="_blank" rel="noreferrer">联系我们</a>|
                <a href="#" title="帮助中心" target="_blank" rel="noreferrer">帮助中心</a>|
                <a href="#" title="资源下载" target="_blank" rel="noreferrer">资源下载</a>|
                <span>服务热线：010-56253825(北京) 0755-85293825(深圳)</span>
                <span>Email：info@atguigu.com</span>
              </section>
            </section>
          </section>
          <div className="clear" />
        </div>
      </section>
    </footer>
  )
}
```

- [x] **Step 3: Run React source assertions for GREEN and retained content**

Run:

```powershell
$target = '.\payment-demo-react\src\components\AppFooter.jsx'
$forbidden = Select-String -Path $target -Pattern '友情链接|www\.atguigu\.com|尚硅谷|课程版权均归谷粒学院所有|京ICP备17055252号'
if ($forbidden) { throw 'React footer still contains removed content.' }
foreach ($required in @('关于我们', '联系我们', '帮助中心', '资源下载', '服务热线', 'Email')) {
    if (-not (Select-String -Path $target -SimpleMatch $required -Quiet)) {
        throw "React footer lost required content: $required"
    }
}
```

Expected: exit code `0`; removed content has no matches and all six retained labels are present.

- [x] **Step 4: Build the React frontend**

Run from `payment-demo-react`:

```powershell
npm run build
```

Expected: Vite exits `0` and writes the production bundle to `dist/`.

### Task 2: Remove the content from the Vue footer

**Files:**
- Modify: `payment-demo-vue/src/components/AppFooter.vue`
- Verify: `payment-demo-vue/package.json`

- [x] **Step 1: Run the Vue source assertion to establish RED**

Run:

```powershell
$target = '.\payment-demo-vue\src\components\AppFooter.vue'
$forbidden = Select-String -Path $target -Pattern '友情链接|www\.atguigu\.com|尚硅谷|课程版权均归谷粒学院所有|京ICP备17055252号'
if ($forbidden) {
    $forbidden
    throw 'Vue footer still contains content that must be removed.'
}
```

Expected before implementation: command fails because the friend-links block and copyright line are still present.

- [x] **Step 2: Delete only the approved Vue nodes**

Leave `AppFooter.vue` with this complete template:

```vue
<template>
  <!-- 公共底 -->
  <footer id="footer">
    <section class="container">
      <div class="b-foot">
        <section class="fl col-7">
          <section class="mr20">
            <section class="b-f-link">
              <a href="#" title="关于我们" target="_blank">关于我们</a>|
              <a href="#" title="联系我们" target="_blank">联系我们</a>|
              <a href="#" title="帮助中心" target="_blank">帮助中心</a>|
              <a href="#" title="资源下载" target="_blank">资源下载</a>|
              <span>服务热线：010-56253825(北京) 0755-85293825(深圳)</span>
              <span>Email：info@atguigu.com</span>
            </section>
          </section>
        </section>
        <div class="clear"/>
      </div>
    </section>
  </footer>
</template>
```

- [x] **Step 3: Run Vue source assertions for GREEN and retained content**

Run:

```powershell
$target = '.\payment-demo-vue\src\components\AppFooter.vue'
$forbidden = Select-String -Path $target -Pattern '友情链接|www\.atguigu\.com|尚硅谷|课程版权均归谷粒学院所有|京ICP备17055252号'
if ($forbidden) { throw 'Vue footer still contains removed content.' }
foreach ($required in @('关于我们', '联系我们', '帮助中心', '资源下载', '服务热线', 'Email')) {
    if (-not (Select-String -Path $target -SimpleMatch $required -Quiet)) {
        throw "Vue footer lost required content: $required"
    }
}
```

Expected: exit code `0`; removed content has no matches and all six retained labels are present.

- [x] **Step 4: Lint and build the Vue frontend**

Run from `payment-demo-vue`:

```powershell
npm run lint
npm run build
```

Expected: both Vue CLI commands exit `0`; the production bundle is written to `dist/`.

### Task 3: Cross-frontend regression and rendered QA

**Files:**
- Verify: `payment-demo-react/src/components/AppFooter.jsx`
- Verify: `payment-demo-vue/src/components/AppFooter.vue`

- [ ] **Step 1: Run the combined content assertion**

Run:

```powershell
$targets = @(
    '.\payment-demo-react\src\components\AppFooter.jsx',
    '.\payment-demo-vue\src\components\AppFooter.vue'
)
$forbidden = Select-String -Path $targets -Pattern '友情链接|www\.atguigu\.com|尚硅谷|课程版权均归谷粒学院所有|京ICP备17055252号'
if ($forbidden) { $forbidden; throw 'One or more frontends still contain removed footer content.' }
foreach ($target in $targets) {
    foreach ($required in @('关于我们', '联系我们', '帮助中心', '资源下载', '服务热线', 'Email')) {
        if (-not (Select-String -Path $target -SimpleMatch $required -Quiet)) {
            throw "$target lost required content: $required"
        }
    }
}
```

Expected: exit code `0` for both files.

- [ ] **Step 2: Verify rendered React and Vue pages**

Use the Browser plugin against the local React and Vue dev servers. For each app verify:

- The page renders without a framework error overlay.
- The footer has no “友情链接”“尚硅谷” or copyright/filing statement.
- About, contact, help, resource, phone, and email content remains visible.
- The browser console contains no relevant new errors or warnings.
- Capture one desktop screenshot that includes the footer.

- [ ] **Step 3: Review the final diff**

Run:

```powershell
git diff --check -- .\payment-demo-react\src\components\AppFooter.jsx .\payment-demo-vue\src\components\AppFooter.vue
git diff -- .\payment-demo-react\src\components\AppFooter.jsx .\payment-demo-vue\src\components\AppFooter.vue
```

Expected: only the approved friend-links and copyright nodes are deleted; no CSS, dependencies, backend code, or unrelated user changes are touched.
