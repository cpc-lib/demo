import {
  ApartmentOutlined,
  BranchesOutlined,
  DatabaseOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import { Radio, Tag } from 'antd';
import type { ReactNode } from 'react';
import type { TransactionMode } from '../types';

interface ModeDefinition {
  name: TransactionMode;
  title: string;
  tagline: string;
  summary: string;
  description: string;
  steps: string[];
  exceptionNote: string;
  icon: ReactNode;
}

export const MODE_DEFINITIONS: Record<TransactionMode, ModeDefinition> = {
  AT: {
    name: 'AT',
    title: '自动事务',
    tagline: '低侵入 · 自动补偿',
    summary: '适合常规 CRUD 业务',
    description: 'Seata 代理数据源并记录 undo_log，业务代码无需实现补偿接口。',
    steps: ['创建订单', '扣减库存', '扣减余额', '统一提交或回滚'],
    exceptionNote: '异常时依据 undo_log 自动回滚，不保留已创建订单。',
    icon: <DatabaseOutlined />,
  },
  TCC: {
    name: 'TCC',
    title: '业务补偿',
    tagline: '资源预留 · 两阶段',
    summary: '适合精细控制资源',
    description: '将业务拆分为 Try、Confirm、Cancel，由业务代码明确管理资源。',
    steps: ['Try 预留资源', '执行各分支', 'Confirm 确认', '异常执行 Cancel'],
    exceptionNote: '异常时调用各分支 Cancel 方法，释放预留资源并回滚订单。',
    icon: <BranchesOutlined />,
  },
  SAGA: {
    name: 'SAGA',
    title: '长事务编排',
    tagline: '状态机 · 逆向补偿',
    summary: '适合长流程业务',
    description: '各本地事务依次提交，出现异常时按相反顺序执行补偿动作。',
    steps: ['创建订单', '扣减库存', '扣减余额', '异常逆向补偿'],
    exceptionNote: '异常后订单保留为 CANCELLED，库存与余额通过补偿动作恢复。',
    icon: <ApartmentOutlined />,
  },
  XA: {
    name: 'XA',
    title: '强一致事务',
    tagline: '标准协议 · 两阶段',
    summary: '适合强一致性场景',
    description: '基于数据库 XA 协议，在准备阶段完成后由事务管理器统一决策。',
    steps: ['执行分支 SQL', 'XA Prepare', '全局状态校验', '统一 Commit 或 Rollback'],
    exceptionNote: '异常时所有 XA 分支统一回滚，不保留已创建订单。',
    icon: <SafetyCertificateOutlined />,
  },
};

interface ModeSelectorProps {
  value: TransactionMode;
  disabled?: boolean;
  onChange: (mode: TransactionMode) => void;
}

export function ModeSelector({ value, disabled, onChange }: ModeSelectorProps) {
  const selected = MODE_DEFINITIONS[value];

  return (
    <section className="mode-section" aria-labelledby="mode-heading">
      <div className="section-heading">
        <div>
          <span className="section-kicker">01 / 选择协议</span>
          <h2 id="mode-heading">事务模式</h2>
        </div>
        <span className="section-caption">四种模式，同一套下单业务</span>
      </div>

      <Radio.Group
        className="mode-grid"
        value={value}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value as TransactionMode)}
        aria-label="事务模式"
      >
        {(Object.keys(MODE_DEFINITIONS) as TransactionMode[]).map((mode) => {
          const definition = MODE_DEFINITIONS[mode];
          return (
            <Radio key={mode} value={mode} className="mode-card">
              <span className="mode-card-topline">
                <span className="mode-icon">{definition.icon}</span>
                <span className="mode-card-title">
                  <strong>{definition.name}</strong>
                  <small>{definition.title}</small>
                </span>
              </span>
              <span className="mode-tagline">{definition.tagline}</span>
              <span className="mode-summary">{definition.summary}</span>
            </Radio>
          );
        })}
      </Radio.Group>

      <div className="mode-detail" aria-live="polite">
        <div className="mode-description">
          <Tag color="blue">{selected.name} 工作方式</Tag>
          <p>{selected.description}</p>
        </div>
        <ol className="flow-steps" aria-label={`${selected.name} 执行流程`}>
          {selected.steps.map((step, index) => (
            <li key={step}>
              <span>{String(index + 1).padStart(2, '0')}</span>
              {step}
            </li>
          ))}
        </ol>
        <p className="rollback-note">{selected.exceptionNote}</p>
      </div>
    </section>
  );
}
