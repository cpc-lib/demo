<template>
  <main class="container page-shell cart-layout">
    <section>
      <header class="page-heading">
        <h1>购物车</h1>
        <p>可购买多个课程并调整份数，结算后生成一笔订单。</p>
      </header>
      <div v-if="loading" class="surface loading-surface">正在加载购物车...</div>
      <div v-else-if="!cart.items.length" class="surface empty-surface">
        <el-empty description="购物车还是空的">
          <el-button type="primary" @click="$router.push('/')">去选课程</el-button>
        </el-empty>
      </div>
      <div v-else class="surface cart-items">
        <article v-for="item in cart.items" :key="item.productId" class="cart-line">
          <div>
            <h2>{{ item.productTitle }}</h2>
            <p>单价 ¥{{ (item.unitPrice / 100).toFixed(2) }}</p>
          </div>
          <el-input-number
            :value="item.quantity"
            :min="1"
            :max="99"
            size="small"
            @change="updateQuantity(item.productId, $event)"
          />
          <strong>¥{{ (item.subtotal / 100).toFixed(2) }}</strong>
          <el-button type="text" class="danger-text" @click="remove(item.productId)">删除</el-button>
        </article>
        <div class="cart-clear-row">
          <el-button @click="clearCart">清空购物车</el-button>
        </div>
      </div>
    </section>

    <aside class="surface checkout-panel">
      <h2>订单结算</h2>
      <div class="checkout-total">
        <span>{{ cart.totalQuantity }} 份课程</span>
        <strong>¥{{ (cart.totalAmount / 100).toFixed(2) }}</strong>
      </div>
      <h3>支付应用</h3>
      <el-radio-group v-model="paymentAppId" class="payment-app-radio">
        <el-radio v-for="app in apps" :key="app.id" :label="app.id">
          <img :src="channelIcon(app.channelCode)" alt="">
          <span>{{ app.appName }}</span>
          <small>{{ app.channelName || app.channelCode }}</small>
        </el-radio>
      </el-radio-group>
      <el-radio-group v-if="selectedApp && selectedApp.channelCode === 'WXPAY'" v-model="wxVersion">
        <el-radio-button label="V3">微信 V3</el-radio-button>
        <el-radio-button label="V2">微信 V2</el-radio-button>
      </el-radio-group>
      <p v-if="!apps.length" class="inline-error">暂无可用支付应用，请联系管理员。</p>
      <el-button type="primary" class="checkout-button" :loading="submitting" :disabled="!cart.items.length || !selectedApp" @click="checkout">
        创建订单并支付
      </el-button>
      <p class="checkout-note">支付未完成时，订单会保留在“我的订单”中。</p>
    </aside>

    <el-dialog :visible.sync="payDialog.open" width="360px" center @close="closePayDialog">
      <div class="wxpay-dialog">
        <h3>微信扫码支付</h3>
        <p>支付完成后，订单状态会自动更新。</p>
        <qriously v-if="payDialog.codeUrl" :value="payDialog.codeUrl" :size="280" />
        <el-button @click="closePayDialog">稍后支付</el-button>
      </div>
    </el-dialog>
  </main>
</template>

<script>
import cartApi from '../api/cart'
import orderInfoApi from '../api/orderInfo'
import paymentConfigApi from '../api/paymentConfig'
import wxPayApi from '../api/wxPay'
import aliPayApi from '../api/aliPay'
import { authState } from '../auth/session'
import wxPayIcon from '../assets/img/wxpay.png'
import aliPayIcon from '../assets/img/alipay.png'

function createRequestId() {
  if (window.crypto && window.crypto.randomUUID) return window.crypto.randomUUID()
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export default {
  data() {
    return {
      loading: true,
      submitting: false,
      cart: { items: [], totalQuantity: 0, totalAmount: 0 },
      apps: [],
      paymentAppId: null,
      wxVersion: 'V3',
      timer: null,
      payDialog: { open: false, codeUrl: '', orderNo: '' }
    }
  },
  computed: {
    selectedApp() {
      return this.apps.find(app => app.id === this.paymentAppId)
    }
  },
  created() {
    this.load()
  },
  beforeDestroy() {
    clearInterval(this.timer)
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const responses = await Promise.all([cartApi.get(), paymentConfigApi.listEnabledApps()])
        this.cart = responses[0].data || { items: [], totalQuantity: 0, totalAmount: 0 }
        this.apps = responses[1].data || []
        if (!this.paymentAppId && this.apps.length) this.paymentAppId = this.apps[0].id
        authState.cartCount = this.cart.totalQuantity || 0
      } finally {
        this.loading = false
      }
    },
    channelIcon(channelCode) {
      return channelCode === 'ALIPAY' ? aliPayIcon : wxPayIcon
    },
    async updateQuantity(productId, quantity) {
      await cartApi.update(productId, quantity || 1)
      await this.load()
    },
    async remove(productId) {
      await cartApi.remove(productId)
      this.$message.success('已移出购物车')
      await this.load()
    },
    async clearCart() {
      try {
        await this.$confirm('确认清空购物车？', '提示', { type: 'warning' })
        await cartApi.clear()
        await this.load()
      } catch (_) {
        // 用户取消时不处理。
      }
    },
    openWxPay(response, orderNo) {
      this.payDialog = { open: true, codeUrl: (response.data && response.data.codeUrl) || '', orderNo }
      clearInterval(this.timer)
      this.timer = setInterval(async () => {
        const status = await orderInfoApi.queryOrderStatus(orderNo)
        if (status.code === 0) {
          clearInterval(this.timer)
          this.payDialog.open = false
          this.$router.push('/success')
        }
      }, 3000)
    },
    async payOrder(order) {
      if (order.paymentChannelCode === 'WXPAY') {
        const response = this.wxVersion === 'V2'
          ? await wxPayApi.nativePayV2Order(order.orderNo)
          : await wxPayApi.nativePayOrder(order.orderNo)
        this.openWxPay(response, order.orderNo)
        return
      }
      if (order.paymentChannelCode === 'ALIPAY') {
        const response = await aliPayApi.tradePagePayOrder(order.orderNo)
        document.open()
        document.write((response.data && response.data.formStr) || '')
        document.close()
        return
      }
      throw new Error(`暂不支持渠道 ${order.paymentChannelCode}`)
    },
    async checkout() {
      if (!this.cart.items.length || !this.selectedApp) {
        this.$message.error('请先选择课程和支付应用')
        return
      }
      this.submitting = true
      try {
        const response = await orderInfoApi.checkout(this.paymentAppId, createRequestId())
        authState.cartCount = 0
        this.$message.success('订单已创建')
        try {
          await this.payOrder(response.data)
        } catch (_) {
          this.$message.warning('订单已保存，可在我的订单中重试支付')
          this.$router.push('/orders')
        }
      } finally {
        this.submitting = false
      }
    },
    closePayDialog() {
      clearInterval(this.timer)
      this.payDialog = { open: false, codeUrl: '', orderNo: '' }
      if (this.$route.path !== '/orders') this.$router.push('/orders')
    }
  }
}
</script>
