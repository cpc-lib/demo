import { useEffect, useState } from 'react'
import { Button, DatePicker, Form, message, Modal, Select, Space, Table, Tag } from 'antd'
import dayjs from 'dayjs'

import reconciliationApi from '@/api/reconciliation'

const STATUS_TEXT = {
  PENDING: '待执行',
  PROCESSING: '执行中',
  COMPLETED: '已完成',
  FAILED: '执行失败'
}

const STATUS_TAG_TYPE = {
  PENDING: 'default',
  PROCESSING: 'processing',
  COMPLETED: 'success',
  FAILED: 'error'
}

const DIFF_TEXT = {
  MATCH: '完全匹配',
  MISSING_LOCAL: '漏单',
  MISSING_CHANNEL: '多单',
  AMOUNT_MISMATCH: '金额不符',
  STATUS_MISMATCH: '状态不符'
}

const DIFF_TAG_TYPE = {
  MATCH: 'success',
  MISSING_LOCAL: 'warning',
  MISSING_CHANNEL: 'warning',
  AMOUNT_MISMATCH: 'error',
  STATUS_MISMATCH: 'error'
}

const CHANNEL_TEXT = {
  WXPAY: '微信支付',
  ALIPAY: '支付宝'
}

export default function Reconciliation() {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(false)
  const [executeDialogVisible, setExecuteDialogVisible] = useState(false)
  const [detailDialogVisible, setDetailDialogVisible] = useState(false)
  const [detailList, setDetailList] = useState([])
  const [detailLoading, setDetailLoading] = useState(false)
  const [currentRecord, setCurrentRecord] = useState(null)
  const [filters, setFilters] = useState({
    channelCode: undefined,
    status: undefined,
    billDateStart: undefined,
    billDateEnd: undefined
  })
  const [form] = Form.useForm()

  const fetchList = (extraFilters = {}) => {
    setLoading(true)
    const params = {
      pageNum: 1,
      pageSize: 100,
      ...filters,
      ...extraFilters
    }
    if (params.billDateStart && typeof params.billDateStart !== 'string') {
      params.billDateStart = params.billDateStart.format('YYYY-MM-DD')
    }
    if (params.billDateEnd && typeof params.billDateEnd !== 'string') {
      params.billDateEnd = params.billDateEnd.format('YYYY-MM-DD')
    }
    reconciliationApi.list(params).then((response) => {
      setList(response?.data?.records || [])
    }).finally(() => {
      setLoading(false)
    })
  }

  useEffect(() => {
    fetchList()
  }, [])

  const openExecute = () => {
    form.resetFields()
    form.setFieldsValue({
      billDate: dayjs().subtract(1, 'day'),
      channelCode: 'WXPAY',
      billType: 'ALL'
    })
    setExecuteDialogVisible(true)
  }

  const closeExecute = () => {
    setExecuteDialogVisible(false)
  }

  const doExecute = () => {
    form.validateFields().then((values) => {
      const data = {
        billDate: values.billDate.format('YYYY-MM-DD'),
        channelCode: values.channelCode,
        billType: values.billType,
        paymentAppId: values.paymentAppId
      }
      reconciliationApi.execute(data).then((response) => {
        message.success(response.message || '对账任务已提交')
        closeExecute()
        fetchList()
      })
    })
  }

  const viewDetails = (record) => {
    setCurrentRecord(record)
    setDetailLoading(true)
    reconciliationApi.listDetails(record.id, { pageNum: 1, pageSize: 100 }).then((response) => {
      setDetailList(response?.data?.records || [])
      setDetailDialogVisible(true)
    }).finally(() => {
      setDetailLoading(false)
    })
  }

  const downloadReport = (record) => {
    const url = reconciliationApi.exportUrl(record.id)
    window.open(url, '_blank')
  }

  const columns = [
    {
      title: '#',
      width: 50,
      render: (_, __, index) => index + 1
    },
    {
      title: '对账日期',
      dataIndex: 'billDate',
      width: 110
    },
    {
      title: '支付渠道',
      dataIndex: 'channelCode',
      width: 100,
      render: (code) => CHANNEL_TEXT[code] || code || '-'
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status) => (
        <Tag color={STATUS_TAG_TYPE[status]}>
          {STATUS_TEXT[status] || status || '-'}
        </Tag>
      )
    },
    {
      title: '总笔数',
      dataIndex: 'totalCount',
      width: 80
    },
    {
      title: '匹配数',
      dataIndex: 'matchCount',
      width: 80,
      render: (value) => <span style={{ color: '#52c41a' }}>{value || 0}</span>
    },
    {
      title: '差异数',
      dataIndex: 'diffCount',
      width: 80,
      render: (value) => (value && value > 0 ? <span style={{ color: '#ff4d4f' }}>{value}</span> : value || 0)
    },
    {
      title: '差异金额',
      dataIndex: 'diffAmount',
      width: 110,
      render: (value) => (
        <span style={{ color: value && value !== 0 ? '#ff4d4f' : undefined }}>
          {(value || 0) / 100} 元
        </span>
      )
    },
    {
      title: '渠道总金额',
      dataIndex: 'channelTotalAmount',
      width: 120,
      render: (value) => `${(value || 0) / 100} 元`
    },
    {
      title: '本地总金额',
      dataIndex: 'localTotalAmount',
      width: 120,
      render: (value) => `${(value || 0) / 100} 元`
    },
    {
      title: '执行时间',
      dataIndex: 'endTime',
      width: 170
    },
    {
      title: '操作',
      width: 200,
      align: 'center',
      render: (_, row) => (
        <Space>
          <Button type="link" onClick={() => viewDetails(row)}>查看明细</Button>
          {row.status === 'COMPLETED' && (
            <Button type="link" onClick={() => downloadReport(row)}>下载报告</Button>
          )}
        </Space>
      )
    }
  ]

  const detailColumns = [
    {
      title: '#',
      width: 50,
      render: (_, __, index) => index + 1
    },
    {
      title: '差异类型',
      dataIndex: 'diffType',
      width: 110,
      render: (type) => (
        <Tag color={DIFF_TAG_TYPE[type]}>
          {DIFF_TEXT[type] || type || '-'}
        </Tag>
      )
    },
    {
      title: '本地订单号',
      dataIndex: 'orderNo',
      width: 200,
      render: (value) => value || '-'
    },
    {
      title: '渠道交易号',
      dataIndex: 'channelTradeNo',
      width: 200,
      render: (value) => value || '-'
    },
    {
      title: '本地金额',
      dataIndex: 'localAmount',
      width: 100,
      render: (value) => value != null ? `${value / 100} 元` : '-'
    },
    {
      title: '渠道金额',
      dataIndex: 'channelAmount',
      width: 100,
      render: (value) => value != null ? `${value / 100} 元` : '-'
    },
    {
      title: '本地状态',
      dataIndex: 'localStatus',
      width: 100,
      render: (value) => value || '-'
    },
    {
      title: '渠道状态',
      dataIndex: 'channelStatus',
      width: 100,
      render: (value) => value || '-'
    }
  ]

  return (
    <div className="bg-fa of">
      <section id="index" className="container">
        <header className="comm-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 className="fl tac">
            <span className="c-333">对账管理</span>
          </h2>
          <Space>
            <Select
              placeholder="渠道"
              style={{ width: 120 }}
              allowClear
              value={filters.channelCode}
              onChange={(value) => {
                setFilters({ ...filters, channelCode: value })
                fetchList({ channelCode: value })
              }}
            >
              <Select.Option value="WXPAY">微信支付</Select.Option>
              <Select.Option value="ALIPAY">支付宝</Select.Option>
            </Select>
            <Select
              placeholder="状态"
              style={{ width: 120 }}
              allowClear
              value={filters.status}
              onChange={(value) => {
                setFilters({ ...filters, status: value })
                fetchList({ status: value })
              }}
            >
              <Select.Option value="PENDING">待执行</Select.Option>
              <Select.Option value="PROCESSING">执行中</Select.Option>
              <Select.Option value="COMPLETED">已完成</Select.Option>
              <Select.Option value="FAILED">执行失败</Select.Option>
            </Select>
            <DatePicker
              placeholder="开始日期"
              value={filters.billDateStart}
              onChange={(date) => {
                setFilters({ ...filters, billDateStart: date })
              }}
            />
            <DatePicker
              placeholder="结束日期"
              value={filters.billDateEnd}
              onChange={(date) => {
                setFilters({ ...filters, billDateEnd: date })
              }}
            />
            <Button type="primary" onClick={() => fetchList()}>查询</Button>
            <Button type="primary" onClick={openExecute}>手动对账</Button>
          </Space>
        </header>

        <Table
          rowKey={(row) => row.id}
          dataSource={list}
          columns={columns}
          bordered
          loading={loading}
          pagination={false}
          scroll={{ x: 1300 }}
        />
      </section>

      <Modal
        open={executeDialogVisible}
        title="手动执行对账"
        width={460}
        centered
        onCancel={closeExecute}
        footer={[
          <Button key="cancel" onClick={closeExecute}>取 消</Button>,
          <Button key="submit" type="primary" onClick={doExecute}>执行对账</Button>
        ]}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="对账日期"
            name="billDate"
            rules={[{ required: true, message: '请选择对账日期' }]}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            label="支付渠道"
            name="channelCode"
            rules={[{ required: true, message: '请选择支付渠道' }]}
          >
            <Select>
              <Select.Option value="WXPAY">微信支付</Select.Option>
              <Select.Option value="ALIPAY">支付宝</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item
            label="账单类型"
            name="billType"
            rules={[{ required: true, message: '请选择账单类型' }]}
          >
            <Select>
              <Select.Option value="ALL">全部账单</Select.Option>
              <Select.Option value="SUCCESS">成功订单</Select.Option>
              <Select.Option value="REFUND">退款账单</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`对账明细 - ${currentRecord?.billDate || ''} ${CHANNEL_TEXT[currentRecord?.channelCode] || ''}`}
        open={detailDialogVisible}
        width={1100}
        centered
        footer={null}
        onCancel={() => setDetailDialogVisible(false)}
      >
        <Table
          rowKey={(row) => row.id}
          dataSource={detailList}
          columns={detailColumns}
          bordered
          loading={detailLoading}
          pagination={false}
          scroll={{ x: 1100 }}
        />
      </Modal>
    </div>
  )
}
