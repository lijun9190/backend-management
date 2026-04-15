<template>
  <div class="login-page">
    <div class="login-banner">
      <div class="login-copy">
        <div class="eyebrow">RBAC · Gateway · JWT · Redis</div>
        <h1>企业后台管理系统 Demo</h1>
        <p>统一网关入口、动态路由、按钮权限、接口二次鉴权，全链路打通。</p>
        <ul>
          <li>支持超级管理员、系统管理员、运营、审计角色</li>
          <li>支持菜单、页面、按钮三级权限控制</li>
          <li>支持登录日志、操作日志与个人中心</li>
        </ul>
      </div>
    </div>
    <div class="login-panel">
      <el-card shadow="hover" class="login-card">
        <div class="login-title">欢迎登录</div>
        <div class="login-subtitle">默认密码均为 `Admin@123456`</div>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="el-icon-user" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" prefix-icon="el-icon-lock" @keyup.enter.native="handleLogin" />
          </el-form-item>
          <el-button :loading="loading" type="primary" class="login-btn" @click="handleLogin">登录系统</el-button>
        </el-form>
        <div class="account-tip">
          可用账号：`admin` / `sysadmin` / `operator` / `auditor`
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      loading: false,
      form: {
        username: 'admin',
        password: 'Admin@123456'
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      }
    }
  },
  methods: {
    isIgnorableNavigationError(error) {
      if (!error) {
        return false
      }
      return error.name === 'NavigationDuplicated' ||
        error.name === 'NavigationRedirected' ||
        /Redirected when going from/.test(error.message || '')
    },
    handleLogin() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        this.loading = true
        try {
          await this.$store.dispatch('user/login', this.form)
          const redirect = this.$route.query.redirect || '/'
          try {
            await this.$router.push(redirect)
          } catch (error) {
            if (!this.isIgnorableNavigationError(error)) {
              throw error
            }
          }
        } catch (error) {
          if (!this.isIgnorableNavigationError(error)) {
            return
          }
        } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  background: radial-gradient(circle at top left, rgba(16, 185, 129, 0.18), transparent 32%),
    linear-gradient(135deg, #e6edf7 0%, #f7fafc 50%, #e9f0f7 100%);
}

.login-banner {
  padding: 72px;
  display: flex;
  align-items: center;
}

.login-copy h1 {
  font-size: 48px;
  line-height: 1.1;
  margin: 12px 0 20px;
}

.login-copy p,
.login-copy li {
  color: #475569;
  line-height: 1.8;
}

.eyebrow {
  color: #0f766e;
  font-weight: 700;
  letter-spacing: 2px;
}

.login-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-card {
  width: 420px;
  border-radius: 24px;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.login-subtitle {
  color: #64748b;
  margin-bottom: 24px;
}

.login-btn {
  width: 100%;
  margin-top: 12px;
}

.account-tip {
  margin-top: 20px;
  font-size: 13px;
  color: #64748b;
}
</style>
