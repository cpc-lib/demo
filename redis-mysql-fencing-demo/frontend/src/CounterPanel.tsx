import { useState } from 'react';
import {
  addCounter,
  generateKey,
  getCounter,
  type ApiError,
  type CounterSnapshot,
  type MutationResponse,
} from './api';

type Log = (kind: 'info' | 'ok' | 'err', text: string) => void;

/**
 * 最简单的 CounterController 测试页（无需登录）：
 *   GET  /api/counters/{resourceId}
 *   POST /api/counters/{resourceId}/add  (Header: Idempotency-Key)
 */
export default function CounterPanel({ log }: { log: Log }) {
  const [resourceId, setResourceId] = useState('counter_demo');
  const [delta, setDelta] = useState(1);
  const [snapshot, setSnapshot] = useState<CounterSnapshot | null>(null);
  const [lastKey, setLastKey] = useState(generateKey('basic'));
  const [lastResp, setLastResp] = useState<MutationResponse | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const refresh = async () => {
    setBusy(true);
    setErr(null);
    try {
      const s = await getCounter(resourceId);
      setSnapshot(s ?? null);
      log('ok', `GET /api/counters/${resourceId} → ${s ? `value=${s.value}, fence=${s.fenceToken}` : '404（计数器尚不存在）'}`);
    } catch (e) {
      const apiErr = e as ApiError;
      setErr(apiErr.message);
      log('err', `GET 失败: ${apiErr.message}`);
    } finally {
      setBusy(false);
    }
  };

  const doAdd = async (reuseKey: boolean) => {
    setBusy(true);
    setErr(null);
    try {
      const key = reuseKey ? lastKey : generateKey('basic');
      const r = await addCounter(resourceId, key, delta);
      setLastKey(key);
      setLastResp(r);
      log(
        'ok',
        `POST /api/counters/${resourceId}/add key=${key} delta=${delta} → value=${r.value}, fence=${r.fenceToken}, replayed=${r.replayed}`,
      );
      const s = await getCounter(resourceId);
      setSnapshot(s ?? null);
    } catch (e) {
      const apiErr = e as ApiError;
      setErr(apiErr.message);
      log('err', `POST 失败: ${apiErr.message}${apiErr.errorCode ? ` [${apiErr.errorCode}]` : ''}`);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="rounded-lg border border-slate-700 p-5">
      <h2 className="text-lg font-semibold mb-3 text-cyan-300">计数器 Demo（Fencing Token 最小闭环）</h2>
      <p className="text-xs text-slate-400 mb-3">
        <code className="font-mono">GET /api/counters/{'{resourceId}'}</code> ·{' '}
        <code className="font-mono">POST /api/counters/{'{resourceId}'}/add</code>
        （无需登录）。同一 Idempotency-Key 重复提交应返回 <code className="font-mono">replayed=true</code>。
      </p>

      <div className="flex gap-2 items-center flex-wrap">
        <label className="text-sm text-slate-400">resourceId:</label>
        <input
          value={resourceId}
          onChange={(e) => setResourceId(e.target.value.trim() || 'counter_demo')}
          className="w-48 bg-slate-800 border border-slate-600 rounded px-2 py-1 font-mono"
        />
        <label className="text-sm text-slate-400">delta:</label>
        <input
          type="number"
          min={1}
          value={delta}
          onChange={(e) => setDelta(Math.max(1, Number(e.target.value)))}
          className="w-20 bg-slate-800 border border-slate-600 rounded px-2 py-1"
        />
        <button
          onClick={refresh}
          disabled={busy}
          className="px-3 py-1 rounded bg-slate-700 hover:bg-slate-600 disabled:opacity-50"
        >
          GET 查询
        </button>
        <button
          onClick={() => doAdd(false)}
          disabled={busy}
          className="px-3 py-1 rounded bg-cyan-600 hover:bg-cyan-500 disabled:opacity-50"
        >
          POST add（新 key）
        </button>
        <button
          onClick={() => doAdd(true)}
          disabled={busy}
          className="px-3 py-1 rounded bg-slate-700 hover:bg-slate-600 disabled:opacity-50"
          title={`重复上一把 key: ${lastKey}`}
        >
          POST add（复用 key）
        </button>
      </div>

      <div className="mt-2 text-xs text-slate-500 font-mono break-all">last key = {lastKey}</div>

      <div className="mt-3 grid grid-cols-1 md:grid-cols-2 gap-2 text-sm">
        <div className="bg-slate-800 rounded p-2">
          <div className="text-slate-400">当前快照</div>
          {snapshot ? (
            <>
              <div>value: <span className="font-mono text-green-300">{snapshot.value}</span></div>
              <div>fence: <span className="font-mono text-yellow-300">{snapshot.fenceToken}</span></div>
            </>
          ) : (
            <div className="text-slate-500">—（404，点击 GET 或 POST 创建）</div>
          )}
        </div>
        <div className="bg-slate-800 rounded p-2">
          <div className="text-slate-400">最后一次响应</div>
          {lastResp ? (
            <>
              <div>value: <span className="font-mono">{lastResp.value}</span></div>
              <div>fence: <span className="font-mono">{lastResp.fenceToken}</span></div>
              <div>
                replayed: <span className={lastResp.replayed ? 'text-amber-300' : 'text-green-300'}>{String(lastResp.replayed)}</span>
              </div>
            </>
          ) : (
            <div className="text-slate-500">—</div>
          )}
        </div>
      </div>

      {err && <div className="mt-3 p-2 rounded bg-red-900/50 text-red-300 text-sm">{err}</div>}
    </div>
  );
}
