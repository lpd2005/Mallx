// 只读联调：不注册账号、不上传文件、不创建订单、不更改业务数据。
import assert from 'node:assert/strict'
import { createApi } from '../shared/http.js'

const origins = process.argv.slice(2)
if (!origins.length)
  origins.push('http://localhost:8080', 'http://localhost:5173/api', 'http://localhost:5174/api')
let count = 0
for (const origin of origins) {
  const api = createApi({ baseURL: origin, session: () => null })
  const page = await api.get('/products', { current: 1, size: 2 })
  assert.ok(Array.isArray(page.records))
  assert.ok(Number(page.total) >= 0)
  count++
  const categories = await api.get('/products/categories'),
    brands = await api.get('/products/brands')
  assert.ok(Array.isArray(categories))
  assert.ok(Array.isArray(brands))
  count += 2
  if (page.records.length) {
    const product = page.records[0]
    assert.ok(typeof product.id === 'string' || Number.isSafeInteger(product.id))
    count++
    const detail = await api.get(`/products/${product.id}`)
    assert.equal(detail.id, product.id)
    assert.equal(detail.status, 1)
    count++
    const skus = await api.get(`/products/${product.id}/skus`)
    assert.ok(Array.isArray(skus))
    count++
    if (skus.length) {
      const sku = await api.get(`/products/skus/${skus[0].id}`)
      assert.equal(sku.id, skus[0].id)
      count++
    }
  }
  for (const path of ['/users/me/cart', '/admin/me']) {
    await assert.rejects(
      () => api.get(path),
      (error) => error.code === 401,
    )
    count++
  }
  console.log(`PASS ${origin}：商品、分类、品牌、详情、鉴权边界`)
}
console.log(`只读联调通过：${count} 项检查。`)
