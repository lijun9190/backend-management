<template>
  <div class="error-page">
    <h1>403</h1>
    <p>抱歉，当前账号没有访问此页面或执行此操作的权限。</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<script>
export default {
  name: 'Error403'
}
</script>
