import { createRouter, createWebHistory } from 'vue-router'
import { navigation } from './navigation.js'
import { api, useSession } from './services.js'
const pages = {
  '/': () => import('./views/Dashboard.vue'),
  '/inventory': () => import('./views/Inventory.vue'),
  '/orders': () => import('./views/Orders.vue'),
  '/after-sales': () => import('./views/AfterSales.vue'),
  '/assignments': () => import('./views/Assignments.vue'),
  '/search': () => import('./views/SearchOps.vue'),
  '/seckill': () => import('./views/Seckill.vue'),
}
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('./views/Login.vue'),
      meta: { public: true, title: '管理员登录' },
    },
    {
      path: '/access',
      component: () => import('./views/Access.vue'),
      meta: { public: true, title: '访问提示' },
    },
    ...navigation.map((n) => ({
      path: n.path,
      component: n.resource ? () => import('./views/Resource.vue') : pages[n.path],
      meta: n,
    })),
    {
      path: '/:pathMatch(.*)*',
      component: () => import('@mallx/shared/components/NotFound.vue'),
      meta: { public: true, title: '页面不存在' },
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
})
router.beforeEach(async (to) => {
  if (to.meta.public) return
  const session = useSession()
  if (!session.loggedIn) return { path: '/login', query: { redirect: to.fullPath } }
  try {
    session.admin = await api.get('/admin/me')
  } catch (e) {
    return e.code === 401
      ? { path: '/login', query: { redirect: to.fullPath } }
      : { path: '/access', query: { reason: e.message } }
  }
  if (!session.can(to.meta.permission)) {
    if (to.path === '/') {
      const first = navigation.find((n) => session.can(n.permission))
      if (first) return first.path
    }
    return { path: '/access', query: { reason: '当前账号没有此页面的访问权限' } }
  }
})
router.afterEach((to) => {
  document.title = `${to.meta.title || '管理平台'} · MallX`
})
export default router
