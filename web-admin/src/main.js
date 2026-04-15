import Vue from 'vue'
import ElementUI, { Message } from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import App from './App.vue'
import router from './router'
import store from './store'
import permission from './directive/permission'
import './styles/index.css'
import { resolveDefaultRoutePath } from './utils/route'

Vue.use(ElementUI)
Vue.directive('permission', permission)
Vue.config.productionTip = false

router.beforeEach(async (to, from, next) => {
  const hasToken = store.getters.token
  if (hasToken) {
    const noRoleMessage = '当前账号未分配角色，请联系管理员'
    const resolveHomeRoute = () => resolveDefaultRoutePath(store.getters.menus)
    if (to.path === '/login') {
      const homeRoute = resolveHomeRoute()
      if (!homeRoute) {
        Message.error(noRoleMessage)
        await store.dispatch('user/logout')
        next('/login')
        return
      }
      next({ path: homeRoute })
      return
    }
    if (!store.getters.routeLoaded) {
      try {
        await store.dispatch('user/fetchProfile')
        const routes = await store.dispatch('permission/generateRoutes', store.getters.menus)
        routes.forEach(route => router.addRoute(route))
        const homeRoute = resolveHomeRoute()
        if (!homeRoute) {
          throw new Error(noRoleMessage)
        }
        next({ path: to.path === '/' ? homeRoute : to.path, replace: true, query: to.query })
      } catch (error) {
        if (error && error.message === noRoleMessage) {
          Message.error(error.message)
        }
        await store.dispatch('user/logout')
        next(`/login?redirect=${to.path}`)
      }
      return
    }
    if (to.path === '/') {
      const homeRoute = resolveHomeRoute()
      if (!homeRoute) {
        Message.error(noRoleMessage)
        await store.dispatch('user/logout')
        next('/login')
        return
      }
      next({ path: homeRoute, replace: true })
      return
    }
    next()
    return
  }

  if (to.meta && to.meta.publicPage) {
    next()
    return
  }
  next(`/login?redirect=${to.path}`)
})

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
