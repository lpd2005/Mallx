import JSONbig from 'json-bigint'
import Decimal from 'decimal.js'

const parser = JSONbig({ storeAsString: true, protoAction: 'error', constructorAction: 'error' })
export function parseResponse(text) {
  return typeof text === 'string' && text.trim() ? parser.parse(text) : text
}
export const money = (value) => (value == null ? '—' : '¥' + new Decimal(value).toFixed(2))
export const dateTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 19) : '—')
export const memberAmount = (amount, rate = 1) =>
  new Decimal(amount || 0)
    .mul(rate ?? 1)
    .toDecimalPlaces(2, Decimal.ROUND_HALF_UP)
    .toFixed(2)
export const difference = (a, b) => new Decimal(a || 0).minus(b || 0).toFixed(2)
export const specText = (value) => {
  try {
    const v = JSON.parse(value)
    return Object.entries(v)
      .map(([k, x]) => `${k}：${typeof x === 'object' ? JSON.stringify(x) : x}`)
      .join(' / ')
  } catch {
    return value || ''
  }
}
export const safeRedirect = (value) =>
  typeof value === 'string' &&
  value.startsWith('/') &&
  !value.startsWith('//') &&
  !value.includes('\\') &&
  !value.startsWith('/login')
    ? value
    : '/'
export function imageSource(value) {
  try {
    const url = new URL(value)
    return url.protocol === 'https:' ? url.href : ''
  } catch {
    return ''
  }
}
export const orderStates = {
  1: '待支付',
  2: '已支付',
  3: '待发货',
  4: '已发货',
  5: '已完成',
  6: '已取消',
  7: '售后中',
  8: '已退款',
}
export const saleStates = {
  1: '待审核',
  2: '待退款',
  3: '待寄回',
  4: '待验收',
  5: '已完成',
  6: '已驳回',
  7: '已撤销',
}
export const productStates = { 0: '草稿', 1: '已上架', 2: '已下架' }
export const couponStates = { 1: '未使用', 2: '已锁定', 3: '已使用', 4: '已过期' }
export const templateStates = { 0: '草稿', 1: '发放中', 2: '已停止', 3: '已结束' }
export const options = (map) =>
  Object.entries(map).map(([value, label]) => ({ value: Number(value), label }))
export const requestNo = (prefix) => `${prefix}-${crypto.randomUUID()}`
