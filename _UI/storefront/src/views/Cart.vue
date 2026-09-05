<script setup>
import { inject, ref, onMounted, computed } from 'vue'
import { useTask, confirmAction } from '@mallx/shared/useTask.js'
import { money } from '@mallx/shared/format.js'
import Picture from '@mallx/shared/components/Picture.vue'
const api = inject('api'),
  { busy, error, run } = useTask(),
  cart = ref({ items: [] })
const all = computed(() => cart.value.items.length > 0 && cart.value.items.every((i) => i.selected))
async function load() {
  cart.value = await api.get('/users/me/cart')
}
function change(fn) {
  run(async () => {
    await fn()
    await load()
  })
}
function remove(id) {
  run(async () => {
    await confirmAction('从购物车中移除这件商品？')
    await api.delete(`/users/me/cart/items/${id}`)
    await load()
  })
}
function removeSelected() {
  run(async () => {
    await confirmAction('确认删除购物车中全部已选商品？')
    await api.delete('/users/me/cart/selected-items')
    await load()
  })
}
onMounted(() => run(load))
</script>
<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">SHOPPING BAG</p>
      <h1>我的购物车</h1>
    </div>
    <RouterLink to="/"><el-button>继续挑选</el-button></RouterLink>
  </div>
  <el-alert v-if="error" :title="error" type="error" class="error-banner" :closable="false">
    <el-button link @click="run(load)">刷新购物车</el-button>
  </el-alert>
  <div class="panel" v-loading="busy">
    <el-checkbox
      :model-value="all"
      :disabled="busy || !cart.items.length"
      @change="(selected) => change(() => api.patch('/users/me/cart/selection', { selected }))"
    >
      全选
    </el-checkbox>
    <el-button
      link
      type="danger"
      style="margin-left: 20px"
      :disabled="busy || !cart.items.some((i) => i.selected)"
      @click="removeSelected"
    >
      删除已选
    </el-button>
    <div v-for="item in cart.items" :key="item.skuId" class="line-item">
      <el-checkbox
        :model-value="item.selected"
        :aria-label="`选择${item.skuName}`"
        :disabled="busy"
        @change="
          (selected) =>
            change(() => api.patch(`/users/me/cart/items/${item.skuId}/selected`, { selected }))
        "
      />
      <Picture :src="item.imageOssKey" :alt="item.productName" />
      <div class="info">
        <RouterLink :to="`/products/${item.spuId}`">
          <h3>{{ item.productName }}</h3>
        </RouterLink>
        <p class="muted">{{ item.skuName }}</p>
        <p v-if="!item.purchasable" style="color: #b84c34">
          {{ item.invalidReason || '当前不可购买' }}
        </p>
        <p>{{ money(item.salePrice) }}</p>
      </div>
      <el-input-number
        :model-value="item.quantity"
        :min="1"
        :max="99"
        :precision="0"
        :disabled="busy"
        :aria-label="`${item.skuName}数量`"
        @change="
          (quantity) =>
            quantity &&
            change(() => api.patch(`/users/me/cart/items/${item.skuId}/quantity`, { quantity }))
        "
      />
      <b>{{ money(item.subtotal) }}</b>
      <el-button link type="danger" :disabled="busy" @click="remove(item.skuId)">移除</el-button>
    </div>
    <el-empty v-if="!cart.items.length && !busy" description="购物车还是空的，去发现喜欢的好物吧" />
    <div v-if="cart.items.length" class="summary-row">
      <div class="muted">已选 {{ cart.selectedQuantity }} 件</div>
      <div class="actions">
        <span>
          合计
          <b class="price">{{ money(cart.selectedAmount) }}</b>
        </span>
        <RouterLink to="/checkout">
          <el-button type="primary" size="large" :disabled="!cart.selectedQuantity || busy">
            去结算
          </el-button>
        </RouterLink>
      </div>
    </div>
  </div>
</template>
