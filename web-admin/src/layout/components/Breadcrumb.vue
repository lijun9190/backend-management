<template>
  <div class="breadcrumb-wrap">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item v-for="item in breadcrumbList" :key="item.path">
        {{ item.meta && item.meta.title ? item.meta.title : item.name }}
      </el-breadcrumb-item>
    </el-breadcrumb>
  </div>
</template>

<script>
export default {
  computed: {
    breadcrumbList() {
      return this.$route.matched.filter(item => item.meta && item.meta.title)
    }
  }
}
</script>
