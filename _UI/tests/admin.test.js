import { afterEach, beforeEach, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import Resource from '../admin/src/views/Resource.vue'
import Dashboard from '../admin/src/views/Dashboard.vue'
import Inventory from '../admin/src/views/Inventory.vue'
import Orders from '../admin/src/views/Orders.vue'
import AfterSales from '../admin/src/views/AfterSales.vue'
import Assignments from '../admin/src/views/Assignments.vue'
import { resources } from '../admin/src/resources.js'
import { useSession } from '../admin/src/services.js'
vi.mock('../admin/src/services.js', async (importOriginal) => ({
  ...(await importOriginal()),
  allPages: vi.fn(async () => []),
}))
let wrapper, api, pinia
beforeEach(() => {
  sessionStorage.clear()
  pinia = createPinia()
  setActivePinia(pinia)
  const session = useSession(pinia)
  session.admin = {
    userId: '1',
    roles: ['SYSTEM_ADMIN'],
    permissions: ['admin:access', 'product:manage', 'rbac:manage'],
  }
  api = {
    get: vi.fn(async (url) =>
      url === '/admin/dashboard'
        ? { paidAmount: 100, refundAmount: 120, netReceiptAmount: -20, paidOrderCount: 1 }
        : url.includes('top-products')
          ? []
          : url.includes('coupon-templates')
            ? []
            : { records: [], total: 0 },
    ),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  }
})
afterEach(() => {
  wrapper?.unmount()
  document.body.innerHTML = ''
})
async function render(component, meta = {}) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/', component: { template: '<div/>' }, meta }],
  })
  await router.push('/')
  await router.isReady()
  wrapper = mount(component, {
    attachTo: document.body,
    global: { plugins: [pinia, router, ElementPlus], provide: { api } },
  })
  await flushPromises()
  return wrapper
}
for (const [resource, config] of Object.entries(resources))
  it(`${config.title}正确加载对应接口`, async () => {
    await render(Resource, { resource })
    expect(api.get).toHaveBeenCalledWith(config.endpoint, expect.any(Object))
    expect(wrapper.text()).toContain(config.title)
  })
for (const [component, label] of [
  [Inventory, '库存管理'],
  [Orders, '订单管理'],
  [AfterSales, '售后管理'],
  [Assignments, '账号角色分配'],
])
  it(`${label}渲染`, async () => {
    await render(component)
    expect(wrapper.text()).toContain(label)
  })
it('看板保留负净收款，不截断为零', async () => {
  await render(Dashboard)
  expect(wrapper.text()).toContain('¥-20.00')
})
it('普通角色显示权限配置，保留角色只可查看', async () => {
  api.get.mockResolvedValue({
    records: [
      { id: '1', roleCode: 'SYSTEM_ADMIN', roleName: '系统管理员', status: 1 },
      { id: '2', roleCode: 'CUSTOM', roleName: '普通角色', status: 1 },
    ],
    total: 2,
  })
  await render(Resource, { resource: 'roles' })
  expect(wrapper.text()).toContain('查看权限')
  expect(wrapper.text()).toContain('权限配置')
  const editButtons = wrapper.findAll('button').filter((b) => b.text() === '编辑')
  expect(editButtons).toHaveLength(1)
})
