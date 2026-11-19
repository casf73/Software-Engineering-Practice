<template>
  <main class="content-width">
    <PageHeader :title="seller ? '卖家订单管理' : '我的订单'">
      <template #actions><RouterLink :to="seller ? '/seller/dashboard' : '/customer/cart'">{{ seller ? '返回卖家后台' : '返回购物车' }}</RouterLink></template>
    </PageHeader>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <p v-if="loading">正在加载订单…</p>
    <p v-else-if="!rows.length && !error">暂无订单。</p>
    <AppCard v-for="order in rows" :key="order.id" class="order-card">
      <template #header><h3>订单 #{{ order.id }} · {{ labels[order.status] }}</h3></template>
      <p v-if="seller">客户 {{ order.customer }}</p>
      <ul>
        <li v-for="line in order.items" :key="line.productId">
          {{ line.name }} · {{ line.quantity }} 件 × ¥{{ formatPrice(line.unitPrice) }}
          <template v-if="line.design">
            <p>定制方案{{ line.artisanName ? ' · 匠人 ' + line.artisanName : '' }}</p>
            <GiftPreview :design="line.design" />
          </template>
        </li>
      </ul>
      <p>总金额 ¥{{ formatPrice(order.total) }}</p>
      <details>
        <summary>{{ order.items.some(line => line.design) ? '制作与订单时间轴' : '订单时间轴' }}</summary>
        <ol><li v-for="(entry, i) in order.timeline" :key="i">{{ labels[entry.status] }} · {{ formatDate(entry.time) }}</li></ol>
      </details>
      <div class="order-actions">
        <el-button v-if="seller && next[order.status]" type="primary" :disabled="busy" @click="advance(order)">{{ labels[next[order.status]] }}</el-button>
        <el-button v-if="canCancel(order)" :disabled="busy" @click="cancel(order)">取消整单</el-button>
      </div>
    </AppCard>
    <div v-if="total > 20" class="order-actions">
      <el-button :disabled="page === 0 || busy || loading" @click="page--; load()">上一页</el-button>
      <span>第 {{ page + 1 }} 页</span>
      <el-button :disabled="(page + 1) * 20 >= total || busy || loading" @click="page++; load()">下一页</el-button>
    </div>
  </main>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { commerceApi } from '../api/commerceApi'
import { apiErrorMessage } from '../api/http'
import AppCard from '../components/common/AppCard.vue'
import PageHeader from '../components/common/PageHeader.vue'
import GiftPreview from '../components/GiftPreview.vue'
import { formatDate, formatPrice } from '../utils/format'

const route = useRoute()
const router = useRouter()
const seller = computed(() => route.name === 'seller-orders')
const labels = { PLACED: '客户下单', CONFIRMED: '商家确认', PREPARED: '备货完成', SHIPPING: '开始发货', COMPLETED: '交易完成', CANCELLED: '已取消' }
const next = { PLACED: 'CONFIRMED', CONFIRMED: 'PREPARED', PREPARED: 'SHIPPING', SHIPPING: 'COMPLETED' }
const rows = ref([])
const total = ref(0)
const page = ref(0)
const error = ref('')
const loading = ref(false)
const busy = ref(false)
let loadVersion = 0

function canCancel(order) {
  return Boolean(next[order.status]) && (seller.value || order.status !== 'SHIPPING')
}

async function load() {
  const version = ++loadVersion
  loading.value = true
  error.value = ''
  try {
    const { data } = await commerceApi.orders(seller.value, page.value)
    if (version === loadVersion) { rows.value = data.items; total.value = data.total }
  } catch (e) {
    if (version !== loadVersion) return
    if (seller.value && e.response?.status === 401) router.replace('/seller/login')
    else error.value = apiErrorMessage(e)
  } finally { if (version === loadVersion) loading.value = false }
}

async function act(action) {
  if (busy.value) return
  busy.value = true
  try { await action() }
  catch (e) { ElMessage.error(apiErrorMessage(e)) }
  finally { await load(); busy.value = false }
}

function advance(order) { return act(() => commerceApi.advance(order.id, next[order.status])) }

async function cancel(order) {
  const sellerOrder = seller.value
  try {
    await ElMessageBox.confirm(`取消订单 #${order.id} 后将恢复库存，订单不能再次推进。`, '确认取消订单', {
      type: 'warning', confirmButtonText: '确认取消订单', cancelButtonText: '保留订单',
      closeOnClickModal: false,
    })
  }
  catch { return }
  return act(() => commerceApi.cancel(sellerOrder, order.id))
}

watch(seller, () => { page.value = 0; rows.value = []; load() }, { immediate: true })
</script>

<style scoped>
.order-card { margin: 16px 0; }
.order-card li { margin: 8px 0; }
.order-actions { display: flex; align-items: center; gap: 12px; margin-top: 12px; }
</style>
