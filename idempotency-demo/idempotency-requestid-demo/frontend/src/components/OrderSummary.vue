<script setup>
import { computed } from 'vue'
import { store } from '../store'

const orders = computed(() => Object.values(store.orders))
const totalSubmits = computed(() => orders.value.reduce((s, o) => s + o.submits, 0))
const totalReplays = computed(() => orders.value.reduce((s, o) => s + o.replays, 0))
</script>

<template>
  <section class="card">
    <div class="card-head">
      <h2>
        ④ 订单汇总
        <span class="sub">由全部 200 响应聚合 —— 同一 key 的重放/重试返回同一 orderNo，不产生新订单</span>
      </h2>
    </div>

    <div v-if="orders.length === 0" class="empty">暂无成功创建的订单。</div>

    <template v-else>
      <p style="margin: 0 0 10px">
        成功响应 <b>{{ totalSubmits }}</b> 次
        <span class="muted">（其中重放 {{ totalReplays }} 次）</span>
        → 落库订单 <b class="ok-text">{{ orders.length }}</b> 条。
        <span class="muted">同一 token 的多次提交收敛到同一 orderNo，即「至多一次落库」。</span>
      </p>
      <table>
        <thead>
          <tr>
            <th>orderNo</th>
            <th style="width: 90px">租户</th>
            <th style="width: 110px">成功响应次数</th>
            <th style="width: 110px">其中 replayed</th>
            <th style="width: 100px">首次时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="o in orders" :key="o.orderNo">
            <td class="mono">{{ o.orderNo }}</td>
            <td>{{ o.tenantId }}</td>
            <td>{{ o.submits }}</td>
            <td>{{ o.replays }}</td>
            <td class="muted">{{ o.firstAt }}</td>
          </tr>
        </tbody>
      </table>
    </template>
  </section>
</template>
