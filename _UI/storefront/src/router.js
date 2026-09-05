import { createRouter, createWebHistory } from 'vue-router'
import Catalog from './views/Catalog.vue'
import { useSession } from './services.js'
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Catalog, meta: { title: '发现好物' } },
    { path: '/login', component: () => import('./views/Login.vue'), meta: { title: '登录' } },
    {
      path: '/products/:id',
      component: () => import('./views/Product.vue'),
      meta: { title: '商品详情' },
    },
    {
      path: '/cart',
      component: () => import('./views/Cart.vue'),
      meta: { title: '购物车', auth: true },
    },
    {
      path: '/checkout',
      component: () => import('./views/Checkout.vue'),
      meta: { title: '确认订单', auth: true },
    },
    {
      path: '/orders',
      component: () => import('./views/Orders.vue'),
      meta: { title: '我的订单', auth: true },
    },
    {
      path: '/orders/:orderNo',
      component: () => import('./views/OrderDetail.vue'),
      meta: { title: '订单详情', auth: true },
    },
    {
      path: '/payment/:orderNo',
      component: () => import('./views/Payment.vue'),
      meta: { title: '收银台', auth: true },
    },
    {
      path: '/coupons',
      component: () => import('./views/Coupons.vue'),
      meta: { title: '优惠券', auth: true },
    },
    {
      path: '/account',
      component: () => import('./views/Account.vue'),
      meta: { title: '个人中心', auth: true },
    },
    {
      path: '/addresses',
      component: () => import('./views/Addresses.vue'),
      meta: { title: '收货地址', auth: true },
    },
    {
      path: '/after-sales',
      component: () => import('./views/AfterSales.vue'),
      meta: { title: '售后服务', auth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      component: () => import('@mallx/shared/components/NotFound.vue'),
      meta: { title: '页面不存在' },
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
})
router.beforeEach((to) => {
  if (to.meta.auth && !useSession().loggedIn)
    return { path: '/login', query: { redirect: to.fullPath } }
})
router.afterEach((to) => {
  document.title = `${to.meta.title || '商城'} · MallX`
})
export default router
