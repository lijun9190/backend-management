<template>
  <div class="sidebar" :class="{ collapse: sidebarCollapse }">
    <div class="sidebar-logo">Admin Demo</div>
    <el-menu
      :collapse="sidebarCollapse"
      :default-active="$route.path"
      background-color="#0f172a"
      text-color="#cbd5e1"
      active-text-color="#f8fafc"
      router
      unique-opened>
      <template v-for="route in routes">
        <SidebarItem :key="route.path" :route="route" />
      </template>
    </el-menu>
  </div>
</template>

<script>
import SidebarItem from './SidebarItem.vue'

export default {
  name: 'Sidebar',
  components: { SidebarItem },
  computed: {
    routes() {
      return this.$store.getters.routes
    },
    sidebarCollapse() {
      return this.$store.getters.sidebarCollapse
    }
  }
}
</script>
