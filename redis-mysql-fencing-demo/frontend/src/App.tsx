import { useCallback, useEffect, useState } from 'react';
import CounterPanel from './CounterPanel';
import {
  buy,
  generateKey,
  getProduct,
  listProducts,
  login,
  resetAll,
  type ApiError,
  type LoginResponse,
  type ProductSnapshot,
  type SeckillResponse,
} from './api';

const DEMO_PASSWORD = '123456';
const DEMO_USERS = ['alice', 'bob', 'charlie', 'dave'];

// ---------- 请求日志 ----------
interface LogEntry {
  id: number;
  time: string;
  kind: 'info' | 'ok' | 'err';
  text: string;
}

let logSeq = 0;
function useLogs() {
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const push = useCallback((kind: LogEntry['kind'], text: string) => {
    setLogs((prev) =>
      [{ id: ++logSeq, time: new Date().toLocaleTimeString(), kind, text }, ...prev].slice(0, 200),
    );
  }, []);
  const clear = useCallback(() => setLogs([]), []);
  return { logs, push, clear };
}

// ---------- 1. 登录 ----------
function LoginSection({
  auth,
  onLogin,
  onLogout,
  log,
}: {
  auth: LoginResponse | null;
  onLogin: (a: LoginResponse) => void;
  onLogout: () => void;
  log: (k: LogEntry['kind'], t: string) => void;
}) {
  const [username, setUsername] = useState('alice');
  const [password, setPassword] = useState(DEMO_PASSWORD);
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    setBusy(true);
    try {
      const r = await login(username, password);
      onLogin(r);
      log('ok', `POST /api/auth/login ${username} → userId=${r.userId}, expiresIn=${r.expiresInMs}ms`);
    } catch (e) {
      log('err', `登录失败: ${(e as ApiError).message}`);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="rounded-lg border border-slate-700 p-5">
      <h2 className="text-lg font-semibold mb-3 text-cyan-300">1. 登录（JWT）</h2>
      {auth ? (
        <div className="flex items-center gap-3 flex-wrap text-sm">
          <span>
            已登录: <span className="font-mono text-green-300">{auth.userId}</span>
          </span>
          <span className="text-slate-500 font-mono break-all max-w-md truncate">{auth.token}</span>
          <button onClick={onLogout} className="px-3 py-1 rounded bg-slate-700 hover:bg-slate-600">
            退出
          </button>
        </div>
      ) : (
        <div className="flex gap-2 items-center flex-wrap">
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="w-32 bg-slate-800 border border-slate-600 rounded px-2 py-1"
            placeholder="username"
          />
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-32 bg-slate-800 border border-slate-600 rounded px-2 py-1"
            placeholder="password"
          />
          <button
            onClick={submit}
            disabled={busy}
            className="px-3 py-1 rounded bg-cyan-600 hover:bg-cyan-500 disabled:opacity-50"
          >
            {busy ? '登录中…' : '登录'}
          </button>
          <span className="text-xs text-slate-500">测试账号: alice / bob / charlie / dave，密码 123456</span>
          <div className="flex gap-1">
            {DEMO_USERS.map((u) => (
              <button
                key={u}
                onClick={() => setUsername(u)}
                className="px-2 py-0.5 rounded bg-slate-800 hover:bg-slate-700 text-xs font-mono"
              >
                {u}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ---------- 2. 商品列表（来自数据库） ----------
function ProductSection({
  products,
  productId,
  onSelect,
  onReload,
  onReset,
  auth,
  busy,
}: {
  products: ProductSnapshot[];
  productId: string;
  onSelect: (id: string) => void;
  onReload: () => void;
  onReset: () => void;
  auth: LoginResponse | null;
  busy: boolean;
}) {
  const selected = products.find((p) => p.productId === productId);

  return (
    <div className="rounded-lg border border-slate-700 p-5">
      <h2 className="text-lg font-semibold mb-3 text-sky-300">2. 商品列表（数据库）</h2>
      <div className="flex gap-2 items-center flex-wrap">
        <select
          value={productId}
          onChange={(e) => onSelect(e.target.value)}
          disabled={!auth || products.length === 0}
          className="bg-slate-800 border border-slate-600 rounded px-2 py-1 font-mono min-w-64 disabled:opacity-50"
        >
          {products.length === 0 && <option value="">请先登录</option>}
          {products.map((p) => (
            <option key={p.productId} value={p.productId}>
              {p.productId} · {p.name} · 库存 {p.stock}
            </option>
          ))}
        </select>
        <button
          onClick={onReload}
          disabled={!auth || busy}
          className="px-3 py-1 rounded bg-sky-600 hover:bg-sky-500 disabled:opacity-50"
        >
          刷新列表
        </button>
        <button
          onClick={onReset}
          disabled={!auth || busy}
          className="px-3 py-1 rounded bg-rose-600 hover:bg-rose-500 disabled:opacity-50"
        >
          {busy ? '处理中…' : '重置全部数据'}
        </button>
      </div>

      {selected && (
        <div className="mt-3 flex gap-4 text-sm bg-slate-800 rounded px-3 py-2 flex-wrap">
          <span className="text-slate-300">{selected.name}</span>
          <span>
            库存 <span className="font-mono text-green-300">{selected.stock}</span>
          </span>
          <span>
            已售 <span className="font-mono text-amber-300">{selected.sold}</span>
          </span>
          <span>
            fence <span className="font-mono text-yellow-300">{selected.fenceToken}</span>
          </span>
        </div>
      )}
      <p className="mt-2 text-xs text-slate-500">
        重置会清空订单与幂等记录，库存恢复为初始值（fence_token 保持单调不回退），并清除 Redis 商品缓存。
      </p>
    </div>
  );
}

// ---------- 3. 单次抢购（幂等） ----------
function BuySection({
  productId,
  auth,
  onSnapshot,
  log,
}: {
  productId: string;
  auth: LoginResponse | null;
  onSnapshot: (s: ProductSnapshot) => void;
  log: (k: LogEntry['kind'], t: string) => void;
}) {
  const [key, setKey] = useState(generateKey('buy'));
  const [resp, setResp] = useState<SeckillResponse | null>(null);
  const [err, setErr] = useState<ApiError | null>(null);
  const [busy, setBusy] = useState(false);

  const rollKey = () => {
    const k = generateKey('buy');
    setKey(k);
    setResp(null);
    setErr(null);
    log('info', `新幂等键: ${k}`);
  };

  const doBuy = async () => {
    if (!auth) {
      log('err', '请先登录');
      return;
    }
    if (!productId) {
      log('err', '请先选择商品');
      return;
    }
    setBusy(true);
    setErr(null);
    try {
      const r = await buy(productId, auth.token, key);
      setResp(r);
      log(
        'ok',
        `POST /api/seckill/${productId}/buy key=${key} → order=${r.orderId}, 剩余库存=${r.remainingStock}, replayed=${r.replayed}`,
      );
      try {
        onSnapshot(await getProduct(productId, auth.token));
      } catch {
        // 忽略刷新失败
      }
    } catch (e) {
      const apiErr = e as ApiError;
      setErr(apiErr);
      log('err', `抢购失败: ${apiErr.message}${apiErr.errorCode ? ` [${apiErr.errorCode}]` : ''}`);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="rounded-lg border border-slate-700 p-5">
      <h2 className="text-lg font-semibold mb-3 text-emerald-300">3. 抢购（一人一单 · 不超卖）</h2>
      <p className="text-xs text-slate-400 mb-3">
        同一 Idempotency-Key 重复提交 → 返回原订单（replayed=true）；换新 key 再买同一商品 → 409 ALREADY_PURCHASED。
      </p>
      <div className="flex gap-2 items-center flex-wrap">
        <button onClick={rollKey} className="px-3 py-1 rounded bg-slate-700 hover:bg-slate-600 text-sm">
          换新幂等键
        </button>
        <button
          onClick={doBuy}
          disabled={busy}
          className="px-3 py-1 rounded bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50"
        >
          {busy ? '抢购中…' : '立即抢购'}
        </button>
        <span className="text-xs text-slate-500 font-mono break-all">key = {key}</span>
      </div>

      {resp && (
        <div className="mt-3 grid grid-cols-2 md:grid-cols-5 gap-2 text-sm">
          <div className="bg-slate-800 rounded p-2">
            <div className="text-slate-400">商品</div>
            <div className="font-mono">{resp.productId}</div>
          </div>
          <div className="bg-slate-800 rounded p-2">
            <div className="text-slate-400">订单号</div>
            <div className="font-mono">{resp.orderId}</div>
          </div>
          <div className="bg-slate-800 rounded p-2">
            <div className="text-slate-400">用户</div>
            <div className="font-mono">{resp.userId}</div>
          </div>
          <div className="bg-slate-800 rounded p-2">
            <div className="text-slate-400">剩余库存</div>
            <div className="font-mono text-green-300">{resp.remainingStock}</div>
          </div>
          <div className="bg-slate-800 rounded p-2">
            <div className="text-slate-400">replayed</div>
            <div className={`font-mono ${resp.replayed ? 'text-amber-300' : 'text-green-300'}`}>
              {String(resp.replayed)}
            </div>
          </div>
        </div>
      )}

      {err && (
        <div className="mt-3 p-2 rounded bg-red-900/50 text-red-300 text-sm">
          HTTP {err.status}
          {err.errorCode ? ` [${err.errorCode}]` : ''} — {err.message}
        </div>
      )}
    </div>
  );
}

// ---------- 4. 并发压测 ----------
type Outcome = { i: number; user: string; ok: boolean; status?: number; code?: string; err?: string };

function ConcurrentSection({
  productId,
  auth,
  stockHint,
  log,
}: {
  productId: string;
  auth: LoginResponse | null;
  stockHint: number | null;
  log: (k: LogEntry['kind'], t: string) => void;
}) {
  const [count, setCount] = useState(10);
  const [mode, setMode] = useState<'same-user' | 'multi-user'>('multi-user');
  const [running, setRunning] = useState(false);
  const [outcomes, setOutcomes] = useState<Outcome[]>([]);
  const [stockBefore, setStockBefore] = useState<number | null>(null);
  const [stockAfter, setStockAfter] = useState<number | null>(null);

  const run = async () => {
    if (!auth) {
      log('err', '请先登录');
      return;
    }
    if (!productId) {
      log('err', '请先选择商品');
      return;
    }
    setRunning(true);
    setOutcomes([]);
    setStockAfter(null);

    // 多用户模式下先给每个测试账号拿 token
    const tokens: Record<string, string> = { [auth.userId]: auth.token };
    if (mode === 'multi-user') {
      await Promise.all(
        DEMO_USERS.filter((u) => u !== auth.userId).map(async (u) => {
          try {
            tokens[u] = (await login(u, DEMO_PASSWORD)).token;
          } catch {
            // 忽略登录失败
          }
        }),
      );
    }

    let before: number | null = null;
    try {
      before = (await getProduct(productId, auth.token)).stock;
      setStockBefore(before);
    } catch {
      setStockBefore(null);
    }

    const keyPrefix = generateKey('rush');
    log('info', `--- 并发抢购 ${count} 次（${mode === 'same-user' ? '同一用户' : '多用户轮换'}） ---`);

    const users = Array.from({ length: count }, (_, i) =>
      mode === 'same-user' ? auth.userId : DEMO_USERS[i % DEMO_USERS.length],
    );

    const tasks = users.map((user, i) =>
      buy(productId, tokens[user], `${keyPrefix}_${i}`)
        .then<Outcome>(() => ({ i, user, ok: true }))
        .catch((e: ApiError) => ({ i, user, ok: false, status: e.status, code: e.errorCode, err: e.message })),
    );
    const out = await Promise.all(tasks);
    setOutcomes(out);

    const success = out.filter((r) => r.ok).length;
    const byCode = out
      .filter((r) => !r.ok)
      .reduce<Record<string, number>>((acc, r) => {
        const k = r.code ?? `HTTP ${r.status}`;
        acc[k] = (acc[k] ?? 0) + 1;
        return acc;
      }, {});
    log('ok', `成功 ${success}/${count}，失败分布 ${JSON.stringify(byCode)}`);

    try {
      const after = await getProduct(productId, auth.token);
      setStockAfter(after.stock);
      const oversold = before !== null && after.stock < 0;
      log(
        oversold ? 'err' : 'ok',
        `库存 ${before} → ${after.stock}，成功 ${success} 单，${oversold ? 'FAIL 超卖！' : '未出现超卖'}`,
      );
    } catch (e) {
      log('err', `刷新库存失败: ${(e as ApiError).message}`);
    }

    setRunning(false);
  };

  const success = outcomes.filter((r) => r.ok).length;
  const expectedCap = mode === 'same-user' ? 1 : DEMO_USERS.length;
  const pass =
    stockBefore !== null && stockAfter !== null
      ? success <= expectedCap && stockBefore - stockAfter === success && stockAfter >= 0
      : null;

  return (
    <div className="rounded-lg border border-slate-700 p-5">
      <h2 className="text-lg font-semibold mb-3 text-fuchsia-300">4. 并发压测</h2>
      <p className="text-xs text-slate-400 mb-3">
        「同一用户」并发 N 次 → 只应成功 1 单（一人一单）；「多用户轮换」最多成功 min(N, 账号数)。
        库存扣减数必须等于成功单数，且始终 ≥ 0（不超卖）。
        {stockHint !== null && (
          <>
            {' '}当前库存 <span className="font-mono text-green-300">{stockHint}</span>。
          </>
        )}
      </p>
      <div className="flex gap-2 items-center flex-wrap">
        <label className="text-sm text-slate-400">请求数:</label>
        <input
          type="number"
          min={1}
          max={200}
          value={count}
          onChange={(e) => setCount(Math.max(1, Math.min(200, Number(e.target.value))))}
          className="w-24 bg-slate-800 border border-slate-600 rounded px-2 py-1"
        />
        <select
          value={mode}
          onChange={(e) => setMode(e.target.value as 'same-user' | 'multi-user')}
          className="bg-slate-800 border border-slate-600 rounded px-2 py-1 text-sm"
        >
          <option value="multi-user">多用户轮换</option>
          <option value="same-user">同一用户</option>
        </select>
        <button
          onClick={run}
          disabled={running}
          className="px-3 py-1 rounded bg-fuchsia-600 hover:bg-fuchsia-500 disabled:opacity-50"
        >
          {running ? '压测中…' : '开始压测'}
        </button>
      </div>

      {outcomes.length > 0 && (
        <div className="mt-3">
          <div className="text-sm mb-1 text-slate-400">逐请求结果（悬停查看原因）:</div>
          <div className="flex flex-wrap gap-1 max-h-24 overflow-auto">
            {outcomes.map((r) => (
              <span
                key={r.i}
                title={r.ok ? `${r.user}: 成功` : `${r.user}: ${r.err}`}
                className={`px-1.5 py-0.5 rounded text-xs font-mono ${
                  r.ok ? 'bg-green-900 text-green-200' : 'bg-red-900 text-red-200'
                }`}
              >
                {r.i}:{r.user}
              </span>
            ))}
          </div>
        </div>
      )}

      {pass !== null && (
        <div
          className={`mt-3 p-2 rounded text-sm font-semibold ${
            pass ? 'bg-green-900/50 text-green-300' : 'bg-red-900/50 text-red-300'
          }`}
        >
          成功 {success} 单，库存 {stockBefore} → {stockAfter} → {pass ? 'PASS ✓ 一人一单 + 未超卖' : 'FAIL ✗'}
        </div>
      )}
    </div>
  );
}

// ---------- App ----------
export default function App() {
  const [view, setView] = useState<'seckill' | 'counter'>('seckill');
  const [auth, setAuth] = useState<LoginResponse | null>(null);
  const [products, setProducts] = useState<ProductSnapshot[]>([]);
  const [productId, setProductId] = useState('');
  const [busy, setBusy] = useState(false);
  const { logs, push, clear: clearLogs } = useLogs();

  const log = useCallback((kind: LogEntry['kind'], text: string) => push(kind, text), [push]);

  const loadProducts = useCallback(
    async (session: LoginResponse) => {
      try {
        const list = await listProducts(session.token);
        setProducts(list);
        setProductId((prev) =>
          list.some((p) => p.productId === prev) ? prev : (list[0]?.productId ?? ''),
        );
        log('ok', `GET /api/seckill → ${list.length} 个商品`);
      } catch (e) {
        log('err', `加载商品列表失败: ${(e as ApiError).message}`);
      }
    },
    [log],
  );

  useEffect(() => {
    if (auth) {
      loadProducts(auth);
    } else {
      setProducts([]);
      setProductId('');
    }
  }, [auth, loadProducts]);

  const doReset = async () => {
    if (!auth) {
      log('err', '请先登录');
      return;
    }
    setBusy(true);
    try {
      const r = await resetAll(auth.token);
      log(
        'ok',
        `POST /api/seckill/reset → 重置 ${r.products} 个商品，清空 ${r.orders} 条订单 / ${r.operations} 条幂等记录`,
      );
      await loadProducts(auth);
    } catch (e) {
      log('err', `重置失败: ${(e as ApiError).message}`);
    } finally {
      setBusy(false);
    }
  };

  const updateSnapshot = (s: ProductSnapshot) =>
    setProducts((prev) => prev.map((p) => (p.productId === s.productId ? s : p)));

  const selected = products.find((p) => p.productId === productId);

  return (
    <div className="max-w-6xl mx-auto p-6">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-cyan-300">Fencing Demo — React 测试面板</h1>
        <div className="flex gap-2 mt-3">
          <button
            onClick={() => setView('seckill')}
            className={`px-3 py-1 rounded text-sm ${
              view === 'seckill' ? 'bg-cyan-600 text-white' : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
            }`}
          >
            秒杀面板
          </button>
          <button
            onClick={() => setView('counter')}
            className={`px-3 py-1 rounded text-sm ${
              view === 'counter' ? 'bg-cyan-600 text-white' : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
            }`}
          >
            计数器 Demo
          </button>
        </div>
        {view === 'seckill' ? (
          <>
            <p className="text-sm text-slate-400 mt-2">
              Backend: <code className="font-mono">POST /api/auth/login</code> ·{' '}
              <code className="font-mono">GET /api/seckill</code> ·{' '}
              <code className="font-mono">GET /api/seckill/{'{productId}'}</code> ·{' '}
              <code className="font-mono">POST /api/seckill/{'{productId}'}/buy</code> ·{' '}
              <code className="font-mono">POST /api/seckill/reset</code>
            </p>
            <p className="text-xs text-slate-500 mt-1">
              Vite dev proxy: <code className="font-mono">/api</code> →{' '}
              <code className="font-mono">http://localhost:8080</code>。请先启动后端；除登录和
              <code className="font-mono">/api/counters</code> 外的接口都需要 Bearer JWT。商品数据来自数据库
              （Flyway 迁移 V2 / V3 种子数据）。
            </p>
          </>
        ) : (
          <p className="text-sm text-slate-400 mt-2">
            Backend: <code className="font-mono">GET /api/counters/{'{resourceId}'}</code> ·{' '}
            <code className="font-mono">POST /api/counters/{'{resourceId}'}/add</code>（无需 JWT）
          </p>
        )}
      </header>

      {view === 'seckill' ? (
        <div className="grid grid-cols-1 gap-5">
          <LoginSection auth={auth} onLogin={setAuth} onLogout={() => setAuth(null)} log={log} />
          <ProductSection
            products={products}
            productId={productId}
            onSelect={setProductId}
            onReload={() => auth && loadProducts(auth)}
            onReset={doReset}
            auth={auth}
            busy={busy}
          />
          <BuySection productId={productId} auth={auth} onSnapshot={updateSnapshot} log={log} />
          <ConcurrentSection
            productId={productId}
            auth={auth}
            stockHint={selected ? selected.stock : null}
            log={log}
          />
        </div>
      ) : (
        <CounterPanel log={log} />
      )}

      <section className="mt-6 rounded-lg border border-slate-700 p-5">
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-lg font-semibold text-slate-300">请求日志</h2>
          <button
            onClick={clearLogs}
            className="text-xs px-2 py-1 rounded bg-slate-700 hover:bg-slate-600"
          >
            清空
          </button>
        </div>
        <div className="bg-black/40 rounded p-2 max-h-64 overflow-auto text-xs font-mono space-y-0.5">
          {logs.length === 0 && <div className="text-slate-600">(暂无请求)</div>}
          {logs.map((l) => (
            <div key={l.id} className="flex gap-2">
              <span className="text-slate-500 shrink-0">{l.time}</span>
              <span
                className={`shrink-0 ${
                  l.kind === 'ok' ? 'text-green-400' : l.kind === 'err' ? 'text-red-400' : 'text-slate-400'
                }`}
              >
                {l.kind.toUpperCase()}
              </span>
              <span className="text-slate-300 break-all">{l.text}</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
