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
      <el-button type="primary" :disabled="busy || !selectedRows.length" @click="checkoutOpen = true">提交所选商品订单</el-button>
    </div>
    <el-dialog v-model="checkoutOpen" title="确认下单" width="min(520px, 92vw)" align-center
      :show-close="false" :close-on-click-modal="false" :close-on-press-escape="!busy">
      <p>所选商品将合成一笔订单，未勾选的商品仍留在购物车。</p>
      <ul class="checkout-summary">
        <li v-for="row in selectedRows" :key="row.productId">
          <span>{{ row.name }} × {{ row.quantity }}</span>
          <span>¥{{ formatPrice(lineTotalCents(row) / 100) }}</span>
        </li>
      </ul>
      <p class="checkout-total">共 {{ totalQuantity }} 件 · 合计 ¥{{ formatPrice(totalCents / 100) }}</p>
      <el-alert v-if="checkoutError" :title="checkoutError" type="error" :closable="false" />
      <template #footer>
        <el-button :disabled="busy" @click="checkoutOpen = false">返回购物车</el-button>
        <el-button type="primary" :loading="busy" :disabled="!selectedRows.length" @click="checkout">确认下单</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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
const checkoutOpen = ref(false)
const checkoutError = ref('')
const selectedRows = computed(() => rows.value.filter(row => selected.value.includes(row.productId)))
const lineTotalCents = row => Math.round(Number(row.price) * 100) * row.quantity
const totalCents = computed(() => selectedRows.value.reduce((sum, row) => sum + lineTotalCents(row), 0))
const totalQuantity = computed(() => selectedRows.value.reduce((sum, row) => sum + row.quantity, 0))
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
  if (busy.value || !checkoutOpen.value || !selectedRows.value.length) return
  const ids = selectedRows.value.map(row => row.productId).sort((a, b) => a - b)
  const signature = ids.join(',')
  if (pendingCheckout?.signature !== signature) pendingCheckout = { signature, key: newOrderRequestId() }
  busy.value = true
  checkoutError.value = ''
  try {
    await commerceApi.checkout(ids, pendingCheckout.key)
    pendingCheckout = null
    checkoutOpen.value = false
    ElMessage.success('订单已生成')
    await router.push('/customer/shop-orders')
  } catch (e) { checkoutError.value = apiErrorMessage(e) }
  finally { busy.value = false }
}

watch(checkoutOpen, open => { if (open) checkoutError.value = '' })
watch(cart, () => { checkoutOpen.value = false; rows.value = []; selected.value = []; load() }, { immediate: true })
</script>

<style scoped>
.shopping-list { list-style: none; padding: 0; }
.shopping-list li, .shopping-actions { display: flex; gap: 14px; align-items: center; flex-wrap: wrap; padding: 12px 0; border-bottom: 1px solid #e1e5ec; }
.shopping-list input[type="number"] { width: 86px; }
.checkout-summary { list-style: none; padding: 0; margin: 16px 0; }
.checkout-summary li { display: flex; justify-content: space-between; gap: 16px; padding: 8px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.checkout-summary li span:first-child { overflow-wrap: anywhere; }
.checkout-summary li span:last-child { white-space: nowrap; }
.checkout-total { text-align: right; font-weight: 600; }
</style>
