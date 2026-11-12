import { HistoryOutlined } from '@ant-design/icons';
import { Button, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { useRef, useState } from 'react';
import { createOrder, OrderApiError } from './api';
import { HistoryDrawer } from './components/HistoryDrawer';
import { ModeSelector } from './components/ModeSelector';
import { OrderForm } from './components/OrderForm';
import { ResultPanel } from './components/ResultPanel';
import { clearHistory, loadHistory, saveHistory } from './history';
import type { HistoryRecord, OrderCreateRequest, TransactionMode } from './types';

export default function App() {
  const [mode, setMode] = useState<TransactionMode>('AT');
  const [submitting, setSubmitting] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<HistoryRecord | null>(null);
  const [history, setHistory] = useState<HistoryRecord[]>(loadHistory);
  const [historyOpen, setHistoryOpen] = useState(false);
  const submittingRef = useRef(false);

  const handleModeChange = (nextMode: TransactionMode) => {
    if (submittingRef.current) return;
    setMode(nextMode);
    setCurrentRecord(null);
  };

  const handleSubmit = async (request: OrderCreateRequest) => {
    if (submittingRef.current) return;
    submittingRef.current = true;
    setSubmitting(true);
    setCurrentRecord(null);
    const startedAt = performance.now();

    let record: HistoryRecord;
    try {
      const response = await createOrder(mode, request);
      record = {
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        mode,
        request,
        response,
        error: null,
        success: true,
        createdAt: Date.now(),
        durationMs: Math.round(performance.now() - startedAt),
      };
    } catch (error) {
      const apiError = error instanceof OrderApiError ? error : null;
      record = {
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        mode,
        request,
        response: apiError?.response ?? null,
        error: error instanceof Error ? error.message : '请求失败，请稍后重试。',
        success: false,
        createdAt: Date.now(),
        durationMs: Math.round(performance.now() - startedAt),
      };
    }

    setCurrentRecord(record);
    setHistory(saveHistory(record));
    submittingRef.current = false;
    setSubmitting(false);
  };

  const handleClearHistory = () => {
    clearHistory();
    setHistory([]);
  };

  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#3157d5',
          colorInfo: '#3157d5',
          colorSuccess: '#17855b',
          colorError: '#cf3f4f',
          colorText: '#172033',
          colorTextSecondary: '#5f6b7c',
          colorBorder: '#dfe5ee',
          colorBgLayout: '#f4f6fa',
          borderRadius: 8,
          fontSize: 14,
          fontFamily: "Inter, 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif",
        },
        components: {
          Button: { controlHeight: 40, fontWeight: 500 },
          Input: { controlHeight: 42 },
          InputNumber: { controlHeight: 42 },
        },
      }}
    >
      <div className="app-shell">
        <header className="topbar">
          <div className="topbar-inner">
            <div className="brand">
              <span className="brand-mark">S</span>
              <span>
                <strong>SEATA</strong>
                <small>TRANSACTION LAB</small>
              </span>
            </div>
            <div className="topbar-actions">
              <span className="service-target"><i />目标服务 · localhost:8081</span>
              <Button icon={<HistoryOutlined />} onClick={() => setHistoryOpen(true)}>
                调用记录
                <span className="history-count">{history.length}</span>
              </Button>
            </div>
          </div>
        </header>

        <main className="main-container">
          <section className="hero">
            <div>
              <span className="hero-eyebrow">DISTRIBUTED TRANSACTION CONSOLE</span>
              <h1>分布式事务，清晰可见。</h1>
              <p>通过同一笔订单，对比 AT、TCC、SAGA、XA 的提交与回滚行为。</p>
            </div>
            <div className="hero-index" aria-hidden="true">
              <strong>04</strong>
              <span>TRANSACTION<br />MODES</span>
            </div>
          </section>

          <ModeSelector value={mode} disabled={submitting} onChange={handleModeChange} />

          <div className="workspace-grid">
            <OrderForm mode={mode} loading={submitting} onSubmit={handleSubmit} />
            <ResultPanel loading={submitting} record={currentRecord} />
          </div>
        </main>

        <footer className="footer">
          <span>SEATA TRANSACTION LAB</span>
          <span>历史记录仅存储于本地浏览器 · 最多保留 50 条</span>
        </footer>

        <HistoryDrawer
          open={historyOpen}
          records={history}
          onClose={() => setHistoryOpen(false)}
          onClear={handleClearHistory}
        />
      </div>
    </ConfigProvider>
  );
}
