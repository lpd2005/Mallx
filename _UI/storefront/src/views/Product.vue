<script setup>
import { inject, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '../services.js'
import { useTask } from '@mallx/shared/useTask.js'
import { money, specText } from '@mallx/shared/format.js'
import Picture from '@mallx/shared/components/Picture.vue'
const api = inject('api'),
  route = useRoute(),
  router = useRouter(),
  session = useSession(),
  { busy, error, run } = useTask()
const product = ref(),
  skus = ref([]),
  skuId = ref(),
  quantity = ref(1),
  stock = ref(null)
const sku = computed(() => skus.value.find((s) => s.id === skuId.value))
watch(
  () => route.params.id,
  () =>
    run(async () => {
      product.value = null
      const [p, s] = await Promise.all([
        api.get(`/products/${route.params.id}`),
        api.get(`/products/${route.params.id}/skus`),
      ])
      product.value = p
      skus.value = s
      skuId.value = s[0]?.id
    }),
  { immediate: true },
)
let stockVersion = 0
watch(skuId, async (id) => {
  const version = ++stockVersion
  stock.value = null
  if (id && session.loggedIn) {
    try {
      const s = await api.get(`/inventory/skus/${id}`)
      if (version === stockVersion) stock.value = s.availableStock
    } catch {
      /* 购物车操作仍由后端检查库存。 */
    }
  }
})
function add() {
  if (!session.loggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  run(
    () => api.post('/users/me/cart/items', { skuId: skuId.value, quantity: quantity.value }),
    '已加入购物车',
  )
}
</script>
<template>
  <el-breadcrumb class="breadcrumb">
    <el-breadcrumb-item to="/">全部商品</el-breadcrumb-item>
    <el-breadcrumb-item>商品详情</el-breadcrumb-item>
  </el-breadcrumb>
  <el-alert v-if="error" :title="error" type="error" :closable="false" />
  <div v-loading="busy" style="min-height: 280px">
    <div v-if="product" class="detail-grid">
      <div class="detail-image">
        <Picture :src="sku?.imageOssKey || product.mainImageOssKey" :alt="product.productName" />
      </div>
      <section class="detail-info">
        <p class="eyebrow">MALLX COLLECTION</p>
        <h1>{{ product.productName }}</h1>
        <p class="muted">{{ product.subtitle }}</p>
        <div class="panel">
          <span class="price">{{ money(sku?.salePrice ?? product.minPrice) }}</span>
          <del v-if="sku?.marketPrice > sku?.salePrice" class="muted" style="margin-left: 16px">
            {{ money(sku.marketPrice) }}
          </del>
        </div>
        <h3>选择规格</h3>
        <el-radio-group v-model="skuId" class="sku-options">
          <el-radio-button v-for="s in skus" :key="s.id" :value="s.id">
            {{ s.skuName }}
          </el-radio-button>
        </el-radio-group>
        <p class="muted">{{ specText(sku?.specJson) }}</p>
        <p v-if="stock !== null" class="muted">可用库存 {{ stock }} 件</p>
        <div class="actions">
          <el-input-number
            v-model="quantity"
            :min="1"
            :max="Math.max(1, Math.min(99, stock ?? 99))"
            :precision="0"
            aria-label="购买数量"
          />
          <el-button
            type="primary"
            size="large"
            :disabled="!sku || stock === 0"
            :loading="busy"
            @click="add"
          >
            {{ stock === 0 ? '暂时缺货' : '加入购物车' }}
          </el-button>
        </div>
        <p v-if="!skus.length" class="muted section-gap">该商品暂无可购买规格</p>
      </section>
    </div>
  </div>
  <section v-if="product" class="panel section-gap">
    <h2>商品介绍</h2>
    <p style="white-space: pre-wrap">{{ product.description || '暂无商品介绍' }}</p>
  </section>
</template>
