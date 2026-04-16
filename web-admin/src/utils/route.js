import Layout from '../layout/index.vue'

const viewModules = {
  'dashboard/index': () => import('../views/dashboard/index.vue'),
  'system/user/index': () => import('../views/system/user/index.vue'),
  'system/role/index': () => import('../views/system/role/index.vue'),
  'system/menu/index': () => import('../views/system/menu/index.vue'),
  'system/dept/index': () => import('../views/system/dept/index.vue'),
  'log/login/index': () => import('../views/log/login/index.vue'),
  'log/operation/index': () => import('../views/log/operation/index.vue'),
  'profile/index': () => import('../views/profile/index.vue')
}

/**
 * 解析组件函数
 * 根据传入的组件名称返回对应的组件实例
 * 如果找不到对应组件，则返回默认的仪表盘组件
 *
 * @param {string} component - 组件名称
 * @returns {Object} 对应的组件实例
 */
function resolveComponent(component) {
  // 如果组件名称为'Layout'，则直接返回Layout组件
  if (component === 'Layout') {
    return Layout
  }
  // 从viewModules对象中查找对应组件，如果找不到则返回默认的仪表盘组件
  return viewModules[component] || viewModules['dashboard/index']
}

export function resolveRoutePath(route, basePath = '') {
  if (!route || !route.path) {
    return basePath || '/'
  }
  if (route.path.startsWith('/')) {
    return route.path
  }
  return `${basePath}/${route.path}`.replace(/\/+/g, '/')
}

/**
 * 将菜单节点映射为路由对象
 * @param {Object} node - 菜单节点对象
 * @returns {Object} - 路由配置对象
 */
function mapNodeToRoute(node) {
  // 创建基础路由配置对象
  const route = {
    path: node.path, // 路由路径
    name: node.routeName || `${node.menuType}_${node.id}`, // 路由名称，如果未提供则使用菜单类型和ID组合
    component: resolveComponent(node.component), // 解析组件
    meta: { // 路由元信息
      title: node.name || node.menuName, // 路由标题
      icon: node.icon, // 路由图标
      permissionCode: node.permissionCode // 权限代码
    }
  }

  // 处理子路由
  if (node.children && node.children.length > 0) {
    // 过滤掉按钮类型的菜单和不可见的菜单
    route.children = node.children
      .filter(item => item.menuType !== 'BUTTON' && item.visible !== 0 && item.status !== 0)
      .map(item => mapNodeToRoute(item)) // 递归处理子节点
  }

  // 如果组件是布局组件
  if (route.component === Layout) {
    route.children = route.children || [] // 确保children数组存在
    if (route.children.length > 0) {
      const firstChild = route.children[0] // 获取第一个子路由
      route.redirect = resolveRoutePath(firstChild, route.path) // 设置重定向路径
    }
  }

  return route // 返回处理后的路由配置对象
}

export function buildAsyncRoutes(menus) {
  return (menus || [])
    .filter(item => item.menuType !== 'BUTTON' && item.visible !== 0 && item.status !== 0)
    .map(item => mapNodeToRoute(item))
}

/**
 * 解析默认路由路径
 * @param {Array} menus - 菜单数据数组
 * @returns {string|null} 返回解析后的路由路径，如果没有则返回null
 */
export function resolveDefaultRoutePath(menus) {
  // 根据菜单数据构建异步路由
  const routes = buildAsyncRoutes(menus)
  // 查找第一个叶子节点路由
  const firstLeaf = findFirstLeafRoute(routes)
  // 如果存在叶子节点路由，则解析并返回其路径，否则返回null
  return firstLeaf ? resolveRoutePath(firstLeaf) : null
}

/**
 * 查找路由树中的第一个叶子节点路由
 * @param {Array} routes - 路由配置数组
 * @param {string} basePath - 基础路径，默认为空字符串
 * @returns {Object|null} 返回第一个叶子节点路由对象，如果没有则返回null
 */
function findFirstLeafRoute(routes, basePath = '') {
  // 遍历路由数组，如果routes为空则直接跳过
  for (const route of routes || []) {
    // 解析当前路由的完整路径
    const fullPath = resolveRoutePath(route, basePath)
    // 如果当前路由有子路由且子路由数量大于0
    if (route.children && route.children.length > 0) {
      // 递归查找子路由中的第一个叶子节点
      const childLeaf = findFirstLeafRoute(route.children, fullPath)
      // 如果找到叶子节点，则直接返回
      if (childLeaf) {
        return childLeaf
      }
    } else {
      // 如果是叶子节点（没有子路由），则返回当前路由信息，包含完整路径
      return {
        ...route,
        path: fullPath
      }
    }
  }
  // 如果遍历完所有路由都没有找到叶子节点，则返回null
  return null
}
