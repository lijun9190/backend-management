import Vue from 'vue'
import VueRouter from 'vue-router'
import Layout from '../layout/index.vue'

Vue.use(VueRouter)

export const constantRoutes = [
  {
    path: '/login',
    component: () => import('../views/login/index.vue'),
    meta: { publicPage: true }
  },
  {
    path: '/403',
    component: () => import('../views/error/403.vue'),
    meta: { publicPage: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard/index',
    children: []
  },
  {
    path: '*',
    component: () => import('../views/error/404.vue')
  }
]

const router = new VueRouter({
  mode: 'hash',
  routes: constantRoutes,
  scrollBehavior: () => ({ y: 0 })
})

export default router
