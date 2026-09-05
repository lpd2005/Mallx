<script setup>
import { inject, ref, reactive, onMounted } from 'vue'
import { useTask, confirmAction } from '@mallx/shared/useTask.js'
import { money, dateTime, saleStates, options } from '@mallx/shared/format.js'
import StatusTag from '@mallx/shared/components/StatusTag.vue'
import AfterSaleDetail from '@mallx/shared/components/AfterSaleDetail.vue'
const api = inject('api'),
  { busy, error, run } = useTask(),
  rows = ref([]),
  total = ref(0),
  current = ref(1),
  status = ref(),
  sale = ref(),
  visible = ref(false),
  audit = reactive({ approved: true, remark: '' }),
  receive = reactive({ restock: true, remark: '' })
async function load() {
  const result = await api.get('/admin/after-sales', {
    current: current.value,
    size: 20,
    status: status.value || undefined,
  })
  rows.value = result.records
  total.value = Number(result.total)
}
function detail(row) {
  run(async () => {
    sale.value = await api.get(`/admin/after-sales/${row.afterSaleNo}`)
    audit.remark = ''
    receive.remark = ''
    audit.approved = true
    receive.restock = true
    visible.value = true
  })
}
function action(name) {
  run(async () => {
    const data =
      name === 'audit' ? { ...audit } : name === 'receive-return' ? { ...receive } : undefined
    if (data && !data.remark.trim()) throw new Error('请填写处理说明')
    await confirmAction(
      name === 'refund'
        ? `确认模拟退款 ${money(sale.value.refundAmount)}？`
        : '确认提交本次售后处理结果？',
    )
    sale.value = await api.post(`/admin/after-sales/${sale.value.afterSaleNo}/${name}`, data)
    await load()
  }, '处理成功')
}
onMounted(() => run(load))
</script>
<template>
  <div class="page-head"><h1>售后管理</h1></div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
  <section class="panel">
    <div class="toolbar">
      <el-select
        v-model="status"
        clearable
        placeholder="全部状态"
        @change="
          () => {
            current = 1
            run(load)
          }
        "
      >
        <el-option v-for="o in options(saleStates)" :key="o.value" v-bind="o" />
      </el-select>
      <el-button type="primary" :loading="busy" @click="run(load)">刷新</el-button>
    </div>
    <el-table :data="rows" v-loading="busy">
      <el-table-column prop="afterSaleNo" label="售后单号" min-width="200" />
      <el-table-column prop="orderNo" label="订单号" min-width="200" />
      <el-table-column label="售后类型" width="110">
        <template #default="{ row }">{{ row.type === 1 ? '仅退款' : '退货退款' }}</template>
      </el-table-column>
      <el-table-column label="退款金额" width="120">
        <template #default="{ row }">{{ money(row.refundAmount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <StatusTag :value="row.status" :states="saleStates" />
        </template>
      </el-table-column>
      <el-table-column label="申请时间" min-width="175">
        <template #default="{ row }">{{ dateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :disabled="busy" @click="detail(row)">查看处理</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="current"
      :page-size="20"
      :total="total"
      layout="total,prev,pager,next"
      @current-change="run(load)"
    />
  </section>
  <el-drawer v-model="visible" title="售后处理" size="min(680px, 100vw)">
    <AfterSaleDetail :sale="sale" />
    <el-form v-if="sale?.status === 1" label-position="top">
      <h3>审核申请</h3>
      <el-form-item label="审核结果">
        <el-radio-group v-model="audit.approved">
          <el-radio :value="true">通过</el-radio>
          <el-radio :value="false">驳回</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="审核说明" required>
        <el-input v-model="audit.remark" type="textarea" maxlength="255" show-word-limit />
      </el-form-item>
      <el-button type="primary" :loading="busy" @click="action('audit')">提交审核</el-button>
    </el-form>
    <el-form v-if="sale?.status === 4" label-position="top">
      <h3>验收退货</h3>
      <el-form-item label="库存处理">
        <el-checkbox v-model="receive.restock">回补可销售库存</el-checkbox>
      </el-form-item>
      <el-form-item label="验收说明" required>
        <el-input v-model="receive.remark" type="textarea" maxlength="255" show-word-limit />
      </el-form-item>
      <el-button type="primary" :loading="busy" @click="action('receive-return')">
        确认验收
      </el-button>
    </el-form>
    <div v-if="sale?.status === 2">
      <el-alert
        title="当前为模拟退款，不发生真实资金转账。"
        type="info"
        :closable="false"
        class="error-banner"
      />
      <el-button type="primary" :loading="busy" @click="action('refund')">执行模拟退款</el-button>
    </div>
  </el-drawer>
</template>
