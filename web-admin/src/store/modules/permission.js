import { buildAsyncRoutes } from '../../utils/route'

export default {
  namespaced: true,
  state: () => ({
    routes: [],
    routeLoaded: false
  }),
  mutations: {
    SET_ROUTES(state, routes) {
      state.routes = routes
      state.routeLoaded = true
    },
    RESET_ROUTES(state) {
      state.routes = []
      state.routeLoaded = false
    }
  },
  actions: {
    generateRoutes({ commit }, menus) {
      const routes = buildAsyncRoutes(menus)
      commit('SET_ROUTES', routes)
      return routes
    },
    resetRoutes({ commit }) {
      commit('RESET_ROUTES')
    }
  }
}
