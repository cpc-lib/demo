<script setup>
import { ref, watch, computed } from 'vue'
import { store } from '../store'
import { submitOrder, buildOrderBody } from '../api'

const userId = ref('1001')
const itemName = ref('keyboard')
const amount = ref('299.00')
const key = ref('')
const tenantId = ref(store.tenantId)
const noKey = ref(false)
const gatewayFailOnce = ref(false)
const feignFailOnce = ref(false)
const sending = ref(false)
const lastResult = ref(null)

// 选中 token 或切换租户时同步到表单
watch(
  () => store.currentTokenId,
  (v) => {
    if (v) key.value = v
  },
)
watch(
  () => store.tenantId,
  (v) => {
    tenantId.value = v
  },
)

const bodyPreview = computed(() => {
  try {
    return buildOrderBody({ userId: userId.value, itemName: itemName.value, amount: amount.value })
  } catch (e) {
    return `（${e.message}）`
  }
})

const bodyValid = computed(() => {
  try {
    buildOrderBody({ userId: userId.value, itemName: itemName.value, amount: amount.value })
    return true
  } catch {
    return false
  }
})

async function send() {
  sending.value = true
  lastResult.value = null
  try {
    const body = buildOrderBody({ userId: userId.value, itemName: itemName.value, amount: amount.value })
    lastResult.value = await submitOrder({
      key: key.value.trim(),
      withKey: !noKey.value,
      tenantId: tenantId.value.trim() || 'demo',
      body,
      gatewayFailOnce: gatewayFailOnce.value,
      feignFailOnce: feignFailOnce.value,
    })
  } catch (e) {
    lastResult.value = { status: null, resBody: null, error: e.message }
  } finally {
    sending.value = false
  }
}

function statusClass(r) {
  if (r == null) return 'err'
  if (r.status >= 200 && r.status < 300) return 'ok'
  if ([400, 409, 428].includes(r.status)) return 'warn'
  return 'err'
}
</script>

<template>
  <section class="card">
    <div class="card-head">
      <h2>
        ② 手动提交订单
        <span class="sub">POST /api/orders · 请求体原样复用（amount 保留字面值，299.00 ≠ 299.0）</span>
      </h2>
    </div>

    <div class="form-grid">
      <div class="field">
        <label>userId</label>
        <input v-model="userId" />
      </div>
      <div class="field">
        <label>itemName</label>
        <input v-model="itemName" />
      </div>
      <div class="field">
        <label>amount</label>
        <input v-model="amount" />
      </div>
      <div class="field">
        <label>租户 X-Tenant-Id（须与签发时一致）</label>
        <input v-model="tenantId" />
      </div>
      <div class="field wide">
        <label>Idempotency-Key（自动带入选中的 requestId，可手动修改以测试伪造 key）</label>
        <input v-model="key" placeholder="idem_..." :disabled="noKey" />
      </div>
    </div>

    <div class="checks">
      <label><input v-model="noKey" type="checkbox" /> 不携带 Key（验证 428 拦截）</label>
      <label>
        <input v-model="gatewayFailOnce" type="checkbox" />
        故障注入 <code>X-Demo-Gateway-Fail-Once</code>（caller 500 → Gateway 重试）
      </label>
      <label>
        <input v-model="feignFailOnce" type="checkbox" />
        故障注入 <code>X-Demo-Feign-Fail-Once</code>（order COMMIT 后 503 → Feign 重试）
      </label>
    </div>

    <div class="checks">
      <span class="muted">请求体预览：</span>
      <code class="mono">{{ bodyPreview }}</code>
      <button class="primary small" style="margin-left: auto" :disabled="sending || !bodyValid" @click="send">
        {{ sending ? '提交中…' : '提交订单' }}
      </button>
    </div>

    <div v-if="lastResult" class="result-box">
      <div class="result-head">
        <span class="badge" :class="statusClass(lastResult)">
          {{ lastResult.status != null ? 'HTTP ' + lastResult.status : '网络错误' }}
        </span>
        <span v-if="lastResult.durationMs != null" class="muted">{{ lastResult.durationMs }} ms</span>
        <span v-if="lastResult.resBody?.orderNo" class="mono">
          orderNo: {{ lastResult.resBody.orderNo }}
        </span>
        <span v-if="lastResult.resBody?.orderNo" class="badge" :class="lastResult.resBody.replayed ? 'info' : 'ok'">
          replayed: {{ lastResult.resBody.replayed }}
        </span>
      </div>
      <div v-if="lastResult.hint" class="result-hint">{{ lastResult.hint }}</div>
      <pre class="result-body">{{ lastResult.error || JSON.stringify(lastResult.resBody, null, 2) }}</pre>
    </div>
  </section>
</template>
