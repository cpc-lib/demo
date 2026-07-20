import { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  message,
  Modal,
  Progress,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag
} from 'antd'
import dayjs from 'dayjs'
import {
  createBatch,
  executeBatch,
  getBatchDetail,
  getBatchList,
  getDetailList,
  getDiscrepancyList,
  getProgress,
  getSummary,
  resolveDiscrepancy
} from '@/api/reconciliation'

const { RangePicker } = DatePicker
const { TextArea } = Input

const CHANNEL_OPTIONS = [
  { label: '全部', value: '' },
  { label: '微信支付', value: 'WXPAY' },
  { label: '支付宝', value: 'ALIPAY' }
]

const STATUS_OPTIONS = [
  { label: '全部', value: '' },
  { label: '待执行', value: 'PENDING' },
  { label: '执行中', value: 'EXECUTING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '失败', value: 'FAILED' }
]

const statusColorMap = {
  PENDING: 'default',
  EXECUTING: 'processing',
  COMPLETED: 'success',
  FAILED: 'error'
}

const statusTextMap = {
  PENDING: '待执行',
  EXECUTING: '执行中',
  COMPLETED: '已完成',
  FAILED: '失败'
}

const channelTextMap = {
  WXPAY: '微信支付',
  ALIPAY: '支付宝'
}

const matchStatusMap = {
  MATCHED: { text: '匹配', color: 'success' },
  MISMATCH: { text: '不匹配', color: 'error' },
  MISSING_LOCAL: { text: '本地缺失', color: 'warning' },
  MISSING_CHANNEL: { text: '渠道缺失', color: 'orange' }
}

const discrepancyTypeMap = {
  AMOUNT_MISMATCH: { text: '金额不一致', color: 'red' },
  STATUS_MISMATCH: { text: '状态不一致', color: 'orange' },
  MISSING_LOCAL: { text: '本地缺失', color: 'gold' },
  MISSING_CHANNEL: { text: '渠道缺失', color: 'purple' }
}

const discrepancyStatusMap = {
  PENDING: { text: '待处理', color: 'processing' },
  RESOLVED: { text: '已处理', color: 'success' },
  IGNORED: { text: '已忽略', color: 'default' }
}

export default function Reconciliation() {
  const [queryForm] = Form.useForm()
  const [createForm] = Form.useForm()
  const [resolveForm] = Form.useForm()

  const [summary, setSummary] = useState({})
  const [batchList, setBatchList] = useState([])
  const [batchTotal, setBatchTotal] = useState(0)
  const [batchPageNum, setBatchPageNum] = useState(1)
  const [batchPageSize, setBatchPageSize] = useState(10)
  const [loading, setLoading] = useState(false)

  const [createDialogVisible, setCreateDialogVisible] = useState(false)
  const [detailDialogVisible, setDetailDialogVisible] = useState(false)
  const [discrepancyDialogVisible, setDiscrepancyDialogVisible] = useState(false)
  const [progressDialogVisible, setProgressDialogVisible] = useState(false)
  const [resolveDialogVisible, setResolveDialogVisible] = useState(false)

  const [currentBatch, setCurrentBatch] = useState(null)
  const [currentDiscrepancy, setCurrentDiscrepancy] = useState(null)

  const [detailList, setDetailList] = useState([])
  const [detailTotal, setDetailTotal] = useState(0)
  const [detailPageNum, setDetailPageNum] = useState(1)
  const [detailPageSize, setDetailPageSize] = useState(10)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailFilters, setDetailFilters] = useState({ matchStatus: '', tradeType: '' })

  const [discrepancyList, setDiscrepancyList] = useState([])
  const [discrepancyTotal, setDiscrepancyTotal] = useState(0)
  const [discrepancyPageNum, setDiscrepancyPageNum] = useState(1)
  const [discrepancyPageSize, setDiscrepancyPageSize] = useState(10)
  const [discrepancyLoading, setDiscrepancyLoading] = useState(false)
  const [discrepancyFilters, setDiscrepancyFilters] = useState({ status: '', discrepancyType: '' })

  const [progressInfo, setProgressInfo] = useState(null)
  const [progressTimer, setProgressTimer] = useState(null)

  const loadSummary = useCallback(() => {
    getSummary().then((res) => {
      setSummary(res?.data || {})
    })
  }, [])

  const loadBatchList = useCallback(() => {
    setLoading(true)
    const values = queryForm.getFieldsValue()
    const params = {
      pageNum: batchPageNum,
      pageSize: batchPageSize
    }
    if (values.dateRange && values.dateRange.length === 2) {
      params.startDate = values.dateRange[0].format('YYYY-MM-DD')
      params.endDate = values.dateRange[1].format('YYYY-MM-DD')
    }
    if (values.channelCode) {
      params.channelCode = values.channelCode
    }
    if (values.status) {
      params.status = values.status
    }
    getBatchList(params)
      .then((res) => {
        setBatchList(res?.data?.list || [])
        setBatchTotal(res?.data?.total || 0)
      })
      .finally(() => {
        setLoading(false)
      })
  }, [batchPageNum, batchPageSize, queryForm])

  useEffect(() => {
    loadSummary()
    loadBatchList()
  }, [loadSummary, loadBatchList])

  const handleQuery = () => {
    setBatchPageNum(1)
    setTimeout(() => loadBatchList(), 0)
  }

  const handleReset = () => {
    queryForm.resetFields()
    setBatchPageNum(1)
    setTimeout(() => loadBatchList(), 0)
  }

  const openCreateDialog = () => {
    createForm.resetFields()
    setCreateDialogVisible(true)
  }

  const handleCreateBatch = () => {
    createForm.validateFields().then((values) => {
      const data = {
        channelCode: values.channelCode,
        billDate: values.billDate.format('YYYY-MM-DD')
      }
      createBatch(data).then((res) => {
        message.success(res.message || '批次创建成功')
        setCreateDialogVisible(false)
        loadSummary()
        loadBatchList()
      })
    })
  }

  const handleExecute = (row) => {
    Modal.confirm({
      title: '确认执行对账',
      content: `确定要执行批次 ${row.batchNo} 的对账任务吗？`,
      onOk: () => {
        executeBatch(row.batchNo).then((res) => {
          message.success(res.message || '对账任务已启动')
          loadBatchList()
        })
      }
    })
  }

  const openDetailDialog = (row) => {
    setCurrentBatch(row)
    setDetailPageNum(1)
    setDetailFilters({ matchStatus: '', tradeType: '' })
    setDetailDialogVisible(true)
  }

  const loadDetailList = useCallback(() => {
    if (!currentBatch) return
    setDetailLoading(true)
    const params = {
      pageNum: detailPageNum,
      pageSize: detailPageSize,
      ...detailFilters
    }
    getDetailList(currentBatch.batchNo, params)
      .then((res) => {
        setDetailList(res?.data?.list || [])
        setDetailTotal(res?.data?.total || 0)
      })
      .finally(() => {
        setDetailLoading(false)
      })
  }, [currentBatch, detailPageNum, detailPageSize, detailFilters])

  useEffect(() => {
    if (detailDialogVisible && currentBatch) {
      loadDetailList()
    }
  }, [detailDialogVisible, currentBatch, loadDetailList])

  const openDiscrepancyDialog = (row) => {
    setCurrentBatch(row)
    setDiscrepancyPageNum(1)
    setDiscrepancyFilters({ status: '', discrepancyType: '' })
    setDiscrepancyDialogVisible(true)
  }

  const loadDiscrepancyList = useCallback(() => {
    if (!currentBatch) return
    setDiscrepancyLoading(true)
    const params = {
      pageNum: discrepancyPageNum,
      pageSize: discrepancyPageSize,
      ...discrepancyFilters
    }
    getDiscrepancyList(currentBatch.batchNo, params)
      .then((res) => {
        setDiscrepancyList(res?.data?.list || [])
        setDiscrepancyTotal(res?.data?.total || 0)
      })
      .finally(() => {
        setDiscrepancyLoading(false)
      })
  }, [currentBatch, discrepancyPageNum, discrepancyPageSize, discrepancyFilters])

  useEffect(() => {
    if (discrepancyDialogVisible && currentBatch) {
      loadDiscrepancyList()
    }
  }, [discrepancyDialogVisible, currentBatch, loadDiscrepancyList])

  const openResolveDialog = (row) => {
    setCurrentDiscrepancy(row)
    resolveForm.resetFields()
    setResolveDialogVisible(true)
  }

  const handleResolve = () => {
    resolveForm.validateFields().then((values) => {
      resolveDiscrepancy(currentDiscrepancy.id, { resolveRemark: values.resolveRemark }).then((res) => {
        message.success(res.message || '处理成功')
        setResolveDialogVisible(false)
        loadDiscrepancyList()
        loadSummary()
        loadBatchList()
      })
    })
  }

  const openProgressDialog = (row) => {
    setCurrentBatch(row)
    setProgressInfo(null)
    setProgressDialogVisible(true)
  }

  const loadProgress = useCallback(() => {
    if (!currentBatch) return
    getProgress(currentBatch.batchNo).then((res) => {
      setProgressInfo(res?.data || null)
      if (res?.data?.status === 'COMPLETED' || res?.data?.status === 'FAILED') {
        if (progressTimer) {
          clearInterval(progressTimer)
          setProgressTimer(null)
        }
      }
    })
  }, [currentBatch, progressTimer])

  useEffect(() => {
    if (progressDialogVisible && currentBatch) {
      loadProgress()
      const timer = setInterval(() => {
        loadProgress()
      }, 2000)
      setProgressTimer(timer)
      return () => {
        if (timer) clearInterval(timer)
      }
    } else {
      if (progressTimer) {
        clearInterval(progressTimer)
        setProgressTimer(null)
      }
    }
  }, [progressDialogVisible, currentBatch, loadProgress])

  const batchColumns = [
    { title: '#', width: 50, render: (_, __, index) => (batchPageNum - 1) * batchPageSize + index + 1 },
    { title: '批次号', dataIndex: 'batchNo', width: 200 },
    {
      title: '渠道',
      dataIndex: 'channelCode',
      width: 100,
      render: (code) => channelTextMap[code] || code
    },
    { title: '账单日期', dataIndex: 'billDate', width: 110 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status) => (
        <Tag color={statusColorMap[status] || 'default'}>{statusTextMap[status] || status}</Tag>
      )
    },
    { title: '渠道笔数', dataIndex: 'channelCount', width: 100 },
    { title: '本地笔数', dataIndex: 'localCount', width: 100 },
    { title: '匹配笔数', dataIndex: 'matchedCount', width: 100 },
    { title: '差异笔数', dataIndex: 'discrepancyCount', width: 100 },
    {
      title: '操作',
      width: 280,
      align: 'center',
      fixed: 'right',
      render: (_, row) => (
        <Space size="small" wrap>
          {row.status !== 'COMPLETED' && (
            <Button type="link" onClick={() => handleExecute(row)}>
              执行
            </Button>
          )}
          <Button type="link" onClick={() => openDetailDialog(row)}>
            查看明细
          </Button>
          <Button type="link" onClick={() => openDiscrepancyDialog(row)}>
            查看差异
          </Button>
          <Button type="link" onClick={() => openProgressDialog(row)}>
            进度
          </Button>
        </Space>
      )
    }
  ]

  const detailColumns = [
    { title: '#', width: 50, render: (_, __, index) => (detailPageNum - 1) * detailPageSize + index + 1 },
    { title: '订单号', dataIndex: 'orderNo', width: 200 },
    { title: '交易类型', dataIndex: 'tradeType', width: 100 },
    {
      title: '匹配状态',
      dataIndex: 'matchStatus',
      width: 110,
      render: (status) => (
        <Tag color={matchStatusMap[status]?.color || 'default'}>{matchStatusMap[status]?.text || status}</Tag>
      )
    },
    { title: '渠道金额', dataIndex: 'channelAmount', width: 110 },
    { title: '本地金额', dataIndex: 'localAmount', width: 110 },
    { title: '渠道时间', dataIndex: 'channelTime', width: 170 },
    { title: '本地时间', dataIndex: 'localTime', width: 170 }
  ]

  const discrepancyColumns = [
    { title: '#', width: 50, render: (_, __, index) => (discrepancyPageNum - 1) * discrepancyPageSize + index + 1 },
    { title: '订单号', dataIndex: 'orderNo', width: 200 },
    {
      title: '差异类型',
      dataIndex: 'discrepancyType',
      width: 130,
      render: (type) => (
        <Tag color={discrepancyTypeMap[type]?.color || 'default'}>
          {discrepancyTypeMap[type]?.text || type}
        </Tag>
      )
    },
    { title: '渠道金额', dataIndex: 'channelAmount', width: 110 },
    { title: '本地金额', dataIndex: 'localAmount', width: 110 },
    {
      title: '处理状态',
      dataIndex: 'status',
      width: 100,
      render: (status) => (
        <Tag color={discrepancyStatusMap[status]?.color || 'default'}>
          {discrepancyStatusMap[status]?.text || status}
        </Tag>
      )
    },
    { title: '处理备注', dataIndex: 'resolveRemark' },
    {
      title: '操作',
      width: 100,
      align: 'center',
      fixed: 'right',
      render: (_, row) =>
        row.status === 'PENDING' ? (
          <Button type="link" onClick={() => openResolveDialog(row)}>
            处理
          </Button>
        ) : null
    }
  ]

  return (
    <div className="bg-fa of">
      <section id="index" className="container">
        <header className="comm-title">
          <h2>对账管理</h2>
        </header>

        <Card style={{ marginBottom: 16 }}>
          <Form form={queryForm} layout="inline">
            <Form.Item label="账单日期" name="dateRange">
              <RangePicker />
            </Form.Item>
            <Form.Item label="渠道" name="channelCode">
              <Select style={{ width: 140 }} options={CHANNEL_OPTIONS} />
            </Form.Item>
            <Form.Item label="状态" name="status">
              <Select style={{ width: 140 }} options={STATUS_OPTIONS} />
            </Form.Item>
            <Form.Item>
              <Space>
                <Button type="primary" onClick={handleQuery}>
                  查询
                </Button>
                <Button onClick={handleReset}>重置</Button>
                <Button type="primary" onClick={openCreateDialog}>
                  手动对账
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Card>

        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Card>
              <Statistic title="今日批次" value={summary.todayBatchCount || 0} />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="完成数"
                value={summary.todayCompletedCount || 0}
                valueStyle={{ color: '#3f8600' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="失败数"
                value={summary.todayFailedCount || 0}
                valueStyle={{ color: '#cf1322' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="待处理差异"
                value={summary.pendingDiscrepancyCount || 0}
                valueStyle={{ color: '#fa8c16' }}
              />
            </Card>
          </Col>
        </Row>

        <Card>
          <Table
            rowKey="id"
            dataSource={batchList}
            columns={batchColumns}
            bordered
            loading={loading}
            scroll={{ x: 1300 }}
            pagination={{
              current: batchPageNum,
              pageSize: batchPageSize,
              total: batchTotal,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total) => `共 ${total} 条`,
              onChange: (page, pageSize) => {
                setBatchPageNum(page)
                setBatchPageSize(pageSize)
              }
            }}
          />
        </Card>
      </section>

      <Modal
        title="创建对账批次"
        open={createDialogVisible}
        width={500}
        onCancel={() => setCreateDialogVisible(false)}
        onOk={handleCreateBatch}
        okText="创建"
        cancelText="取消"
      >
        <Form form={createForm} layout="vertical">
          <Form.Item label="支付渠道" name="channelCode" rules={[{ required: true, message: '请选择支付渠道' }]}>
            <Select placeholder="请选择支付渠道">
              <Select.Option value="WXPAY">微信支付</Select.Option>
              <Select.Option value="ALIPAY">支付宝</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item label="账单日期" name="billDate" rules={[{ required: true, message: '请选择账单日期' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="对账明细"
        open={detailDialogVisible}
        width={1100}
        onCancel={() => setDetailDialogVisible(false)}
        footer={null}
      >
        <Space style={{ marginBottom: 16 }}>
          <Select
            placeholder="匹配状态"
            style={{ width: 140 }}
            allowClear
            value={detailFilters.matchStatus || undefined}
            onChange={(val) => {
              setDetailFilters({ ...detailFilters, matchStatus: val || '' })
              setDetailPageNum(1)
            }}
          >
            <Select.Option value="MATCHED">匹配</Select.Option>
            <Select.Option value="MISMATCH">不匹配</Select.Option>
            <Select.Option value="MISSING_LOCAL">本地缺失</Select.Option>
            <Select.Option value="MISSING_CHANNEL">渠道缺失</Select.Option>
          </Select>
        </Space>
        <Table
          rowKey="id"
          dataSource={detailList}
          columns={detailColumns}
          bordered
          loading={detailLoading}
          scroll={{ x: 1000 }}
          pagination={{
            current: detailPageNum,
            pageSize: detailPageSize,
            total: detailTotal,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => {
              setDetailPageNum(page)
              setDetailPageSize(pageSize)
            }
          }}
        />
      </Modal>

      <Modal
        title="差异列表"
        open={discrepancyDialogVisible}
        width={1100}
        onCancel={() => setDiscrepancyDialogVisible(false)}
        footer={null}
      >
        <Space style={{ marginBottom: 16 }}>
          <Select
            placeholder="处理状态"
            style={{ width: 140 }}
            allowClear
            value={discrepancyFilters.status || undefined}
            onChange={(val) => {
              setDiscrepancyFilters({ ...discrepancyFilters, status: val || '' })
              setDiscrepancyPageNum(1)
            }}
          >
            <Select.Option value="PENDING">待处理</Select.Option>
            <Select.Option value="RESOLVED">已处理</Select.Option>
            <Select.Option value="IGNORED">已忽略</Select.Option>
          </Select>
          <Select
            placeholder="差异类型"
            style={{ width: 160 }}
            allowClear
            value={discrepancyFilters.discrepancyType || undefined}
            onChange={(val) => {
              setDiscrepancyFilters({ ...discrepancyFilters, discrepancyType: val || '' })
              setDiscrepancyPageNum(1)
            }}
          >
            <Select.Option value="AMOUNT_MISMATCH">金额不一致</Select.Option>
            <Select.Option value="STATUS_MISMATCH">状态不一致</Select.Option>
            <Select.Option value="MISSING_LOCAL">本地缺失</Select.Option>
            <Select.Option value="MISSING_CHANNEL">渠道缺失</Select.Option>
          </Select>
        </Space>
        <Table
          rowKey="id"
          dataSource={discrepancyList}
          columns={discrepancyColumns}
          bordered
          loading={discrepancyLoading}
          scroll={{ x: 1100 }}
          pagination={{
            current: discrepancyPageNum,
            pageSize: discrepancyPageSize,
            total: discrepancyTotal,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => {
              setDiscrepancyPageNum(page)
              setDiscrepancyPageSize(pageSize)
            }
          }}
        />
      </Modal>

      <Modal
        title="处理差异"
        open={resolveDialogVisible}
        width={500}
        onCancel={() => setResolveDialogVisible(false)}
        onOk={handleResolve}
        okText="确认处理"
        cancelText="取消"
      >
        <Form form={resolveForm} layout="vertical">
          <Form.Item label="处理备注" name="resolveRemark" rules={[{ required: true, message: '请输入处理备注' }]}>
            <TextArea rows={4} placeholder="请输入差异处理说明" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="执行进度"
        open={progressDialogVisible}
        width={500}
        onCancel={() => setProgressDialogVisible(false)}
        footer={[
          <Button key="close" onClick={() => setProgressDialogVisible(false)}>
            关闭
          </Button>
        ]}
      >
        {progressInfo ? (
          <div>
            <p>
              批次号：<strong>{progressInfo.batchNo}</strong>
            </p>
            <p>
              状态：
              <Tag color={statusColorMap[progressInfo.status] || 'default'}>
                {statusTextMap[progressInfo.status] || progressInfo.status}
              </Tag>
            </p>
            <p>
              总笔数：{progressInfo.totalCount || 0}
            </p>
            <p>
              已处理：{progressInfo.processedCount || 0}
            </p>
            <Progress
              percent={progressInfo.totalCount ? Math.round((progressInfo.processedCount / progressInfo.totalCount) * 100) : 0}
              status={progressInfo.status === 'FAILED' ? 'exception' : progressInfo.status === 'COMPLETED' ? 'success' : 'active'}
            />
            {progressInfo.errorMessage && (
              <p style={{ color: '#cf1322', marginTop: 12 }}>错误信息：{progressInfo.errorMessage}</p>
            )}
          </div>
        ) : (
          <p>加载中...</p>
        )}
      </Modal>
    </div>
  )
}
