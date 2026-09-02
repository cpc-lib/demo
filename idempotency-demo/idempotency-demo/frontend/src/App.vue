<script setup>
import { reactive, ref } from 'vue'
import { createOrder, newIdempotencyKey } from './api/orderApi'
import OrderForm from './components/OrderForm.vue'
import ControlPanel from './components/ControlPanel.vue'
import ResultPanel from './components/ResultPanel.vue'

const form = reactive({
  key: newIdempotencyKey(),
  tenantId: 'demo',
  userId: 1001,
  itemName: 'keyboard',
  amount: 299,
  gatewayFailOnce: false,
  feignFailOnce: false,
})

const results = ref([])
const running = ref(false)
let seq = 0

function buildPayload() {
  return {
    userId: Number(form.userId),
    itemName: form.itemName,
    amount: Number(form.amount),
  }
}

const round2 = (n) => Math.round(n * 100) / 100

function record(outcome, sentKey, payload) {
  results.value.unshift({
    id: ++seq,
    sentKey,
    payload,
    time: new Date(),
    ...outcome,
  })
}

// 发送一次请求（可覆盖 key / payload，用于 Key 复用、非法 Key 等场景）
async function sendOnce({ payload, key } = {}) {
  const sentKey = key ?? form.key
  const reqPayload = payload ?? buildPayload()
  let outcome
  try {
    outcome = await createOrder({
      key: sentKey,
      tenantId: form.tenantId,
      gatewayFailOnce: form.gatewayFailOnce,
      feignFailOnce: form.feignFailOnce,
      payload: reqPayload,
    })
  } catch (err) {
    // fetch 本身失败（后端未启动 / 网络错误 / 非法 Header 值）
    outcome = {
      ok: false,
      httpStatus: 0,
      echoedKey: null,
      body: { code: 'NETWORK_ERROR', message: String(err) },
      durationMs: null,
    }
  }
  record(outcome, sentKey, reqPayload)
  return outcome
}

// run() 统一加锁；内部动作不再各自加锁，避免嵌套被拦截
async function run(fn) {
  if (running.value) return
  running.value = true
  try {
    await fn()
  } finally {
    running.value = false
  }
}

async function doSequential(n) {
  for (let i = 0; i < n; i++) await sendOnce()
}

async function doConcurrent(n) {
  await Promise.all(Array.from({ length: n }, () => sendOnce()))
}

// 同 Key、不同 Payload：预期 order-service 抛 KeyReusedException（409）
async function doKeyReuse() {
  await sendOnce()
  await sendOnce({ payload: { ...buildPayload(), amount: round2(Number(form.amount) + 1) } })
}

function resetFaults() {
  form.gatewayFailOnce = false
  form.feignFailOnce = false
}

const scenarios = [
  {
    id: 'duplicate',
    title: '普通重复请求（同 Key × 5）',
    detail: '第 1 次新建订单，其余 4 次直接回放同一结果，最终只有一条 biz_order。',
    run() {
      resetFaults()
      form.key = newIdempotencyKey()
      return doSequential(5)
    },
  },
  {
    id: 'gateway-retry',
    title: '模拟 Gateway Retry',
    detail: 'caller 下游成功后故意 500，Gateway 对 POST 自动重试一次，重试命中 replay。',
    run() {
      resetFaults()
      form.gatewayFailOnce = true
      form.key = newIdempotencyKey()
      return sendOnce()
    },
  },
  {
    id: 'feign-retry',
    title: '模拟 Feign Retry',
    detail: 'order-service 事务 COMMIT 后故意 503，Feign 重试同 Key 请求，直接回放第一次结果。',
    run() {
      resetFaults()
      form.feignFailOnce = true
      form.key = newIdempotencyKey()
      return sendOnce()
    },
  },
  {
    id: 'concurrent',
    title: '并发同 Key × 10',
    detail: 'Redis 锁 + MySQL UNIQUE 兜底：最终最多一条订单，个别请求可能收到 409 IN_PROGRESS。',
    run() {
      resetFaults()
      form.key = newIdempotencyKey()
      return doConcurrent(10)
    },
  },
  {
    id: 'key-reused',
    title: '同 Key 不同 Payload（AC-03）',
    detail: '创建成功后改 amount 用同一 Key 重发：order-service 返回 409 IDEMPOTENCY_KEY_REUSED（caller 未透传冲突时链路终态为 500）。',
    run() {
      resetFaults()
      form.key = newIdempotencyKey()
      return doKeyReuse()
    },
  },
  {
    id: 'invalid-key',
    title: '非法 Key（长度 < 8）',
    detail: '发送 5 字符 Key：order-service 返回 409 INVALID_IDEMPOTENCY_KEY（同样可能表现为链路终态 500）。',
    run() {
      resetFaults()
      return sendOnce({ key: 'short' })
    },
  },
]

const onOnce = () => run(() => sendOnce())
const onRepeat = (n) => run(() => doSequential(n))
const onConcurrent = (n) => run(() => doConcurrent(n))
const onScenario = (s) => run(() => s.run())
const newKey = () => {
  form.key = newIdempotencyKey()
}
const clearResults = () => {
  results.value = []
}
</script>

<template>
  <div class="page">
    <header class="hero">
      <h1>幂等下单 Demo · 前端控制台</h1>
      <p class="chain">
        浏览器<span class="arrow">→</span>Nginx<span class="arrow">→</span>Gateway<span class="arrow">→</span>caller-service<span class="arrow">→</span>Feign<span class="arrow">→</span>order-service
      </p>
      <p class="hero-sub">At-Least-Once Transport + Business Idempotency ≈ Exactly-Once Effect</p>
    </header>

    <main class="layout">
      <div class="col-left">
        <OrderForm :form="form" @new-key="newKey" />
        <ControlPanel
          :running="running"
          :scenarios="scenarios"
          @once="onOnce"
          @repeat="onRepeat"
          @concurrent="onConcurrent"
          @new-key="newKey"
          @clear="clearResults"
          @scenario="onScenario"
        />
      </div>
      <div class="col-right">
        <ResultPanel :results="results" />
      </div>
    </main>
  </div>
</template>
