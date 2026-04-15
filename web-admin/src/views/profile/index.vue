<template>
  <div class="page-card">
    <div class="page-header">
      <div class="page-title">个人中心</div>
    </div>
    <el-row :gutter="24">
      <el-col :span="10">
        <el-descriptions title="个人信息" :column="1" border>
          <el-descriptions-item label="用户名">{{ userInfo.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ userInfo.nickname }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ userInfo.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ userInfo.deptName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ userInfo.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ userInfo.email || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <div slot="header">修改密码</div>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="form.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="form.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSubmit">提交修改</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  data() {
    return {
      form: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      rules: {
        oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
        newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
        confirmPassword: [{ required: true, message: '请输入确认密码', trigger: 'blur' }]
      }
    }
  },
  computed: {
    userInfo() {
      return this.$store.getters.userInfo || {}
    }
  },
  methods: {
    handleSubmit() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        await this.$store.dispatch('user/updateMyPassword', this.form)
        this.$message.success('密码修改成功，请重新登录')
        await this.$store.dispatch('user/logout')
        this.$router.push('/login')
      })
    },
    resetForm() {
      this.form = {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
    }
  }
}
</script>
