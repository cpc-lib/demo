import { store } from './store'
import { issueToken, submitOrder, checkConflict } from './api'

// 场景固定使用字节级一致的请求体字符串（amount 保留字面小数位，request_hash 才稳定）
const BODY_A = '{"userId":1001,"itemName":"keyboard","amount":299.00}'
const BODY_B = '{"userId":1001,"itemName":"mouse","amount":199.00}'

function randomHex(n) {
  let s = ''
  for (let i = 0; i < n; i++) s += Math.floor(Math.random() * 16).toString(16)
  return s
}

function issueFail(entry) {
  const why = entry.hint || (entry.resBody?.message ? `HTTP ${entry.status} · ${entry.resBody.message}` : `HTTP ${entry.status}`)
  return { pass: false, summary: `签发 requestId 失败（${why}）` }
}

/**
 * 测试场景，对应 docs/ACCEPTANCE-CRITERIA.md 与 docs/FAILURE-MATRIX.md。
 * 每个场景返回 { pass, summary, note?, skip? }，过程通过 step(text) 记录。
 */
export const SCENARIOS = [
  {
    id: 'A',
    name: '正常创建订单',
    doc: '两步协议基础流程',
    expect: '200 · replayed=false · 返回 orderNo',
    async run(step) {
      const t = await issueToken()
      if (!t.ok) return issueFail(t.entry)
      step(`签发 requestId：${t.token.requestId}`)
      const r = await submitOrder({ key: t.token.requestId, body: BODY_A })
      step(`携带 Idempotency-Key 提交：HTTP ${r.status} · ${r.error ? r.error : JSON.stringify(r.resBody)}`)
      const pass = r.status === 200 && r.resBody?.replayed === false && !!r.resBody?.orderNo
      return {
        pass,
        summary: pass
          ? `HTTP 200 · orderNo=${r.resBody.orderNo} · replayed=false`
          : `期望 200 / replayed=false，实际 HTTP ${r.status}`,
      }
    },
  },
  {
    id: 'B',
    name: '重复提交 / 并发竞争（同 Key ×10）',
    doc: '验收③：同 key 同 payload 调 10 次只产生 1 条订单',
    expect: '10×200 · 唯一 orderNo=1 · replayed=false 仅 1 次',
    async run(step) {
      const t = await issueToken()
      if (!t.ok) return issueFail(t.entry)
      step(`签发 requestId：${t.token.requestId}`)
      step('并发发送 10 次完全相同的请求（同 Key + 同 payload 字符串）…')
      const rs = await Promise.all(
        Array.from({ length: 10 }, () => submitOrder({ key: t.token.requestId, body: BODY_A })),
      )
      const ok = rs.filter((r) => r.status === 200)
      const orderNos = [...new Set(ok.map((r) => r.resBody?.orderNo).filter(Boolean))]
      const first = ok.filter((r) => r.resBody?.replayed === false).length
      step(`完成：200×${ok.length} · 唯一 orderNo ${orderNos.length} 个 · replayed=false ${first} 次`)
      const pass = ok.length === 10 && orderNos.length === 1 && first === 1
      return {
        pass,
        summary: pass
          ? `10 次提交 → 同一订单 ${orderNos[0]}，仅首次真正创建（SELECT FOR UPDATE 行锁串行化）`
          : `期望 10×200 / 唯一订单 / 首次 1 次，实际 200×${ok.length}、订单 ${orderNos.length} 个、首次 ${first} 次`,
      }
    },
  },
  {
    id: 'C',
    name: '缺失 Idempotency-Key',
    doc: '验收②：POST /api/orders 必须携带预签发的 key',
    expect: '经 Gateway 链路 428 IDEMPOTENCY_KEY_REQUIRED；直连后端为 400（缺必需头）',
    async run(step) {
      step('不携带 Idempotency-Key 直接 POST 订单接口')
      const r = await submitOrder({ key: null, withKey: false, body: BODY_A })
      step(`响应：HTTP ${r.status} · ${JSON.stringify(r.resBody)}`)
      if (store.channel === 'nginx' || store.channel === 'gateway') {
        const pass = r.status === 428 && r.resBody?.code === 'IDEMPOTENCY_KEY_REQUIRED'
        return {
          pass,
          summary: `HTTP ${r.status} · ${r.resBody?.code || '未返回错误码'}${
            pass ? '（Gateway 全局过滤器拦截）' : ''
          }`,
        }
      }
      return {
        pass: r.status === 400,
        summary: `HTTP ${r.status}（当前链路不经过 Gateway，由 Spring 返回缺必需头 400，属预期）`,
      }
    },
  },
  {
    id: 'D',
    name: '同 Key 不同 payload',
    doc: '验收④：同一 key 换 payload 被拒绝且不产生第二条订单',
    expect: '首次 200；再次 409 IDEMPOTENCY_KEY_REUSED（经 caller 链路显示为包装后的 500）',
    async run(step) {
      const t = await issueToken()
      if (!t.ok) return issueFail(t.entry)
      step(`签发 requestId：${t.token.requestId}`)
      const r1 = await submitOrder({ key: t.token.requestId, body: BODY_A })
      step(`首次提交（keyboard/299.00）：HTTP ${r1.status} · orderNo=${r1.resBody?.orderNo ?? '-'}`)
      const r2 = await submitOrder({ key: t.token.requestId, body: BODY_B })
      const c = checkConflict(r2, 'IDEMPOTENCY_KEY_REUSED')
      step(`同 Key 换 payload 提交（mouse/199.00）：HTTP ${r2.status} · ${c.via}`)
      const pass = r1.status === 200 && c.hit
      return {
        pass,
        summary: pass
          ? `首次 200 创建成功；换 payload 的重放被拒绝（${c.via}）`
          : `期望首次 200 + 冲突拒绝，实际 ${r1.status} / ${r2.status}`,
      }
    },
  },
  {
    id: 'E',
    name: '跨租户使用 token',
    doc: '验收⑤：token 绑定签发时的 tenant + operation scope',
    expect: '409 IDEMPOTENCY_TOKEN_SCOPE_MISMATCH',
    async run(step) {
      const t = await issueToken('tenant-a')
      if (!t.ok) return issueFail(t.entry)
      step(`以租户 tenant-a 签发：${t.token.requestId}`)
      const r = await submitOrder({ key: t.token.requestId, tenantId: 'tenant-b', body: BODY_A })
      const c = checkConflict(r, 'IDEMPOTENCY_TOKEN_SCOPE_MISMATCH')
      step(`以租户 tenant-b 提交：HTTP ${r.status} · ${c.via}`)
      return {
        pass: c.hit,
        summary: c.hit ? `scope 不匹配被拒绝（${c.via}）` : `期望冲突拒绝，实际 HTTP ${r.status}`,
      }
    },
  },
  {
    id: 'F',
    name: '伪造不存在的 token',
    doc: '故障矩阵：客户端自造 key 无法通过校验（库里只存 SHA256，明文不可猜）',
    expect: '409 IDEMPOTENCY_TOKEN_NOT_FOUND',
    async run(step) {
      const fake = 'idem_' + randomHex(32)
      step(`伪造格式合法但后端未签发的 key：${fake}`)
      const r = await submitOrder({ key: fake, body: BODY_A })
      const c = checkConflict(r, 'IDEMPOTENCY_TOKEN_NOT_FOUND')
      step(`提交：HTTP ${r.status} · ${c.via}`)
      return {
        pass: c.hit,
        summary: c.hit ? `伪造 token 被拒绝（${c.via}）` : `期望冲突拒绝，实际 HTTP ${r.status}`,
      }
    },
  },
  {
    id: 'G',
    name: '非法长度的 Idempotency-Key',
    doc: 'key 长度必须在 8~128 字符之间',
    expect: '409 INVALID_IDEMPOTENCY_KEY',
    async run(step) {
      step('发送长度仅 5 的 key：short')
      const r = await submitOrder({ key: 'short', body: BODY_A })
      const c = checkConflict(r, 'INVALID_IDEMPOTENCY_KEY')
      step(`提交：HTTP ${r.status} · ${c.via}`)
      return {
        pass: c.hit,
        summary: c.hit ? `非法 key 被拒绝（${c.via}）` : `期望冲突拒绝，实际 HTTP ${r.status}`,
      }
    },
  },
  {
    id: 'H',
    name: '非法参数（amount=0）',
    doc: 'Bean Validation：amount >= 0.01，且不会占用 token',
    expect: '400（直连 order-service）；经 caller 链路被包装为 500',
    async run(step) {
      const t = await issueToken()
      if (!t.ok) return issueFail(t.entry)
      step(`签发 requestId：${t.token.requestId}`)
      const r = await submitOrder({ key: t.token.requestId, body: '{"userId":1001,"itemName":"keyboard","amount":0}' })
      step(`提交 amount=0：HTTP ${r.status}`)
      const pass = r.status === 400 || r.status === 500
      return {
        pass,
        summary: pass
          ? `非法参数被拒绝（HTTP ${r.status}${r.status === 500 ? '，校验错误经 caller-service 被包装为 500，属 demo 已知行为' : ''}）`
          : `期望 400/500 拒绝，实际 HTTP ${r.status}`,
      }
    },
  },
  {
    id: 'I',
    name: 'COMMIT 后 503（Feign 自动重试）',
    doc: '验收⑦：order-service 事务提交后返回 503，Feign Retryer 用同 Key 重放',
    expect: '最终 200 且 replayed=true（直连 order-service 时首次显式 503，手动重放同样成功）',
    async run(step) {
      const t = await issueToken()
      if (!t.ok) return issueFail(t.entry)
      step(`签发 requestId：${t.token.requestId}`)
      if (store.channel === 'order') {
        const r1 = await submitOrder({ key: t.token.requestId, body: BODY_A, feignFailOnce: true })
        step(`携带 X-Demo-Feign-Fail-Once 提交：HTTP ${r1.status}（事务已 COMMIT，响应被劫持为 503）`)
        const r2 = await submitOrder({ key: t.token.requestId, body: BODY_A })
        step(`同 Key 重放：HTTP ${r2.status} · replayed=${r2.resBody?.replayed}`)
        const pass = r1.status === 503 && r2.status === 200 && r2.resBody?.replayed === true
        return {
          pass,
          summary: pass
            ? '503 后重放成功，orderNo 不变，订单未重复'
            : `期望 503 → 200/replayed=true，实际 ${r1.status} → ${r2.status}`,
        }
      }
      const r = await submitOrder({ key: t.token.requestId, body: BODY_A, feignFailOnce: true })
      step(`携带 X-Demo-Feign-Fail-Once 提交：HTTP ${r.status} · replayed=${r.resBody?.replayed}（Feign 已在链路内自动重试）`)
      const pass = r.status === 200 && r.resBody?.replayed === true
      return {
        pass,
        summary: pass
          ? '客户端一次请求即得到 200，重试发生在 Feign 内部，订单未重复'
          : `期望 200/replayed=true，实际 HTTP ${r.status}`,
      }
    },
  },
  {
    id: 'J',
    name: 'caller 500（Gateway 自动重试）',
    doc: '验收⑧：caller 在下游成功后返回 500，Gateway Retry 过滤器重放 POST',
    expect: '最终 200 且 replayed=true（直连 caller 时首次显式 500；直连 order-service 时故障注入不生效）',
    async run(step) {
      const t = await issueToken()
      if (!t.ok) return issueFail(t.entry)
      step(`签发 requestId：${t.token.requestId}`)
      if (store.channel === 'order') {
        const r = await submitOrder({ key: t.token.requestId, body: BODY_A, gatewayFailOnce: true })
        return {
          skip: true,
          summary: `该链路不经过 caller/Gateway，故障注入头不生效（HTTP ${r.status} · replayed=${r.resBody?.replayed}），请切换链路验证`,
        }
      }
      if (store.channel === 'caller') {
        const r1 = await submitOrder({ key: t.token.requestId, body: BODY_A, gatewayFailOnce: true })
        step(`携带 X-Demo-Gateway-Fail-Once 提交：HTTP ${r1.status}（下游已成功，caller 显式 500；此链路无 Gateway 重试）`)
        const r2 = await submitOrder({ key: t.token.requestId, body: BODY_A })
        step(`同 Key 重放：HTTP ${r2.status} · replayed=${r2.resBody?.replayed}`)
        const pass = r1.status === 500 && r2.status === 200 && r2.resBody?.replayed === true
        return {
          pass,
          summary: pass ? '500 后重放成功，订单未重复' : `期望 500 → 200/replayed=true，实际 ${r1.status} → ${r2.status}`,
        }
      }
      const r = await submitOrder({ key: t.token.requestId, body: BODY_A, gatewayFailOnce: true })
      step(`携带 X-Demo-Gateway-Fail-Once 提交：HTTP ${r.status} · replayed=${r.resBody?.replayed}（Gateway 已自动重试）`)
      const pass = r.status === 200 && r.resBody?.replayed === true
      return {
        pass,
        summary: pass
          ? '客户端一次请求即得到 200，重试发生在 Gateway，订单未重复'
          : `期望 200/replayed=true，实际 HTTP ${r.status}`,
      }
    },
  },
]
