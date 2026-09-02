<script setup>
import { reactive, computed } from 'vue'
import { SCENARIOS } from '../scenarios'

const runs = reactive({})

const running = computed(() => Object.values(runs).some((r) => r.status === 'running'))

const doneCount = computed(
  () => Object.values(runs).filter((r) => r.status === 'pass' || r.status === 'fail' || r.status === 'skip').length,
)

async function runOne(sc) {
  const run = reactive({ status: 'running', steps: [], summary: '', note: '' })
  runs[sc.id] = run
  const step = (text) => run.steps.push(`${new Date().toLocaleTimeString()}  ${text}`)
  try {
    const r = (await sc.run(step)) || {}
    run.status = r.skip ? 'skip' : r.pass ? 'pass' : 'fail'
    run.summary = r.summary || ''
  } catch (e) {
    run.status = 'fail'
    run.summary = '执行异常：' + (e?.message || e)
  }
}

async function runAll() {
  for (const sc of SCENARIOS) {
    // eslint-disable-next-line no-await-in-loop
    await runOne(sc)
  }
}

const STATUS_META = {
  running: { label: '运行中…', cls: 'info' },
  pass: { label: '通过', cls: 'ok' },
  fail: { label: '失败', cls: 'err' },
  skip: { label: '不适用', cls: 'muted' },
}
</script>

<template>
  <section class="card">
    <div class="card-head">
      <h2>
        ③ 自动化验收场景
        <span class="sub">一键执行 docs/ACCEPTANCE-CRITERIA.md 的核心场景，断言随当前链路自适应</span>
      </h2>
      <button class="primary" :disabled="running" @click="runAll">
        {{ running ? `执行中（${doneCount}/${SCENARIOS.length}）…` : '全部运行' }}
      </button>
    </div>

    <div v-for="sc in SCENARIOS" :key="sc.id" class="scenario">
      <div class="scenario-head">
        <span class="scenario-id">{{ sc.id }}</span>
        <span class="scenario-name">{{ sc.name }}</span>
        <span class="scenario-doc">{{ sc.doc }}</span>
        <button class="small" :disabled="running" @click="runOne(sc)">运行</button>
        <span v-if="runs[sc.id]" class="badge" :class="STATUS_META[runs[sc.id].status]?.cls">
          {{ STATUS_META[runs[sc.id].status]?.label }}
        </span>
      </div>

      <div class="scenario-expect">预期：<b>{{ sc.expect }}</b></div>

      <div v-if="runs[sc.id]" class="scenario-body">
        <ul v-if="runs[sc.id].steps.length" class="scenario-steps">
          <li v-for="(s, i) in runs[sc.id].steps" :key="i">{{ s }}</li>
        </ul>
        <div
          v-if="runs[sc.id].summary"
          class="scenario-summary"
          :class="runs[sc.id].status === 'fail' ? 'err-text' : runs[sc.id].status === 'pass' ? 'ok-text' : 'muted'"
        >
          {{ runs[sc.id].summary }}
        </div>
      </div>
    </div>
  </section>
</template>
