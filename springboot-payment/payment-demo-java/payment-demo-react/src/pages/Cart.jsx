import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Button, Empty, InputNumber, message, Popconfirm, Radio, Skeleton } from 'antd'
import { useNavigate } from 'react-router-dom'

import cartApi from '@/api/cart'
import orderInfoApi from '@/api/orderInfo'
import paymentConfigApi from '@/api/paymentConfig'
import wxPayApi from '@/api/wxPay'
import aliPayApi from '@/api/aliPay'
import { useAuth } from '@/auth/AuthContext'
import WxPayDialog from '@/components/WxPayDialog'
import wxpayImg from '@/assets/img/wxpay.png'
import alipayImg from '@/assets/img/alipay.png'

function createRequestId() {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export default function Cart() {
  const auth = useAuth()
  const navigate = useNavigate()
  const timerRef = useRef(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [cart, setCart] = useState({ items: [], totalQuantity: 0, totalAmount: 0 })
  const [apps, setApps] = useState([])
  const [paymentAppId, setPaymentAppId] = useState(null)
  const [wxVersion, setWxVersion] = useState('V3')
  const [payDialog, setPayDialog] = useState({ open: false, codeUrl: '', orderNo: '' })

  const selectedApp = useMemo(() => apps.find((app) => app.id === paymentAppId), [apps, paymentAppId])

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [cartResponse, appResponse] = await Promise.all([cartApi.get(), paymentConfigApi.listEnabledApps()])
      const nextCart = cartResponse?.data || { items: [], totalQuantity: 0, totalAmount: 0 }
      const nextApps = appResponse?.data || []
      setCart(nextCart)
      setApps(nextApps)
      setPaymentAppId((current) => current || nextApps[0]?.id || null)
      auth.refreshCartCount()
    } finally {
      setLoading(false)
    }
  }, [auth.refreshCartCount])

  useEffect(() => {
    load()
    return () => clearInterval(timerRef.current)
  }, [load])

  const updateQuantity = async (productId, quantity) => {
    await cartApi.update(productId, quantity)
    await load()
  }

  const remove = async (productId) => {
    await cartApi.remove(productId)
    message.success('已移出购物车')
    await load()
  }

  const openWxPay = (response, orderNo) => {
    setPayDialog({ open: true, codeUrl: response?.data?.codeUrl || '', orderNo })
    clearInterval(timerRef.current)
    timerRef.current = setInterval(async () => {
      const status = await orderInfoApi.queryOrderStatus(orderNo)
      if (status.code === 0) {
        clearInterval(timerRef.current)
        setPayDialog({ open: false, codeUrl: '', orderNo: '' })
        navigate('/success')
      }
    }, 3000)
  }

  const payOrder = async (checkout) => {
    if (checkout.paymentChannelCode === 'WXPAY') {
      const response = wxVersion === 'V2'
        ? await wxPayApi.nativePayV2Order(checkout.orderNo)
        : await wxPayApi.nativePayOrder(checkout.orderNo)
      openWxPay(response, checkout.orderNo)
      return
    }
    if (checkout.paymentChannelCode === 'ALIPAY') {
      const response = await aliPayApi.tradePagePayOrder(checkout.orderNo)
      document.open()
      document.write(response?.data?.formStr || '')
      document.close()
      return
    }
    throw new Error(`暂不支持渠道 ${checkout.paymentChannelCode}`)
  }

  const checkout = async () => {
    if (!cart.items.length || !selectedApp) {
      message.error('请先选择课程和支付应用')
      return
    }
    setSubmitting(true)
    try {
      const response = await orderInfoApi.checkout(paymentAppId, createRequestId())
      await auth.refreshCartCount()
      message.success('订单已创建')
      try {
        await payOrder(response.data)
      } catch (_) {
        message.warning('订单已保存，可在我的订单中重试支付')
        navigate('/orders')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <main className="container page-shell"><Skeleton active paragraph={{ rows: 7 }} /></main>
  }

  return (
    <main className="container page-shell cart-layout">
      <section>
        <header className="page-heading">
          <h1>购物车</h1>
          <p>可购买多个课程并调整份数，结算后生成一笔订单。</p>
        </header>
        {!cart.items.length ? (
          <div className="surface empty-surface">
            <Empty description="购物车还是空的">
              <Button type="primary" onClick={() => navigate('/')}>去选课程</Button>
            </Empty>
          </div>
        ) : (
          <div className="surface cart-items">
            {cart.items.map((item) => (
              <article className="cart-line" key={item.productId}>
                <div>
                  <h2>{item.productTitle}</h2>
                  <p>单价 ¥{(item.unitPrice / 100).toFixed(2)}</p>
                </div>
                <InputNumber
                  aria-label={`${item.productTitle}数量`}
                  min={1}
                  max={99}
                  value={item.quantity}
                  onChange={(value) => updateQuantity(item.productId, value || 1)}
                />
                <strong>¥{(item.subtotal / 100).toFixed(2)}</strong>
                <Button type="link" danger onClick={() => remove(item.productId)}>删除</Button>
              </article>
            ))}
            <div className="cart-clear-row">
              <Popconfirm title="确认清空购物车？" onConfirm={async () => { await cartApi.clear(); await load() }}>
                <Button>清空购物车</Button>
              </Popconfirm>
            </div>
          </div>
        )}
      </section>

      <aside className="surface checkout-panel">
        <h2>订单结算</h2>
        <div className="checkout-total"><span>{cart.totalQuantity} 份课程</span><strong>¥{(cart.totalAmount / 100).toFixed(2)}</strong></div>
        <h3>支付应用</h3>
        <Radio.Group value={paymentAppId} onChange={(event) => setPaymentAppId(event.target.value)} className="payment-app-radio">
          {apps.map((app) => (
            <Radio value={app.id} key={app.id}>
              <img src={app.channelCode === 'ALIPAY' ? alipayImg : wxpayImg} alt="" />
              <span>{app.appName}</span>
              <small>{app.channelName || app.channelCode}</small>
            </Radio>
          ))}
        </Radio.Group>
        {selectedApp?.channelCode === 'WXPAY' ? (
          <Radio.Group value={wxVersion} onChange={(event) => setWxVersion(event.target.value)} optionType="button" buttonStyle="solid">
            <Radio.Button value="V3">微信 V3</Radio.Button>
            <Radio.Button value="V2">微信 V2</Radio.Button>
          </Radio.Group>
        ) : null}
        {!apps.length ? <p className="inline-error">暂无可用支付应用，请联系管理员。</p> : null}
        <Button type="primary" size="large" block loading={submitting} disabled={!cart.items.length || !selectedApp} onClick={checkout}>
          创建订单并支付
        </Button>
        <p className="checkout-note">支付未完成时，订单会保留在“我的订单”中。</p>
      </aside>

      <WxPayDialog
        open={payDialog.open}
        codeUrl={payDialog.codeUrl}
        onClose={() => {
          clearInterval(timerRef.current)
          setPayDialog({ open: false, codeUrl: '', orderNo: '' })
          navigate('/orders')
        }}
      />
    </main>
  )
}
