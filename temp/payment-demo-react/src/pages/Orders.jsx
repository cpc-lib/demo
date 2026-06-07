import { useEffect, useState } from 'react'
import { Button, Form, InputNumber, message, Modal, Select, Table, Tag } from 'antd'

import aliPay from '@/api/aliPay'
import orderInfoApi from '@/api/orderInfo'
import wxPayApi from '@/api/wxPay'
import refundInfoApi from '@/api/refundInfo'

const APPROVAL_STATUS_TEXT = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝'
}

const REFUND_STATUS_TEXT = {
  CREATED: '待处理',
  PROCESSING: '退款中',
  SUCCESS: '退款成功',
  FAILED: '退款失败',
  CLOSED: '已关闭',
  ABNORMAL: '退款异常'
}

const APPROVAL_TAG_TYPE = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'default'
}

const REFUND_TAG_TYPE = {
  CREATED: 'warning',
  PROCESSING: 'error',
  SUCCESS: 'success',
  FAILED: 'error',
  CLOSED: 'default',
  ABNORMAL: 'error'
}

function orderStatusTag(status) {
  const colorMap = {
    未支付: 'processing',
    支付成功: 'success',
    部分退款: 'warning',
    超时已关闭: 'warning',
    用户已取消: 'default',
    退款中: 'error',
    已退款: 'default',
    退款异常: 'error'
  }

  return <Tag color={colorMap[status]}>{status || '-'}</Tag>
}

