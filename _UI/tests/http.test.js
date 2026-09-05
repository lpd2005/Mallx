import { beforeEach, describe, it, expect, vi } from 'vitest'
import axios, { AxiosError } from 'axios'
import { createApi } from '../shared/http.js'
import {
  parseResponse,
  memberAmount,
  difference,
  safeRedirect,
  imageSource,
} from '../shared/format.js'

let adapter
beforeEach(() => {
  adapter = vi.fn(async (config) => ({
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
    data: '{"code":200,"data":null}',
  }))
  axios.defaults.adapter = adapter
})
const make = (extra) =>
  createApi({
    session: () => ({ tokenName: 'Authorization', tokenValue: 'test-session' }),
    ...extra,
  })
describe('后端接口边界', () => {
  it('在JSON解析阶段保留雪花ID，同时保持状态和页码为数字', async () => {
    adapter.mockImplementation(async (config) => ({
      status: 200,
      headers: {},
      config,
      data: '{"code":200,"data":{"id":2095845066894499842,"status":1,"current":1,"amount":10.50}}',
    }))
    const result = await make().get('/products')
    expect(result.id).toBe('2095845066894499842')
    expect(result.status).toBe(1)
    expect(result.current).toBe(1)
    expect(result.amount).toBe(10.5)
  })
  it('使用后端指定的Token请求头，并保持请求ID不丢位', async () => {
    await make().post('/orders', { addressId: '2095845066894499842' })
    const config = adapter.mock.calls[0][0]
    expect(config.headers.get('Authorization')).toBe('test-session')
    expect(JSON.parse(config.data).addressId).toBe('2095845066894499842')
  })
  it('支持动态tokenName，不自行追加Bearer', async () => {
    await make({ session: () => ({ tokenName: 'satoken', tokenValue: 'abc' }) }).get('/auth/me')
    expect(adapter.mock.calls[0][0].headers.get('satoken')).toBe('abc')
  })
  it('上传成功直接解析URL文本，不要求Result包装', async () => {
    adapter.mockImplementation(async (config) => ({
      status: 200,
      headers: {},
      config,
      data: 'https://images.example.com/test.png',
    }))
    expect(await make().upload('/files', new File(['x'], 'x.png', { type: 'image/png' }))).toBe(
      'https://images.example.com/test.png',
    )
    expect(adapter.mock.calls[0][0].data.get('file').name).toBe('x.png')
  })
  it('上传失败识别JSON业务错误', async () => {
    adapter.mockImplementation(async (config) => ({
      status: 200,
      headers: {},
      config,
      data: '{"code":400,"message":"图片内容无效"}',
    }))
    await expect(make().upload('/files', new File(['x'], 'x.png'))).rejects.toThrow('图片内容无效')
  })
  it('HTTP401清除登录态，403保持会话', async () => {
    const unauthorized = vi.fn()
    adapter.mockImplementation(async (config) => {
      throw new AxiosError('unauthorized', undefined, config, null, {
        status: 401,
        data: '{"code":401,"message":"登录已过期"}',
      })
    })
    await expect(make({ onUnauthorized: unauthorized }).get('/me')).rejects.toThrow('登录已过期')
    expect(unauthorized).toHaveBeenCalledTimes(1)
    adapter.mockImplementation(async (config) => {
      throw new AxiosError('forbidden', undefined, config, null, {
        status: 403,
        data: '{"code":403,"message":"无权访问"}',
      })
    })
    await expect(make({ onUnauthorized: unauthorized }).get('/admin')).rejects.toThrow('无权访问')
    expect(unauthorized).toHaveBeenCalledTimes(1)
  })
  it('HTTP200内的401也触发过期处理', async () => {
    const unauthorized = vi.fn()
    adapter.mockImplementation(async (config) => ({
      status: 200,
      headers: {},
      config,
      data: '{"code":401,"message":"登录过期"}',
    }))
    await expect(make({ onUnauthorized: unauthorized }).get('/me')).rejects.toThrow('登录过期')
    expect(unauthorized).toHaveBeenCalledOnce()
  })
  it('网络失败不伪装成成功', async () => {
    adapter.mockRejectedValue(new AxiosError('Network Error', 'ERR_NETWORK'))
    await expect(make().get('/products')).rejects.toThrow('无法连接服务')
  })
  it('不接受污染原型的响应', () => {
    expect(() => parseResponse('{"__proto__":{"admin":true}}')).toThrow()
  })
})
describe('金额与展示', () => {
  it('会员折后金额遵循十进制HALF_UP', () => {
    expect(memberAmount('0.10', '0.95')).toBe('0.10')
    expect(memberAmount('100.10', '0.95')).toBe('95.10')
    expect(difference('100.10', '95.10')).toBe('5.00')
  })
  it('登录回跳只接受站内路径', () => {
    expect(safeRedirect('/checkout?x=1')).toBe('/checkout?x=1')
    for (const url of ['https://evil.test', '//evil.test', '/\\evil.test', '/login'])
      expect(safeRedirect(url)).toBe('/')
  })
  it('公开图片直接使用HTTPS地址，旧Key和脚本地址不作为src', () => {
    expect(imageSource('https://example.com/a.png')).toBe('https://example.com/a.png')
    for (const value of ['javascript:alert(1)', 'products/a.png', 'data:image/svg+xml,x', null])
      expect(imageSource(value)).toBe('')
  })
})
