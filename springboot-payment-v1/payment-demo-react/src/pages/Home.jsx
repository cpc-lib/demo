import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, message, Modal } from 'antd'
import { QRCodeCanvas } from 'qrcode.react'
import { useNavigate } from 'react-router-dom'

import productApi from '@/api/product'
import wxPayApi from '@/api/wxPay'
import aliPayApi from '@/api/aliPay'
import orderInfoApi from '@/api/orderInfo'
import wxpayImg from '@/assets/img/wxpay.png'
import alipayImg from '@/assets/img/alipay.png'

export default function Home() {
  const navigate = useNavigate()
  const timerRef = useRef(null)

  const [payBtnDisabled, setPayBtnDisabled] = useState(false)
  const [codeDialogVisible, setCodeDialogVisible] = useState(false)
  const [productList, setProductList] = useState([])
  const [payOrder, setPayOrder] = useState({
    productId: '',
    payType: 'wxpay'
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
    return () => {
      clearPayTimer()
    }
  }, [clearPayTimer])

  const selectItem = (productId) => {
    setPayOrder((prev) => ({ ...prev, productId }))
  }

  const selectPayType = (payType) => {
    setPayOrder((prev) => ({ ...prev, payType }))
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

    setPayBtnDisabled(true)

    if (payOrder.payType === 'wxpay') {
      wxPayApi.nativePay(payOrder.productId)
        .then(openWxPayDialog)
        .catch(() => setPayBtnDisabled(false))
      return
    }

    if (payOrder.payType === 'alipay') {
      aliPayApi.tradePagePay(payOrder.productId)
        .then((response) => {
          document.open()
          document.write(response?.data?.formStr || '')
          document.close()
        })
        .catch(() => setPayBtnDisabled(false))
    }
  }

  const toPayV2 = () => {
    if (!payOrder.productId) {
      message.warning('请选择课程')
      return
    }

    if (payOrder.payType !== 'wxpay') {
      message.warning('微信 V2 支付只支持微信支付方式')
      return
    }

    setPayBtnDisabled(true)

    wxPayApi.nativePayV2(payOrder.productId)
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

        <div className="PaymentChannel_payment-channel-panel">
          <h3 className="PaymentChannel_title">选择支付方式</h3>
          <div className="PaymentChannel_channel-options">
            <div
              className={`ChannelOption_payment-channel-option ${payOrder.payType === 'wxpay' ? 'current' : ''}`}
              onClick={() => selectPayType('wxpay')}
            >
              <div className="ChannelOption_channel-icon">
                <img src={wxpayImg} className="ChannelOption_icon" alt="微信支付" />
              </div>
              <div className="ChannelOption_channel-info">
                <div className="ChannelOption_channel-label">
                  <div className="ChannelOption_label">微信支付</div>
                  <div className="ChannelOption_sub-label" />
                  <div className="ChannelOption_check-option" />
                </div>
              </div>
            </div>

            <div
              className={`ChannelOption_payment-channel-option ${payOrder.payType === 'alipay' ? 'current' : ''}`}
              onClick={() => selectPayType('alipay')}
            >
              <div className="ChannelOption_channel-icon">
                <img src={alipayImg} className="ChannelOption_icon" alt="支付宝" />
              </div>
              <div className="ChannelOption_channel-info">
                <div className="ChannelOption_channel-label">
                  <div className="ChannelOption_label">支付宝</div>
                  <div className="ChannelOption_sub-label" />
                  <div className="ChannelOption_check-option" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="payButtom">
          <Button
            disabled={payBtnDisabled}
            type="primary"
            shape="round"
            size="large"
            style={{ width: 280, height: 44, fontSize: 18, marginRight: 12 }}
            onClick={toPay}
          >
            确认支付（支付宝和微信V3）
          </Button>
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
