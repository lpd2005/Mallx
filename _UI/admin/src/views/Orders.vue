<script setup>
import { inject, ref, reactive, onMounted } from 'vue'
import { useTask, confirmAction } from '@mallx/shared/useTask.js'
import { money, dateTime, orderStates, options } from '@mallx/shared/format.js'
import StatusTag from '@mallx/shared/components/StatusTag.vue'
import OrderSummary from '@mallx/shared/components/OrderSummary.vue'
const api = inject('api'),
  { busy, error, run } = useTask(),
  rows = ref([]),
  total = ref(0),
  order = ref(),
  visible = ref(false),
  query = reactive({ current: 1, size: 20, keyword: '', userId: '', status: undefined })
async function load() {
  const result = await api.get('/admin/orders', {
    ...query,
    keyword: query.keyword.trim() || undefined,
    userId: query.userId.trim() || undefined,
  })
  rows.value = result.records
  total.value = Number(result.total)
}
function detail(row) {
  run(async () => {
    order.value = await api.get(`/admin/orders/${row.orderNo}`)
    visible.value = true
  })
}
function change(row, action) {
  run(async () => {
    await confirmAction(action === 'ship' ? '确认此订单已发货？' : '确认开始为此订单备货？')
    await api.post(`/admin/orders/${row.orderNo}/${action}`)
    if (order.value?.orderNo === row.orderNo)
      order.value = await api.get(`/admin/orders/${row.orderNo}`)
    await load()
  }, '订单状态已更新')
}
onMounted(() => run(load))
</script>
<template>
  <div class="page-head"><h1>订单管理</h1></div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
  <section class="panel">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="精确订单号" clearable />
      <el-input v-model="query.userId" placeholder="用户ID" clearable class="filter-id" />
      <el-select v-model="query.status" clearable placeholder="全部状态">
        <el-option v-for="o in options(orderStates)" :key="o.value" v-bind="o" />
      </el-select>
      <el-button
        type="primary"
        :loading="busy"
        @click="
          () => {
            query.current = 1
            run(load)
          }
        "
      >
        查询
      </el-button>
    </div>
    <el-table :data="rows" v-loading="busy">
      <el-table-column prop="orderNo" label="订单号" min-width="210" />
      <el-table-column prop="userId" label="用户ID" min-width="190" />
      <el-table-column label="应付金额" width="120">
        <template #default="{ row }">{{ money(row.payAmount) }}</template>
      </el-table-column>
      <el-table-column prop="totalQuantity" label="件数" width="80" />
      <el-table-column label="订单状态" width="110">
        <template #default="{ row }">
          <StatusTag :value="row.status" :states="orderStates" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="175">
        <template #default="{ row }">{{ dateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :disabled="busy" @click="detail(row)">详情</el-button>
          <el-button
            v-if="row.status === 2"
            type="primary"
            link
            :disabled="busy"
            @click="change(row, 'prepare-shipment')"
          >
            备货
          </el-button>
          <el-button
            v-if="row.status === 3"
            type="primary"
            link
            :disabled="busy"
            @click="change(row, 'ship')"
          >
            发货
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="query.current"
      :total="total"
      :page-size="20"
      layout="total,prev,pager,next"
      @current-change="run(load)"
    />
  </section>
  <el-drawer v-model="visible" title="订单详情" size="min(760px, 100vw)">
    <OrderSummary :order="order" />
    <el-button
      v-if="order?.status === 2"
      type="primary"
      :loading="busy"
      @click="change(order, 'prepare-shipment')"
    >
      开始备货
    </el-button>
    <el-button
      v-if="order?.status === 3"
      type="primary"
      :loading="busy"
      @click="change(order, 'ship')"
    >
      确认发货
    </el-button>
  </el-drawer>
</template>
