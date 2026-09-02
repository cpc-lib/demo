<script setup>
import { ref } from 'vue'
import { store, clearLogs } from '../store'

const expanded = ref({})

function toggle(id) {
  expanded.value[id] = !expanded.value[id]
}

function fmt(v) {
  if (v == null) return ''
  return typeof v === 'string' ? v : JSON.stringify(v, null, 2)
}

function statusClass(s) {
  if (s == null) return 'err'
  if (s >= 200 && s < 300) return 'ok'
  if ([400, 409, 428].includes(s)) return 'warn'
  return 'err'
}
</script>

<template>
  <section class="card">
    <div class="card-head">
      <h2>
        ⑤ 请求日志
        <span class="sub">每次请求的完整收发记录；对比 X-Request-Id（每次可变，仅追踪）与 Idempotency-Key（回显，恒定）</span>
      </h2>
      <button class="small" :disabled="store.logs.length === 0" @click="clearLogs">清空</button>
    </div>

    <div v-if="store.logs.length === 0" class="empty">暂无请求记录。</div>

    <div v-for="e in store.logs" :key="e.id" class="log-entry">
      <div class="log-line" @click="toggle(e.id)">
        <span class="badge" :class="statusClass(e.status)">
          {{ e.status != null ? e.status : 'ERR' }}
        </span>
        <span class="muted" style="font-size: 12px">{{ e.time }}</span>
        <span class="mono" style="font-size: 12px">{{ e.method }} {{ e.path }}</span>
        <span v-if="e.tag" class="badge muted">{{ e.tag }}</span>
        <span v-if="e.hint" class="badge err">链路不通</span>
        <span v-if="e.durationMs != null" class="muted" style="font-size: 12px">{{ e.durationMs }}ms</span>
        <span class="muted" style="font-size: 11.5px">
          trace:
          <span class="mono">{{ e.resHeaders['x-request-id'] || '-' }}</span>
        </span>
        <span class="muted" style="font-size: 11.5px">
          key:
          <span class="mono">{{ e.resHeaders['idempotency-key'] || e.reqHeaders['Idempotency-Key'] || '-' }}</span>
        </span>
        <span style="margin-left: auto; color: var(--muted)">{{ expanded[e.id] ? '▲' : '▼' }}</span>
      </div>

      <div v-if="expanded[e.id]" class="log-detail">
        <div v-if="e.hint" class="result-hint" style="grid-column: 1 / -1">{{ e.hint }}</div>
        <div>
          <h4>请求头</h4>
          <pre>{{ fmt(e.reqHeaders) }}</pre>
        </div>
        <div>
          <h4>请求体</h4>
          <pre>{{ e.reqBody || '（空）' }}</pre>
        </div>
        <div>
          <h4>响应头</h4>
          <pre>{{ fmt(e.resHeaders) }}</pre>
        </div>
        <div>
          <h4>响应体</h4>
          <pre>{{ e.error ? '网络错误：' + e.error : fmt(e.resBody) || '（空）' }}</pre>
        </div>
      </div>
    </div>
  </section>
</template>
