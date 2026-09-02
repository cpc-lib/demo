<script setup>
import { computed } from 'vue'

const props = defineProps({
  results: { type: Array, required: true },
})

const CODE_HINTS = {
  IDEMPOTENCY_KEY_REUSED: '同 Key 不同 Payload，服务端拒绝执行',
  IDEMPOTENCY_REQUEST_IN_PROGRESS: '同 Key 请求仍在处理中（并发快挡）',
  INVALID_IDEMPOTENCY_KEY: 'Key 长度必须为 8~128',
  NETWORK_ERROR: '网络不可达：请确认 Nginx/Gateway 与后端服务已启动',
}

const summary = computed(() => {
  const list = props.results
  const success = list.filter((r) => r.ok)
  const replay = success.filter((r) => r.body && r.body.replayed)
  return {
    total: list.length,
    success: success.length,
    first: success.length - replay.length,
    replay: replay.length,
    errors: list.length - success.length,
    keyGroups: byKey.value.length,
  }
})

// 幂等性按 Key 校验：不同 Key 天然对应不同订单，不能跨 Key 聚合
const byKey = computed(() => {
  const map = new Map()
  for (const r of props.results) {
    if (!r.ok) continue
    const k = r.sentKey || '(空 Key)'
    let group = map.get(k)
    if (!group) {
      group = { key: k, success: 0, replay: 0, orderNos: new Set() }
      map.set(k, group)
    }
    group.success++
    if (r.body && r.body.replayed) group.replay++
    if (r.body && r.body.orderNo) group.orderNos.add(r.body.orderNo)
  }
  return [...map.values()]
})

const verdict = computed(() => {
  if (!props.results.length) return null
  const groups = byKey.value
  if (!groups.length) {
    return { level: 'warn', text: '暂无成功请求：请确认后端服务与基础设施（MySQL/Redis/Nacos）已启动' }
  }
  const broken = groups.find((g) => g.orderNos.size > 1)
  if (broken) {
    return {
      level: 'bad',
      text: `Key ${shortKey(broken.key)} 产生了 ${broken.orderNos.size} 个不同订单号 —— 幂等被破坏！`,
    }
  }
  const multi = groups.find((g) => g.success > 1)
  if (multi) {
    return {
      level: 'good',
      text: `Key ${shortKey(multi.key)}：${multi.success} 次成功请求只产生 1 个订单号（${[...multi.orderNos][0]}），回放 ${multi.replay} 次 —— 幂等生效 ✓`,
    }
  }
  return null
})

function statusClass(r) {
  if (r.ok) return 's2xx'
  if (r.httpStatus === 409) return 's409'
  return 's5xx'
}

function describe(r) {
  if (r.ok) return (r.body && r.body.status) || 'OK'
  const code = r.body && r.body.code
  if (code) return `${code} · ${CODE_HINTS[code] || r.body.message || ''}`
  return (r.body && (r.body.error || r.body.message)) || '请求失败'
}

function keyMatch(r) {
  if (!r.sentKey) return r.echoedKey ? '自动生成' : '—'
  if (!r.echoedKey) return '—'
  return r.echoedKey === r.sentKey ? '✓ 一致' : '✗ 不一致'
}

function keyMatchClass(r) {
  if (!r.sentKey || !r.echoedKey) return 'muted'
  return r.echoedKey === r.sentKey ? 'match' : 'mismatch'
}

function shortKey(k) {
  if (!k) return '—'
  return k.length > 14 ? `${k.slice(0, 8)}…${k.slice(-4)}` : k
}

function fmtTime(t) {
  return t.toLocaleTimeString('zh-CN', { hour12: false })
}

function tooltip(r) {
  return JSON.stringify(
    { key: r.sentKey, payload: r.payload, response: r.body, echoedKey: r.echoedKey },
    null,
    2,
  )
}
</script>

<template>
  <section class="card result-card">
    <h2 class="card-title">
      请求结果
      <span v-if="results.length" class="count">{{ results.length }} 条</span>
    </h2>

    <div v-if="!results.length" class="empty">
      还没有请求记录。左侧选择一个「一键场景」，观察重复请求如何被幂等层拦截。
    </div>

    <template v-else>
      <div class="chips">
        <div class="chip"><b>{{ summary.total }}</b><span>总请求</span></div>
        <div class="chip ok"><b>{{ summary.success }}</b><span>成功</span></div>
        <div class="chip"><b>{{ summary.first }}</b><span>首次执行</span></div>
        <div class="chip replay"><b>{{ summary.replay }}</b><span>回放 replayed</span></div>
        <div class="chip err"><b>{{ summary.errors }}</b><span>失败/拒绝</span></div>
        <div class="chip order"><b>{{ summary.keyGroups }}</b><span>幂等 Key 数</span></div>
      </div>

      <p v-if="verdict" class="verdict" :class="verdict.level">{{ verdict.text }}</p>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>时间</th>
              <th>HTTP</th>
              <th>订单号</th>
              <th>replayed</th>
              <th>耗时</th>
              <th>Key 回传</th>
              <th>说明</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in results" :key="r.id" :title="tooltip(r)">
              <td class="muted">{{ r.id }}</td>
              <td class="muted">{{ fmtTime(r.time) }}</td>
              <td><span class="status" :class="statusClass(r)">{{ r.httpStatus || '—' }}</span></td>
              <td class="mono">{{ (r.body && r.body.orderNo) || '—' }}</td>
              <td>
                <span v-if="r.ok" class="badge" :class="{ replay: r.body && r.body.replayed }">
                  {{ r.body && r.body.replayed ? 'REPLAY' : 'FIRST' }}
                </span>
                <span v-else class="muted">—</span>
              </td>
              <td class="muted">{{ r.durationMs != null ? r.durationMs + 'ms' : '—' }}</td>
              <td :class="keyMatchClass(r)" class="key-cell">{{ keyMatch(r) }}</td>
              <td class="desc">{{ describe(r) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="hint">鼠标悬停行可查看本次请求的 Key、Payload 与响应原文；「Key 回传」验证 Idempotency-Key 全链路未变化（AC-08）。</p>
    </template>
  </section>
</template>
