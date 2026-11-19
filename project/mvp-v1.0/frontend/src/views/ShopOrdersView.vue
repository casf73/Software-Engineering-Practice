<template>
  <section :class="{ 'content-width': !seller }">
    <PageHeader :title="seller ? '订单管理' : '我的订单'"
      :description="seller ? '查看客户订单，按顺序处理订单状态。' : '商城订单的备货、发货与签收进度；未发货订单可在此取消。'">
      <template #actions>
        <el-button :disabled="loading || busyId !== 0" @click="reload">刷新</el-button>
      </template>
    </PageHeader>

    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <p v-if="loading">正在加载…</p>
    <p v-else-if="!orders.length && !error">暂无订单。<RouterLink v-if="!seller" to="/">去逛逛</RouterLink></p>

    <ul v-else class="order-list">
      <li v-for="order in orders" :key="order.id">
        <div class="order-head">
          <strong>订单 #{{ order.id }}</strong>
          <el-tag :type="tagType(order.status)">{{ statusLabel(order.status) }}</el-tag>
          <span>合计 ¥{{ formatPrice(order.total) }}</span>
          <span class="muted">{{ formatDateTime(order.createdAt) }}</span>
          <span v-if="seller" class="muted">客户：{{ order.customer }}</span>
          <el-button v-if="seller && nextOrderStatus[order.status]" size="small" type="primary"
            :disabled="busyId !== 0 || loading" @click="advance(order)">
            {{ nextOrderAction[order.status] }}
          </el-button>
          <el-button
            v-if="canCancel(order.status)"
            size="small"
            :loading="busyId === order.id"
            :disabled="busyId !== 0 || loading"
            @click="cancel(order)"
          >取消订单</el-button>
        </div>

        <ul class="order-lines">
          <li v-for="(line, index) in order.items" :key="index">
            {{ line.name }} × {{ line.quantity }} · ¥{{ formatPrice(line.unitPrice) }}
            <span v-if="line.artisanName" class="muted">匠人：{{ line.artisanName }}</span>
            <span v-if="line.design" class="design">定制：{{ designLabel(line.design) }}</span>
          </li>
        </ul>

        <p v-if="order.timeline?.length" class="timeline">
          <span v-for="(event, index) in order.timeline" :key="index">
            {{ statusLabel(event.status) }} · {{ formatDateTime(event.time) }}
          </span>
        </p>
      </li>
    </ul>

    <div v-if="orders.length < total" class="more">
      <el-button :disabled="loading || busyId !== 0" @click="loadMore">加载更多</el-button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../components/common/PageHeader.vue'
import { commerceApi } from '../api/commerceApi'
import { apiErrorMessage } from '../api/http'
import { formatDateTime, formatPrice } from '../utils/format'

const props = defineProps({ seller: Boolean })
const route = useRoute()
const router = useRouter()
const orders = ref([])
const total = ref(0)
const page = ref(0)
const loading = ref(false)
const busyId = ref(0)
const error = ref('')

const statusLabels = {
  PLACED: '待确认', CONFIRMED: '已确认', PREPARED: '备货中',
  SHIPPING: '已发货', COMPLETED: '已完成', CANCELLED: '已取消',
}
const statusTags = {
  PLACED: 'primary', CONFIRMED: 'primary', PREPARED: 'warning',
  SHIPPING: 'warning', COMPLETED: 'success', CANCELLED: 'info',
}
const templates = { classic: '经典留白', modern: '现代简约', craft: '手作纸纹' }
const motifs = { none: '无', heart: '爱心', star: '星星', leaf: '枝叶' }
const colors = { ink: '墨蓝', rose: '玫瑰红', forest: '森林绿' }

const statusLabel = status => statusLabels[status] || '状态未知'
const tagType = status => statusTags[status] || 'info'
const nextOrderStatus = { PLACED: 'CONFIRMED', CONFIRMED: 'PREPARED', PREPARED: 'SHIPPING', SHIPPING: 'COMPLETED' }
const nextOrderAction = { PLACED: '确认订单', CONFIRMED: '备货完成', PREPARED: '开始发货', SHIPPING: '完成交易' }
const canCancel = status => Boolean(nextOrderStatus[status]) && (props.seller || status !== 'SHIPPING')

function redirectExpiredSeller(e) {
  if (!props.seller || e.response?.status !== 401) return false
  if (route.name === 'seller-orders') {
    router.replace({ name: 'seller-login', query: { redirect: route.fullPath } })
  }
  return true
}
const designLabel = design => [
  templates[design.template],
  design.motifLabel || (Object.hasOwn(motifs, design.motif) ? motifs[design.motif] : design.motif),
  design.colorLabel || colors[design.color] || design.color,
  design.text,
].filter(Boolean).join(' / ')

async function fetchPage(next) {
  if (loading.value || busyId.value) return
  loading.value = true
  error.value = ''
  try {
    const getOrders = props.seller ? commerceApi.sellerOrders : commerceApi.orders
    const { data } = await getOrders({ page: next, size: 20 })
    orders.value = next === 0 ? data.items : [...orders.value, ...data.items]
    total.value = data.total
    page.value = next
  } catch (e) {
    if (!redirectExpiredSeller(e)) error.value = apiErrorMessage(e)
  } finally {
    loading.value = false
  }
}

const reload = () => fetchPage(0)
const loadMore = () => fetchPage(page.value + 1)

async function updateOrder(order, action, successMessage) {
  if (busyId.value || loading.value) return
  busyId.value = order.id
  try {
    const { data } = await action()
    const index = orders.value.findIndex(row => row.id === order.id)
    if (index >= 0) orders.value[index] = data
    ElMessage.success(successMessage)
  } catch (e) {
    if (!redirectExpiredSeller(e)) ElMessage.error(apiErrorMessage(e))
  } finally {
    busyId.value = 0
  }
}

function advance(order) {
  if (!props.seller || !nextOrderStatus[order.status]) return
  return updateOrder(order, () => commerceApi.advanceSellerOrder(order.id, nextOrderStatus[order.status]), '订单状态已更新')
}

async function cancel(order) {
  if (busyId.value || loading.value || !canCancel(order.status)) return
  try {
    await ElMessageBox.confirm(`确认取消订单 #${order.id}？取消后库存会退回。`, '取消订单', { type: 'warning' })
  } catch {
    return
  }
  const cancelOrder = props.seller ? commerceApi.cancelSellerOrder : commerceApi.cancelOrder
  return updateOrder(order, () => cancelOrder(order.id), '订单已取消')
}

onMounted(reload)
</script>

<style scoped>
.order-list { list-style: none; padding: 0; display: grid; gap: 14px; }
.order-list > li { border: 1px solid #e1e5ec; border-radius: 6px; padding: 14px; }
.order-head { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; }
.order-head .muted, .order-lines .muted { color: #7b8494; }
.order-lines { list-style: none; padding: 8px 0 0; display: grid; gap: 6px; }
.order-lines .design { margin-left: 10px; color: #7b8494; }
.timeline { display: flex; flex-wrap: wrap; gap: 12px; margin: 10px 0 0; color: #7b8494; font-size: 13px; }
.more { display: flex; justify-content: center; padding-top: 16px; }
</style>
