import {
  CheckCircleFilled,
  CloseCircleFilled,
  ExperimentOutlined,
  LoadingOutlined,
} from '@ant-design/icons';
import { Alert, Descriptions, Skeleton, Tag, Typography } from 'antd';
import type { HistoryRecord } from '../types';
import { MODE_DEFINITIONS } from './ModeSelector';

interface ResultPanelProps {
  loading: boolean;
  record: HistoryRecord | null;
}

function formatTime(timestamp: number | string | null): string {
  if (!timestamp) return '—';
  return new Date(timestamp).toLocaleString('zh-CN', { hour12: false });
}

export function ResultPanel({ loading, record }: ResultPanelProps) {
  const order = record?.response?.data;
  const response = record?.response;

  return (
    <section className="console-card result-card" aria-labelledby="result-heading">
      <div className="card-heading">
        <div>
          <span className="section-kicker">03 / 观察结果</span>
          <h2 id="result-heading">事务回执</h2>
        </div>
        {record && (
          <Tag color={record.success ? 'success' : 'error'}>
            {record.success ? 'SUCCESS' : 'FAILED'}
          </Tag>
        )}
      </div>

      {loading ? (
        <div className="result-loading" role="status">
          <span className="loading-icon"><LoadingOutlined spin /></span>
          <h3>正在执行全局事务</h3>
          <p>订单、库存、账户服务正在协同处理，请等待最终回执。</p>
          <Skeleton active paragraph={{ rows: 4 }} title={false} />
        </div>
      ) : !record ? (
        <div className="result-empty">
          <span className="empty-icon"><ExperimentOutlined /></span>
          <h3>等待发起事务</h3>
          <p>选择事务模式并提交订单，这里将展示真实接口回执与订单状态。</p>
          <div className="empty-guide">
            <span>正常下单</span>
            <i />
            <span>开启异常注入</span>
            <i />
            <span>对比结果</span>
          </div>
        </div>
      ) : (
        <div className="result-content">
          <Alert
            type={record.success ? 'success' : 'error'}
            showIcon
            icon={record.success ? <CheckCircleFilled /> : <CloseCircleFilled />}
            message={record.success ? '全局事务提交成功' : response ? '业务执行失败' : '请求未完成'}
            description={record.success ? response?.message || '订单创建成功。' : record.error}
          />

          {order ? (
            <Descriptions
              className="order-details"
              column={1}
              size="small"
              items={[
                {
                  key: 'orderNo',
                  label: '订单号',
                  children: <Typography.Text copyable>{order.orderNo}</Typography.Text>,
                },
                { key: 'mode', label: '事务模式', children: <Tag>{order.mode}</Tag> },
                {
                  key: 'status',
                  label: '订单状态',
                  children: <Tag color={order.status === 'SUCCESS' ? 'success' : 'warning'}>{order.status}</Tag>,
                },
                { key: 'user', label: '用户 / 商品', children: `${order.userId} / ${order.commodityCode}` },
                { key: 'count', label: '购买数量', children: order.count },
                {
                  key: 'money',
                  label: '订单金额',
                  children: <span className="money">¥ {Number(order.money).toFixed(2)}</span>,
                },
                { key: 'time', label: '创建时间', children: formatTime(order.createTime) },
              ]}
            />
          ) : (
            <div className="no-order-data">
              <span>接口未返回订单详情</span>
              <p>订单号：{record.request.orderNo}</p>
            </div>
          )}

          {record.request.mockException && !record.success && (
            <p className="result-note">{MODE_DEFINITIONS[record.mode].exceptionNote}</p>
          )}

          <div className="response-meta">
            <span>耗时 {record.durationMs} ms</span>
            <span>{formatTime(record.createdAt)}</span>
          </div>

          {response && (
            <details className="response-details">
              <summary>查看原始响应 · code {response.code}</summary>
              <pre>{JSON.stringify(response, null, 2)}</pre>
            </details>
          )}
        </div>
      )}
    </section>
  );
}
