import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, message, Modal } from 'antd'
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

  const [payBtnDisabled, setPayBtnDisabled] = useState(false)
  const [codeDialogVisible, setCodeDialogVisible] = useState(false)
  const [productList, setProductList] = useState([])
  const [paymentApps, setPaymentApps] = useState([])
  const [payOrder, setPayOrder] = useState({
    productId: '',
    paymentAppId: ''
  })
  const [codeUrl, setCodeUrl] = useState('')
  const [orderNo, setOrderNo] = useState('')

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

  useEffect(() => {
    productApi.list().then((response) => {
      const products = response?.data?.productList || []
      setProductList(products)
      if (products.length > 0) {
        setPayOrder((prev) => ({ ...prev, productId: products[0].id }))
      }
    })
  }, [])

  useEffect(() => {
    paymentConfigApi.apps().then((response) => {
      const apps = (response?.data?.apps || []).filter((app) => app.enabled)
      setPaymentApps(apps)
    })
  }, [])

  useEffect(() => {
    setPayOrder((prev) => {
      if (paymentApps.some((app) => app.id === prev.paymentAppId)) {
        return prev
      }
      return { ...prev, paymentAppId: paymentApps.length > 0 ? paymentApps[0].id : '' }
    })
  }, [paymentApps])

  useEffect(() => {
    return () => {
      clearPayTimer()
    }
  }, [clearPayTimer])

  const selectItem = (productId) => {
    setPayOrder((prev) => ({ ...prev, productId }))
  }

  const selectPaymentApp = (paymentAppId) => {
    setPayOrder((prev) => ({ ...prev, paymentAppId }))
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
    if (!payOrder.productId) {
      message.warning('请选择课程')
      return
    }
    if (!payOrder.paymentAppId) {
      message.warning('请选择支付应用')
      return
    }

    setPayBtnDisabled(true)

    if (isWxPaymentApp(selectedPaymentApp)) {
      wxPayApi.nativePay(payOrder.productId, payOrder.paymentAppId)
        .then(openWxPayDialog)
        .catch(() => setPayBtnDisabled(false))
      return
    }

    if (isAliPaymentApp(selectedPaymentApp)) {
      aliPayApi.tradePagePay(payOrder.productId, payOrder.paymentAppId)
        .then((response) => {
          document.open()
          document.write(response?.data?.formStr || '')
          document.close()
        })
        .catch(() => setPayBtnDisabled(false))
      return
    }

    message.warning('当前支付应用暂不支持下单')
    setPayBtnDisabled(false)
  }

  const toPayV2 = () => {
    if (!payOrder.productId) {
      message.warning('请选择课程')
      return
    }
    if (!payOrder.paymentAppId) {
      message.warning('请选择支付应用')
      return
    }

    if (!isWxPaymentApp(selectedPaymentApp)) {
      message.warning('微信 V2 支付只支持微信支付应用')
      return
    }

    setPayBtnDisabled(true)

    wxPayApi.nativePayV2(payOrder.productId, payOrder.paymentAppId)
      .then(openWxPayDialog)
      .catch(() => setPayBtnDisabled(false))
  }

  const selectedPaymentApp = paymentApps.find((app) => app.id === payOrder.paymentAppId)
  const payActionDisabled = payBtnDisabled || !payOrder.productId || !payOrder.paymentAppId

  return (
    <div className="bg-fa of">
      <section id="index" className="container">
        <header className="comm-title">
          <h2 className="fl tac">
            <span className="c-333">课程列表</span>
          </h2>
        </header>

        <ul>
          {productList.map((product) => (
            <li key={product.id}>
              <a
                className={`orderBtn ${payOrder.productId === product.id ? 'current' : ''}`}
                onClick={() => selectItem(product.id)}
                href="javascript:void(0);"
              >
                {product.title}
                ¥{product.price / 100}
              </a>
            </li>
          ))}
        </ul>

        <div className="PaymentApp_payment-app-panel">
          <h3 className="PaymentChannel_title">选择支付应用</h3>
          <div className="PaymentApp_app-options">
            {paymentApps.length > 0 ? paymentApps.map((app) => (
              <div
                key={app.id}
                className={`PaymentApp_app-option ${payOrder.paymentAppId === app.id ? 'current' : ''}`}
                onClick={() => selectPaymentApp(app.id)}
              >
                <div className="PaymentApp_app-heading">
                  <img src={paymentAppIcon(app)} className="PaymentApp_app-icon" alt={app.paymentType || app.channelCode} />
                  <div className="PaymentApp_app-name">{app.appName}</div>
                </div>
                <div className="PaymentApp_app-code">{app.appCode} · ID {app.id} · {app.paymentType}</div>
              </div>
            )) : (
              <div className="PaymentApp_empty">暂无可用支付应用</div>
            )}
          </div>
        </div>

        <div className="payButtom">
          <Button
            disabled={payActionDisabled}
            type="primary"
            shape="round"
            size="large"
            style={{ width: 280, height: 44, fontSize: 18, marginRight: 12 }}
            onClick={toPay}
          >
            确认支付
          </Button>
          <Button
            disabled={payActionDisabled}
            type="primary"
            shape="round"
            size="large"
            style={{ width: 280, height: 44, fontSize: 18 }}
            onClick={toPayV2}
          >
            确认支付（微信V2）
          </Button>
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

function isWxPaymentApp(app) {
  return app?.paymentType === '微信' || app?.channelCode === 'wxpay'
}

function isAliPaymentApp(app) {
  return app?.paymentType === '支付宝' || app?.channelCode === 'alipay'
}

function paymentAppIcon(app) {
  return isAliPaymentApp(app) ? alipayImg : wxpayImg
}