export default function Orders() {
  const [list, setList] = useState([])
  const [refundDialogVisible, setRefundDialogVisible] = useState(false)
  const [refundRecordDialogVisible, setRefundRecordDialogVisible] = useState(false)
  const [refundRecordList, setRefundRecordList] = useState([])
  const [orderNo, setOrderNo] = useState('')
  const [reason, setReason] = useState('不喜欢')
  const [refundAmountYuan, setRefundAmountYuan] = useState(0.01)
  const [currentOrderTotalFee, setCurrentOrderTotalFee] = useState(0)
  const [refundSubmitBtnDisabled, setRefundSubmitBtnDisabled] = useState(false)
  const [paymentType, setPaymentType] = useState('')

  const showOrderList = () => {
    orderInfoApi.list().then((response) => {
      setList(response?.data?.list || [])
    })
  }

  useEffect(() => {
    showOrderList()
  }, [])

  const canApplyRefund = (orderStatus) => {
    return orderStatus === '支付成功' || orderStatus === '部分退款' || orderStatus === '退款中'
  }

  const cancel = (targetOrderNo, targetPaymentType) => {
    const request = targetPaymentType === '微信'
      ? wxPayApi.cancel(targetOrderNo)
      : aliPay.cancel(targetOrderNo)

    request.then((response) => {
      message.success(response.message || '取消成功')
      showOrderList()
    })
  }

  const refund = (row) => {
    setRefundDialogVisible(true)
    setOrderNo(row.orderNo)
    setPaymentType(row.paymentType)
    setCurrentOrderTotalFee(row.totalFee || 0)
    setRefundAmountYuan(Number(((row.totalFee || 1) / 100).toFixed(2)))
    setReason('不喜欢')
  }

  const showRefundRecords = (row) => {
    refundInfoApi.listByOrderNo(row.orderNo).then((response) => {
      setRefundRecordList(response?.data?.list || [])
      setRefundRecordDialogVisible(true)
    })
  }

  const closeDialog = () => {
    setRefundDialogVisible(false)
    setOrderNo('')
    setReason('')
    setRefundAmountYuan(0.01)
    setCurrentOrderTotalFee(0)
    setRefundSubmitBtnDisabled(false)
    setPaymentType('')
  }

  const toRefunds = () => {
    if (!refundAmountYuan || Number(refundAmountYuan) <= 0) {
      message.error('请输入正确的退款金额')
      return
    }

    setRefundSubmitBtnDisabled(true)

    const requestData = {
      orderNo,
      refundAmount: Math.round(Number(refundAmountYuan) * 100),
      reason: reason || '正常退款'
    }

    const request = paymentType === '微信'
      ? wxPayApi.refunds(requestData)
      : aliPay.refunds(requestData)

    request.then((response) => {
      message.success(response.message || '退款申请提交成功')
      closeDialog()
      showOrderList()
    }).catch(() => {
      setRefundSubmitBtnDisabled(false)
    })
  }

  const columns = [
    {
      title: '#',
      width: 50,
      render: (_, __, index) => index + 1
    },
    {
      title: '订单编号',
      dataIndex: 'orderNo',
      width: 230
    },
    {
      title: '订单标题',
      dataIndex: 'title'
    },
    {
      title: '订单金额',
      dataIndex: 'totalFee',
      render: (value) => `${(value || 0) / 100} 元`
    },
    {
      title: '支付方式',
      dataIndex: 'paymentType'
    },
    {
      title: '订单状态',
      dataIndex: 'orderStatus',
      render: orderStatusTag
    },
    {
      title: '操作',
      width: 220,
      align: 'center',
      render: (_, row) => (
        <>
          {row.orderStatus === '未支付' ? (
            <Button type="link" onClick={() => cancel(row.orderNo, row.paymentType)}>取消</Button>
          ) : null}
          {canApplyRefund(row.orderStatus) ? (
            <Button type="link" onClick={() => refund(row)}>退款申请</Button>
          ) : null}
          <Button type="link" onClick={() => showRefundRecords(row)}>退款申请记录</Button>
        </>
      )
    }
  ]

  const refundRecordColumns = [
    {
      title: '退款申请单号',
      dataIndex: 'refundNo',
      width: 210
    },
    {
      title: '退款金额',
      dataIndex: 'refund',
      width: 110,
      render: (value) => `${(value || 0) / 100} 元`
    },
    {
      title: '审核状态',
      dataIndex: 'approvalStatus',
      width: 110,
      render: (status) => (
        <Tag color={APPROVAL_TAG_TYPE[status]}>
          {APPROVAL_STATUS_TEXT[status] || status || '-'}
        </Tag>
      )
    },
    {
      title: '退款状态',
      dataIndex: 'refundStatus',
      width: 110,
      render: (status) => (
        <Tag color={REFUND_TAG_TYPE[status]}>
          {REFUND_STATUS_TEXT[status] || status || '-'}
        </Tag>
      )
    },
    {
      title: '退款原因',
      dataIndex: 'reason'
    },
    {
      title: '审核备注',
      dataIndex: 'approveRemark'
    },
    {
      title: '申请时间',
      dataIndex: 'createTime',
      width: 170
    }
  ]

  return (
    <div className="bg-fa of">
      <section id="index" className="container">
        <header className="comm-title">
          <h2 className="fl tac">
            <span className="c-333">订单列表</span>
          </h2>
        </header>
        <Table
          rowKey={(row) => row.orderNo}
          dataSource={list}
          columns={columns}
          bordered
          pagination={false}
        />
      </section>

      <Modal
        open={refundDialogVisible}
        width={420}
        centered
        title={null}
        onCancel={closeDialog}
        footer={[
          <Button key="cancel" onClick={closeDialog}>取 消</Button>,
          <Button key="submit" type="primary" onClick={toRefunds} disabled={refundSubmitBtnDisabled}>确 定</Button>
        ]}
      >
        <Form labelCol={{ span: 6 }} wrapperCol={{ span: 18 }}>
          <Form.Item label="订单编号">
            <span>{orderNo}</span>
          </Form.Item>
          <Form.Item label="订单金额">
            <span>{currentOrderTotalFee / 100} 元</span>
          </Form.Item>
          <Form.Item label="退款金额">
            <InputNumber
              value={refundAmountYuan}
              precision={2}
              step={0.01}
              min={0.01}
              max={currentOrderTotalFee / 100}
              controls
              style={{ width: '100%' }}
              onChange={(value) => setRefundAmountYuan(value || 0.01)}
            />
          </Form.Item>
          <Form.Item label="退款原因">
            <Select value={reason} placeholder="请选择退款原因" style={{ width: '100%' }} onChange={setReason}>
              <Select.Option value="不喜欢">不喜欢</Select.Option>
              <Select.Option value="买错了">买错了</Select.Option>
              <Select.Option value="其他">其他</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="退款申请记录"
        open={refundRecordDialogVisible}
        width={900}
        centered
        footer={null}
        onCancel={() => setRefundRecordDialogVisible(false)}
      >
        <Table
          rowKey={(row) => row.refundNo}
          dataSource={refundRecordList}
          columns={refundRecordColumns}
          bordered
          pagination={false}
        />
      </Modal>
    </div>
  )
}
