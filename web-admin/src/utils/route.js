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

function resolveComponent(component) {
  if (component === 'Layout') {
    return Layout
  }
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

function mapNodeToRoute(node) {
  const route = {
    path: node.path,
    name: node.routeName || `${node.menuType}_${node.id}`,
    component: resolveComponent(node.component),
    meta: {
      title: node.name || node.menuName,
      icon: node.icon,
      permissionCode: node.permissionCode
    }
  }

  if (node.children && node.children.length > 0) {
    route.children = node.children
      .filter(item => item.menuType !== 'BUTTON' && item.visible !== 0 && item.status !== 0)
      .map(item => mapNodeToRoute(item))
  }

  if (route.component === Layout) {
    route.children = route.children || []
    if (route.children.length > 0) {
      const firstChild = route.children[0]
      route.redirect = resolveRoutePath(firstChild, route.path)
    }
  }

  return route
}

export function buildAsyncRoutes(menus) {
  return (menus || [])
    .filter(item => item.menuType !== 'BUTTON' && item.visible !== 0 && item.status !== 0)
    .map(item => mapNodeToRoute(item))
}

export function resolveDefaultRoutePath(menus) {
  const routes = buildAsyncRoutes(menus)
  const firstLeaf = findFirstLeafRoute(routes)
  return firstLeaf ? resolveRoutePath(firstLeaf) : null
}

function findFirstLeafRoute(routes, basePath = '') {
  for (const route of routes || []) {
    const fullPath = resolveRoutePath(route, basePath)
    if (route.children && route.children.length > 0) {
      const childLeaf = findFirstLeafRoute(route.children, fullPath)
      if (childLeaf) {
        return childLeaf
      }
    } else {
      return {
        ...route,
        path: fullPath
      }
    }
  }
  return null
}
