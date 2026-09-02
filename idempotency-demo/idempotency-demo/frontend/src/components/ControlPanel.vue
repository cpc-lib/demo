<script setup>
import { ref } from 'vue'

defineProps({
  running: { type: Boolean, default: false },
  scenarios: { type: Array, required: true },
})

const emit = defineEmits(['once', 'repeat', 'concurrent', 'new-key', 'clear', 'scenario'])

const count = ref(5)
const counts = [3, 5, 10]
</script>

<template>
  <section class="card">
    <h2 class="card-title">操作</h2>

    <div class="actions">
      <button class="btn btn-primary" :disabled="running" @click="emit('once')">提交一次</button>
      <select v-model.number="count" class="count-select" :disabled="running" title="重复/并发次数">
        <option v-for="c in counts" :key="c" :value="c">× {{ c }}</option>
      </select>
      <button class="btn" :disabled="running" @click="emit('repeat', count)">顺序重复</button>
      <button class="btn" :disabled="running" @click="emit('concurrent', count)">并发提交</button>
    </div>
    <div class="actions">
      <button class="btn btn-light" :disabled="running" @click="emit('new-key')">生成新 Key</button>
      <button class="btn btn-light" :disabled="running" @click="emit('clear')">清空结果</button>
      <span v-if="running" class="running">请求执行中…</span>
    </div>

    <h2 class="card-title spaced">一键场景</h2>
    <p class="hint">每个场景自动生成新 Idempotency-Key，并设置对应的故障注入开关</p>
    <div class="scenario-list">
      <button
        v-for="s in scenarios"
        :key="s.id"
        type="button"
        class="scenario"
        :disabled="running"
        @click="emit('scenario', s)"
      >
        <strong>{{ s.title }}</strong>
        <span>{{ s.detail }}</span>
      </button>
    </div>
  </section>
</template>
