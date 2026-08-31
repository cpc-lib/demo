import { useEffect, useState } from 'react'
import { Button, Empty, Input, message, Modal, Space, Table, Tag } from 'antd'

import refundInfoApi from '@/api/refundInfo'

const APPROVAL_STATUS_TEXT = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝'
}

const APPROVAL_TAG_TYPE = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'default'
}

const REFUND_STATUS_TEXT = {
  CREATED: '待处理',
  PROCESSING: '退款中',
  SUCCESS: '退款成功',
  FAILED: '退款失败',
  CLOSED: '已关闭',
  ABNORMAL: '退款异常'
}

const REFUND_TAG_TYPE = {
  CREATED: 'warning',
  PROCESSING: 'processing',
  SUCCESS: 'success',
  FAILED: 'error',
  CLOSED: 'default',
  ABNORMAL: 'error'
}

function errorMessage(error, fallback) {
  return error?.response?.data?.message || error?.message || fallback
}

export default function Refunds() {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [actionKey, setActionKey] = useState('')
  const [decision, setDecision] = useState({ open: false, refundNo: '', action: '', remark: '' })

  const loadList = () => {
    setLoading(true)
    refundInfoApi.list().then((response) => {
      setList(response?.data?.list || [])
    }).catch((error) => {
      message.error(errorMessage(error, '退款申请单加载失败'))
      setList([])
    }).finally(() => setLoading(false))
  }

  useEffect(() => {
    loadList()
  }, [])

  const openDecision = (row, action) => {
    setDecision({
      open: true,
      refundNo: row.refundNo,
      action,
      remark: ''
    })
  }

  const closeDecision = () => {
    if (!actionKey) {
      setDecision({ open: false, refundNo: '', action: '', remark: '' })
    }
  }

  const submitDecision = async () => {
    const { refundNo, action, remark } = decision
    if (!refundNo || !action) {
      return
    }

    const key = `${action}-${refundNo}`
    setActionKey(key)
    try {
      const api = action === 'approve' ? refundInfoApi.approve : refundInfoApi.reject
      const response = await api(refundNo, remark.trim() || undefined)
      message.success(response.message || (action === 'approve' ? '审核通过，退款已提交处理' : '退款申请已拒绝'))
      setDecision({ open: false, refundNo: '', action: '', remark: '' })
      loadList()
    } catch (error) {
      message.error(errorMessage(error, '退款审核操作失败'))
    } finally {
      setActionKey('')
    }
  }

  const queryRefund = async (row) => {
    const key = `query-${row.refundNo}`
    setActionKey(key)
    try {
      const response = await refundInfoApi.query(row.refundNo)
      const latest = response?.data?.refundInfo
      if (latest) {
        setList((current) => current.map((item) => (
          item.refundNo === row.refundNo ? { ...item, ...latest } : item
        )))
      }
      message.success(response.message || '退款状态查询完成')
    } catch (error) {
      message.error(errorMessage(error, '退款状态查询失败'))
    } finally {
      setActionKey('')
    }
  }

  const columns = [
    { title: '#', width: 50, render: (_, __, index) => index + 1 },
    { title: '退款申请单号', dataIndex: 'refundNo', width: 210 },
    { title: '订单编号', dataIndex: 'orderNo', width: 210 },
    {
      title: '退款金额',
      dataIndex: 'refund',
      width: 110,
      render: (value) => `${(value || 0) / 100} 元`
    },
    {
      title: '审核状态',
      dataIndex: 'approvalStatus',
      width: 100,
      render: (status) => (
        <Tag color={APPROVAL_TAG_TYPE[status]}>
          {APPROVAL_STATUS_TEXT[status] || status || '-'}
        </Tag>
      )
    },
    {
      title: '退款状态',
      dataIndex: 'refundStatus',
      width: 100,
      render: (status) => (
        <Tag color={REFUND_TAG_TYPE[status]}>
          {REFUND_STATUS_TEXT[status] || status || '-'}
        </Tag>
      )
    },
    { title: '退款原因', dataIndex: 'reason', width: 150 },
    { title: '审核备注', dataIndex: 'approveRemark', width: 180 },
    { title: '申请时间', dataIndex: 'createTime', width: 170 },
    {
      title: '操作',
      width: 220,
      align: 'center',
      render: (_, row) => (
        <Space size="small">
          {row.approvalStatus === 'PENDING' ? (
            <>
              <Button type="link" onClick={() => openDecision(row, 'approve')}>通过</Button>
              <Button type="link" danger onClick={() => openDecision(row, 'reject')}>驳回</Button>
            </>
          ) : null}
          {row.approvalStatus === 'APPROVED' ? (
            <Button
              type="link"
              loading={actionKey === `query-${row.refundNo}`}
              onClick={() => queryRefund(row)}
            >
              查询退款状态
            </Button>
          ) : null}
        </Space>
      )
    }
  ]

  const decisionTitle = decision.action === 'approve' ? '通过退款申请' : '驳回退款申请'
  const decisionButtonText = decision.action === 'approve' ? '通过并执行退款' : '确认驳回'

  return (
    <main className="container page-shell">
      <header className="page-heading">
        <div>
          <h1>退款审批</h1>
          <p>审核退款申请，并主动查询已通过退款单的渠道状态。</p>
        </div>
        <Button onClick={loadList} loading={loading}>刷新</Button>
      </header>
      <Table
        rowKey={(row) => row.refundNo}
        dataSource={list}
        columns={columns}
        bordered
        loading={loading}
        scroll={{ x: 1500 }}
        locale={{ emptyText: <Empty description="暂无退款申请单" /> }}
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title={decisionTitle}
        open={decision.open}
        onCancel={closeDecision}
        onOk={submitDecision}
        okText={decisionButtonText}
        cancelText="取消"
        confirmLoading={Boolean(actionKey)}
      >
        <p>退款申请单号：{decision.refundNo}</p>
        <Input.TextArea
          rows={4}
          maxLength={255}
          showCount
          placeholder="请输入审核备注（可选）"
          value={decision.remark}
          onChange={(event) => setDecision((current) => ({ ...current, remark: event.target.value }))}
        />
      </Modal>
    </main>
  )
}
