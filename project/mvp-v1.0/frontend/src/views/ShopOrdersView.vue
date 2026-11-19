<template>
  <main class="content-width">
    <PageHeader title="我的订单" description="商城订单的备货、发货与签收进度；未发货订单可在此取消。">
      <template #actions>
        <el-button :disabled="loading" @click="reload">刷新</el-button>
      </template>
    </PageHeader>

    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <p v-if="loading">正在加载…</p>
    <p v-else-if="!orders.length && !error">暂无订单。<RouterLink to="/">去逛逛</RouterLink></p>

    <ul v-else class="order-list">
      <li v-for="order in orders" :key="order.id">
        <div class="order-head">
          <strong>订单 #{{ order.id }}</strong>
          <el-tag :type="tagType(order.status)">{{ statusLabel(order.status) }}</el-tag>
          <span>合计 ¥{{ formatPrice(order.total) }}</span>
          <span class="muted">{{ formatDateTime(order.createdAt) }}</span>
          <el-button
            v-if="canCancel(order.status)"
            size="small"
            :loading="busyId === order.id"
            :disabled="busyId !== 0"
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
      <el-button :disabled="loading" @click="loadMore">加载更多</el-button>
    </div>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../components/common/PageHeader.vue'
import { commerceApi } from '../api/commerceApi'
import { apiErrorMessage } from '../api/http'
import { formatDateTime, formatPrice } from '../utils/format'

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
const canCancel = status => ['PLACED', 'CONFIRMED', 'PREPARED'].includes(status)
const designLabel = design => [
  templates[design.template],
  design.motifLabel || (Object.hasOwn(motifs, design.motif) ? motifs[design.motif] : design.motif),
  design.colorLabel || colors[design.color] || design.color,
  design.text,
].filter(Boolean).join(' / ')

async function fetchPage(next) {
  loading.value = true
  error.value = ''
  try {
    const { data } = await commerceApi.orders({ page: next, size: 20 })
    orders.value = next === 0 ? data.items : [...orders.value, ...data.items]
    total.value = data.total
    page.value = next
  } catch (e) {
    error.value = apiErrorMessage(e)
  } finally {
    loading.value = false
  }
}

const reload = () => fetchPage(0)
const loadMore = () => fetchPage(page.value + 1)

async function cancel(order) {
  try {
    await ElMessageBox.confirm(`确认取消订单 #${order.id}？取消后库存会退回。`, '取消订单', { type: 'warning' })
  } catch {
    return
  }
  busyId.value = order.id
  try {
    const { data } = await commerceApi.cancelOrder(order.id)
    const index = orders.value.findIndex(row => row.id === order.id)
    if (index >= 0) orders.value[index] = data
    ElMessage.success('订单已取消')
  } catch (e) {
    ElMessage.error(apiErrorMessage(e))
  } finally {
    busyId.value = 0
  }
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
