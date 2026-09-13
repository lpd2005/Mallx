<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Picture from '@mallx/shared/components/Picture.vue'
import { money, dateTime } from '@mallx/shared/format.js'
import { useTask } from '@mallx/shared/useTask.js'
import { useSession } from '../services.js'

const api = inject('api'),
  router = useRouter(),
  session = useSession(),
  { busy, error, run } = useTask()
const activities = ref([]),
  products = ref({}),
  dialog = ref(false),
  selectedItem = ref(),
  addresses = ref([]),
  addressId = ref(),
  quantity = ref(1),
  results = ref({}),
  now = ref(Date.now())
const timers = new Set()
let clock
const selectedProduct = computed(() => products.value[selectedItem.value?.skuId])

function phase(activity) {
  if (new Date(activity.startAt).getTime() > now.value) return 'upcoming'
  if (new Date(activity.endAt).getTime() <= now.value) return 'ended'
  return 'active'
}
function phaseText(activity) {
  return { upcoming: '即将开始', active: '抢购中', ended: '已结束' }[phase(activity)]
}
async function loadProducts(data) {
  const skuIds = [...new Set(data.flatMap((activity) => activity.items.map((item) => item.skuId)))]
  const skus = await Promise.all(
    skuIds.map(async (skuId) => {
      try {
        return await api.get(`/products/skus/${skuId}`)
      } catch {
        return { id: skuId }
      }
    }),
  )
  const spuIds = [...new Set(skus.map((sku) => sku.spuId).filter(Boolean))]
  const spus = await Promise.all(
    spuIds.map(async (spuId) => {
      try {
        return await api.get(`/products/${spuId}`)
      } catch {
        return { id: spuId }
      }
    }),
  )
  const spuMap = Object.fromEntries(spus.map((spu) => [spu.id, spu]))
  products.value = Object.fromEntries(
    skus.map((sku) => {
      const spu = spuMap[sku.spuId] || {}
      return [
        sku.id,
        {
          ...sku,
          productName: spu.productName || sku.skuName || `SKU ${sku.id}`,
          subtitle: sku.skuName || spu.subtitle,
          image: sku.imageOssKey || spu.mainImageOssKey,
        },
      ]
    }),
  )
}
function reload() {
  return run(async () => {
    activities.value = await api.get('/seckill/activities')
    await loadProducts(activities.value)
  })
}
function buy(item, activity) {
  if (!session.loggedIn) {
    router.push({ path: '/login', query: { redirect: '/seckill' } })
    return
  }
  run(async () => {
    addresses.value = await api.get('/users/me/addresses')
    if (!addresses.value.length) throw new Error('请先在个人中心添加收货地址')
    selectedItem.value = { ...item, activity }
    addressId.value =
      addresses.value.find((address) => address.defaultAddress)?.id || addresses.value[0].id
    quantity.value = 1
    dialog.value = true
  })
}
function schedulePoll(requestNo, itemId, attempt) {
  const timer = setTimeout(() => {
    timers.delete(timer)
    poll(requestNo, itemId, attempt)
  }, 1000)
  timers.add(timer)
}
async function poll(requestNo, itemId, attempt = 0) {
  try {
    const result = await api.get(`/seckill/requests/${requestNo}`)
    results.value = { ...results.value, [itemId]: result }
    if (result.status === 0 && attempt < 30) schedulePoll(requestNo, itemId, attempt + 1)
  } catch (pollError) {
    results.value = {
      ...results.value,
      [itemId]: { requestNo, status: 0, failReason: pollError.message },
    }
  }
}
function submit() {
  run(async () => {
    const item = selectedItem.value
    const result = await api.post(`/seckill/items/${item.itemId}/purchase`, {
      addressId: addressId.value,
      quantity: quantity.value,
    })
    dialog.value = false
    results.value = { ...results.value, [item.itemId]: result }
    schedulePoll(result.requestNo, item.itemId, 0)
  }, '抢购请求已提交，正在确认订单')
}
function refreshResult(item) {
  const result = results.value[item.itemId]
  if (result?.requestNo) poll(result.requestNo, item.itemId)
}
onMounted(() => {
  reload()
  clock = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})
