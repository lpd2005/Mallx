import axios from 'axios'
import { parseResponse } from './format.js'

export function createApi({ baseURL = '/api', session, onUnauthorized }) {
  const client = axios.create({ baseURL, timeout: 20000, transformResponse: [(text) => text] })
  client.interceptors.request.use((config) => {
    const auth = session()
    if (auth?.tokenName && auth?.tokenValue) config.headers.set(auth.tokenName, auth.tokenValue)
    return config
  })
  async function request(method, url, data, params, raw = false) {
    try {
      const response = await client.request({
        method,
        url,
        data,
        params,
        timeout: raw ? 60000 : 20000,
      })
      if (raw && typeof response.data === 'string' && /^https:\/\/\S+$/.test(response.data.trim()))
        return response.data.trim()
      const body = parseResponse(response.data)
      if (!body || Number(body.code) !== 200) {
        const error = new Error(body?.message || '服务器响应格式不正确')
        error.code = Number(body?.code)
        throw error
      }
      if (raw) throw new Error('图片上传未返回有效地址')
      return body.data
    } catch (error) {
      let body
      try {
        body = parseResponse(error.response?.data)
      } catch {
        /* 网关可能返回HTML。 */
      }
      const code = Number(body?.code || error.code || error.response?.status)
      if (code === 401) onUnauthorized?.()
      const failure = new Error(
        body?.message ||
          (error.response
            ? `请求失败（${error.response.status}）`
            : error.code === 'ERR_NETWORK'
              ? '无法连接服务，请检查后端是否启动后重试'
              : error.code === 'ECONNABORTED'
                ? '请求超时，请刷新确认操作结果'
                : error.message),
      )
      failure.code = code
      throw failure
    }
  }
  return {
    get: (url, params) => request('GET', url, undefined, params),
    post: (url, data) => request('POST', url, data),
    put: (url, data) => request('PUT', url, data),
    patch: (url, data) => request('PATCH', url, data),
    delete: (url) => request('DELETE', url),
    upload: (url, file) => {
      const body = new FormData()
      body.append('file', file)
      return request('POST', url, body, undefined, true)
    },
  }
}
