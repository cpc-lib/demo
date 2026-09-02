<script setup>
defineProps({
  form: { type: Object, required: true },
})

defineEmits(['new-key'])

const keyLength = (k) => (k ? k.length : 0)
</script>

<template>
  <section class="card">
    <h2 class="card-title">
      请求参数
      <span class="endpoint">POST /api/orders</span>
    </h2>

    <div class="field">
      <label for="idem-key">Idempotency-Key</label>
      <div class="key-row">
        <input
          id="idem-key"
          v-model="form.key"
          class="mono"
          spellcheck="false"
          placeholder="同一逻辑命令永远复用同一个 Key"
        />
        <button type="button" class="btn btn-light" @click="$emit('new-key')">换一个</button>
      </div>
      <p class="hint" :class="{ bad: keyLength(form.key) < 8 || keyLength(form.key) > 128 }">
        长度 8~128（当前 {{ keyLength(form.key) }}）· 由浏览器生成，Nginx / Gateway / Feign 全链路原样透传
      </p>
    </div>

    <div class="grid2">
      <div class="field">
        <label for="tenant">X-Tenant-Id</label>
        <input id="tenant" v-model="form.tenantId" spellcheck="false" />
      </div>
      <div class="field">
        <label for="user-id">userId</label>
        <input id="user-id" v-model.number="form.userId" type="number" required />
      </div>
      <div class="field">
        <label for="item-name">itemName</label>
        <input id="item-name" v-model="form.itemName" required />
      </div>
      <div class="field">
        <label for="amount">amount</label>
        <input id="amount" v-model.number="form.amount" type="number" step="0.01" min="0.01" required />
      </div>
    </div>

    <h3 class="section-title">故障注入（Demo Header）</h3>
    <label class="check">
      <input v-model="form.gatewayFailOnce" type="checkbox" />
      <span><code>X-Demo-Gateway-Fail-Once</code>：caller 在下游成功后故意返回 500 一次，触发 Gateway 的 POST Retry</span>
    </label>
    <label class="check">
      <input v-model="form.feignFailOnce" type="checkbox" />
      <span><code>X-Demo-Feign-Fail-Once</code>：order-service 本地事务 COMMIT 后故意返回 503 一次，触发 Feign Retry</span>
    </label>
  </section>
</template>
