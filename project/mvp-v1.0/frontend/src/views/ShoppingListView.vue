<template>
  <main class="content-width">
    <PageHeader :title="cart ? '购物车' : '收藏夹'" />
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <p v-if="loading">正在加载…</p>
    <p v-else-if="!rows.length && !error">列表为空。<RouterLink to="/">浏览商品</RouterLink></p>
    <ul v-else class="shopping-list">
      <li v-for="row in rows" :key="row.productId">
        <label v-if="cart"><input v-model="selected" type="checkbox" :value="row.productId" :disabled="busy" /> 选择 {{ row.name }}</label>
        <RouterLink :to="{ name: 'storefront', query: { product: row.productId } }">{{ cart ? '查看详情' : row.name }}</RouterLink>
        <span>¥{{ formatPrice(row.price) }} · {{ row.stock === 0 ? '无货' : productLabel(row.status) }}</span>
        <span v-if="row.stock === 0 && row.status === 'DELISTED'">已下架</span>
        <label v-if="cart">数量 <input type="number" min="1" step="1" :value="row.quantity" :disabled="busy" @change="quantity(row, $event)" /></label>
        <el-button :disabled="busy" @click="remove(row)">{{ cart ? '移除' : '取消收藏' }}</el-button>
      </li>
    </ul>
    <div v-if="cart && rows.length" class="shopping-actions">
      <span>已选 {{ selected.length }} 种 · ¥{{ formatPrice(totalCents / 100) }}</span>
      <el-button :disabled="busy || !selected.length" @click="move">所选商品转收藏</el-button>
      <el-button type="primary" :disabled="busy || !selected.length" @click="checkout">提交所选商品订单</el-button>
    </div>
  </main>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../components/common/PageHeader.vue'
import { commerceApi, newOrderRequestId } from '../api/commerceApi'
import { apiErrorMessage } from '../api/http'
import { formatPrice } from '../utils/format'
import { productLabel } from '../utils/status'

const route = useRoute()
const router = useRouter()
const cart = computed(() => route.name === 'customer-cart')
const rows = ref([])
const selected = ref([])
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const totalCents = computed(() => rows.value.filter(row => selected.value.includes(row.productId))
  .reduce((sum, row) => sum + Math.round(Number(row.price) * 100) * row.quantity, 0))
let pendingCheckout
let loadVersion = 0

async function load() {
  const version = ++loadVersion
  loading.value = true
  error.value = ''
  try {
    const { data } = await (cart.value ? commerceApi.cart() : commerceApi.favorites())
    if (version !== loadVersion) return
    rows.value = data
    selected.value = selected.value.filter(id => rows.value.some(row => row.productId === id))
  } catch (e) { if (version === loadVersion) error.value = apiErrorMessage(e) }
  finally { if (version === loadVersion) loading.value = false }
}

async function act(action) {
  if (busy.value) return
  busy.value = true
  try { await action(); await load() }
  catch (e) { ElMessage.error(apiErrorMessage(e)) }
  finally { busy.value = false }
}

function quantity(row, event) {
  const value = Number(event.target.value)
  if (!Number.isInteger(value) || value < 1 || value > 2147483647) {
    event.target.value = row.quantity
    ElMessage.warning('请输入正整数')
    return
  }
  event.target.value = row.quantity
  return act(() => commerceApi.quantity(row.productId, value))
}

function remove(row) {
  return act(() => cart.value ? commerceApi.removeCart(row.productId) : commerceApi.removeFavorite(row.productId))
}

function move() { return act(() => commerceApi.move([...selected.value])) }

async function checkout() {
  if (busy.value || !selected.value.length) return
  const ids = [...selected.value].sort((a, b) => a - b)
  const signature = ids.join(',')
  try { await ElMessageBox.confirm('将所选商品合成一笔订单。', '确认下单', { type: 'info' }) }
  catch { return }
  if (pendingCheckout?.signature !== signature) pendingCheckout = { signature, key: newOrderRequestId() }
  return act(async () => {
    await commerceApi.checkout(ids, pendingCheckout.key)
    pendingCheckout = null
    ElMessage.success('订单已生成')
    router.push('/customer/shop-orders')
  })
}

watch(cart, () => { rows.value = []; selected.value = []; load() }, { immediate: true })
</script>

<style scoped>
.shopping-list { list-style: none; padding: 0; }
.shopping-list li, .shopping-actions { display: flex; gap: 14px; align-items: center; flex-wrap: wrap; padding: 12px 0; border-bottom: 1px solid #e1e5ec; }
.shopping-list input[type="number"] { width: 86px; }
</style>
