<script setup>
import { inject, ref, onMounted, computed } from 'vue'
import { useTask } from '@mallx/shared/useTask.js'
import { money } from '@mallx/shared/format.js'
const api = inject('api'),
  { busy, error, run } = useTask(),
  range = ref([]),
  summary = ref(),
  top = ref([])
const cards = computed(() => [
  { label: '实收金额', value: money(summary.value?.paidAmount), note: '按支付发生时间统计' },
  { label: '退款金额', value: money(summary.value?.refundAmount), note: '按退款成功时间统计' },
  {
    label: '净收款',
    value: money(summary.value?.netReceiptAmount),
    note: '实收减退款，可能为负数',
  },
  { label: '支付订单', value: summary.value?.paidOrderCount ?? '—', note: '包含后来退款的订单' },
])
async function load() {
  if (
    range.value?.length &&
    (new Date(range.value[1]) - new Date(range.value[0])) / 86400000 + 1 > 93
  )
    throw new Error('查询范围最多93天')
  const params = range.value?.length ? { startDate: range.value[0], endDate: range.value[1] } : {}
  ;[summary.value, top.value] = await Promise.all([
    api.get('/admin/dashboard', params),
    api.get('/admin/dashboard/top-products', { ...params, limit: 10 }),
  ])
}
const maximum = computed(() => Math.max(1, ...top.value.map((p) => Number(p.quantity))))
onMounted(() => run(load))
</script>
<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">BUSINESS OVERVIEW</p>
      <h1>运营概览</h1>
    </div>
  </div>
  <div class="toolbar">
    <el-date-picker
      v-model="range"
      type="daterange"
      range-separator="至"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
      value-format="YYYY-MM-DD"
    />
    <el-button type="primary" :loading="busy" @click="run(load)">查询</el-button>
    <span class="muted">{{ summary?.startDate }} — {{ summary?.endDate }}</span>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
  <div v-loading="busy">
    <div class="metric-grid">
      <article v-for="c in cards" :key="c.label" class="metric">
        <p>{{ c.label }}</p>
        <strong>{{ c.value }}</strong>
        <small>{{ c.note }}</small>
      </article>
    </div>
    <div class="dashboard-grid">
      <section class="panel">
        <div class="page-head">
          <h2>热销商品</h2>
          <span class="muted">{{ range?.length ? '所选日期' : '最近7天' }} · 按购买件数</span>
        </div>
        <div v-for="(p, i) in top" :key="p.skuId" class="rank-row">
          <span class="rank-index">{{ i + 1 }}</span>
          <div class="rank-info">
            <p>{{ p.skuName }}</p>
            <div class="rank-bar">
              <span :style="{ width: `${(Number(p.quantity) / maximum) * 100}%` }" />
            </div>
          </div>
          <b>{{ p.quantity }}</b>
        </div>
        <el-empty v-if="!top.length && !busy" description="暂无销售数据" />
      </section>
      <section class="panel">
        <h2>经营数据</h2>
        <div class="summary-row">
          <span>用户总数</span>
          <b>{{ summary?.userCount ?? '—' }}</b>
        </div>
        <div class="summary-row">
          <span>新增用户</span>
          <b>{{ summary?.newUserCount ?? '—' }}</b>
        </div>
        <el-divider />
        <div class="summary-row">
          <span>创建订单数</span>
          <b>{{ summary?.orderCount ?? '—' }}</b>
        </div>
        <div class="summary-row">
          <span>订单应付金额</span>
          <b>{{ money(summary?.orderAmount) }}</b>
        </div>
        <p class="muted section-gap">
          订单数与应付金额包含未支付及取消订单。用户总数包含禁用账号。
        </p>
      </section>
    </div>
  </div>
</template>
