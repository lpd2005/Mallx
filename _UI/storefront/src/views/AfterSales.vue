<script setup>
import { inject, ref, onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { useTask, confirmAction } from '@mallx/shared/useTask.js'
import { money, dateTime, saleStates, options } from '@mallx/shared/format.js'
import StatusTag from '@mallx/shared/components/StatusTag.vue'
import AfterSaleDetail from '@mallx/shared/components/AfterSaleDetail.vue'
const api = inject('api'),
  route = useRoute(),
  { busy, error, run } = useTask(),
  items = ref([]),
  total = ref(0),
  page = ref(1),
  status = ref(),
  sale = ref(),
  visible = ref(false),
  shipment = reactive({ carrier: '', trackingNo: '' })
async function load() {
  const result = await api.get('/users/me/after-sales', {
    current: page.value,
    size: 10,
    status: status.value || undefined,
  })
  items.value = result.records
  total.value = Number(result.total)
}
async function detail(no) {
  sale.value = await api.get(`/users/me/after-sales/${no}`)
  shipment.carrier = ''
  shipment.trackingNo = ''
  visible.value = true
}
function cancel() {
  run(async () => {
    await confirmAction('确认撤销此售后申请？')
    sale.value = await api.post(`/users/me/after-sales/${sale.value.afterSaleNo}/cancel`)
    await load()
  }, '已撤销申请')
}
function send() {
  run(async () => {
    if (!shipment.carrier.trim() || !shipment.trackingNo.trim())
      throw new Error('请填写物流公司和物流单号')
    sale.value = await api.post(`/users/me/after-sales/${sale.value.afterSaleNo}/return-shipment`, {
      ...shipment,
    })
    await load()
  }, '退货物流已提交')
}
onMounted(() =>
  run(async () => {
    await load()
    if (route.query.detail) await detail(route.query.detail)
  }),
)
</script>
<template>
  <div class="page-head">
    <h1>售后服务</h1>
    <RouterLink to="/orders">前往订单申请售后</RouterLink>
  </div>
  <div class="toolbar">
    <el-select
      v-model="status"
      clearable
      placeholder="全部状态"
      @change="
        () => {
          page = 1
          run(load)
        }
      "
    >
      <el-option v-for="o in options(saleStates)" :key="o.value" v-bind="o" />
    </el-select>
    <el-button :loading="busy" @click="run(load)">刷新</el-button>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
  <section class="panel" v-loading="busy">
    <el-table :data="items">
      <el-table-column prop="afterSaleNo" label="售后单号" min-width="210" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ row.type === 1 ? '仅退款' : '退货退款' }}</template>
      </el-table-column>
      <el-table-column label="金额" width="120">
        <template #default="{ row }">{{ money(row.refundAmount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <StatusTag :value="row.status" :states="saleStates" />
        </template>
      </el-table-column>
      <el-table-column label="申请时间" min-width="170">
        <template #default="{ row }">{{ dateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="run(() => detail(row.afterSaleNo))">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="total > 10"
      v-model:current-page="page"
      :page-size="10"
      :total="total"
      layout="prev,pager,next"
      @current-change="run(load)"
    />
  </section>
  <el-drawer v-model="visible" title="售后详情" size="min(600px, 100vw)">
    <AfterSaleDetail :sale="sale" />
    <el-form v-if="sale?.status === 3" label-position="top" class="section-gap">
      <el-form-item label="物流公司" required>
        <el-input v-model="shipment.carrier" maxlength="64" />
      </el-form-item>
      <el-form-item label="物流单号" required>
        <el-input v-model="shipment.trackingNo" maxlength="64" />
      </el-form-item>
      <el-button type="primary" :loading="busy" @click="send">提交寄回信息</el-button>
    </el-form>
    <el-button
      v-if="sale && ([1, 3].includes(sale.status) || (sale.type === 1 && sale.status === 2))"
      type="danger"
      plain
      class="section-gap"
      :loading="busy"
      @click="cancel"
    >
      撤销售后
    </el-button>
  </el-drawer>
</template>
