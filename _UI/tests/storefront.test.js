import { afterEach, beforeEach, describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus, { ElMessageBox } from 'element-plus'
import LoginForm from '../shared/components/LoginForm.vue'
import Catalog from '../storefront/src/views/Catalog.vue'
import Product from '../storefront/src/views/Product.vue'
import Cart from '../storefront/src/views/Cart.vue'
import Checkout from '../storefront/src/views/Checkout.vue'
import Payment from '../storefront/src/views/Payment.vue'
import Orders from '../storefront/src/views/Orders.vue'
import OrderDetail from '../storefront/src/views/OrderDetail.vue'
import Coupons from '../storefront/src/views/Coupons.vue'
import Account from '../storefront/src/views/Account.vue'
import Addresses from '../storefront/src/views/Addresses.vue'
import AfterSales from '../storefront/src/views/AfterSales.vue'

const id = '2095845066894499842'
const product = {
  id,
  productName: '测试商品',
  minPrice: 100,
  maxPrice: 100,
  status: 1,
  description: '商品介绍',
}
const item = {
  skuId: id,
  spuId: id,
  productName: '测试商品',
  skuName: '黑色',
  quantity: 1,
  selected: true,
  purchasable: true,
  subtotal: 100,
  salePrice: 100,
}
const order = {
  orderNo: 'ORDER-1',
  status: 1,
  items: [item],
  productAmount: 100,
  payAmount: 100,
  totalQuantity: 1,
  recipientName: '测试用户',
  recipientMobile: '13800000000',
  fullAddress: '测试地址',
}
let wrappers = [],
  api
beforeEach(() => {
  sessionStorage.clear()
  setActivePinia(createPinia())
  api = {
    get: vi.fn(async (url) => {
      if (url === '/products') return { records: [product], total: 1 }
      if (url === '/products/categories') return [{ id, categoryName: '分类', children: [] }]
      if (url === '/products/brands') return [{ id, brandName: '品牌' }]
      if (url.endsWith('/skus'))
        return [{ id, skuName: '黑色', salePrice: 100, specJson: '{"颜色":"黑色"}' }]
      if (url.startsWith('/products/')) return product
      if (url === '/users/me/cart')
        return { items: [item], selectedQuantity: 1, selectedAmount: 100 }
      if (url === '/users/me/cart/checkout')
        return { items: [item], totalQuantity: 1, productAmount: 100 }
      if (url === '/users/me/addresses')
        return [
          {
            id,
            recipientName: '测试用户',
            recipientMobile: '13800000000',
            defaultAddress: true,
            provinceCode: '110000',
            cityCode: '110100',
            districtCode: '110101',
            provinceName: '北京市',
            cityName: '市辖区',
            districtName: '东城区',
            detailAddress: '测试路1号',
          },
        ]
      if (url === '/users/me/membership')
        return { member: true, discountRate: 0.95, levelName: '普通会员' }
      if (url === '/users/me/coupons/usable')
        return [{ coupon: { id, couponName: '满减券' }, discountAmount: 5, payableAmount: 90 }]
      if (url === '/users/me/orders') return [order]
      if (url.startsWith('/users/me/orders/')) return order
      if (url === '/users/me') return { nickname: '测试用户', gender: 0 }
      if (url === '/users/me/after-sales') return { records: [], total: 0 }
      return []
    }),
    post: vi.fn(async (url) =>
      url.endsWith('/payments')
        ? { paymentNo: 'PAY-1', amount: 100 }
        : url === '/users/me/orders'
          ? order
          : { tokenName: 'Authorization', tokenValue: 'session' },
    ),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  }
})
afterEach(() => {
  wrappers.forEach((w) => w.unmount())
  wrappers = []
  document.body.innerHTML = ''
})
async function render(component, path = '/') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/:pathMatch(.*)*', component: { template: '<div />' } },
      { path: '/products/:id', component: { template: '<div />' } },
      { path: '/payment/:orderNo', component: { template: '<div />' } },
      { path: '/orders/:orderNo', component: { template: '<div />' } },
    ],
  })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(component, {
    attachTo: document.body,
    global: { plugins: [createPinia(), router, ElementPlus], provide: { api } },
  })
  wrappers.push(wrapper)
  await flushPromises()
  return { wrapper, router }
}
describe('商城页面', () => {
  for (const [name, component, path, text] of [
    ['商品列表', Catalog, '/', '测试商品'],
    ['商品详情', Product, `/products/${id}`, '选择规格'],
    ['购物车', Cart, '/cart', '测试商品'],
    ['结算', Checkout, '/checkout', '预计应付'],
    ['订单列表', Orders, '/orders', 'ORDER-1'],
    ['订单详情', OrderDetail, '/orders/ORDER-1', '测试商品'],
    ['模拟支付', Payment, '/payment/ORDER-1', '不会产生真实扣款'],
    ['优惠券', Coupons, '/coupons', '暂无可领取'],
    ['个人资料', Account, '/account', '普通会员'],
    ['收货地址', Addresses, '/addresses', '测试用户'],
    ['售后列表', AfterSales, '/after-sales', '售后服务'],
  ])
    it(`${name}按接口数据渲染`, async () => {
      const { wrapper } = await render(component, path)
      expect(wrapper.text()).toContain(text)
    })
  it('登录提交真实凭据并向父组件返回会话', async () => {
    const { wrapper } = await render(LoginForm)
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('13800000000')
    await inputs[1].setValue('password123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/auth/login', {
      phone: '13800000000',
      password: 'password123',
    })
    expect(wrapper.emitted('authenticated')[0][0].tokenValue).toBe('session')
  })
  it('结算使用会员折后金额查券，下单只传地址/券/备注', async () => {
    const { wrapper, router } = await render(Checkout, '/checkout')
    expect(api.get).toHaveBeenCalledWith('/users/me/coupons/usable', { amount: '95.00' })
    const select = wrapper.findComponent({ name: 'ElSelect' })
    select.vm.$emit('update:modelValue', id)
    await flushPromises()
    const submit = wrapper.findAll('button').find((b) => b.text() === '提交订单')
    await submit.trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/users/me/orders', {
      addressId: id,
      couponUserId: id,
      remark: undefined,
    })
    expect(router.currentRoute.value.path).toBe('/payment/ORDER-1')
  })
  it('后端失败有可见错误和重试入口，不展示伪造商品', async () => {
    api.get.mockRejectedValue(new Error('后端暂不可用'))
    const { wrapper } = await render(Catalog)
    expect(wrapper.text()).toContain('后端暂不可用')
    expect(wrapper.text()).toContain('重新加载')
    expect(wrapper.findAll('.product-card')).toHaveLength(0)
  })
  it('同一支付失败重试复用请求号，成功后清理', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    let attempts = 0
    api.post.mockImplementation(async (url) => {
      if (url.endsWith('/payments')) return { paymentNo: 'PAY-1', amount: 100 }
      if (url.endsWith('/mock-pay') && ++attempts === 1) throw new Error('请求超时')
      return { status: 2 }
    })
    const { wrapper, router } = await render(Payment, '/payment/ORDER-1')
    const pay = wrapper.findAll('button').find((b) => b.text() === '模拟支付成功')
    await pay.trigger('click')
    await flushPromises()
    const first = api.post.mock.calls.find(([url]) => url.endsWith('/mock-pay'))[1].requestNo
    expect(sessionStorage.getItem('mallx-pay-ORDER-1')).toBe(first)
    await pay.trigger('click')
    await flushPromises()
    const calls = api.post.mock.calls.filter(([url]) => url.endsWith('/mock-pay'))
    expect(calls).toHaveLength(2)
    expect(calls[1][1].requestNo).toBe(first)
    expect(sessionStorage.getItem('mallx-pay-ORDER-1')).toBeNull()
    expect(router.currentRoute.value.path).toBe('/orders/ORDER-1')
  })
  it('非会员可通过个人中心免费开通普通会员', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    api.get.mockImplementation(async (url) => {
      if (url === '/users/me') return { nickname: '测试用户', gender: 0 }
      if (url === '/users/me/membership')
        return { member: false, levelCode: 0, levelName: '非会员', discountRate: 1 }
      return []
    })
    api.post.mockResolvedValue({
      member: true,
      levelCode: 1,
      levelName: '普通会员',
      discountRate: 0.95,
    })
    const { wrapper } = await render(Account, '/account')
    const button = wrapper.findAll('button').find((item) => item.text() === '免费开通普通会员')
    await button.trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/users/me/membership/normal')
    expect(wrapper.text()).toContain('普通会员')
    expect(wrapper.text()).toContain('永久有效')
  })
  it('VIP开通提交1到36个月内的所选时长', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    api.post.mockResolvedValue({
      member: true,
      levelCode: 2,
      levelName: 'VIP会员',
      discountRate: 0.8,
      expireAt: '2027-09-05T12:00:00',
    })
    const { wrapper } = await render(Account, '/account')
    const upgrade = wrapper.findAll('button').find((item) => item.text() === '升级VIP')
    await upgrade.trigger('click')
    await flushPromises()
    const months = wrapper.findComponent({ name: 'ElRadioGroup' })
    months.vm.$emit('update:modelValue', 6)
    await flushPromises()
    const submit = wrapper.findAll('button').find((item) => item.text() === '确认模拟开通')
    await submit.trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/users/me/membership/vip', { durationMonths: 6 })
    expect(wrapper.text()).toContain('VIP会员')
    expect(wrapper.text()).toContain('VIP有效期至')
  })
})
