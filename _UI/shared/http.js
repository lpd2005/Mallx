import axios from 'axios'
import { parseResponse } from './format.js'

export function createApi({ baseURL = '/api', session, updateSession, onUnauthorized }) {
  const client = axios.create({ baseURL, timeout: 20000, transformResponse: [(text) => text] })
  let refreshPromise
  client.interceptors.request.use((config) => {
    const auth = session()
    if (auth?.tokenName && auth?.tokenValue) config.headers.set(auth.tokenName, auth.tokenValue)
    return config
  })
  function resultData(response, raw = false) {
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
  }
  function failure(error) {
    let body
    try {
      body = parseResponse(error.response?.data)
    } catch {
      /* 网关可能返回HTML。 */
    }
    const numericCode = Number(body?.code || error.code || error.response?.status)
    const normalized = new Error(
      body?.message ||
        (error.response
          ? `请求失败（${error.response.status}）`
          : error.code === 'ERR_NETWORK'
            ? '无法连接服务，请检查后端是否启动后重试'
            : error.code === 'ECONNABORTED'
              ? '请求超时，请刷新确认操作结果'
              : error.message),
    )
    normalized.code = Number.isFinite(numericCode) ? numericCode : undefined
    return normalized
  }
  async function refresh(refreshToken) {
    if (!refreshPromise) {
      refreshPromise = client
        .request({ method: 'POST', url: '/auth/refresh', data: { refreshToken } })
        .then((response) => {
          const auth = resultData(response)
          if (!auth?.tokenName || !auth?.tokenValue || !auth?.refreshToken)
            throw new Error('刷新登录状态时服务器响应不完整')
          updateSession(auth)
          return auth
        })
        .catch((error) => {
          throw failure(error)
        })
        .finally(() => {
          refreshPromise = null
        })
    }
    return refreshPromise
  }
  async function request(method, url, data, params, raw = false, retried = false, options = {}) {
    const sentToken = session()?.tokenValue
    try {
      const response = await client.request({
        method,
        url,
        data,
        params,
        timeout: options.timeout || (raw ? 60000 : 20000),
      })
      return resultData(response, raw)
    } catch (error) {
      const normalized = failure(error)
      const auth = session()
      const refreshable = !['/auth/login', '/auth/register', '/auth/refresh'].includes(url)
      if (normalized.code === 401 && !retried && refreshable && updateSession) {
        try {
          if (auth?.tokenValue !== sentToken)
            return request(method, url, data, params, raw, true, options)
          if (auth?.refreshToken) {
            await refresh(auth.refreshToken)
            return request(method, url, data, params, raw, true, options)
          }
        } catch (refreshError) {
          onUnauthorized?.()
          throw refreshError
        }
      }
      if (normalized.code === 401) onUnauthorized?.()
      throw normalized
    }
  }
  return {
    get: (url, params) => request('GET', url, undefined, params),
    post: (url, data, options) => request('POST', url, data, undefined, false, false, options),
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
