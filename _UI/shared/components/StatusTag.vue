<script setup>
import { computed } from 'vue'
const props = defineProps({ value: [String, Number], states: Object })
const label = computed(() => props.states?.[props.value] || '未知状态')
const tone = computed(() => {
  if (['已完成', '已支付', '启用', '已上架', '发放中'].includes(label.value)) return 'success'
  if (['已驳回', '禁用'].includes(label.value)) return 'danger'
  if (label.value.startsWith('待') || label.value === '售后中') return 'warning'
  return 'info'
})
</script>
<template>
  <el-tag effect="light" :type="tone">
    {{ label }}
  </el-tag>
</template>
