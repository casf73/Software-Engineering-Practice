<template>
  <div class="content-width">
    <PageHeader
      title="商品"
      description="浏览、搜索商品，选择商品提交购买意向。"
    >
      <template #actions><el-button @click="openQueryDialog()">口令码查询</el-button></template>
    </PageHeader>

    <form class="catalog-filters" @submit.prevent="page = 0; loadProduct()">
      <el-input v-model="search" placeholder="搜索商品名称或描述" maxlength="100" clearable aria-label="搜索商品" />
      <select v-model="categoryId" class="native-select" aria-label="商品分类" @change="page = 0; loadProduct()"><option value="">全部分类</option><option v-for="c in categoryOptions" :key="c.id" :value="c.id">{{ c.label }}</option></select>
      <el-button native-type="submit" type="primary">搜索</el-button><el-button @click="search = ''; categoryId = ''; page = 0; loadProduct()">重置</el-button>
    </form>
    <div v-if="items.length" class="catalog-grid">
      <button v-for="item in items" :key="item.id" class="catalog-item" :class="{ selected: product?.id === item.id }" @click="selectProduct(item)">
        <img v-if="item.images?.length" :src="item.images[0]" :alt="item.name"><span v-else class="catalog-no-image">暂无图片</span>
        <strong>{{ item.name }}</strong><span>¥{{ formatPrice(item.price) }} · 库存 {{ item.stock }}</span><small>{{ item.categoryName || '未分类' }} · {{ productLabel(item.status) }}</small>
      </button>
    </div>
    <div class="pager"><el-button :disabled="page === 0 || loading" @click="page--; loadProduct()">上一页</el-button><span>共 {{ total }} 件 · 第 {{ page + 1 }} 页</span><el-button :disabled="(page + 1) * 12 >= total || loading" @click="page++; loadProduct()">下一页</el-button></div>
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
          <p>库存 {{ product.stock }} · {{ product.categoryName || '未分类' }}</p><div class="rich-description" v-html="safeDescription(product.description)" />
        </div>
      </AppCard>

      <AppCard class="intent-card">
        <template #header>
          <div class="card-heading-copy">
            <h2>{{ product.status === 'ON_SALE' ? '提交购买意向' : productLabel(product.status) }}</h2>
          </div>
        </template>
        <el-alert v-if="product.status !== 'ON_SALE'" :title="product.status === 'MANUALLY_FROZEN' ? '商品已手动冻结' : '商品交易中'" description="暂不接收新的购买意向。仍在排队的买家可以查询、修改联系信息或撤销意向。" type="info" :closable="false" show-icon />
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
    <el-input v-model="queryCode" size="large" placeholder="请输入口令码" clearable :disabled="querying || savingContact" @input="queryResult = null; editingContact = false; resolvedCode = ''" @keyup.enter="queryIntent" />
    <div v-if="queryResult" class="query-result">
      <div class="result-header"><strong>{{ queryResult.name }}</strong><StatusBadge :status="queryResult.status" /></div>
      <div class="result-grid">
        <div><small>联系电话</small><strong>{{ queryResult.phone }}</strong></div>
        <div><small>意向状态</small><strong>{{ intentLabel(queryResult.status) }}</strong></div>
        <div><small>当前排位</small><strong>{{ queryResult.position ? '第 ' + queryResult.position + ' 位' : '—' }}</strong></div>
        <div><small>商品状态</small><strong>{{ productLabel(queryResult.productStatus) }}</strong></div>
      </div>
      <p v-if="queryResult.status === 'WAITING' && queryResult.productStatus === 'IN_TRADE'" class="inline-hint">你尚未进入交易，可以修改联系信息或撤销；交易中的买家不能修改或撤销。</p>
    </div>
    <el-form v-if="editingContact && queryResult?.status === 'WAITING'" ref="contactFormRef" :model="contactForm" :rules="intentRules" label-position="top" @submit.prevent="saveContact">
      <el-form-item label="姓名" prop="name"><el-input v-model="contactForm.name" maxlength="80" autocomplete="name" /></el-form-item>
      <el-form-item label="联系电话" prop="phone"><el-input v-model="contactForm.phone" maxlength="30" autocomplete="tel" /></el-form-item>
      <p class="field-hint">保存后保留原口令码和当前排位。</p>
      <el-button type="primary" :loading="savingContact" @click="saveContact">保存联系信息</el-button>
      <el-button :disabled="savingContact" @click="editingContact = false">取消修改</el-button>
    </el-form>
    <template #footer>
      <el-button :disabled="savingContact" @click="queryDialogVisible = false">关闭</el-button>
      <el-button type="primary" :loading="querying" @click="queryIntent">查询</el-button>
      <el-button v-if="queryResult?.status === 'WAITING' && !editingContact" :disabled="querying" @click="openContactEditor">修改联系信息</el-button>
      <el-button v-if="queryResult?.status === 'WAITING'" :disabled="querying || savingContact" type="danger" plain @click="cancelDialogVisible = true">撤销意向</el-button>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppCard from '../components/common/AppCard.vue'
