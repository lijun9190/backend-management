import Vue from 'vue'
import Vuex from 'vuex'
import user from './modules/user'
import permission from './modules/permission'
import app from './modules/app'

Vue.use(Vuex)

export default new Vuex.Store({
  modules: {
    app,
    user,
    permission
  },
  getters: {
    token: state => state.user.token,
    userInfo: state => state.user.userInfo,
    roles: state => state.user.roles,
    permissions: state => state.user.permissions,
    menus: state => state.user.menus,
    routeLoaded: state => state.permission.routeLoaded,
    routes: state => state.permission.routes,
    sidebarCollapse: state => state.app.sidebarCollapse,
    isSuperAdmin: state => state.user.roles.includes('SUPER_ADMIN')
  }
})
