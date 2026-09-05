import { sessionStore } from '@mallx/shared/session.js'
import { createApi } from '@mallx/shared/http.js'
export const useSession = sessionStore('mallx-storefront')
export function initApi(router, pinia) {
  const store = useSession(pinia)
  return createApi({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    session: () => store.auth,
    onUnauthorized: () => {
      store.clear()
      if (router.currentRoute.value.meta.auth)
        router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    },
  })
}
