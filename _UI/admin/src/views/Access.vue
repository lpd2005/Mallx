<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
<template>
  <el-result
    icon="warning"
    title="暂时无法访问"
    :sub-title="String(route.query.reason || '请确认账号权限及后端服务状态')"
  >
    <template #extra>
      <div class="actions">
        <RouterLink to="/"><el-button type="primary">重试进入工作台</el-button></RouterLink>
        <RouterLink to="/login"><el-button>重新登录</el-button></RouterLink>
      </div>
    </template>
  </el-result>
</template>
