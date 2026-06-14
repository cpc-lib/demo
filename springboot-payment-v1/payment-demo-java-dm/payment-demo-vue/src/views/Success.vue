<template>
  <div class="bg-fa of">
    <section id="index" class="container">
      <div class="success-container">
        <div class="success-icon">
          <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10" />
            <polyline points="16 10 10 16 8 14" />
          </svg>
        </div>
        <h2>支付成功！</h2>
        <p class="success-tip">感谢您的购买，课程已开通</p>
        <div class="success-actions">
          <el-button type="primary" @click="goToOrders">查看订单</el-button>
          <el-button @click="goHome">返回首页</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
export default {
  methods: {
    goToOrders() {
      this.$router.push('/orders')
    },
    goHome() {
      this.$router.push('/')
    }
  }
}
</script>

<style scoped>
.success-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 20px;
    text-align: center;
}
.success-icon {
    color: #68cb9b;
    margin-bottom: 24px;
    animation: success-bounce 0.6s ease;
}
@keyframes success-bounce {
    0% { transform: scale(0); opacity: 0; }
    50% { transform: scale(1.1); }
    100% { transform: scale(1); opacity: 1; }
}
.success-container h2 {
    font-size: 24px;
    font-weight: 600;
    color: #353535;
    margin-bottom: 8px;
}
.success-tip {
    color: #909399;
    font-size: 14px;
    margin-bottom: 32px;
}
.success-actions {
    display: flex;
    gap: 16px;
}
.success-actions .el-button {
    width: 140px;
    height: 44px;
    border-radius: 8px;
}
</style>
