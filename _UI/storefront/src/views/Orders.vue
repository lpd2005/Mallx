<script setup>
import { inject, ref, onMounted } from 'vue'
import { money, dateTime, orderStates, options } from '@mallx/shared/format.js'
import { useTask } from '@mallx/shared/useTask.js'
import Picture from '@mallx/shared/components/Picture.vue'
import StatusTag from '@mallx/shared/components/StatusTag.vue'
const api = inject('api'),
  { busy, error, run } = useTask(),
  orders = ref([]),
  status = ref()
async function load() {
  orders.value = await api.get('/users/me/orders', { status: status.value || undefined })
}
onMounted(() => run(load))
</script>
<template>
  <div class="page-head">
    <h1>我的订单</h1>
    <RouterLink to="/after-sales"><el-button>售后记录</el-button></RouterLink>
  </div>
  <div class="toolbar">
    <el-select v-model="status" clearable placeholder="全部订单" @change="run(load)">
      <el-option v-for="o in options(orderStates)" :key="o.value" v-bind="o" />
    </el-select>
    <el-button :loading="busy" @click="run(load)">刷新</el-button>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
  <div v-loading="busy" style="min-height: 200px">
    <section v-for="o in orders" :key="o.orderNo" class="panel order-card">
      <div class="order-top">
        <div>
          <span class="mono">{{ o.orderNo }}</span>
          <p class="muted">{{ dateTime(o.createdAt) }}</p>
        </div>
        <StatusTag :value="o.status" :states="orderStates" />
      </div>
      <div v-for="i in o.items" :key="i.skuId" class="line-item">
        <Picture :src="i.imageOssKey" :alt="i.productName" />
        <div class="info">
          <h3>{{ i.productName }}</h3>
          <p class="muted">{{ i.skuName }} × {{ i.quantity }}</p>
        </div>
        <span>{{ money(i.subtotal) }}</span>
      </div>
      <div class="order-bottom">
        <span>
          共 {{ o.totalQuantity }} 件，合计
          <b>{{ money(o.payAmount) }}</b>
        </span>
        <RouterLink :to="`/orders/${o.orderNo}`"><el-button>查看详情</el-button></RouterLink>
        <RouterLink v-if="o.status === 1" :to="`/payment/${o.orderNo}`">
          <el-button type="primary">去支付</el-button>
        </RouterLink>
      </div>
    </section>
    <el-empty v-if="!orders.length && !busy" description="暂无订单" />
  </div>
</template>
