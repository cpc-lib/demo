<template>
  <main class="catalog-page">
    <section class="catalog-intro container">
      <div>
        <span class="catalog-kicker">课程目录</span>
        <h1>一次选择，多门课程一起结算</h1>
        <p>课程可重复添加并在购物车调整份数。结算价格以创建订单时的最新价格为准。</p>
      </div>
      <div class="catalog-summary" aria-label="购买流程">
        <strong>选择课程</strong>
        <span>加入服务端购物车</span>
        <span>合并创建一笔订单</span>
      </div>
    </section>

    <section class="container catalog-content" :aria-busy="loading">
      <div v-if="loading" class="course-grid">
        <div class="surface loading-surface">正在加载课程...</div>
        <div class="surface loading-surface">正在加载课程...</div>
      </div>
      <div v-else-if="products.length" class="course-grid">
        <article v-for="(product, index) in products" :key="product.id" :class="['course-card', `course-card-${index % 2}`]">
          <div class="course-index">{{ String(index + 1).padStart(2, '0') }}</div>
          <div>
            <h2>{{ product.title }}</h2>
            <p>购买后可在订单中查看本次课程组合与数量快照。</p>
          </div>
          <div class="course-buy-row">
            <strong>¥{{ (product.price / 100).toFixed(2) }}</strong>
            <el-button type="primary" :loading="addingId === product.id" @click="addToCart(product)">加入购物车</el-button>
          </div>
        </article>
      </div>
      <div v-else class="surface empty-surface">
        <el-empty description="暂无可购买课程" />
      </div>
    </section>
  </main>
</template>

<script>
import productApi from '../api/product'
import cartApi from '../api/cart'
import { authState } from '../auth/session'

export default {
  data() {
    return {
      loading: true,
      addingId: null,
      products: []
    }
  },
  created() {
    if (authState.user && authState.user.role === 'ADMIN') {
      this.$router.replace('/orders')
      return
    }
    productApi.list().then(response => {
      this.products = (response.data && response.data.productList) || []
    }).catch(() => {
      this.products = []
    }).finally(() => {
      this.loading = false
    })
  },
  methods: {
    async addToCart(product) {
      if (!authState.user) {
        this.$router.push({ path: '/login', query: { redirect: '/' } })
        return
      }
      this.addingId = product.id
      try {
        await cartApi.add(product.id, 1)
        const response = await cartApi.get()
        authState.cartCount = (response.data && response.data.totalQuantity) || 0
        this.$message.success(`已将“${product.title}”加入购物车`)
      } finally {
        this.addingId = null
      }
    }
  }
}
</script>
