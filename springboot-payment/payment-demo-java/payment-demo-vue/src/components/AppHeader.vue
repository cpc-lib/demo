<template>
  <header id="header">
    <div class="container header-inner">
      <router-link to="/" class="brand" title="课程支付中心">
        <img src="../assets/img/logo.png" alt="课程支付中心">
      </router-link>
      <nav class="nav" aria-label="主导航">
        <router-link v-if="!isAdmin" to="/" exact>课程</router-link>
        <router-link v-if="auth.user" to="/orders">{{ isAdmin ? '全部订单' : '我的订单' }}</router-link>
        <router-link v-if="isAdmin" to="/refunds">退款审批</router-link>
        <router-link v-if="isAdmin" to="/download">账单</router-link>
        <router-link v-if="isAdmin" to="/payment-config">支付配置</router-link>
        <router-link v-if="isAdmin" to="/reconciliation">对账</router-link>
      </nav>
      <div class="header-actions">
        <template v-if="auth.user">
          <el-badge v-if="!isAdmin" :value="auth.cartCount" :hidden="!auth.cartCount" :max="99">
            <el-button @click="$router.push('/cart')">购物车</el-button>
          </el-badge>
          <el-dropdown trigger="click" @command="handleCommand">
            <el-button type="text">{{ auth.user.username }}</el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="account">账号安全</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
        <el-button v-else type="primary" @click="$router.push('/login')">登录</el-button>
      </div>
    </div>
  </header>
</template>

<script>
import authApi from '../api/auth'
import cartApi from '../api/cart'
import { authState, clearSession } from '../auth/session'

export default {
  computed: {
    auth() {
      return authState
    },
    isAdmin() {
      return authState.user && authState.user.role === 'ADMIN'
    }
  },
  watch: {
    'auth.user': {
      immediate: true,
      handler(user) {
        if (user && user.role === 'USER') {
          cartApi.get().then(response => {
            authState.cartCount = (response.data && response.data.totalQuantity) || 0
          }).catch(() => {
            authState.cartCount = 0
          })
        } else {
          authState.cartCount = 0
        }
      }
    }
  },
  methods: {
    async handleCommand(command) {
      if (command === 'account') {
        this.$router.push('/account')
        return
      }
      if (command === 'logout') {
        try {
          await authApi.logout()
        } finally {
          clearSession()
          this.$router.push('/login')
        }
      }
    }
  }
}
</script>
