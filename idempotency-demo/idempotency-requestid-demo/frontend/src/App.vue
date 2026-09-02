<script setup>
import { computed } from 'vue'
import { store } from './store'
import { CHANNELS } from './api'
import TokenPanel from './components/TokenPanel.vue'
import ManualPanel from './components/ManualPanel.vue'
import ScenarioPanel from './components/ScenarioPanel.vue'
import OrderSummary from './components/OrderSummary.vue'
import RequestLog from './components/RequestLog.vue'

const isDev = import.meta.env.DEV

// 构建产物部署在 Nginx 下时，直连链路的 Vite 代理不存在，只保留 Nginx 全链路
if (!isDev && CHANNELS[store.channel]?.devOnly) {
  store.channel = 'nginx'
}

const channelOptions = Object.values(CHANNELS).filter((c) => isDev || !c.devOnly)
const channel = computed(() => CHANNELS[store.channel])

// 与 vite.config.js 中的默认值保持一致，可用 VITE_PROXY_* 环境变量覆盖
const proxyTargets = {
  nginx: import.meta.env.VITE_PROXY_NGINX || 'http://localhost:80',
  gateway: import.meta.env.VITE_PROXY_GATEWAY || 'http://localhost:8080',
  caller: import.meta.env.VITE_PROXY_CALLER || 'http://localhost:8081',
  order: import.meta.env.VITE_PROXY_ORDER || 'http://localhost:8082',
}
</script>

<template>
  <div class="app">
    <header class="topbar">
      <div>
        <h1>幂等 Demo · 前端测试控制台</h1>
        <p>后端预签发 requestId（Idempotency-Key）+ MySQL 行锁 —— 场景对应 docs/ACCEPTANCE-CRITERIA.md</p>
      </div>
      <div class="topbar-controls">
        <label>访问链路</label>
        <select v-model="store.channel">
          <option v-for="c in channelOptions" :key="c.key" :value="c.key">{{ c.label }}</option>
        </select>
        <label>租户 X-Tenant-Id</label>
        <input v-model="store.tenantId" style="width: 110px" />
      </div>
    </header>

    <div class="channel-note">
      <b>{{ channel.label }}：</b>{{ channel.desc }}
      <span v-if="isDev">（当前代理目标：<code>{{ proxyTargets[store.channel] }}</code>，可用环境变量 VITE_PROXY_* 覆盖）</span>
      <span v-else>（当前为构建产物，直连链路需 <code>npm run dev</code> 使用 Vite 代理）</span>
    </div>

    <TokenPanel />
    <ManualPanel />
    <ScenarioPanel />
    <OrderSummary />
    <RequestLog />

    <footer class="footer">
      两步协议：① <code>POST /api/idempotency/order-create</code> 签发 requestId →
      ② <code>POST /api/orders</code> 携带 <code>Idempotency-Key</code> 提交。
      <code>X-Request-Id</code> 仅用于链路追踪，<code>Idempotency-Key</code> 才是业务幂等键，二者严格分离。
    </footer>
  </div>
</template>