import AppDialog from '../components/common/AppDialog.vue'
import EmptyState from '../components/common/EmptyState.vue'
import PageHeader from '../components/common/PageHeader.vue'
import ProductGallery from '../components/common/ProductGallery.vue'
import StatusBadge from '../components/common/StatusBadge.vue'
import { apiErrorMessage } from '../api/http'
import { publicApi } from '../api/publicApi'
import { catalogApi } from '../api/catalogApi'
import { safeDescription } from '../utils/richText'
import { formatPrice } from '../utils/format'
import { intentLabel, productLabel } from '../utils/status'

const product = ref(null)
const items = ref([]), total = ref(0), page = ref(0), search = ref(''), categoryId = ref(''), categories = ref([])
const categoryOptions = computed(() => categories.value.filter(c => !c.parentId).flatMap(c => [{ ...c, label: c.name }, ...categories.value.filter(x => x.parentId === c.id).map(x => ({ ...x, label: c.name + ' / ' + x.name }))]))
function selectProduct(item) { product.value = item }
const loading = ref(true)
const submitting = ref(false)
const querying = ref(false)
const cancelling = ref(false)
const intentFormRef = ref()
const intentForm = reactive({ name: '', phone: '' })
const intentRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }, { pattern: /^(?=.*[0-9])[0-9+() -]{6,30}$/, message: '请输入有效联系电话', trigger: 'blur' }],
}
const queryDialogVisible = ref(false)
const issueDialogVisible = ref(false)
const cancelDialogVisible = ref(false)
const queryCode = ref('')
const queryResult = ref(null)
const issuedPasscode = ref('')
const resolvedCode = ref('')
const editingContact = ref(false)
const savingContact = ref(false)
const contactFormRef = ref()
const contactForm = reactive({ name: '', phone: '' })

async function loadProduct() {
  loading.value = true
  try {
    const { data } = await catalogApi.products({ q: search.value, categoryId: categoryId.value || undefined, page: page.value, size: 12 })
    items.value = data.items; total.value = data.total
    product.value = data.items.find(x => x.id === product.value?.id) || data.items[0] || null
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
    const { data } = await catalogApi.submit(product.value.id, { ...intentForm })
    issuedPasscode.value = data.passcode
    issueDialogVisible.value = true
    intentForm.name = ''
    intentForm.phone = ''
  } catch (error) { ElMessage.error(apiErrorMessage(error, '提交失败')) }
  finally { submitting.value = false }
}

function openQueryDialog(code = '') {
  queryCode.value = code
  resolvedCode.value = ''
  editingContact.value = false
  queryResult.value = null
  queryDialogVisible.value = true
  if (code) queryIntent()
}

async function queryIntent() {
  if (!queryCode.value.trim()) { ElMessage.warning('请先输入口令码'); return }
  querying.value = true
  editingContact.value = false
  const code = queryCode.value.trim()
  try {
    const { data } = await publicApi.getIntent(code)
    resolvedCode.value = code
    queryResult.value = data
  } catch (error) {
    queryResult.value = null
    ElMessage.error(apiErrorMessage(error, '口令码无效或已失效'))
  } finally { querying.value = false }
}

function openContactEditor() {
  if (!resolvedCode.value || queryResult.value?.status !== 'WAITING') return
  Object.assign(contactForm, { name: queryResult.value.name, phone: queryResult.value.phone })
  editingContact.value = true
}

async function saveContact() {
  const code = resolvedCode.value
  if (!code || savingContact.value || queryResult.value?.status !== 'WAITING') return
  const valid = await contactFormRef.value?.validate().catch(() => false)
  if (!valid) return
  savingContact.value = true
  try {
    const { data } = await publicApi.updateIntent(code, { name: contactForm.name.trim(), phone: contactForm.phone.trim() })
    queryResult.value = data
    editingContact.value = false
    ElMessage.success('联系信息已更新，口令码和排位保持不变')
  } catch (error) {
    ElMessage.error(apiErrorMessage(error, '修改失败'))
    if ([404, 409].includes(error.response?.status)) await queryIntent()
  } finally { savingContact.value = false }
}

async function cancelIntent() {
  if (!resolvedCode.value) return
  cancelling.value = true
  try {
    await publicApi.cancelIntent(resolvedCode.value)
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

onMounted(async () => {
  await loadProduct()
  try { categories.value = (await catalogApi.categories()).data } catch (e) { ElMessage.error(apiErrorMessage(e, '分类加载失败')) }
})
</script>
