<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { store } from '../store'
import { issueToken } from '../api'

const issuing = ref(false)
const now = ref(Date.now())
const toast = ref('')
let timer = null
let toastTimer = null

onMounted(() => {
  timer = setInterval(() => (now.value = Date.now()), 1000)
})
onUnmounted(() => clearInterval(timer))

async function doIssue() {
  issuing.value = true
  try {
    await issueToken()
  } finally {
    issuing.value = false
  }
}

function remaining(t) {
  if (!t.expiresAt) return '-'
  const ms = new Date(t.expiresAt).getTime() - now.value
  if (ms <= 0) return '已过期'
  const s = Math.floor(ms / 1000)
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}

function isExpired(t) {
  return !!t.expiresAt && new Date(t.expiresAt).getTime() <= now.value
}

function select(t) {
  store.currentTokenId = t.requestId
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(t.requestId).then(showToast)
  }
}

function showToast() {
  toast.value = 'requestId 已复制到剪贴板'
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 1500)
}
</script>

<template>
  <section class="card">
    <div class="card-head">
      <h2>
        ① requestId 管理
        <span class="sub">POST /api/idempotency/order-create · TTL 10 分钟 · token 只存 SHA256</span>
      </h2>
      <button class="primary" :disabled="issuing" @click="doIssue">
        {{ issuing ? '签发中…' : '签发新 requestId' }}
      </button>
    </div>

    <div v-if="store.tokens.length === 0" class="empty">
      尚未签发。点击右上角按钮，后端会写入一条 <code>ISSUED</code> 状态的 token 记录。
    </div>

    <table v-else>
      <thead>
        <tr>
          <th style="width: 36px">#</th>
          <th>requestId（点击选中并复制）</th>
          <th style="width: 90px">租户</th>
          <th style="width: 70px">剩余有效期</th>
          <th style="width: 80px">状态</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(t, i) in store.tokens"
          :key="t.requestId"
          :class="{ selected: t.requestId === store.currentTokenId, clickable: true }"
          @click="select(t)"
        >
          <td class="muted">{{ store.tokens.length - i }}</td>
          <td class="mono break-all">{{ t.requestId }}</td>
          <td>{{ t.tenantId }}</td>
          <td>
            <span :class="isExpired(t) ? 'muted' : remaining(t) < '01:00' ? 'warn-text' : ''">
              {{ remaining(t) }}
            </span>
          </td>
          <td>
            <span v-if="isExpired(t)" class="badge muted">已过期</span>
            <span v-else-if="t.used" class="badge info">已使用</span>
            <span v-else class="badge ok">未使用</span>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="toast" class="copy-toast show">{{ toast }}</div>
  </section>
</template>
