<template>
  <main class="container page-shell narrow-page">
    <header class="page-heading">
      <h1>账号安全</h1>
      <p>修改密码后，所有设备上的刷新会话都会失效。</p>
    </header>
    <section class="surface account-panel">
      <div class="account-name">
        <span>当前账号</span>
        <strong>{{ auth.user && auth.user.username }}</strong>
        <small>{{ auth.user && auth.user.role }}</small>
      </div>
      <el-form ref="form" :model="form" :rules="rules" label-position="top">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-button type="primary" @click="submit">修改密码</el-button>
      </el-form>
    </section>
  </main>
</template>

<script>
import authApi from '../api/auth'
import { authState, clearSession } from '../auth/session'

export default {
  data() {
    return {
      auth: authState,
      form: { oldPassword: '', newPassword: '' },
      rules: {
        oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 8, message: '密码至少 8 位', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    submit() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        await authApi.changePassword(this.form)
        try {
          await authApi.logout()
        } finally {
          clearSession()
          this.$message.success('密码已修改，请重新登录')
          this.$router.replace('/login')
        }
      })
    }
  }
}
</script>
