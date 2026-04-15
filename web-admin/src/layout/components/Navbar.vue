<template>
  <div class="navbar">
    <div class="navbar-left">
      <i class="el-icon-s-fold navbar-trigger" @click="toggleSidebar"></i>
      <span class="navbar-title">企业后台管理系统 Demo</span>
    </div>
    <div class="navbar-right">
      <el-dropdown>
        <span class="el-dropdown-link">
          {{ userInfo.nickname || userInfo.username || '未登录用户' }}
          <i class="el-icon-arrow-down el-icon--right"></i>
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item @click.native="goProfile">个人中心</el-dropdown-item>
          <el-dropdown-item divided @click.native="handleLogout">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
export default {
  computed: {
    userInfo() {
      return this.$store.getters.userInfo || {}
    }
  },
  methods: {
    toggleSidebar() {
      this.$store.dispatch('app/toggleSidebar')
    },
    goProfile() {
      this.$router.push('/profile')
    },
    async handleLogout() {
      await this.$store.dispatch('user/logout')
      await this.$store.dispatch('permission/resetRoutes')
      this.$router.push('/login')
    }
  }
}
</script>
