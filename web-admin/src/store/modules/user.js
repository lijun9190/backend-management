import { getToken, removeToken, setToken } from '../../utils/auth'
import { getProfile, login, logout, updateMyPassword } from '../../api/auth'

const getDefaultState = () => ({
  token: getToken() || '',
  userInfo: {},
  roles: [],
  permissions: [],
  menus: []
})

export default {
  namespaced: true,
  state: getDefaultState(),
  mutations: {
    RESET_STATE(state) {
      Object.assign(state, getDefaultState())
    },
    SET_TOKEN(state, token) {
      state.token = token
    },
    SET_PROFILE(state, payload) {
      state.userInfo = payload
      state.roles = payload.roles || []
      state.permissions = payload.permissions || []
      state.menus = payload.menus || []
    }
  },
  actions: {
    async login({ commit }, form) {
      const res = await login(form)
      setToken(res.data.accessToken)
      commit('SET_TOKEN', res.data.accessToken)
    },
    async fetchProfile({ commit }) {
      const res = await getProfile()
      commit('SET_PROFILE', res.data)
      return res.data
    },
    async logout({ commit }) {
      try {
        await logout()
      } finally {
        removeToken()
        commit('RESET_STATE')
      }
    },
    async updateMyPassword(_, form) {
      return updateMyPassword(form)
    }
  }
}
