import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export function sessionStore(key) {
  return defineStore(key, () => {
    let saved = null
    try {
      saved = JSON.parse(sessionStorage.getItem(key))
    } catch {
      /* 无有效会话。 */
    }
    const auth = ref(saved)
    const admin = ref(null)
    const loggedIn = computed(() => Boolean(auth.value?.tokenValue))
    function save(value) {
      auth.value = value
      sessionStorage.setItem(key, JSON.stringify(value))
      admin.value = null
    }
    function renew(value) {
      auth.value = value
      sessionStorage.setItem(key, JSON.stringify(value))
    }
    function clear() {
      auth.value = null
      admin.value = null
      sessionStorage.removeItem(key)
    }
    const can = (permission) => admin.value?.permissions?.includes(permission) || false
    return { auth, admin, loggedIn, save, renew, clear, can }
  })
}
