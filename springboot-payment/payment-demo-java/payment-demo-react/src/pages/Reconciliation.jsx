import { useEffect, useState } from 'react'
import { Alert, Button, DatePicker, Form, message, Modal, Select, Space, Switch, Table, Tag, Upload } from 'antd'
import dayjs from 'dayjs'

import reconciliationApi from '@/api/reconciliation'
import billApi from '@/api/bill'

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

const SOURCE_TEXT = {
  AUTO_DOWNLOAD: '自动拉取',
  MANUAL_UPLOAD: '手动上传'
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

  const [billList, setBillList] = useState([])
  const [billLoading, setBillLoading] = useState(false)
  const [billFilters, setBillFilters] = useState({
    channelCode: undefined,
    billSource: undefined,
    billDateStart: undefined,
    billDateEnd: undefined
  })
  const [fetchDialogVisible, setFetchDialogVisible] = useState(false)
  const [fetchSubmitting, setFetchSubmitting] = useState(false)
  const [fetchForm] = Form.useForm()
  const [uploadDialogVisible, setUploadDialogVisible] = useState(false)
  const [uploadSubmitting, setUploadSubmitting] = useState(false)
  const [uploadForm] = Form.useForm()
  const [uploadFile, setUploadFile] = useState(null)
  const [recordDialogVisible, setRecordDialogVisible] = useState(false)
  const [recordList, setRecordList] = useState([])
  const [recordLoading, setRecordLoading] = useState(false)
  const [currentBill, setCurrentBill] = useState(null)

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
    fetchBillList()
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

  const fetchBillList = (extraFilters = {}) => {
    setBillLoading(true)
    const params = {
      pageNum: 1,
      pageSize: 100,
      ...billFilters,
      ...extraFilters
    }
    if (params.billDateStart && typeof params.billDateStart !== 'string') {
      params.billDateStart = params.billDateStart.format('YYYY-MM-DD')
    }
    if (params.billDateEnd && typeof params.billDateEnd !== 'string') {
      params.billDateEnd = params.billDateEnd.format('YYYY-MM-DD')
    }
    billApi.list(params).then((response) => {
      setBillList(response?.data?.records || [])
    }).finally(() => {
      setBillLoading(false)
    })
  }

  const openFetch = () => {
    fetchForm.resetFields()
    fetchForm.setFieldsValue({
      billDate: dayjs().subtract(1, 'day'),
      channelCode: 'WXPAY',
      billType: 'ALL',
      force: false
    })
    setFetchDialogVisible(true)
  }

  const closeFetch = () => {
    setFetchDialogVisible(false)
  }

  const doFetch = () => {
    fetchForm.validateFields().then((values) => {
      setFetchSubmitting(true)
      const data = {
        billDate: values.billDate.format('YYYY-MM-DD'),
        channelCode: values.channelCode,
        billType: values.billType,
        force: values.force || false
      }
      billApi.autoFetch(data).then((response) => {
        message.success(`账单导入成功，共 ${response?.data?.recordCount || 0} 条记录`)
        closeFetch()
        fetchBillList()
      }).finally(() => {
        setFetchSubmitting(false)
      })
    })
  }

  const openUpload = () => {
    uploadForm.resetFields()
    setUploadFile(null)
    uploadForm.setFieldsValue({
      billDate: dayjs().subtract(1, 'day'),
      billType: 'ALL',
      force: false
    })
    setUploadDialogVisible(true)
  }

  const closeUpload = () => {
    setUploadDialogVisible(false)
  }

  const handleUploadChange = (file) => {
    setUploadFile(file)
    return false
  }

  const doUpload = () => {
    uploadForm.validateFields().then((values) => {
      if (!uploadFile) {
        message.warning('请选择账单文件')
        return
      }
      const formData = new FormData()
      formData.append('file', uploadFile)
      formData.append('billDate', values.billDate.format('YYYY-MM-DD'))
      formData.append('channelCode', 'WXPAY')
      formData.append('billType', values.billType || 'ALL')
      formData.append('force', values.force || false)

      setUploadSubmitting(true)
      billApi.upload(formData).then((response) => {
        message.success(`账单导入成功，共 ${response?.data?.recordCount || 0} 条记录`)
        closeUpload()
        fetchBillList()
      }).finally(() => {
        setUploadSubmitting(false)
      })
    })
  }

  const viewBillRecords = (bill) => {
    setCurrentBill(bill)
    setRecordLoading(true)
    billApi.listRecords(bill.id, { pageNum: 1, pageSize: 100 }).then((response) => {
      setRecordList(response?.data?.records || [])
      setRecordDialogVisible(true)
    }).finally(() => {
      setRecordLoading(false)
    })
  }

  const removeBill = (bill) => {
    Modal.confirm({
      title: '删除账单',
      content: `确认删除 ${bill.billDate} 的${CHANNEL_TEXT[bill.channelCode] || bill.channelCode}账单？`,
      okText: '确定',
      cancelText: '取消',
      onOk: () => {
        billApi.remove(bill.id).then(() => {
          message.success('删除成功')
          fetchBillList()
        })
      }
    })
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
      title: '业务类型',
      dataIndex: 'businessType',
      width: 90,
      render: (value) => value === 'PAYMENT' ? '进账' : value === 'REFUND' ? '退款' : '-'
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
      dataIndex: 'transactionId',
      width: 200,
      render: (value) => value || '-'
    },
    {
      title: '商户退款单号',
      dataIndex: 'refundNo',
      width: 190,
      render: (value) => value || '-'
    },
    {
      title: '微信退款单号',
      dataIndex: 'refundId',
      width: 190,
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

  const billColumns = [
    {
      title: '#',
      width: 50,
      render: (_, __, index) => index + 1
    },
    {
      title: '账单日期',
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
      title: '账单类型',
      dataIndex: 'billType',
      width: 90
    },
    {
      title: '来源',
      dataIndex: 'billSource',
      width: 100,
      render: (source) => SOURCE_TEXT[source] || source || '-'
    },
    {
      title: '记录数',
      dataIndex: 'recordCount',
      width: 80
    },
    {
      title: '账单总金额',
      dataIndex: 'totalAmount',
      width: 120,
      render: (value) => `${(value || 0) / 100} 元`
    },
    {
      title: '文件名',
      dataIndex: 'fileName',
      width: 160,
      render: (value) => value || '-'
    },
    {
      title: '导入时间',
      dataIndex: 'importTime',
      width: 170
    },
    {
      title: '操作',
      width: 160,
      align: 'center',
      render: (_, row) => (
        <Space>
          <Button type="link" onClick={() => viewBillRecords(row)}>查看记录</Button>
          <Button type="link" danger onClick={() => removeBill(row)}>删除</Button>
        </Space>
      )
    }
  ]

  const recordColumns = [
    {
      title: '#',
      width: 50,
      render: (_, __, index) => index + 1
    },
    {
      title: '业务类型',
      dataIndex: 'businessType',
      width: 90,
      render: (value) => value === 'PAYMENT' ? '进账' : value === 'REFUND' ? '退款' : '-'
    },
    {
      title: '交易时间',
      dataIndex: 'tradeTime',
      width: 170
    },
    {
      title: '商户订单号',
      dataIndex: 'orderNo',
      width: 200,
      render: (value) => value || '-'
    },
    {
      title: '渠道交易号',
      dataIndex: 'transactionId',
      width: 220,
      render: (value) => value || '-'
    },
    {
      title: '商户退款单号',
      dataIndex: 'refundNo',
      width: 190,
      render: (value) => value || '-'
    },
    {
      title: '微信退款单号',
      dataIndex: 'refundId',
      width: 190,
      render: (value) => value || '-'
    },
    {
      title: '交易类型',
      dataIndex: 'tradeType',
      width: 100,
      render: (value) => value || '-'
    },
    {
      title: '交易状态',
      dataIndex: 'status',
      width: 100,
      render: (value) => value || '-'
    },
    {
      title: '订单金额',
      dataIndex: 'amount',
      width: 110,
      render: (value) => value != null ? `${value / 100} 元` : '-'
    },
    {
      title: '退款金额',
      dataIndex: 'refundAmount',
      width: 110,
      render: (value) => value != null ? `${value / 100} 元` : '-'
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

        <header className="comm-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 30 }}>
          <h2 className="fl tac">
            <span className="c-333">渠道账单（对账依据）</span>
          </h2>
          <Space>
            <Select
              placeholder="渠道"
              style={{ width: 120 }}
              allowClear
              value={billFilters.channelCode}
              onChange={(value) => {
                setBillFilters({ ...billFilters, channelCode: value })
                fetchBillList({ channelCode: value })
              }}
            >
              <Select.Option value="WXPAY">微信支付</Select.Option>
              <Select.Option value="ALIPAY">支付宝</Select.Option>
            </Select>
            <Select
              placeholder="来源"
              style={{ width: 130 }}
              allowClear
              value={billFilters.billSource}
              onChange={(value) => {
                setBillFilters({ ...billFilters, billSource: value })
                fetchBillList({ billSource: value })
              }}
            >
              <Select.Option value="AUTO_DOWNLOAD">自动拉取</Select.Option>
              <Select.Option value="MANUAL_UPLOAD">手动上传</Select.Option>
            </Select>
            <DatePicker
              placeholder="开始日期"
              value={billFilters.billDateStart}
              onChange={(date) => {
                setBillFilters({ ...billFilters, billDateStart: date })
              }}
            />
            <DatePicker
              placeholder="结束日期"
              value={billFilters.billDateEnd}
              onChange={(date) => {
                setBillFilters({ ...billFilters, billDateEnd: date })
              }}
            />
            <Button type="primary" onClick={() => fetchBillList()}>查询</Button>
            <Button type="primary" onClick={openFetch}>自动拉取</Button>
            <Button type="primary" onClick={openUpload}>上传账单</Button>
          </Space>
        </header>

        <Table
          rowKey={(row) => row.id}
          dataSource={billList}
          columns={billColumns}
          bordered
          loading={billLoading}
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
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
            message="渠道账单为T+1出账：请先在下方导入对应日期的渠道账单，未导入账单无法对账"
          />
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

      <Modal
        open={fetchDialogVisible}
        title="自动拉取渠道账单"
        width={480}
        centered
        onCancel={closeFetch}
        footer={[
          <Button key="cancel" onClick={closeFetch}>取 消</Button>,
          <Button key="submit" type="primary" loading={fetchSubmitting} onClick={doFetch}>拉取导入</Button>
        ]}
      >
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="渠道账单为T+1出账：微信昨日账单次日10:00后可拉取"
        />
        <Form form={fetchForm} layout="vertical">
          <Form.Item
            label="账单日期"
            name="billDate"
            rules={[{ required: true, message: '请选择账单日期' }]}
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
          <Form.Item label="覆盖导入" name="force" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={uploadDialogVisible}
        title="上传渠道账单"
        width={520}
        centered
        onCancel={closeUpload}
        footer={[
          <Button key="cancel" onClick={closeUpload}>取 消</Button>,
          <Button key="submit" type="primary" loading={uploadSubmitting} onClick={doUpload}>上传导入</Button>
        ]}
      >
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="支持微信交易账单 CSV、TXT、XLSX（T+1 下载），导入后按进账与退款逐笔对账"
        />
        <Form form={uploadForm} layout="vertical">
          <Form.Item label="账单文件" required>
            <Upload
              accept=".csv,.txt,.xlsx"
              maxCount={1}
              beforeUpload={handleUploadChange}
              onRemove={() => setUploadFile(null)}
            >
              <Button>选择账单文件</Button>
            </Upload>
          </Form.Item>
          <Form.Item
            label="账单日期"
            name="billDate"
            rules={[{ required: true, message: '请选择账单日期' }]}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="账单类型" name="billType">
            <Select>
              <Select.Option value="ALL">全部账单</Select.Option>
              <Select.Option value="SUCCESS">成功订单</Select.Option>
              <Select.Option value="REFUND">退款账单</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item label="覆盖导入" name="force" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`账单记录 - ${currentBill?.billDate || ''} ${CHANNEL_TEXT[currentBill?.channelCode] || ''}`}
        open={recordDialogVisible}
        width={1000}
        centered
        footer={null}
        onCancel={() => setRecordDialogVisible(false)}
      >
        <Table
          rowKey={(row) => `${row.businessType}-${row.transactionId || row.refundId}-${row.orderNo || row.refundNo}`}
          dataSource={recordList}
          columns={recordColumns}
          bordered
          loading={recordLoading}
          pagination={false}
          scroll={{ x: 1600 }}
        />
      </Modal>
    </div>
  )
}
