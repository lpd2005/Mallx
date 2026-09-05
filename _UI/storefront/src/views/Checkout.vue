<script setup>
import { inject, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTask } from '@mallx/shared/useTask.js'
import { money, memberAmount, difference } from '@mallx/shared/format.js'
import Picture from '@mallx/shared/components/Picture.vue'
const api = inject('api'),
  router = useRouter(),
  { busy, error, run } = useTask(),
  checkout = ref(),
  addresses = ref([]),
  membership = ref(),
  coupons = ref([]),
  addressId = ref(),
  couponUserId = ref(),
  remark = ref(''),
  submitted = ref(false)
const base = computed(() =>
    memberAmount(checkout.value?.productAmount, membership.value?.discountRate),
  ),
  selectedCoupon = computed(() => coupons.value.find((c) => c.coupon.id === couponUserId.value)),
  payable = computed(() => selectedCoupon.value?.payableAmount ?? base.value)
async function load() {
  const [c, a, m] = await Promise.all([
    api.get('/users/me/cart/checkout'),
    api.get('/users/me/addresses'),
    api.get('/users/me/membership'),
  ])
  checkout.value = c
  addresses.value = a
  membership.value = m
  addressId.value = a.find((x) => x.defaultAddress)?.id || a[0]?.id
  coupons.value = await api.get('/users/me/coupons/usable', { amount: base.value })
  couponUserId.value = undefined
}
function submit() {
  run(async () => {
    if (!addressId.value) throw new Error('请先选择收货地址')
    const order = await api.post('/users/me/orders', {
      addressId: addressId.value,
      couponUserId: couponUserId.value || undefined,
      remark: remark.value || undefined,
    })
    submitted.value = true
    router.replace(`/payment/${order.orderNo}`)
  })
}
onMounted(() => run(load))
</script>
<template>
  <div class="page-head">
    <h1>确认订单</h1>
    <RouterLink to="/cart">返回购物车</RouterLink>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner">
    <el-button link @click="run(load)">重新加载结算</el-button>
    <RouterLink to="/orders">查看已有订单</RouterLink>
  </el-alert>
  <div class="two-cols" v-loading="busy">
    <div>
      <section class="panel">
        <div class="page-head">
          <h2>收货地址</h2>
          <RouterLink to="/addresses?redirect=/checkout">管理地址</RouterLink>
        </div>
        <el-radio-group v-model="addressId" style="display: grid; gap: 15px">
          <el-radio
            v-for="a in addresses"
            :key="a.id"
            :value="a.id"
            style="height: auto; white-space: normal; align-items: flex-start"
          >
            <b>{{ a.recipientName }} · {{ a.recipientMobile }}</b>
            <p class="muted" style="white-space: normal; margin: 5px 0">
              {{ a.provinceName }}{{ a.cityName }}{{ a.districtName }}{{ a.detailAddress }}
            </p>
          </el-radio>
        </el-radio-group>
        <el-empty v-if="!addresses.length" description="请先添加收货地址" />
      </section>
      <section class="panel">
        <h2>商品清单</h2>
        <div v-for="i in checkout?.items" :key="i.skuId" class="line-item">
          <Picture :src="i.imageOssKey" :alt="i.productName" />
          <div class="info">
            <h3>{{ i.productName }}</h3>
            <p class="muted">{{ i.skuName }} × {{ i.quantity }}</p>
          </div>
          <b>{{ money(i.subtotal) }}</b>
        </div>
        <el-form label-position="top" class="section-gap">
          <el-form-item label="订单备注（选填）">
            <el-input v-model="remark" type="textarea" maxlength="255" show-word-limit />
          </el-form-item>
        </el-form>
      </section>
    </div>
    <aside class="panel checkout-summary">
      <h2>订单汇总</h2>
      <div class="summary-row">
        <span>商品金额</span>
        <span>{{ money(checkout?.productAmount) }}</span>
      </div>
      <div class="summary-row">
        <span>会员优惠</span>
        <span>−{{ money(difference(checkout?.productAmount, base)) }}</span>
      </div>
      <el-select v-model="couponUserId" clearable placeholder="选择优惠券（可不选）">
        <el-option
          v-for="c in coupons"
          :key="c.coupon.id"
          :value="c.coupon.id"
          :label="`${c.coupon.couponName} · 减 ${money(c.discountAmount)}`"
        />
      </el-select>
      <div class="summary-row">
        <span>优惠券抵扣</span>
        <span>−{{ money(selectedCoupon?.discountAmount || 0) }}</span>
      </div>
      <el-divider />
      <div class="summary-row">
        <b>预计应付</b>
        <span class="price">{{ money(payable) }}</span>
      </div>
      <p class="muted">以创建订单时的实际金额为准。</p>
      <el-button
        type="primary"
        size="large"
        style="width: 100%"
        :disabled="!checkout?.items?.length || !addressId || submitted"
        :loading="busy"
        @click="submit"
      >
        提交订单
      </el-button>
    </aside>
  </div>
</template>
