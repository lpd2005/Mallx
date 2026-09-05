import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export function useTask() {
  const busy = ref(false),
    error = ref('')
  async function run(fn, message) {
    if (busy.value) return
    busy.value = true
    error.value = ''
    try {
      const value = await fn()
      if (message) ElMessage.success(message)
      return value
    } catch (e) {
      if (e !== 'cancel' && e !== 'close') {
        error.value = e.message || '操作失败，请重试'
        ElMessage.error(error.value)
      }
    } finally {
      busy.value = false
    }
  }
  return { busy, error, run }
}
export const confirmAction = (message) =>
  ElMessageBox.confirm(message, '请确认', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  })
