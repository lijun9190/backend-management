<template>
  <div class="error-page">
    <h1>404</h1>
    <p>页面不存在，可能是路由未配置或访问地址有误。</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<script>
export default {
  name: 'Error404'
}
</script>
