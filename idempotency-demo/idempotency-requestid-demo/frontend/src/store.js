import { reactive } from 'vue'

/**
 * 全局响应式状态：链路配置、已签发 token、请求日志、订单汇总。
 */
export const store = reactive({
  // 访问链路，见 api.js CHANNELS
  channel: 'nginx',
  tenantId: 'demo',

  // 已签发的 requestId（新的在前）
  tokens: [],
  currentTokenId: null,

  // 请求日志（新的在前，最多 300 条）
  logs: [],

  // 订单汇总：orderNo -> { orderNo, tenantId, submits, replays, firstAt }
  orders: {},
})

let logSeq = 1

export function addLog(entry) {
  entry.id = logSeq++
  store.logs.unshift(entry)
  if (store.logs.length > 300) store.logs.length = 300
}

export function addToken(token) {
  store.tokens.unshift(token)
  store.currentTokenId = token.requestId
}

export function markTokenUsed(requestId) {
  const t = store.tokens.find((x) => x.requestId === requestId)
  if (t) t.used = true
}

export function addOrder(orderNo, replayed, tenantId) {
  const o = store.orders[orderNo]
  if (o) {
    o.submits++
    if (replayed) o.replays++
  } else {
    store.orders[orderNo] = {
      orderNo,
      tenantId,
      submits: 1,
      replays: replayed ? 1 : 0,
      firstAt: new Date().toLocaleTimeString(),
    }
  }
}

export function clearLogs() {
  store.logs = []
}
