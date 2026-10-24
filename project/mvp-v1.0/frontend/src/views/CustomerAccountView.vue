<template>
  <div class="content-width customer-account-page">
    <PageHeader title="我的购买" description="查看当前排队意向以及已完成、撤销或结束的购买记录。">
      <template #actions><el-button :loading="loading" @click="load">刷新</el-button><el-button @click="router.push('/')">继续浏览</el-button><el-button :loading="loggingOut" @click="logout">退出客户账号</el-button></template>
    </PageHeader>
    <AppCard v-if="customerSession.customer">
      <dl class="customer-profile-list">
        <div><dt>用户名</dt><dd>{{ customerSession.customer.username }}</dd></div>
        <div><dt>联系电话</dt><dd>{{ customerSession.customer.phone }}</dd></div>
        <div><dt>默认交易地点</dt><dd>{{ customerSession.customer.defaultLocation }}</dd></div>
      </dl>
    </AppCard>
    <AppCard>
      <template #header><h2>下单记录</h2></template>
      <p class="field-hint">每条购买意向对应 1 件商品。商品名称与价格按提交时保存；访客意向不会通过姓名或电话自动绑定账号。</p>
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" />
      <el-skeleton v-if="loading" :rows="4" animated />
      <el-table v-else :data="orders" empty-text="暂无购买记录">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="productName" label="商品" min-width="160" />
        <el-table-column label="单价" width="110"><template #default="{ row }">¥{{ formatPrice(row.price) }}</template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="{ row }"><StatusBadge :status="row.status" /></template></el-table-column>
        <el-table-column label="排位" width="80"><template #default="{ row }">{{ row.position ? '第 ' + row.position + ' 位' : '—' }}</template></el-table-column>
        <el-table-column prop="meetingLocation" label="交易地点" min-width="160" />
        <el-table-column label="提交时间" min-width="170"><template #default="{ row }">{{ formatDate(row.submittedAt) }}</template></el-table-column>
        <el-table-column label="操作" width="140"><template #default="{ row }"><el-button v-if="row.passcode" type="primary" text @click="router.push({ path: '/', query: { passcode: row.passcode } })">查询 / 管理意向</el-button><span v-else>已结束</span></template></el-table-column>
      </el-table>
      <div class="pager"><el-button :disabled="page === 0 || loading" @click="page--; load()">上一页</el-button><span>共 {{ total }} 条 · 第 {{ page + 1 }} 页</span><el-button :disabled="(page + 1) * 20 >= total || loading" @click="page++; load()">下一页</el-button></div>
    </AppCard>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppCard from '../components/common/AppCard.vue'
import PageHeader from '../components/common/PageHeader.vue'
import StatusBadge from '../components/common/StatusBadge.vue'
import { customerApi } from '../api/customerApi'
import { apiErrorMessage } from '../api/http'
import { customerSession, loadCustomerSession, clearCustomerSession } from '../stores/customerSession'
import { formatDate, formatPrice } from '../utils/format'

const router = useRouter()
const orders = ref([]), total = ref(0), page = ref(0), loading = ref(false), loggingOut = ref(false), loadError = ref('')
async function load() {
  if (loading.value) return
  loading.value = true; loadError.value = ''
  try {
    if (!await loadCustomerSession(true)) { router.replace('/customer/login'); return }
    const { data } = await customerApi.orders({ page: page.value, size: 20 })
    orders.value = data.items; total.value = data.total
  } catch (error) {
    orders.value = []; total.value = 0
    if (error.response?.status === 401) { clearCustomerSession(); router.replace('/customer/login') }
    else loadError.value = apiErrorMessage(error, '购买记录加载失败，请重试')
  } finally { loading.value = false }
}
async function logout() {
  if (loggingOut.value) return
  loggingOut.value = true
  try { await customerApi.logout(); clearCustomerSession(); ElMessage.success('已退出客户账号'); router.replace('/') }
  catch (error) { ElMessage.error(apiErrorMessage(error, '退出失败，请重试')) }
  finally { loggingOut.value = false }
}
onMounted(load)
</script>
