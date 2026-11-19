<template>
  <main class="content-width customization-page">
    <PageHeader title="在线定制" description="选择商品，设计专属文字与图案，确认后生成定制订单。">
      <template #actions>
        <RouterLink v-if="product" :to="{ name: 'storefront', query: { product: product.id } }">返回商品详情</RouterLink>
        <RouterLink v-else to="/">返回商品首页</RouterLink>
      </template>
    </PageHeader>
    <p v-if="loading">正在加载…</p>
    <el-alert v-else-if="error" :title="error" type="error" :closable="false" show-icon />
    <template v-else-if="product">
      <div class="selected-product">
        <h2>{{ product.name }}</h2>
        <span>¥{{ formatPrice(product.price) }} · 库存 {{ product.stock }} · {{ productLabel(product.status) }}</span>
        <RouterLink :to="{ name: 'customization' }">更换定制商品</RouterLink>
      </div>
      <ProductCommerce :key="product.id" :product="product" studio />
    </template>
    <template v-else>
      <p class="catalog-hint">选择一件可定制商品，进入工作台。</p>
      <el-empty v-if="!items.length" description="暂无可定制商品" />
      <div class="custom-catalog">
        <el-card v-for="item in items" :key="item.id" shadow="never">
          <h2>{{ item.name }}</h2>
          <p>¥{{ formatPrice(item.price) }} · 库存 {{ item.stock }} · {{ productLabel(item.status) }}</p>
          <el-button type="primary" @click="select(item.id)">开始定制</el-button>
        </el-card>
      </div>
      <div v-if="total > size" class="custom-pager">
        <el-button :disabled="page === 0" @click="changePage(-1)">上一页</el-button>
        <span>共 {{ total }} 件 · 第 {{ page + 1 }} 页</span>
        <el-button :disabled="(page + 1) * size >= total" @click="changePage(1)">下一页</el-button>
      </div>
    </template>
  </main>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { catalogApi } from '../api/catalogApi'
import { apiErrorMessage } from '../api/http'
import { formatPrice } from '../utils/format'
import { productLabel } from '../utils/status'
import PageHeader from '../components/common/PageHeader.vue'
import ProductCommerce from '../components/ProductCommerce.vue'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const items = ref([])
const total = ref(0)
const page = ref(0)
const size = 12
const loading = ref(false)
const error = ref('')
let loadVersion = 0

async function load() {
  const version = ++loadVersion
  const id = route.query.product
  loading.value = true
  error.value = ''
  product.value = null
  try {
    if (id !== undefined) {
      if (typeof id !== 'string' || !/^[1-9]\d*$/.test(id)) throw new Error('商品编号无效')
      const { data } = await catalogApi.product(id)
      if (!data.customizable) throw new Error('该商品尚未启用在线定制，请更换商品。')
      if (version === loadVersion) product.value = data
    } else {
      const { data } = await catalogApi.products({ customizable: true, page: page.value, size })
      if (version === loadVersion) {
        items.value = data.items
        total.value = data.total
      }
    }
  } catch (failure) {
    if (version === loadVersion) error.value = failure.response
      ? apiErrorMessage(failure, '商品暂不可定制')
      : failure.message
  } finally {
    if (version === loadVersion) loading.value = false
  }
}
function select(id) {
  router.push({ name: 'customization', query: { product: id } })
}
function changePage(delta) {
  page.value += delta
  load()
}
watch(() => route.query.product, () => { page.value = 0; load() }, { immediate: true })
</script>

<style scoped>
.selected-product { display: flex; flex-wrap: wrap; gap: 12px 24px; align-items: center; border-bottom: 1px solid #e1e5ec; padding-bottom: 20px; }
.selected-product h2, .custom-catalog h2 { margin: 0; font-size: 20px; }
.catalog-hint { color: #64748b; }
.custom-catalog { display: grid; grid-template-columns: repeat(auto-fit, minmax(235px, 1fr)); gap: 18px; }
.custom-pager { display: flex; gap: 16px; align-items: center; justify-content: center; margin: 24px 0; }
</style>
