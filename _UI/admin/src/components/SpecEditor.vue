<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ modelValue: String }),
  emit = defineEmits(['update:modelValue']),
  rows = ref([]),
  error = ref('')
watch(
  () => props.modelValue,
  (value) => {
    try {
      const parsed = JSON.parse(value || '{}')
      if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') throw Error()
      const entries = Object.entries(parsed)
      rows.value = entries.length
        ? entries.map(([key, value]) => ({
            key,
            value: typeof value === 'string' ? value : JSON.stringify(value),
          }))
        : [{ key: '', value: '' }]
      error.value = ''
    } catch {
      error.value = '原规格不是有效的JSON对象，请重新填写'
      rows.value = [{ key: '', value: '' }]
    }
  },
  { immediate: true },
)
function sync() {
  const valid = rows.value.filter((r) => r.key.trim())
  if (new Set(valid.map((r) => r.key.trim())).size !== valid.length) {
    error.value = '规格名称不能重复'
    emit('update:modelValue', '')
    return
  }
  error.value = ''
  const value = JSON.stringify(Object.fromEntries(valid.map((r) => [r.key.trim(), r.value])))
  emit('update:modelValue', value)
}
function remove(i) {
  rows.value.splice(i, 1)
  sync()
}
</script>
<template>
  <div style="width: 100%">
    <div v-for="(row, i) in rows" :key="i" class="spec-row">
      <el-input v-model="row.key" placeholder="规格，如颜色" @change="sync" />
      <el-input v-model="row.value" placeholder="值，如黑色" @change="sync" />
      <el-button type="danger" link @click="remove(i)">删除</el-button>
    </div>
    <el-button @click="rows.push({ key: '', value: '' })">添加规格</el-button>
    <p v-if="error" style="color: #c43b3b">{{ error }}</p>
  </div>
</template>
