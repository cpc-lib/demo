<template>
  <main class="auth-page">
    <section class="auth-panel" aria-labelledby="auth-title">
      <div class="auth-copy">
        <span class="auth-mark">课程支付中心</span>
        <h1 id="auth-title">登录后统一管理购物车与订单</h1>
        <p>购物车按账号保存在服务端。刷新页面或重新登录后仍可继续结算。</p>
      </div>
      <div class="auth-form-wrap">
        <el-radio-group v-model="mode" class="auth-mode">
          <el-radio-button label="login">登录</el-radio-button>
          <el-radio-button label="register">注册</el-radio-button>
        </el-radio-group>
        <el-form ref="form" :model="form" :rules="rules" label-position="top" @submit.native.prevent="submit">
          <el-form-item label="用户名" prop="username">
            <el-input v-model.trim="form.username" autocomplete="username" maxlength="50" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password :autocomplete="mode === 'login' ? 'current-password' : 'new-password'" maxlength="72" />
          </el-form-item>
          <el-button type="primary" native-type="submit" :loading="submitting" class="full-button">
            {{ mode === 'login' ? '登录' : '创建账号' }}
          </el-button>
        </el-form>
      </div>
    </section>
  </main>
</template>

<script>
import authApi from '../api/auth'
import cartApi from '../api/cart'
import { authState, setSession } from '../auth/session'

export default {
  data() {
    return {
      mode: 'login',
      submitting: false,
      form: { username: '', password: '' },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 8, message: '密码至少 8 位', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    submit() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.submitting = true
        try {
          const response = this.mode === 'login'
            ? await authApi.login(this.form)
            : await authApi.register(this.form)
          setSession(response.data)
          const cartResponse = await cartApi.get()
          authState.cartCount = (cartResponse.data && cartResponse.data.totalQuantity) || 0
          this.$message.success(this.mode === 'login' ? '登录成功' : '注册成功')
          this.$router.replace(this.$route.query.redirect || '/')
        } finally {
          this.submitting = false
        }
      })
    }
  }
}
</script>
