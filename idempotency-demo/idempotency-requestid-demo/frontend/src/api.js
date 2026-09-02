import { store, addLog, addToken, markTokenUsed, addOrder } from './store'

/**
 * 四条访问链路。dev 模式经 Vite 代理转发（同源、无 CORS）；
 * 构建产物部署在 Nginx(:80) 下时只有 nginx 链路可用。
 */
export const CHANNELS = {
  nginx: {
    key: 'nginx',
    label: 'Nginx 全链路',
    desc: '浏览器 → Nginx:80 → Gateway:8080 → caller:8081 → Feign → order:8082',
    issuePath: '/api/idempotency/order-create',
    orderPath: '/api/orders',
    devOnly: false,
  },
  gateway: {
    key: 'gateway',
    label: '直连 Gateway',
    desc: '浏览器 → Gateway:8080 → caller:8081 → order:8082（绕过 Nginx）',
    issuePath: '/direct-gateway/idempotency/order-create',
    orderPath: '/direct-gateway/orders',
    devOnly: true,
  },
  caller: {
    key: 'caller',
    label: '直连 caller-service',
    desc: '浏览器 → caller:8081 → Feign → order:8082（绕过 Gateway：无 428 拦截、无 Gateway 重试；签发仍走 Gateway）',
    issuePath: '/direct-gateway/idempotency/order-create',
    orderPath: '/direct-caller/orders',
    devOnly: true,
  },
  order: {
    key: 'order',
    label: '直连 order-service',
    desc: '浏览器 → order:8082 原生端点（绕过整条链路，可观察纯净的 409 错误体；故障注入头不经过 caller/Gateway）',
    issuePath: '/direct-order/idempotency/order-create',
    orderPath: '/direct-order/internal/orders',
    devOnly: true,
  },
}

export function currentChannel() {
  return CHANNELS[store.channel]
}

/** 识别代理/网络层故障（区别于后端业务错误），生成可读提示 */
function detectHint(entry) {
  if (entry.error) {
    return `网络请求失败：${entry.error}`
  }
  if (entry.status === 502 && entry.resBody?.code === 'PROXY_TARGET_UNREACHABLE') {
    return entry.resBody.message
  }
  if (entry.status === 500 && entry.resBody == null) {
    return '代理返回 500 且无响应体：大概率是代理目标不可达，请确认后端已启动（dev 终端会有 http proxy error 日志）'
  }
  return ''
}

/** 统一请求入口：记录请求/响应全量信息到日志 */
async function request({ method, path, headers = {}, body = null, tag }) {
  const started = performance.now()
  const entry = {
    time: new Date().toLocaleTimeString(),
    channel: store.channel,
    tag: tag || '',
    method,
    path,
    reqHeaders: { ...headers },
    reqBody: body,
    status: null,
    ok: false,
    resHeaders: {},
    resBody: null,
    durationMs: null,
    error: null,
  }
  try {
    const res = await fetch(path, {
      method,
      headers: Object.keys(headers).length ? headers : undefined,
      body,
    })
    entry.status = res.status
    entry.ok = res.ok
    const h = {}
    res.headers.forEach((value, name) => (h[name] = value))
    entry.resHeaders = h
    const text = await res.text()
    if (text) {
      try {
        entry.resBody = JSON.parse(text)
      } catch {
        entry.resBody = text
      }
    }
  } catch (e) {
    entry.error = e?.message || String(e)
  }
  entry.durationMs = Math.round(performance.now() - started)
  entry.hint = detectHint(entry)
  addLog(entry)
  return entry
}

/** 第一步：POST /api/idempotency/order-create 签发 requestId（TTL 10 分钟） */
export async function issueToken(tenantId) {
  const tenant = tenantId || store.tenantId
  const entry = await request({
    method: 'POST',
    path: currentChannel().issuePath,
    headers: { 'X-Tenant-Id': tenant },
    tag: '① 签发 requestId',
  })
  if (entry.status === 200 && entry.resBody?.requestId) {
    const token = {
      requestId: entry.resBody.requestId,
      expiresAt: entry.resBody.expiresAt ? new Date(entry.resBody.expiresAt) : null,
      issuedAt: new Date(),
      tenantId: tenant,
      used: false,
    }
    addToken(token)
    return { ok: true, token, entry }
  }
  return { ok: false, token: null, entry }
}

/**
 * 第二步：POST /api/orders 携带 Idempotency-Key 提交订单。
 * body 必须传字符串，重放时字节级一致（299.00 与 299.0 的 request_hash 不同）。
 */
export async function submitOrder({
  key,
  tenantId,
  body,
  withKey = true,
  gatewayFailOnce = false,
  feignFailOnce = false,
  tag = '② 提交订单',
}) {
  const tenant = tenantId || store.tenantId
  const headers = { 'Content-Type': 'application/json', 'X-Tenant-Id': tenant }
  if (withKey && key) headers['Idempotency-Key'] = key
  if (gatewayFailOnce) headers['X-Demo-Gateway-Fail-Once'] = 'true'
  if (feignFailOnce) headers['X-Demo-Feign-Fail-Once'] = 'true'

  const entry = await request({
    method: 'POST',
    path: currentChannel().orderPath,
    headers,
    body,
    tag,
  })
  if (entry.status === 200 && entry.resBody?.orderNo) {
    addOrder(entry.resBody.orderNo, !!entry.resBody.replayed, tenant)
    if (withKey && key) markTokenUsed(key)
  }
  return entry
}

/** 构造订单请求体字符串。amount 保留字面值（299.00 不被规整为 299）。 */
export function buildOrderBody({ userId, itemName, amount }) {
  const uid = String(userId).trim()
  const amt = String(amount).trim()
  if (!/^-?\d+$/.test(uid)) throw new Error('userId 需为整数')
  if (!/^-?\d+(\.\d+)?$/.test(amt)) throw new Error('amount 需为数字')
  return `{"userId":${uid},"itemName":${JSON.stringify(String(itemName))},"amount":${amt}}`
}

/**
 * 幂等冲突断言（对齐 demo 的真实行为）：
 * - 直连 order-service：干净的 409 + {"code","message"}
 * - 经 caller-service 链路：FeignException 被 Spring 默认包装为 500（demo 未配置错误透传）
 */
export function checkConflict(entry, expectedCode) {
  if (entry.status === 409) {
    return {
      hit: entry.resBody?.code === expectedCode,
      via: `409 · ${entry.resBody?.code || '无错误码'}`,
    }
  }
  if (entry.status === 500) {
    return { hit: true, via: '500（409 被 caller-service 包装为默认错误体）' }
  }
  return { hit: false, via: `实际 HTTP ${entry.status}` }
}
