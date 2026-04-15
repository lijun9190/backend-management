export default {
  namespaced: true,
  state: () => ({
    sidebarCollapse: false
  }),
  mutations: {
    TOGGLE_SIDEBAR(state) {
      state.sidebarCollapse = !state.sidebarCollapse
    }
  },
  actions: {
    toggleSidebar({ commit }) {
      commit('TOGGLE_SIDEBAR')
    }
  }
}
