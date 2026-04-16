import { clearAuthTokens, getRefreshToken, getToken, setAuthTokens } from '../../utils/auth'
import { getProfile, login, logout, updateMyPassword } from '../../api/auth'

const getDefaultState = () => ({
  token: getToken() || '',
  refreshToken: getRefreshToken() || '',
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
    SET_TOKENS(state, payload) {
      state.token = payload.accessToken
      state.refreshToken = payload.refreshToken
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
      const tokens = {
        accessToken: res.data.accessToken,
        refreshToken: res.data.refreshToken
      }
      setAuthTokens(tokens)
      commit('SET_TOKENS', tokens)
    },
    async fetchProfile({ commit }) {
      const res = await getProfile()
      commit('SET_PROFILE', res.data)
      return res.data
    },
    async clearSession({ commit, dispatch }) {
      clearAuthTokens()
      commit('RESET_STATE')
      await dispatch('permission/resetRoutes', null, { root: true })
    },
    async logout({ dispatch }) {
      try {
        await logout({
          skipAuthRefresh: true,
          skipUnauthorizedHandler: true
        })
      } finally {
        await dispatch('clearSession')
      }
    },
    async updateMyPassword(_, form) {
      return updateMyPassword(form)
    }
  }
}
