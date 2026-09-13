<script setup>
import { inject, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dateTime } from '@mallx/shared/format.js'
import { confirmAction, useTask } from '@mallx/shared/useTask.js'

const api = inject('api'),
  { busy, error, run } = useTask()
const status = ref(),
  failures = ref([]),
  spuId = ref('')

async function fetchData() {
  ;[status.value, failures.value] = await Promise.all([
    api.get('/admin/search/index/status'),
    api.get('/admin/search/failures'),
  ])
}
function load() {
  return run(fetchData)
}
function rebuild() {
  run(async () => {
    await confirmAction('全量重建会创建新索引并在完成后切换别名，确认开始？')
    const count = await api.post('/admin/search/index/rebuild', undefined, { timeout: 120000 })
    ElMessage.success(`索引重建完成，共写入 ${count} 个商品文档`)
    await fetchData()
  })
}
function syncProduct() {
  const id = spuId.value.trim()
  if (!/^[1-9]\d*$/.test(id)) return
  run(async () => {
    await api.post(`/admin/search/index/products/${id}/sync`)
    ElMessage.success('商品索引已同步')
    spuId.value = ''
    await fetchData()
  })
}
function replay(event) {
  run(async () => {
    await confirmAction(`确认重放失败事件 ${event.eventId}？`)
    await api.post(`/admin/search/failures/${encodeURIComponent(event.eventId)}/replay`)
    await fetchData()
  }, '失败事件已重新投递')
}
onMounted(load)
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">ELASTICSEARCH</p>
      <h1>搜索索引</h1>
    </div>
    <el-button :loading="busy" @click="load">刷新状态</el-button>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
  <div class="search-status-grid">
    <section class="panel search-status">
      <p class="muted">索引别名</p>
      <strong>{{ status?.alias || '—' }}</strong>
    </section>
    <section class="panel search-status">
      <p class="muted">运行状态</p>
      <el-tag :type="status?.available ? 'success' : 'danger'">
        {{ status?.available ? '可用' : '未建立' }}
      </el-tag>
    </section>
    <section class="panel search-status">
      <p class="muted">商品文档数</p>
      <strong>{{ status?.documentCount ?? '—' }}</strong>
    </section>
  </div>
  <section class="panel">
    <div class="page-head compact">
      <div>
        <h2>索引维护</h2>
        <p class="muted">全量重建使用新物理索引，完成后原子切换别名。</p>
      </div>
      <el-button type="warning" :loading="busy" @click="rebuild">全量重建</el-button>
    </div>
    <el-divider />
    <div class="inline-form">
      <el-input
        v-model="spuId"
        placeholder="输入SPU ID进行单商品同步"
        maxlength="20"
        @keyup.enter="syncProduct"
      />
      <el-button type="primary" :disabled="!/^[1-9]\d*$/.test(spuId.trim())" @click="syncProduct">
        同步商品
      </el-button>
    </div>
  </section>
  <section class="panel section-gap">
    <div class="page-head compact">
      <div>
        <h2>同步失败事件</h2>
        <p class="muted">排除 Elasticsearch 或消息队列故障后再执行重放。</p>
      </div>
      <el-tag :type="failures.length ? 'danger' : 'success'">{{ failures.length }} 条</el-tag>
    </div>
    <el-table :data="failures" v-loading="busy" row-key="eventId">
      <el-table-column prop="eventId" label="事件编号" min-width="260" />
      <el-table-column prop="spuId" label="SPU ID" min-width="190" />
      <el-table-column label="失败时间" width="185">
        <template #default="{ row }">{{ dateTime(row.failedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="busy" @click="replay(row)">重放</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!busy && !failures.length" description="当前没有同步失败事件" />
  </section>
</template>
