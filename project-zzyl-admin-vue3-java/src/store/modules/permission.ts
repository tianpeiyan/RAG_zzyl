import { defineStore } from 'pinia'
import { RouteRecordRaw } from 'vue-router'
import router, { asyncRouterList } from '@/router'
import { store, useUserStore } from '@/store'
import { routerFormat } from '@/utils'
import fixRouter from '@/router/modules/fixRouter'

function normalizePath(path = '') {
  if (!path) return ''
  return path.startsWith('/') ? path : `/${path}`
}

function joinPath(parentPath = '', childPath = '') {
  const normalizedParent = normalizePath(parentPath)
  if (!childPath) return normalizedParent
  if (childPath.startsWith('/')) return childPath
  return `${normalizedParent}/${childPath}`.replace(/\/+/g, '/')
}

const localRouteMap = new Map<string, RouteRecordRaw>()

function collectLocalRoutes(routes: RouteRecordRaw[], parentPath = '') {
  routes.forEach((route) => {
    const fullPath = joinPath(parentPath, route.path)
    localRouteMap.set(fullPath, route)
    if (route.children?.length) {
      collectLocalRoutes(route.children, fullPath)
    }
  })
}

collectLocalRoutes(asyncRouterList)

const assistantDynamicRoute = {
  path: '/assistant',
  name: 'assistant',
  redirect: '/assistant/chat',
  meta: { title: '智能助手', icon: '', single: true, orderNo: 99 },
  children: [
    {
      path: '/assistant/chat',
      name: 'assistantChat',
      meta: { title: '智能问答' },
      children: []
    },
    {
      path: '/assistant/kb',
      name: 'assistantKb',
      meta: { title: '知识库管理' },
      children: []
    },
    {
      path: '/assistant/evidence',
      name: 'assistantEvidence',
      meta: { title: '命中详情', hidden: true },
      children: []
    }
  ]
}

function hydrateDynamicRoutes(routes: Array<any> = []) {
  return routes.map((route) => {
    const localRoute = localRouteMap.get(normalizePath(route.path))
    const localChildren =
      localRoute?.children?.map((child) => {
        const fullPath = joinPath(localRoute.path, child.path)
        return {
          ...child,
          path: fullPath,
          meta: {
            ...child.meta,
            title: child.meta?.title,
            hidden: child.meta?.hidden
          },
          children: []
        }
      }) || []
    const routeChildren =
      localChildren.length > 0
        ? localChildren
        : route.children?.length > 0
        ? route.children
        : []
    return {
      ...route,
      path: normalizePath(route.path),
      redirect: route.redirect || localRoute?.redirect || routeChildren[0]?.path,
      meta: {
        ...(localRoute?.meta || {}),
        ...(route.meta || {}),
        title: route.meta?.title || localRoute?.meta?.title || route.name,
        single: route.meta?.single ?? localRoute?.meta?.single ?? true
      },
      children: routeChildren
    }
  })
}

function ensureAssistantRoute(routes: Array<any> = []) {
  if (routes.some((route) => route.path === '/assistant')) {
    return routes
  }
  return [...routes, assistantDynamicRoute]
}

function filterPermissionsRouters(
  routes: Array<RouteRecordRaw>,
  roles: Array<unknown>
) {
  const res = []
  const removeRoutes = []
  routes.forEach((route) => {
    const children = []
    route.children?.forEach((childRouter) => {
      const roleCode = childRouter.meta?.roleCode || childRouter.name
      if (roles.indexOf(roleCode) !== -1) {
        children.push(childRouter)
      } else {
        removeRoutes.push(childRouter)
      }
    })
    if (children.length > 0) {
      route.children = children
      res.push(route)
    }
  })
  return { accessedRouters: res, removeRoutes }
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    whiteListRouters: ['/login'],
    isDynamic: false, // 是否使用动态路由
    routers: [], // 静态路由
    dynamic: [], // 动态路由
    removeRoutes: [],
    childrenRoutes: [] // 左侧子菜单路由
  }),
  actions: {
    // 用于style风格中的tooMany当页面刷新时， 左侧菜单数据保存,并且将路由的二级路由带parent字段的整合到parent对应的二级路由中作为三级路由
    initCurrentRoutes() {
      if (this.isDynamic) {
        this.dynamic = ensureAssistantRoute(this.dynamic)
      }
      const routeArr = this.isDynamic
        ? this.dynamic.concat(fixRouter) // 给动态路由添加不需要配置的固定路由
        : this.routers
      console.log(routeArr, 'routeArr')
      // 动态路由(动态路由配置时需要保证菜单路径和本地路由路径统一)
      const childrenRoutes = routeArr.filter((item) =>
        window.location.hash.includes(item.path)
      )
      const currentRoutes = childrenRoutes.length
        ? childrenRoutes[0].children
        : []
      this.childrenRoutes = currentRoutes
    },
    // 根据roles的角色权限动态处理路由
    async initRoutes(roles: Array<unknown>) {
      // 从后端拿到的动态路由信息
      const routeStore = useUserStore()
      const baseRouter = routerFormat(routeStore.$state.trendsRoute)

      let accessedRouters = []
      let removeRoutes = []
      if (roles.includes('all')) {
        accessedRouters = asyncRouterList // 这里是控制权限菜单 默
      } else {
        const res = filterPermissionsRouters(baseRouter, roles)
        accessedRouters = res.accessedRouters
        removeRoutes = res.removeRoutes
      }
      this.routers = accessedRouters
      this.removeRoutes = removeRoutes

      removeRoutes.forEach((item: RouteRecordRaw) => {
        if (router.hasRoute(item.name)) {
          router.removeRoute(item.name)
        }
      })
    },
    async restore() {
      this.removeRoutes.forEach((item: RouteRecordRaw) => {
        router.addRoute(item)
      })
    },
    setRouters(data: any) {
      this.dynamic = ensureAssistantRoute(hydrateDynamicRoutes(data))
    }
  },
  persist: true
})

export function getPermissionStore() {
  return usePermissionStore(store)
}
