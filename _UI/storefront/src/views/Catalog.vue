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
  hotWords = ref([]),
  items = ref([]),
  total = ref(0),
  page = ref(1),
  keyword = ref(''),
  categoryId = ref(),
  brandId = ref(),
  minPrice = ref(''),
  maxPrice = ref(''),
  inStock = ref(),
  sort = ref('RELEVANCE')
const treeProps = { label: 'categoryName', children: 'children' }

function price(value, label) {
  if (value === '') return undefined
  const number = Number(value)
  if (!Number.isFinite(number) || number < 0) throw new Error(`${label}必须是大于或等于0的数字`)
  return value
}
async function fetchItems() {
  const minimum = price(minPrice.value, '最低价格')
  const maximum = price(maxPrice.value, '最高价格')
  if (minimum !== undefined && maximum !== undefined && Number(minimum) > Number(maximum))
    throw new Error('最低价格不能高于最高价格')
  const data = await api.get('/search/products', {
    current: page.value,
    size: 12,
    keyword: keyword.value.trim() || undefined,
    categoryId: categoryId.value || undefined,
    brandId: brandId.value || undefined,
    minPrice: minimum,
    maxPrice: maximum,
    inStock: inStock.value,
    sort: sort.value,
  })
  items.value = data.records || []
  total.value = Number(data.total || 0)
}
function search() {
  page.value = 1
  run(fetchItems)
}
function category(node) {
  categoryId.value = node.id
  search()
}
function chooseWord(value) {
  keyword.value = typeof value === 'string' ? value : value.value
  search()
}
async function suggestions(value, callback) {
  const query = value.trim()
  if (!query) return callback(hotWords.value.map((word) => ({ value: word })))
  try {
    const words = await api.get('/search/suggestions', { keyword: query })
    callback(words.map((word) => ({ value: word })))
  } catch {
    callback([])
  }
}
function highlightParts(value, fallback) {
  const source = String(value || fallback || '')
  const result = []
  const tags = /<\/?em>/g
  let index = 0,
    marked = false,
    match
  while ((match = tags.exec(source))) {
    if (match.index > index) result.push({ text: source.slice(index, match.index), marked })
    marked = match[0] === '<em>'
    index = match.index + match[0].length
  }
  if (index < source.length) result.push({ text: source.slice(index), marked })
  return result
}
function resetFilters() {
  categoryId.value = undefined
  brandId.value = undefined
  minPrice.value = ''
  maxPrice.value = ''
  inStock.value = undefined
  sort.value = 'RELEVANCE'
  search()
}
function reload() {
  return run(async () => {
    const [categoryData, brandData, words] = await Promise.all([
      api.get('/products/categories'),
      api.get('/products/brands'),
      api.get('/search/hot-words'),
    ])
    categories.value = categoryData
    brands.value = brandData
    hotWords.value = words
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
      <p class="muted">全文检索商品名称、品牌、分类和规格。</p>
    </div>
    <el-autocomplete
      v-model="keyword"
      size="large"
      maxlength="120"
      placeholder="搜索商品、品牌或规格"
      :prefix-icon="Search"
      :fetch-suggestions="suggestions"
      clearable
      aria-label="搜索商品"
      @keyup.enter="search"
      @clear="search"
      @select="chooseWord"
    >
      <template #append><el-button @click="search">搜索</el-button></template>
    </el-autocomplete>
  </section>
  <div v-if="hotWords.length" class="hot-words">
    <span class="muted">热门搜索</span>
    <el-button v-for="word in hotWords" :key="word" link @click="chooseWord(word)">
      {{ word }}
    </el-button>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner">
    <el-button link @click="reload">重新加载</el-button>
  </el-alert>
  <div class="mobile-category">
    <el-cascader
      v-model="categoryId"
      :options="categories"
      :props="{ label: 'categoryName', value: 'id', emitPath: false, checkStrictly: true }"
      clearable
      placeholder="全部分类"
      @change="search"
    />
  </div>
  <div class="catalog-layout">
    <aside class="category-nav">
      <h3>商品分类</h3>
      <el-button text @click="resetFilters">全部商品</el-button>
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
      <div class="search-filters">
        <el-select v-model="brandId" placeholder="全部品牌" clearable @change="search">
          <el-option v-for="b in brands" :key="b.id" :label="b.brandName" :value="b.id" />
        </el-select>
        <el-input
          v-model="minPrice"
          inputmode="decimal"
          placeholder="最低价"
          @keyup.enter="search"
        />
        <span>—</span>
        <el-input
          v-model="maxPrice"
          inputmode="decimal"
          placeholder="最高价"
          @keyup.enter="search"
        />
        <el-select v-model="inStock" placeholder="全部库存" clearable @change="search">
          <el-option label="仅看有货" :value="true" />
          <el-option label="仅看无货" :value="false" />
        </el-select>
        <el-select v-model="sort" aria-label="商品排序" @change="search">
          <el-option label="综合排序" value="RELEVANCE" />
          <el-option label="价格从低到高" value="PRICE_ASC" />
          <el-option label="价格从高到低" value="PRICE_DESC" />
          <el-option label="销量优先" value="SALES_DESC" />
          <el-option label="最新商品" value="NEWEST" />
        </el-select>
      </div>
      <div class="catalog-bar">
        <span class="muted">
          找到
          <b>{{ total }}</b>
          件商品
        </span>
        <el-button link @click="resetFilters">重置筛选</el-button>
      </div>
      <div class="product-grid">
        <RouterLink
          v-for="p in items"
          :key="p.spuId"
          :to="`/products/${p.spuId}`"
          class="product-card"
        >
          <Picture :src="p.mainImageUrl" :alt="p.productName" />
          <div class="product-copy">
            <h3>
              <template
                v-for="(part, index) in highlightParts(p.highlightedProductName, p.productName)"
                :key="index"
              >
                <em v-if="part.marked">{{ part.text }}</em>
                <span v-else>{{ part.text }}</span>
              </template>
            </h3>
            <p class="muted">
              <template
                v-for="(part, index) in highlightParts(p.highlightedSubtitle, p.subtitle)"
                :key="index"
              >
                <em v-if="part.marked">{{ part.text }}</em>
                <span v-else>{{ part.text }}</span>
              </template>
            </p>
            <div class="product-meta muted">
              <span>{{ p.brandName || p.categoryName || 'MallX' }}</span>
              <span>{{ p.hasStock ? `已售 ${p.soldCount || 0}` : '暂时无货' }}</span>
            </div>
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
        :description="error ? '搜索服务暂时无法使用' : '没有找到符合条件的商品'"
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
