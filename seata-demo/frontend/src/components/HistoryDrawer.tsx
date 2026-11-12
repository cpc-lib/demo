import { DeleteOutlined } from '@ant-design/icons';
import { Button, Collapse, Descriptions, Drawer, Empty, Popconfirm, Tag } from 'antd';
import type { HistoryRecord } from '../types';

interface HistoryDrawerProps {
  open: boolean;
  records: HistoryRecord[];
  onClose: () => void;
  onClear: () => void;
}

function formatTime(timestamp: number): string {
  return new Date(timestamp).toLocaleString('zh-CN', { hour12: false });
}

export function HistoryDrawer({ open, records, onClose, onClear }: HistoryDrawerProps) {
  return (
    <Drawer
      open={open}
      onClose={onClose}
      width="min(560px, 100vw)"
      className="history-drawer"
      title={
        <div className="history-header">
          <span className="section-kicker">LOCAL HISTORY</span>
          <h2>调用记录 <span>{records.length} / 50</span></h2>
        </div>
      }
      extra={
        <Popconfirm
          title="清空全部本地记录？"
          description="此操作不会影响服务端业务数据。"
          okText="清空"
          cancelText="取消"
          okButtonProps={{ danger: true }}
          onConfirm={onClear}
          disabled={records.length === 0}
        >
          <Button danger icon={<DeleteOutlined />} disabled={records.length === 0}>清空记录</Button>
        </Popconfirm>
      }
    >
      <p className="history-caption">仅保存在当前浏览器，不是服务端实时事务记录。</p>
      <div className="history-body">
        {records.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无调用记录" />
        ) : (
          <Collapse
            ghost
            accordion
            items={records.map((record) => ({
              key: record.id,
              label: (
                <span className="history-item-label">
                  <Tag color={record.success ? 'success' : 'error'}>{record.mode}</Tag>
                  <span>
                    <strong>{record.request.orderNo}</strong>
                    <small>{formatTime(record.createdAt)}</small>
                  </span>
                </span>
              ),
              children: (
                <div className="history-item-content">
                  <Descriptions
                    column={1}
                    size="small"
                    items={[
                      { key: 'user', label: '用户 ID', children: record.request.userId },
                      { key: 'commodity', label: '商品编码', children: record.request.commodityCode },
                      { key: 'count', label: '数量', children: record.request.count },
                      { key: 'money', label: '金额', children: `¥ ${record.request.money.toFixed(2)}` },
                      { key: 'mock', label: '异常注入', children: record.request.mockException ? '开启' : '关闭' },
                      { key: 'duration', label: '请求耗时', children: `${record.durationMs} ms` },
                    ]}
                  />
                  {record.error && <p className="history-error">{record.error}</p>}
                  <span className="json-label">{record.response ? '接口响应' : '无接口响应'}</span>
                  {record.response && <pre>{JSON.stringify(record.response, null, 2)}</pre>}
                </div>
              ),
            }))}
          />
        )}
      </div>
    </Drawer>
  );
}
