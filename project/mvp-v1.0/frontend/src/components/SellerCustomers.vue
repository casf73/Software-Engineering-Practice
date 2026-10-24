<template>
  <AppCard>
    <template #header><h2>注册客户</h2></template>
    <form class="catalog-filters" @submit.prevent="page = 0; loadCustomers()">
      <el-input v-model="query" maxlength="100" clearable placeholder="搜索用户名或联系电话" aria-label="搜索客户" />
      <el-button native-type="submit" type="primary" :loading="loading">搜索</el-button>
      <el-button :disabled="loading" @click="query = ''; page = 0; loadCustomers()">重置</el-button>
    </form>
    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" />
    <el-table :data="customers" empty-text="暂无注册客户">
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="username" label="用户名" min-width="130" />
      <el-table-column prop="phone" label="联系电话" min-width="140" />
      <el-table-column prop="defaultLocation" label="默认交易地点" min-width="180" />
      <el-table-column label="注册时间" min-width="170"><template #default="{ row }">{{ formatDate(row.createdAt) }}</template></el-table-column>
      <el-table-column label="操作" width="140"><template #default="{ row }"><el-button type="primary" text @click="openOrders(row)">查看购买历史</el-button></template></el-table-column>
    </el-table>
    <div class="pager"><el-button :disabled="page === 0 || loading" @click="page--; loadCustomers()">上一页</el-button><span>共 {{ total }} 位 · 第 {{ page + 1 }} 页</span><el-button :disabled="(page + 1) * 20 >= total || loading" @click="page++; loadCustomers()">下一页</el-button></div>
  </AppCard>
  <AppDialog v-model="dialogVisible" :title="(selected?.username || '') + '的购买历史'" width="1100px">
    <p class="dialog-lead">联系电话：{{ selected?.phone }}；默认交易地点：{{ selected?.defaultLocation }}</p>
    <p class="field-hint">记录包含等待、交易中、已完成和已结束的意向。商品名称和价格使用下单时的数据。</p>
    <el-alert v-if="orderError" :title="orderError" type="error" :closable="false" />
    <el-skeleton v-if="orderLoading" :rows="4" animated />
    <el-table v-else :data="orders" empty-text="该客户暂无购买记录">
      <el-table-column prop="id" label="编号" width="75" />
      <el-table-column prop="productName" label="商品" min-width="140" />
      <el-table-column label="单价" width="100"><template #default="{ row }">¥{{ formatPrice(row.price) }}</template></el-table-column>
      <el-table-column label="状态" width="110"><template #default="{ row }"><StatusBadge :status="row.status" /></template></el-table-column>
      <el-table-column prop="name" label="本次联系人" min-width="110" />
      <el-table-column prop="phone" label="本次联系电话" min-width="140" />
      <el-table-column prop="meetingLocation" label="本次交易地点" min-width="160" />
      <el-table-column label="提交时间" min-width="170"><template #default="{ row }">{{ formatDate(row.submittedAt) }}</template></el-table-column>
    </el-table>
    <div class="pager"><el-button :disabled="orderPage === 0 || orderLoading" @click="orderPage--; loadOrders()">上一页</el-button><span>共 {{ orderTotal }} 条 · 第 {{ orderPage + 1 }} 页</span><el-button :disabled="(orderPage + 1) * 20 >= orderTotal || orderLoading" @click="orderPage++; loadOrders()">下一页</el-button></div>
    <template #footer><el-button :loading="orderLoading" @click="loadOrders">刷新历史</el-button><el-button @click="dialogVisible = false">关闭</el-button></template>
  </AppDialog>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppCard from './common/AppCard.vue'
import AppDialog from './common/AppDialog.vue'
import StatusBadge from './common/StatusBadge.vue'
import { sellerApi } from '../api/sellerApi'
import { apiErrorMessage } from '../api/http'
import { formatDate, formatPrice } from '../utils/format'

const router = useRouter()
const query = ref(''), page = ref(0), total = ref(0), customers = ref([]), loading = ref(false), loadError = ref('')
const selected = ref(null), dialogVisible = ref(false), orders = ref([]), orderPage = ref(0), orderTotal = ref(0), orderLoading = ref(false), orderError = ref('')
let customerRequest = 0, orderRequest = 0
function handleAuth(error) {
  if ([401, 403].includes(error.response?.status)) router.replace('/seller/login')
}
async function loadCustomers() {
  const request = ++customerRequest
  loading.value = true; loadError.value = ''
  try {
    const { data } = await sellerApi.customers({ q: query.value.trim(), page: page.value, size: 20 })
    if (request !== customerRequest) return
    customers.value = data.items; total.value = data.total
  } catch (error) {
    if (request !== customerRequest) return
    customers.value = []; total.value = 0; handleAuth(error)
    loadError.value = apiErrorMessage(error, '客户列表加载失败')
  } finally { if (request === customerRequest) loading.value = false }
}
function openOrders(customer) {
  selected.value = customer; orderPage.value = 0; orders.value = []; orderTotal.value = 0
  dialogVisible.value = true; loadOrders()
}
async function loadOrders() {
  if (!selected.value) return
  const request = ++orderRequest
  orderLoading.value = true; orderError.value = ''
  try {
    const { data } = await sellerApi.customerOrders(selected.value.id, { page: orderPage.value, size: 20 })
    if (request !== orderRequest) return
    orders.value = data.items; orderTotal.value = data.total
  } catch (error) {
    if (request !== orderRequest) return
    orders.value = []; orderTotal.value = 0; handleAuth(error)
    orderError.value = apiErrorMessage(error, '购买历史加载失败')
  } finally { if (request === orderRequest) orderLoading.value = false }
}
onMounted(loadCustomers)
</script>
