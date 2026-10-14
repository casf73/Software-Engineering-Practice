<template>
  <div class="content-width">
    <PageHeader
      title="商品"
      description="查看当前商品，提交购买意向或查询排队进度。"
    >
      <template #actions><el-button @click="openQueryDialog">口令码查询</el-button></template>
    </PageHeader>

    <div v-if="loading" class="loading-panel"><el-skeleton :rows="6" animated /></div>
    <el-card v-else-if="!product" class="empty-product-panel" shadow="never">
      <EmptyState title="暂无在售商品" description="卖家上架商品后会显示在这里。" />
    </el-card>
    <div v-else class="store-layout">
      <AppCard class="product-card">
        <ProductGallery :images="product.images" :alt="product.name">
          <template #overlay>
            <div class="image-topline">
              <StatusBadge kind="product" :status="product.status" />
            </div>
          </template>
        </ProductGallery>
        <div class="product-copy">
          <div class="product-title-row">
            <h2>{{ product.name }}</h2>
            <div class="product-price"><small>¥</small>{{ formatPrice(product.price) }}</div>
          </div>
          <p class="product-description">{{ product.description || '卖家暂未补充商品描述。' }}</p>
        </div>
      </AppCard>

      <AppCard class="intent-card">
        <template #header>
          <div class="card-heading-copy">
            <h2>{{ product.status === 'IN_TRADE' ? '商品正在交易中' : '提交购买意向' }}</h2>
          </div>
        </template>
        <el-alert v-if="product.status === 'IN_TRADE'" title="商品交易中" description="暂不接收新的购买意向。已排队的买家仍可查询或撤销自己的意向。" type="info" :closable="false" show-icon />
        <template v-else>
          <p class="form-intro">留下姓名和联系电话，提交后即可获得专属口令码。</p>
          <el-form ref="intentFormRef" :model="intentForm" :rules="intentRules" label-position="top" @submit.prevent="submitIntent">
            <el-form-item label="姓名" prop="name"><el-input v-model="intentForm.name" placeholder="请输入姓名" size="large" autocomplete="name" /></el-form-item>
            <el-form-item label="联系电话" prop="phone"><el-input v-model="intentForm.phone" placeholder="请输入联系电话" size="large" autocomplete="tel" /></el-form-item>
            <el-button type="primary" size="large" class="full-button" :loading="submitting" @click="submitIntent">提交购买意向</el-button>
          </el-form>
          <p class="privacy-note">无需注册。请妥善保存提交后获得的口令码。</p>
        </template>
      </AppCard>
    </div>

    <el-alert class="store-guidance" title="排队说明" description="买家免注册提交意向，系统按有效提交时间排序；卖家开启交易并登记结果。" type="info" :closable="false" show-icon />
  </div>

  <AppDialog v-model="queryDialogVisible" title="口令码查询" width="520px">
    <p class="dialog-lead">输入提交意向后获得的口令码，查询排队位置和处理状态。</p>
    <el-input v-model="queryCode" size="large" placeholder="请输入口令码" clearable @keyup.enter="queryIntent" />
    <div v-if="queryResult" class="query-result">
      <div class="result-header"><strong>{{ queryResult.name }}</strong><StatusBadge :status="queryResult.status" /></div>
      <div class="result-grid">
        <div><small>意向状态</small><strong>{{ intentLabel(queryResult.status) }}</strong></div>
        <div><small>当前排位</small><strong>{{ queryResult.position ? '第 ' + queryResult.position + ' 位' : '—' }}</strong></div>
        <div><small>商品状态</small><strong>{{ productLabel(queryResult.productStatus) }}</strong></div>
      </div>
      <p v-if="queryResult.status === 'WAITING' && queryResult.productStatus === 'IN_TRADE'" class="inline-hint">你尚未进入交易，可以撤销；交易中的买家不能撤销。</p>
    </div>
    <template #footer>
      <el-button @click="queryDialogVisible = false">关闭</el-button>
      <el-button type="primary" :loading="querying" @click="queryIntent">查询</el-button>
      <el-button v-if="queryResult?.status === 'WAITING'" type="danger" plain @click="cancelDialogVisible = true">撤销意向</el-button>
    </template>
  </AppDialog>

  <AppDialog v-model="issueDialogVisible" title="意向已提交" width="460px">
    <el-result icon="success" title="提交成功" sub-title="请保存口令码，关闭后不会再次显示。" />
    <div class="passcode-box"><span>{{ issuedPasscode }}</span><el-button text type="primary" @click="copyPasscode">复制</el-button></div>
    <template #footer><el-button type="primary" @click="issueDialogVisible = false; openQueryDialog(issuedPasscode)">查看排队状态</el-button></template>
  </AppDialog>

  <AppDialog v-model="cancelDialogVisible" title="确认撤销意向" width="440px">
    <p class="dialog-lead">撤销后会退出队列，口令码立即失效，后续买家的位置会向前移动。你确定继续吗？</p>
    <template #footer>
      <el-button @click="cancelDialogVisible = false">再想想</el-button>
      <el-button type="danger" :loading="cancelling" @click="cancelIntent">确认撤销</el-button>
    </template>
  </AppDialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppCard from '../components/common/AppCard.vue'
