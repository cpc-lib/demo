<template>
  <div class="bg-fa of">
    <section id="index" class="container">
      <header class="comm-title">
        <h2>课程列表</h2>
      </header>

      <div v-if="loading" class="loading-container">
        <el-spinner type="circle" size="48" />
        <p class="loading-text">加载中...</p>
      </div>

      <transition name="fade">
        <ul v-show="!loading">
          <li v-for="product in productList" :key="product.id">
            <a
              :class="['orderBtn', { current: payOrder.productId === product.id }]"
              @click="selectItem(product.id)"
              href="javascript:void(0);"
            >
              {{ product.title }}
              <span class="price">¥{{ (product.price / 100).toFixed(2) }}</span>
            </a>
          </li>
        </ul>
      </transition>

      <transition name="slide-up">
        <div v-show="!loading" class="PaymentChannel_payment-channel-panel">
          <h3 class="PaymentChannel_title">选择支付应用</h3>
          <div v-if="paymentAppList.length === 0" class="empty-config-tip">
            <div class="empty-icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8" />
                <polyline points="16 13 20 17 24 13" />
                <line x1="12" y1="4" x2="12" y2="20" />
              </svg>
            </div>
            <p>未查询到启用的支付应用</p>
            <p class="empty-tip">请先到“支付配置”页面维护支付渠道与支付应用</p>
          </div>
          <div v-else class="PaymentChannel_channel-options">
            <transition-group name="list">
              <div
                v-for="app in paymentAppList"
                :key="app.id"
                :class="['ChannelOption_payment-channel-option', { current: payOrder.paymentAppId === app.id }]"
                @click="selectPaymentApp(app)"
              >
                <div class="ChannelOption_channel-icon">
                  <img :src="channelIcon(app.channelCode)" class="ChannelOption_icon"/>
                </div>
                <div class="ChannelOption_channel-info">
                  <div class="ChannelOption_label">{{ app.appName }}</div>
                  <div class="ChannelOption_sub-label">{{ app.channelName || app.channelCode }}</div>
                </div>
              </div>
            </transition-group>
          </div>
        </div>
      </transition>

      <div class="payButtom">
        <el-button
          :disabled="payBtnDisabled || !selectedPaymentApp"
          type="warning"
          round
          style="width: 280px; height: 44px; font-size: 18px"
          @click="toPay()"
        >
          确认支付（{{ selectedPaymentApp ? selectedPaymentApp.channelName || selectedPaymentApp.channelCode : '未选择应用' }}）
        </el-button>
        <el-button
          v-if="selectedPaymentApp && selectedPaymentApp.channelCode === 'WXPAY'"
          :disabled="payBtnDisabled"
          type="warning"
          round
          style="width: 280px; height: 44px; font-size: 18px"
          @click="toPayV2()"
        >
          确认支付（微信V2）
        </el-button>
      </div>
    </section>

    <el-dialog
      :visible.sync="codeDialogVisible"
      :show-close="false"
      @close="closeDialog"
      width="350px"
      center
    >
      <qriously :value="codeUrl" :size="300"/>
      使用微信扫码支付
    </el-dialog>
  </div>
</template>

<script>
import productApi from '../api/product'
import wxPayApi from '../api/wxPay'
import aliPayApi from '../api/aliPay'
import orderInfoApi from '../api/orderInfo'
import paymentConfigApi from '../api/paymentConfig'
import wxPayIcon from '../assets/img/wxpay.png'
import aliPayIcon from '../assets/img/alipay.png'

export default {
  data() {
    return {
      loading: true,
      payBtnDisabled: false,
      codeDialogVisible: false,
      productList: [],
      paymentAppList: [],
      payOrder: {
        productId: '',
        paymentAppId: ''
      },
      codeUrl: '',
      orderNo: '',
      timer: null
    }
  },

  computed: {
    selectedPaymentApp() {
      return this.paymentAppList.find(item => item.id === this.payOrder.paymentAppId)
    }
  },

  created() {
    this.loadProducts()
    this.loadPaymentApps()
  },

  methods: {
    loadProducts() {
      productApi.list().then((response) => {
        this.productList = response.data.productList || []
        if (this.productList.length > 0) {
          this.payOrder.productId = this.productList[0].id
        }
        this.checkLoadingComplete()
      }).catch(() => {
        this.checkLoadingComplete()
      })
    },

    loadPaymentApps() {
      paymentConfigApi.listEnabledApps().then((response) => {
        this.paymentAppList = response.data || []
        if (this.paymentAppList.length > 0) {
          this.payOrder.paymentAppId = this.paymentAppList[0].id
        }
        this.checkLoadingComplete()
      }).catch(() => {
        this.checkLoadingComplete()
      })
    },

    checkLoadingComplete() {
      setTimeout(() => {
        this.loading = false
      }, 300)
    },

    selectItem(productId) {
      this.payOrder.productId = productId
    },

    selectPaymentApp(app) {
      this.payOrder.paymentAppId = app.id
    },

    channelIcon(channelCode) {
      if (channelCode === 'ALIPAY') {
        return aliPayIcon
      }
      return wxPayIcon
    },

    validateBeforePay() {
      if (!this.payOrder.productId) {
        this.$message.error('请选择课程')
        return false
      }
      if (!this.selectedPaymentApp) {
        this.$message.error('请选择支付应用')
        return false
      }
      return true
    },

    toPay() {
      if (!this.validateBeforePay()) {
        return
      }
      this.payBtnDisabled = true

      if (this.selectedPaymentApp.channelCode === 'WXPAY') {
        wxPayApi.nativePay(this.payOrder.productId, this.payOrder.paymentAppId).then((response) => {
          this.codeUrl = response.data.codeUrl
          this.orderNo = response.data.orderNo
          this.codeDialogVisible = true
          this.startQueryTimer()
        }).catch(() => {
          this.payBtnDisabled = false
        })
        return
      }

      if (this.selectedPaymentApp.channelCode === 'ALIPAY') {
        aliPayApi.tradePagePay(this.payOrder.productId, this.payOrder.paymentAppId).then((response) => {
          document.write(response.data.formStr)
        }).catch(() => {
          this.payBtnDisabled = false
        })
        return
      }

      this.payBtnDisabled = false
      this.$message.error('暂不支持的支付渠道：' + this.selectedPaymentApp.channelCode)
    },

    toPayV2() {
      if (!this.validateBeforePay()) {
        return
      }
      if (this.selectedPaymentApp.channelCode !== 'WXPAY') {
        this.$message.error('微信V2仅支持微信支付应用')
        return
      }
      this.payBtnDisabled = true
      wxPayApi.nativePayV2(this.payOrder.productId, this.payOrder.paymentAppId).then((response) => {
        this.codeUrl = response.data.codeUrl
        this.orderNo = response.data.orderNo
        this.codeDialogVisible = true
        this.startQueryTimer()
      }).catch(() => {
        this.payBtnDisabled = false
      })
    },

    startQueryTimer() {
      clearInterval(this.timer)
      this.timer = setInterval(() => {
        this.queryOrderStatus()
      }, 3000)
    },

    closeDialog() {
      this.payBtnDisabled = false
      clearInterval(this.timer)
    },

    queryOrderStatus() {
      orderInfoApi.queryOrderStatus(this.orderNo).then((response) => {
        if (response.code === 0) {
          clearInterval(this.timer)
          setTimeout(() => {
            this.$router.push({ path: '/success' })
          }, 3000)
        }
      })
    }
  }
}
</script>

<style scoped>
.empty-config-tip {
  padding: 24px 30px 30px;
  color: #909399;
  font-size: 14px;
}
</style>
