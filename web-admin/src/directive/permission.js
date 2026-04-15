import store from '../store'

/**
 * 按钮权限指令。
 *
 * 前端只做显示控制，真正权限仍以后端接口校验为准。
 */
export default {
  inserted(el, binding) {
    const permissionCode = binding.value
    const permissions = store.getters.permissions || []
    const isSuperAdmin = store.getters.isSuperAdmin
    if (!isSuperAdmin && permissionCode && !permissions.includes(permissionCode)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}
