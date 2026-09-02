// 对接后端唯一对外接口：
//   POST /api/orders
//   Nginx(:80) -> Gateway(:8080, StripPrefix /api) -> caller-service(:8081)
//   -> OpenFeign -> order-service(:8082, /internal/orders)

const API_BASE = import.meta.env.VITE_API_BASE || ''
const ORDERS_URL = `${API_BASE}/api/orders`

/**
 * 幂等 Key 必须在「最早的重试边界之前」生成（README 第 3 节），
 * 即浏览器/最上游调用方，保证 Nginx/Gateway/Feign 各层重试都带同一个 Key。
 */
export function newIdempotencyKey() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return 'key-' + Date.now().toString(36) + Math.random().toString(36).slice(2)
}

/**
 * 创建订单（幂等）。
 *
 * @param {object} options
 * @param {string} options.key             Idempotency-Key（8~128 字符）
 * @param {string} [options.tenantId]      X-Tenant-Id，默认 demo
 * @param {boolean} [options.gatewayFailOnce] X-Demo-Gateway-Fail-Once 故障注入
 * @param {boolean} [options.feignFailOnce]   X-Demo-Feign-Fail-Once 故障注入
 * @param {{userId:number,itemName:string,amount:number}} options.payload
 * @returns {Promise<{ok:boolean,httpStatus:number,echoedKey:string|null,body:any,durationMs:number}>}
 */
export async function createOrder({
  key,
  tenantId = 'demo',
  gatewayFailOnce = false,
  feignFailOnce = false,
  payload,
}) {
  const headers = {
    'Content-Type': 'application/json',
    'Idempotency-Key': key,
  }
  if (tenantId && tenantId.trim()) {
    headers['X-Tenant-Id'] = tenantId.trim()
  }
  if (gatewayFailOnce) {
    headers['X-Demo-Gateway-Fail-Once'] = 'true'
  }
  if (feignFailOnce) {
    headers['X-Demo-Feign-Fail-Once'] = 'true'
  }

  const started = performance.now()
  const res = await fetch(ORDERS_URL, {
    method: 'POST',
    headers,
    body: JSON.stringify(payload),
  })
  const durationMs = Math.round(performance.now() - started)

  return {
    ok: res.ok,
    httpStatus: res.status,
    // Gateway 会把最终生效的 Key 回传在响应头，用于验证全链路 Key 不变（AC-08）
    echoedKey: res.headers.get('Idempotency-Key'),
    body: await parseBody(res),
    durationMs,
  }
}

async function parseBody(res) {
  const text = await res.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return { message: text }
  }
}
