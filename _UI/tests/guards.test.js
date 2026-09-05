import { beforeEach, it, expect, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import axios from 'axios'
import adminRouter from '../admin/src/router.js'
import { initApi, useSession } from '../admin/src/services.js'
import storeRouter from '../storefront/src/router.js'
import { useSession as useStoreSession } from '../storefront/src/services.js'

let permissions
beforeEach(async () => {
  sessionStorage.clear()
  const pinia = createPinia()
  setActivePinia(pinia)
  permissions = ['admin:access', 'product:manage']
  axios.defaults.adapter = vi.fn(async (config) => ({
    status: 200,
    headers: {},
    config,
    data: JSON.stringify({ code: 200, data: { userId: '9', roles: ['OPERATOR'], permissions } }),
  }))
  initApi(adminRouter, pinia)
  await adminRouter.push('/login')
  await storeRouter.push('/')
})
it('商城私有页面在无会话时跳转登录并保留原路径', async () => {
  await storeRouter.push('/checkout')
  expect(storeRouter.currentRoute.value.path).toBe('/login')
  expect(storeRouter.currentRoute.value.query.redirect).toBe('/checkout')
})
it('游客可直接浏览商品详情', async () => {
  await storeRouter.push('/products/2095845066894499842')
  expect(storeRouter.currentRoute.value.path).toBe('/products/2095845066894499842')
})
it('管理端无会话时不能进入后台', async () => {
  await adminRouter.push('/roles')
  expect(adminRouter.currentRoute.value.path).toBe('/login')
})
it('按当前权限拒绝角色管理，并允许商品管理', async () => {
  useSession().save({ tokenName: 'Authorization', tokenValue: 'test' })
  await adminRouter.push('/roles')
  expect(adminRouter.currentRoute.value.path).toBe('/access')
  await adminRouter.push('/products')
  expect(adminRouter.currentRoute.value.path).toBe('/products')
})
it('无看板权限时首页跳转到第一个获授权页面', async () => {
  useSession().save({ tokenName: 'Authorization', tokenValue: 'test' })
  await adminRouter.push('/')
  expect(adminRouter.currentRoute.value.path).toBe('/products')
})
it('权限回收后下一次导航即时失效', async () => {
  useSession().save({ tokenName: 'Authorization', tokenValue: 'test' })
  await adminRouter.push('/products')
  permissions = ['admin:access']
  await adminRouter.push('/skus')
  expect(adminRouter.currentRoute.value.path).toBe('/access')
})
it('两个应用不共用会话', () => {
  useStoreSession().save({ tokenName: 'Authorization', tokenValue: 'store-token' })
  expect(useSession().loggedIn).toBe(false)
  useSession().save({ tokenName: 'Authorization', tokenValue: 'admin-token' })
  useSession().clear()
  expect(useStoreSession().auth.tokenValue).toBe('store-token')
})
