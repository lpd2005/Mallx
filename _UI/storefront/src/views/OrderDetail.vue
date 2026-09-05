<script setup>
import { inject, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTask, confirmAction } from '@mallx/shared/useTask.js'
import { ElMessageBox } from 'element-plus'
import OrderSummary from '@mallx/shared/components/OrderSummary.vue'
const api = inject('api'),
  route = useRoute(),
  router = useRouter(),
  { busy, error, run } = useTask(),
  order = ref(),
  saleDialog = ref(false),
  reason = ref('')
async function load() {
  order.value = await api.get(`/users/me/orders/${route.params.orderNo}`)
}
function cancel() {
  run(async () => {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
      inputValidator: (v) => (Boolean(v?.trim()) && v.length <= 255) || '请填写255字以内的原因',
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
    })
    await api.post(`/users/me/orders/${order.value.orderNo}/cancel`, { reason: value })
    await load()
  }, '订单已取消')
}
function receipt() {
  run(async () => {
    await confirmAction('确认已经收到全部商品？')
    await api.post(`/users/me/orders/${order.value.orderNo}/receipt`)
    await load()
  }, '已确认收货')
}
function apply() {
  run(async () => {
    if (!reason.value.trim()) throw new Error('请填写售后原因')
    const sale = await api.post('/users/me/after-sales', {
      orderNo: order.value.orderNo,
      type: [2, 3].includes(order.value.status) ? 1 : 2,
      reason: reason.value,
    })
    saleDialog.value = false
    router.push({ path: '/after-sales', query: { detail: sale.afterSaleNo } })
  }, '售后申请已提交')
}
onMounted(() => run(load))
</script>
<template>
  <div class="page-head">
    <h1>订单详情</h1>
    <RouterLink to="/orders">返回订单列表</RouterLink>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
  <div class="panel" v-loading="busy">
    <OrderSummary :order="order" />
    <div class="actions" v-if="order">
      <RouterLink v-if="order.status === 1" :to="`/payment/${order.orderNo}`">
        <el-button type="primary">去支付</el-button>
      </RouterLink>
      <el-button v-if="order.status === 1" :disabled="busy" @click="cancel">取消订单</el-button>
      <el-button v-if="order.status === 4" type="primary" :disabled="busy" @click="receipt">
        确认收货
      </el-button>
      <el-button
        v-if="[2, 3, 4, 5].includes(order.status)"
        :disabled="busy"
        @click="saleDialog = true"
      >
        申请售后
      </el-button>
      <RouterLink
        v-if="order.afterSaleNo"
        :to="{ path: '/after-sales', query: { detail: order.afterSaleNo } }"
      >
        <el-button>查看售后</el-button>
      </RouterLink>
      <el-button :disabled="busy" @click="run(load)">刷新状态</el-button>
    </div>
  </div>
  <el-dialog v-model="saleDialog" title="申请售后" width="500px">
    <p>{{ [2, 3].includes(order?.status) ? '仅退款' : '退货退款' }} · 整单售后</p>
    <el-input
      v-model="reason"
      type="textarea"
      :rows="4"
      placeholder="请描述售后原因"
      maxlength="500"
      show-word-limit
    />
    <template #footer>
      <el-button @click="saleDialog = false">返回</el-button>
      <el-button type="primary" :loading="busy" @click="apply">提交申请</el-button>
    </template>
  </el-dialog>
</template>
