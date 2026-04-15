<template>
  <el-submenu v-if="hasChildren" :index="fullPath">
    <template slot="title">
      <i :class="route.meta && route.meta.icon ? route.meta.icon : 'el-icon-menu'"></i>
      <span>{{ route.meta && route.meta.title }}</span>
    </template>
    <SidebarItem v-for="item in route.children" :key="item.path" :route="item" :base-path="fullPath" />
  </el-submenu>
  <el-menu-item v-else :index="fullPath">
    <i :class="route.meta && route.meta.icon ? route.meta.icon : 'el-icon-document'"></i>
    <span slot="title">{{ route.meta && route.meta.title }}</span>
  </el-menu-item>
</template>

<script>
export default {
  name: 'SidebarItem',
  props: {
    route: {
      type: Object,
      required: true
    },
    basePath: {
      type: String,
      default: ''
    }
  },
  computed: {
    hasChildren() {
      return this.route.children && this.route.children.length > 0
    },
    fullPath() {
      if (this.route.path.startsWith('/')) {
        return this.route.path
      }
      const base = this.basePath || ''
      return `${base}/${this.route.path}`.replace(/\/+/g, '/')
    }
  }
}
</script>
