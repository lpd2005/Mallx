<script setup>
import { inject, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTask, confirmAction } from '@mallx/shared/useTask.js'
import { money, dateTime, requestNo, orderStates } from '@mallx/shared/format.js'
const api = inject('api'),
  route = useRoute(),
  router = useRouter(),
  { busy, error, run } = useTask(),
  payment = ref(),
  order = ref()
const key = `mallx-pay-${route.params.orderNo}`
async function load() {
  order.value = await api.get(`/users/me/orders/${route.params.orderNo}`)
  if (order.value.status === 1)
    payment.value = await api.post(`/users/me/orders/${route.params.orderNo}/payments`)
}
function pay() {
  run(async () => {
    await confirmAction(`模拟支付 ${money(payment.value.amount)}？本次不会发生真实扣款。`)
    let no = sessionStorage.getItem(key)
    if (!no) {
      no = requestNo('PAY')
      sessionStorage.setItem(key, no)
    }
    await api.post(`/users/me/payments/${payment.value.paymentNo}/mock-pay`, { requestNo: no })
    sessionStorage.removeItem(key)
    router.replace(`/orders/${route.params.orderNo}`)
  }, '模拟支付成功')
}
onMounted(() => run(load))
</script>
<template>
  <section class="panel payment-box" v-loading="busy">
    <p class="eyebrow">CHECKOUT</p>
    <h1>收银台</h1>
    <el-alert title="当前为模拟支付，不会产生真实扣款。" type="info" :closable="false" />
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <p class="mono section-gap">订单号 {{ route.params.orderNo }}</p>
    <p class="price">{{ money(order?.payAmount) }}</p>
    <p v-if="order?.status === 1" class="muted">请在 {{ dateTime(order.expireAt) }} 前完成支付</p>
    <p v-else-if="order">订单{{ orderStates[order.status] }}，无需继续支付。</p>
    <div class="actions">
      <el-button
        v-if="order?.status === 1 && payment"
        type="primary"
        size="large"
        :loading="busy"
        @click="pay"
      >
        模拟支付成功
      </el-button>
      <el-button :disabled="busy" @click="run(load)">刷新支付状态</el-button>
      <RouterLink :to="`/orders/${route.params.orderNo}`">
        <el-button>查看订单</el-button>
      </RouterLink>
    </div>
  </section>
</template>
