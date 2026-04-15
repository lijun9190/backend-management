<template>
  <div class="layout-shell">
    <Sidebar />
    <div class="layout-main">
      <Navbar />
      <div class="layout-body">
        <Breadcrumb />
        <AppMain />
      </div>
    </div>
  </div>
</template>

<script>
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import Breadcrumb from './components/Breadcrumb.vue'
import AppMain from './components/AppMain.vue'

export default {
  name: 'Layout',
  components: {
    Sidebar,
    Navbar,
    Breadcrumb,
    AppMain
  }
}
</script>
