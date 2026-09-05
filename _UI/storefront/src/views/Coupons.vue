<script setup>
import { inject, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useTask } from '@mallx/shared/useTask.js'
import { money, dateTime, couponStates } from '@mallx/shared/format.js'
const api = inject('api'),
  route = useRoute(),
  { busy, error, run } = useTask(),
  tab = ref(route.query.tab === 'mine' ? 'mine' : 'center'),
  coupons = ref([]),
  mine = ref([])
async function load() {
  ;[coupons.value, mine.value] = await Promise.all([
    api.get('/coupons'),
    api.get('/users/me/coupons'),
  ])
}
function claimed(id) {
  return mine.value.some((c) => c.templateId === id)
}
function claim(c) {
  run(async () => {
    await api.post(`/coupons/${c.id}/claim`)
    await load()
  }, '领取成功')
}
onMounted(() => run(load))
</script>
<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">LITTLE EXTRA JOY</p>
      <h1>优惠券</h1>
    </div>
    <el-button :loading="busy" @click="run(load)">刷新</el-button>
  </div>
  <el-tabs v-model="tab">
    <el-tab-pane label="领券中心" name="center" />
    <el-tab-pane label="我的优惠券" name="mine" />
  </el-tabs>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
  <div class="coupon-grid" v-loading="busy">
    <article v-for="c in tab === 'center' ? coupons : mine" :key="c.id" class="coupon-card">
      <span class="price">
        {{ c.couponType === 1 ? money(c.discountAmount) : `${Number(c.discountRate) * 10} 折` }}
      </span>
      <h2>{{ c.couponName }}</h2>
      <p class="muted">
        满 {{ money(c.thresholdAmount) }} 可用
        <span v-if="c.maxDiscountAmount">，最高减 {{ money(c.maxDiscountAmount) }}</span>
      </p>
      <p class="muted">
        使用期限
        <br />
        {{ dateTime(c.validStartAt) }}
        <br />
        至 {{ dateTime(c.validEndAt) }}
      </p>
      <el-button
        v-if="tab === 'center'"
        type="primary"
        :disabled="claimed(c.id) || !c.remainingStock || busy"
        @click="claim(c)"
      >
        {{ claimed(c.id) ? '已领取' : '立即领取' }}
      </el-button>
      <el-tag v-else>{{ couponStates[c.status] || '未知状态' }}</el-tag>
    </article>
  </div>
  <el-empty
    v-if="!(tab === 'center' ? coupons : mine).length && !busy"
    :description="tab === 'center' ? '暂无可领取的优惠券' : '你还没有优惠券'"
  />
</template>
