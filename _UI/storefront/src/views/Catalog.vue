<script setup>
import { inject, ref, onMounted } from 'vue'
import { Search, ArrowRight } from '@element-plus/icons-vue'
import Picture from '@mallx/shared/components/Picture.vue'
import { money } from '@mallx/shared/format.js'
import { useTask } from '@mallx/shared/useTask.js'
const api = inject('api'),
  { busy, error, run } = useTask()
const categories = ref([]),
  brands = ref([]),
  items = ref([]),
  total = ref(0),
  page = ref(1),
  keyword = ref(''),
  categoryId = ref(),
  brandId = ref()
const treeProps = { label: 'categoryName', children: 'children' }
async function fetchItems() {
  const data = await api.get('/products', {
    current: page.value,
    size: 12,
    keyword: keyword.value.trim() || undefined,
    categoryId: categoryId.value || undefined,
    brandId: brandId.value || undefined,
  })
  items.value = data.records
  total.value = Number(data.total)
}
function search() {
  page.value = 1
  run(fetchItems)
}
function category(node) {
  if (node.children?.length) return
  categoryId.value = node.id
  search()
}
function reload() {
  return run(async () => {
    const results = await Promise.all([
      api.get('/products/categories'),
      api.get('/products/brands'),
    ])
    ;[categories.value, brands.value] = results
    await fetchItems()
  })
}
onMounted(reload)
</script>
<template>
  <section class="catalog-intro">
    <div>
      <p class="eyebrow">THE EVERYDAY COLLECTION</p>
      <h1>发现生活里的好物</h1>
      <p class="muted">从心仪的一件开始。</p>
    </div>
    <el-input
      v-model="keyword"
      size="large"
      maxlength="120"
      placeholder="输入商品名称"
      :prefix-icon="Search"
      clearable
      aria-label="搜索商品"
      @keyup.enter="search"
      @clear="search"
    >
      <template #append><el-button @click="search">搜索</el-button></template>
    </el-input>
  </section>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner">
    <el-button link @click="reload">重新加载</el-button>
  </el-alert>
  <div class="mobile-category">
    <el-cascader
      v-model="categoryId"
      :options="categories"
      :props="{ label: 'categoryName', value: 'id', emitPath: false }"
      clearable
      placeholder="全部分类"
      @change="search"
    />
  </div>
  <div class="catalog-layout">
    <aside class="category-nav">
      <h3>商品分类</h3>
      <el-button
        text
        @click="
          () => {
            categoryId = undefined
            search()
          }
        "
      >
        全部商品
      </el-button>
      <el-tree
        :data="categories"
        :props="treeProps"
        node-key="id"
        highlight-current
        :current-node-key="categoryId"
        @node-click="category"
      />
    </aside>
    <section v-loading="busy">
      <div class="catalog-bar">
        <span class="muted">
          全部好物
          <b>{{ total }}</b>
          件
        </span>
        <el-select v-model="brandId" placeholder="全部品牌" clearable @change="search">
          <el-option v-for="b in brands" :key="b.id" :label="b.brandName" :value="b.id" />
        </el-select>
      </div>
      <div class="product-grid">
        <RouterLink v-for="p in items" :key="p.id" :to="`/products/${p.id}`" class="product-card">
          <Picture :src="p.mainImageOssKey" :alt="p.productName" />
          <div class="product-copy">
            <h3>{{ p.productName }}</h3>
            <p class="muted">{{ p.subtitle || p.description || '查看商品详情' }}</p>
            <div class="product-bottom">
              <span class="price">
                {{ money(p.minPrice) }}
                <small v-if="p.minPrice !== p.maxPrice">起</small>
              </span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </RouterLink>
      </div>
      <el-empty
        v-if="!busy && !items.length"
        :description="error ? '商品暂时无法加载' : '没有找到符合条件的商品'"
      />
      <el-pagination
        v-if="total > 12"
        v-model:current-page="page"
        :page-size="12"
        :total="total"
        layout="prev, pager, next"
        @current-change="run(fetchItems)"
      />
    </section>
  </div>
</template>
