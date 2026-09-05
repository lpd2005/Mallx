import { sessionStore } from '@mallx/shared/session.js'
import { createApi } from '@mallx/shared/http.js'
export const useSession = sessionStore('mallx-admin')
export let api
export function initApi(router, pinia) {
  const session = useSession(pinia)
  api = createApi({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    session: () => session.auth,
    onUnauthorized: () => {
      session.clear()
      if (router.currentRoute.value.path !== '/login')
        router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    },
  })
  return api
}
export async function allPages(url, params = {}) {
  const result = []
  let current = 1
  while (true) {
    const page = await api.get(url, { ...params, current, size: 100 })
    result.push(...page.records)
    if (result.length >= Number(page.total) || !page.records.length) break
    current++
  }
  return result
}
