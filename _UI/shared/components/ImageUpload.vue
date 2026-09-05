<script setup>
import { inject, ref } from 'vue'
import { ElMessage } from 'element-plus'
import Picture from './Picture.vue'
const props = defineProps({ modelValue: String, avatar: Boolean })
const emit = defineEmits(['update:modelValue', 'uploading'])
const api = inject('api'),
  busy = ref(false)
async function upload({ file, onSuccess, onError }) {
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    const e = new Error('请选择不超过5 MB的JPEG或PNG图片')
    ElMessage.error(e.message)
    onError(e)
    return
  }
  busy.value = true
  emit('uploading', true)
  try {
    const url = await api.upload(
      props.avatar ? '/users/me/files/avatar/upload' : '/admin/files/images/upload',
      file,
    )
    emit('update:modelValue', url)
    onSuccess(url)
    ElMessage.success('上传成功，请保存表单')
  } catch (e) {
    ElMessage.error(e.message)
    onError(e)
  } finally {
    busy.value = false
    emit('uploading', false)
  }
}
</script>
<template>
  <div class="image-editor">
    <Picture :src="modelValue" alt="图片预览" />
    <div>
      <el-upload
        :show-file-list="false"
        :http-request="upload"
        accept="image/jpeg,image/png"
        :disabled="busy"
      >
        <el-button :loading="busy">上传图片</el-button>
      </el-upload>
      <p class="muted">JPEG / PNG，最大 5 MB</p>
      <el-button
        v-if="modelValue"
        link
        type="danger"
        :disabled="busy"
        @click="emit('update:modelValue', '')"
      >
        移除图片
      </el-button>
    </div>
  </div>
</template>
