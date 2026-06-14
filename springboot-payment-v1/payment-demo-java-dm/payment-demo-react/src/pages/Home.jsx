import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, message, Modal, Spin } from 'antd'
import { QRCodeCanvas } from 'qrcode.react'
import { useNavigate } from 'react-router-dom'

import productApi from '@/api/product'
import wxPayApi from '@/api/wxPay'
import aliPayApi from '@/api/aliPay'
import orderInfoApi from '@/api/orderInfo'
import paymentConfigApi from '@/api/paymentConfig'
import wxpayImg from '@/assets/img/wxpay.png'
import alipayImg from '@/assets/img/alipay.png'

export default function Home() {
  const navigate = useNavigate()
  const timerRef = useRef(null)

  const [loading, setLoading] = useState(true)
  const [payBtnDisabled, setPayBtnDisabled] = useState(false)
  const [codeDialogVisible, setCodeDialogVisible] = useState(false)
  const [productList, setProductList] = useState([])
  const [paymentAppList, setPaymentAppList] = useState([])
  const [payOrder, setPayOrder] = useState({
    productId: '',
    paymentAppId: ''
  })
  const [codeUrl, setCodeUrl] = useState('')
  const [orderNo, setOrderNo] = useState('')

  const selectedPaymentApp = paymentAppList.find(item => item.id === payOrder.paymentAppId)

  const clearPayTimer = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const closeDialog = useCallback(() => {
    setCodeDialogVisible(false)
    setPayBtnDisabled(false)
    clearPayTimer()
  }, [clearPayTimer])

  const queryOrderStatus = useCallback((targetOrderNo) => {
    if (!targetOrderNo) {
      return
    }

    orderInfoApi.queryOrderStatus(targetOrderNo).then((response) => {
      if (response.code === 0) {
        clearPayTimer()
        setTimeout(() => {
          navigate('/success')
        }, 3000)
      }
    })
  }, [clearPayTimer, navigate])

  const loadProducts = useCallback(() => {
    productApi.list().then((response) => {
      const products = response?.data?.productList || []
      setProductList(products)
      if (products.length > 0) {
        setPayOrder((prev) => ({ ...prev, productId: products[0].id }))
      }
    }).finally(() => {
      setTimeout(() => setLoading(false), 300)
    })
  }, [])

  const loadPaymentApps = useCallback(() => {
    paymentConfigApi.listEnabledApps().then((response) => {
      const apps = response?.data || []
      setPaymentAppList(apps)
      if (apps.length > 0) {
        setPayOrder((prev) => ({ ...prev, paymentAppId: apps[0].id }))
      }
    }).finally(() => {
      setTimeout(() => setLoading(false), 300)
    })
  }, [])

  useEffect(() => {
    loadProducts()
    loadPaymentApps()
  }, [loadProducts, loadPaymentApps])

  useEffect(() => {
    return () => {
      clearPayTimer()
    }
  }, [clearPayTimer])

  const selectItem = (productId) => {
    setPayOrder((prev) => ({ ...prev, productId }))
  }

  const selectPaymentApp = (app) => {
    setPayOrder((prev) => ({ ...prev, paymentAppId: app.id }))
  }

  const channelIcon = (channelCode) => {
    return channelCode === 'ALIPAY' ? alipayImg : wxpayImg
  }

  const validateBeforePay = () => {
    if (!payOrder.productId) {
      message.error('请选择课程')
      return false
    }
    if (!selectedPaymentApp) {
      message.error('请选择支付应用')
      return false
    }
    return true
  }

  const openWxPayDialog = (response) => {
    const nextCodeUrl = response?.data?.codeUrl || ''
    const nextOrderNo = response?.data?.orderNo || ''

    setCodeUrl(nextCodeUrl)
    setOrderNo(nextOrderNo)
    setCodeDialogVisible(true)

    clearPayTimer()
    timerRef.current = setInterval(() => {
      queryOrderStatus(nextOrderNo)
    }, 3000)
  }

  const toPay = () => {
    if (!validateBeforePay()) {
      return
    }

    setPayBtnDisabled(true)

    if (selectedPaymentApp.channelCode === 'WXPAY') {
      wxPayApi.nativePay(payOrder.productId, payOrder.paymentAppId)
        .then(openWxPayDialog)
        .catch(() => setPayBtnDisabled(false))
      return
    }

    if (selectedPaymentApp.channelCode === 'ALIPAY') {
      aliPayApi.tradePagePay(payOrder.productId, payOrder.paymentAppId)
        .then((response) => {
          document.open()
          document.write(response?.data?.formStr || '')
          document.close()
        })
        .catch(() => setPayBtnDisabled(false))
      return
    }

    setPayBtnDisabled(false)
    message.error('暂不支持的支付渠道：' + selectedPaymentApp.channelCode)
  }

  const toPayV2 = () => {
    if (!validateBeforePay()) {
      return
    }

    if (selectedPaymentApp.channelCode !== 'WXPAY') {
      message.error('微信V2仅支持微信支付应用')
      return
    }

    setPayBtnDisabled(true)

    wxPayApi.nativePayV2(payOrder.productId, payOrder.paymentAppId)
      .then(openWxPayDialog)
      .catch(() => setPayBtnDisabled(false))
  }

  return (
    <div className="bg-fa of">
      <section id="index" className="container">
        <header className="comm-title">
          <h2 className="fl tac">
            <span className="c-333">课程列表</span>
          </h2>
        </header>

        {loading && (
          <div className="loading-container">
            <Spin size="large" />
            <p className="loading-text">加载中...</p>
          </div>
        )}

        <ul style={{ opacity: loading ? 0 : 1, transition: 'opacity 0.3s' }}>
          {productList.map((product) => (
            <li key={product.id}>
              <a
                className={`orderBtn ${payOrder.productId === product.id ? 'current' : ''}`}
                onClick={() => selectItem(product.id)}
                href="javascript:void(0);"
              >
                {product.title}
                <span className="price">¥{product.price / 100}</span>
              </a>
            </li>
          ))}
        </ul>

        <div className="PaymentChannel_payment-channel-panel" style={{ opacity: loading ? 0 : 1, transition: 'opacity 0.3s' }}>
          <h3 className="PaymentChannel_title">选择支付应用</h3>
          {paymentAppList.length === 0 ? (
            <div className="empty-config-tip">
              <div className="empty-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8" />
                  <polyline points="16 13 20 17 24 13" />
                  <line x1="12" y1="4" x2="12" y2="20" />
                </svg>
              </div>
              <p>未查询到启用的支付应用</p>
              <p className="empty-tip">请先到"支付配置"页面维护支付渠道与支付应用</p>
            </div>
          ) : (
            <div className="PaymentChannel_channel-options">
              {paymentAppList.map((app) => (
                <div
                  key={app.id}
                  className={`ChannelOption_payment-channel-option ${payOrder.paymentAppId === app.id ? 'current' : ''}`}
                  onClick={() => selectPaymentApp(app)}
                >
                  <div className="ChannelOption_channel-icon">
                    <img src={channelIcon(app.channelCode)} className="ChannelOption_icon" alt="" />
                  </div>
                  <div className="ChannelOption_channel-info">
                    <div className="ChannelOption_label">{app.appName}</div>
                    <div className="ChannelOption_sub-label">{app.channelName || app.channelCode}</div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="payButtom">
          <Button
            disabled={payBtnDisabled || !selectedPaymentApp}
            type="primary"
            shape="round"
            size="large"
            style={{ width: 280, height: 44, fontSize: 18, marginRight: 12 }}
            onClick={toPay}
          >
            确认支付（{selectedPaymentApp ? selectedPaymentApp.channelName || selectedPaymentApp.channelCode : '未选择应用'}）
          </Button>
          {selectedPaymentApp && selectedPaymentApp.channelCode === 'WXPAY' && (
            <Button
              disabled={payBtnDisabled}
              type="primary"
              shape="round"
              size="large"
              style={{ width: 280, height: 44, fontSize: 18 }}
              onClick={toPayV2}
            >
              确认支付（微信V2）
            </Button>
          )}
        </div>
      </section>

      <Modal
        open={codeDialogVisible}
        footer={null}
        closable={false}
        width={350}
        centered
        onCancel={closeDialog}
        afterClose={closeDialog}
      >
        <div style={{ textAlign: 'center' }}>
          {codeUrl ? <QRCodeCanvas value={codeUrl} size={300} /> : null}
          <div style={{ marginTop: 12 }}>使用微信扫码支付</div>
          <Button style={{ marginTop: 16 }} onClick={closeDialog}>关闭</Button>
        </div>
      </Modal>
    </div>
  )
}