onBeforeUnmount(() => {
  clearInterval(clock)
  timers.forEach(clearTimeout)
})
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">LIMITED TIME</p>
      <h1>限时秒杀</h1>
      <p class="muted">抢购成功后系统异步创建订单，请及时完成支付。</p>
    </div>
    <el-button :loading="busy" @click="reload">刷新活动</el-button>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
  <div v-for="activity in activities" :key="activity.activityId" class="seckill-activity">
    <div class="seckill-heading">
      <div>
        <el-tag :type="phase(activity) === 'active' ? 'danger' : 'warning'">
          {{ phaseText(activity) }}
        </el-tag>
        <h2>{{ activity.activityName }}</h2>
      </div>
      <p class="muted">{{ dateTime(activity.startAt) }} — {{ dateTime(activity.endAt) }}</p>
    </div>
    <div class="seckill-grid">
      <article v-for="item in activity.items" :key="item.itemId" class="seckill-card">
        <RouterLink
          v-if="products[item.skuId]?.spuId"
          :to="`/products/${products[item.skuId].spuId}`"
        >
          <Picture :src="products[item.skuId]?.image" :alt="products[item.skuId]?.productName" />
        </RouterLink>
        <Picture v-else :src="products[item.skuId]?.image" />
        <div class="seckill-copy">
          <h3>{{ products[item.skuId]?.productName || `SKU ${item.skuId}` }}</h3>
          <p class="muted">{{ products[item.skuId]?.subtitle || `商品规格 ${item.skuId}` }}</p>
          <div class="seckill-price">
            <span class="price">{{ money(item.seckillPrice) }}</span>
            <del v-if="products[item.skuId]?.salePrice">
              {{ money(products[item.skuId].salePrice) }}
            </del>
          </div>
          <p class="muted">
            已成单 {{ item.soldStock }} / 资格库存 {{ item.totalStock }} · 每人限购
            {{ item.limitPerUser }} 件
          </p>
          <div v-if="results[item.itemId]" class="seckill-result">
            <el-tag v-if="results[item.itemId].status === 0" type="warning">订单处理中</el-tag>
            <el-tag v-else-if="results[item.itemId].status === 1" type="success">抢购成功</el-tag>
            <el-tag v-else type="danger">抢购失败</el-tag>
            <span v-if="results[item.itemId].failReason" class="muted">
              {{ results[item.itemId].failReason }}
            </span>
            <RouterLink
              v-if="results[item.itemId].status === 1"
              :to="`/orders/${results[item.itemId].orderNo}`"
            >
              查看订单
            </RouterLink>
            <el-button
              v-else-if="results[item.itemId].status === 0"
              link
              @click="refreshResult(item)"
            >
              查询结果
            </el-button>
          </div>
          <el-button
            type="danger"
            size="large"
            :disabled="phase(activity) !== 'active' || item.status !== 1 || busy"
            @click="buy(item, activity)"
          >
            {{ phase(activity) === 'active' ? '立即抢购' : phaseText(activity) }}
          </el-button>
        </div>
      </article>
    </div>
  </div>
  <el-empty v-if="!busy && !activities.length" description="当前没有开放的秒杀活动" />

  <el-dialog v-model="dialog" title="确认秒杀订单" width="520px" destroy-on-close>
    <el-descriptions v-if="selectedItem" :column="1" border>
      <el-descriptions-item label="商品">{{ selectedProduct?.productName }}</el-descriptions-item>
      <el-descriptions-item label="秒杀价">
        {{ money(selectedItem.seckillPrice) }}
      </el-descriptions-item>
      <el-descriptions-item label="限购">{{ selectedItem.limitPerUser }} 件</el-descriptions-item>
    </el-descriptions>
    <el-form label-position="top" class="section-gap">
      <el-form-item label="收货地址" required>
        <el-select v-model="addressId" placeholder="选择收货地址">
          <el-option
            v-for="address in addresses"
            :key="address.id"
            :value="address.id"
            :label="`${address.recipientName} · ${address.provinceName}${address.cityName}${address.districtName}${address.detailAddress}`"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="购买数量" required>
        <el-input-number v-model="quantity" :min="1" :max="selectedItem?.limitPerUser || 1" />
      </el-form-item>
    </el-form>
    <el-alert
      title="提交后先进入异步处理，不代表订单已经创建；页面会自动查询最终结果。"
      type="info"
      :closable="false"
      show-icon
    />
    <template #footer>
      <el-button @click="dialog = false">取消</el-button>
      <el-button type="danger" :loading="busy" :disabled="!addressId" @click="submit">
        提交抢购
      </el-button>
    </template>
  </el-dialog>
</template>
