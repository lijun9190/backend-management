import { clearAuthTokens, getRefreshToken, getToken, setAuthTokens } from '../../utils/auth'
import { getProfile, login, logout, updateMyPassword } from '../../api/auth'
import { resetRouter } from '../../router'

const getDefaultState = () => ({
  token: getToken() || '',
  refreshToken: getRefreshToken() || '',
  userInfo: {},
  roles: [],
  permissions: [],
  menus: []
})

/**
 * 用户认证相关的Vuex模块
 * 包含用户登录、获取用户信息、登出等功能
 */
export default {
  namespaced: true,  // 启用命名空间，使该模块可以独立调用
  state: getDefaultState(),  // 初始化状态，使用默认状态函数
  /**
   * mutations用于修改状态
   * 同步操作，直接修改state
   */
  mutations: {
    /**
     * 重置状态到初始值
     * @param {Object} state - Vuex状态对象
     */
    RESET_STATE(state) {
      Object.assign(state, getDefaultState())
    },
    /**
     * 设置访问令牌和刷新令牌
     * @param {Object} state - Vuex状态对象
     * @param {Object} payload - 包含accessToken和refreshToken的对象
     */
    SET_TOKENS(state, payload) {
      state.token = payload.accessToken
      state.refreshToken = payload.refreshToken
    },
    /**
     * 设置用户资料信息
     * @param {Object} state - Vuex状态对象
     * @param {Object} payload - 用户资料信息对象，包含roles、permissions、menus等
     */
    SET_PROFILE(state, payload) {
      state.userInfo = payload
      state.roles = payload.roles || []
      state.permissions = payload.permissions || []
      state.menus = payload.menus || []
    }
  },
  /**
   * actions用于处理异步操作和提交mutations
   */
  actions: {
    /**
     * 用户登录
     * @param {Object} context - Vuex上下文对象，包含commit等
     * @param {Object} form - 登录表单数据
     */
    async login({ commit }, form) {
      const res = await login(form)
      const tokens = {
        accessToken: res.data.accessToken,
        refreshToken: res.data.refreshToken
      }
      setAuthTokens(tokens)
      commit('SET_TOKENS', tokens)
    },
    /**
     * 获取用户资料信息
     * @param {Object} context - Vuex上下文对象，包含commit等
     */
    async fetchProfile({ commit }) {
      const res = await getProfile()
      commit('SET_PROFILE', res.data)
      return res.data
    },
    /**
     * 清除会话
     * @param {Object} context - Vuex上下文对象，包含commit和dispatch
     */
    async clearSession({ commit, dispatch }) {
      clearAuthTokens()
      commit('RESET_STATE')
      await dispatch('permission/resetRoutes', null, { root: true })
      resetRouter()
    },
    /**
     * 用户登出
     * @param {Object} context - Vuex上下文对象，包含dispatch
     */
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
    /**
     * 更新用户密码
     * @param {Object} context - Vuex上下文对象
     * @param {Object} form - 包含新密码信息的表单
     */
    async updateMyPassword(_, form) {
      return updateMyPassword(form)
    }
  }
}