import AppDialog from '../components/common/AppDialog.vue'
import EmptyState from '../components/common/EmptyState.vue'
import PageHeader from '../components/common/PageHeader.vue'
import ProductGallery from '../components/common/ProductGallery.vue'
import StatusBadge from '../components/common/StatusBadge.vue'
import { apiErrorMessage } from '../api/http'
import { publicApi } from '../api/publicApi'
import { formatPrice } from '../utils/format'
import { intentLabel, productLabel } from '../utils/status'

const product = ref(null)
const loading = ref(true)
const submitting = ref(false)
const querying = ref(false)
const cancelling = ref(false)
const intentFormRef = ref()
const intentForm = reactive({ name: '', phone: '' })
const intentRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
}
const queryDialogVisible = ref(false)
const issueDialogVisible = ref(false)
const cancelDialogVisible = ref(false)
const queryCode = ref('')
const queryResult = ref(null)
const issuedPasscode = ref('')

async function loadProduct() {
  loading.value = true
  try {
    const { data } = await publicApi.getCurrentProduct()
    product.value = data || null
  } catch (error) {
    if (error.response?.status === 404 || error.response?.status === 204) product.value = null
    else ElMessage.error(apiErrorMessage(error, '暂时无法加载商品'))
  } finally { loading.value = false }
}

async function submitIntent() {
  if (!intentFormRef.value || product.value?.status !== 'ON_SALE') return
  const valid = await intentFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const { data } = await publicApi.submitIntent({ ...intentForm })
    issuedPasscode.value = data.passcode
    issueDialogVisible.value = true
    intentForm.name = ''
    intentForm.phone = ''
  } catch (error) { ElMessage.error(apiErrorMessage(error, '提交失败')) }
  finally { submitting.value = false }
}

function openQueryDialog(code = '') {
  queryCode.value = code
  queryResult.value = null
  queryDialogVisible.value = true
  if (code) queryIntent()
}

async function queryIntent() {
  if (!queryCode.value.trim()) { ElMessage.warning('请先输入口令码'); return }
  querying.value = true
  try {
    const { data } = await publicApi.getIntent(queryCode.value.trim())
    queryResult.value = data
  } catch (error) {
    queryResult.value = null
    ElMessage.error(apiErrorMessage(error, '口令码无效或已失效'))
  } finally { querying.value = false }
}

async function cancelIntent() {
  if (!queryCode.value.trim()) return
  cancelling.value = true
  try {
    await publicApi.cancelIntent(queryCode.value.trim())
    cancelDialogVisible.value = false
    queryDialogVisible.value = false
    queryResult.value = null
    queryCode.value = ''
    ElMessage.success('意向已撤销，口令码已失效')
  } catch (error) { ElMessage.error(apiErrorMessage(error, '撤销失败')) }
  finally { cancelling.value = false }
}

async function copyPasscode() {
  try {
    await navigator.clipboard.writeText(issuedPasscode.value)
    ElMessage.success('口令码已复制')
  } catch { ElMessage.warning('复制失败，请手动保存口令码') }
}

onMounted(loadProduct)
</script>
