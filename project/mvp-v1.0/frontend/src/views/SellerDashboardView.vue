<template>
  <div class="content-width dashboard-page seller-layout">
    <aside class="seller-sidebar" aria-label="卖家后台目录">
      <div class="seller-sidebar-title">卖家后台</div>
      <el-menu :default-active="activeSection" @select="activeSection = $event">
        <el-menu-item index="overview">概览</el-menu-item>
        <el-menu-item index="product">商品管理</el-menu-item>
        <el-menu-item index="queue">购买意向</el-menu-item>
      </el-menu>
    </aside>
    <div class="seller-content">
      <PageHeader :title="sectionMeta[activeSection].title" :description="sectionMeta[activeSection].description">
        <template #actions><el-button @click="refresh">刷新数据</el-button><el-button @click="logout">退出登录</el-button></template>
      </PageHeader>

      <div v-if="loading" class="loading-panel"><el-skeleton :rows="8" animated /></div>
      <template v-else>
        <div v-if="activeSection === 'overview'" class="dashboard-summary">
          <AppCard class="summary-card"><span class="summary-label">当前商品</span><strong>{{ workbench.product ? workbench.product.name : '暂无商品' }}</strong><small>{{ workbench.product ? productLabel(workbench.product.status) : '发布后买家即可查看' }}</small></AppCard>
          <AppCard class="summary-card"><span class="summary-label">等待处理</span><strong>{{ workbench.waitingCount || 0 }}<small class="summary-unit"> 位</small></strong><small>意向按提交时间先后排列</small></AppCard>
          <AppCard class="summary-card"><span class="summary-label">本次交易</span><strong>{{ workbench.activeIntent ? '进行中' : '未开始' }}</strong><small>{{ workbench.activeIntent ? '正在处理队首买家' : '由你决定何时开始' }}</small></AppCard>
        </div>
        <el-card v-if="activeSection === 'overview'" shadow="never" class="overview-actions">
          <template #header>快捷操作</template>
          <el-button type="primary" @click="activeSection = 'product'">商品管理</el-button>
          <el-button @click="activeSection = 'queue'">查看购买意向</el-button>
        </el-card>

      <AppCard v-if="activeSection === 'product'" class="seller-product-card">
        <template #header>
          <div class="section-title"><h2>{{ workbench.product ? '当前上架商品' : '发布商品' }}</h2><StatusBadge v-if="workbench.product" kind="product" :status="workbench.product.status" /></div>
        </template>
        <div v-if="workbench.product" class="seller-product-content">
          <ProductGallery class="seller-product-image" :images="workbench.product.images" :alt="workbench.product.name" />
          <div class="seller-product-info"><div class="product-title-row"><h3>{{ workbench.product.name }}</h3><div class="product-price"><small>¥</small>{{ formatPrice(workbench.product.price) }}</div></div><p>{{ workbench.product.description || '暂无商品描述' }}</p><small>发布时间：{{ formatDate(workbench.product.createdAt) }}</small></div>
          <div class="seller-product-actions">
            <el-button :disabled="workbench.product.status === 'IN_TRADE'" @click="openProductDialog">编辑商品</el-button>
            <el-button v-if="workbench.product.status === 'ON_SALE'" type="primary" @click="startTrade">与队首买家开始交易</el-button>
            <el-button v-if="workbench.product.status === 'IN_TRADE'" type="primary" @click="openTradeResult">登记交易结果</el-button>
            <el-button v-if="workbench.product.status !== 'DELISTED'" type="danger" plain :disabled="workbench.product.status === 'IN_TRADE'" @click="delistDialogVisible = true">商品下架</el-button>
          </div>
        </div>
        <div v-else class="publish-empty"><EmptyState title="暂无上架商品" description="目前只支持同时上架一件商品。"><el-button type="primary" @click="openProductDialog">发布商品</el-button></EmptyState></div>
      </AppCard>

      <AppCard v-if="activeSection === 'queue'" class="queue-card">
        <template #header><div class="section-title"><h2>购买意向队列</h2><el-tag effect="plain">{{ workbench.intents?.length || 0 }} 条记录</el-tag></div></template>
        <div v-if="workbench.intents?.length" class="queue-table-wrap">
          <el-table :data="workbench.intents" row-key="id" stripe>
            <el-table-column label="排位" width="86"><template #default="scope"><span class="queue-position">{{ scope.row.position ? String(scope.row.position).padStart(2, '0') : '—' }}</span></template></el-table-column>
            <el-table-column prop="name" label="买家" min-width="120" />
            <el-table-column prop="phone" label="联系电话" min-width="140" />
            <el-table-column label="排队时间" min-width="170"><template #default="scope">{{ formatDate(scope.row.queueAt) }}</template></el-table-column>
            <el-table-column label="状态" width="130"><template #default="scope"><StatusBadge :status="scope.row.status" /></template></el-table-column>
            <el-table-column label="操作" min-width="220" fixed="right">
              <template #default="scope">
                <template v-if="scope.row.status === 'WAITING'">
                  <el-button link type="danger" @click="openIntentAction(scope.row, 'VOID')">作废</el-button>
                  <el-button link type="primary" @click="openIntentAction(scope.row, 'REQUEUE')">排到队尾</el-button>
                  <el-button v-if="scope.row.position === 1 && workbench.product?.status === 'ON_SALE'" link type="success" @click="startTrade">开始交易</el-button>
                </template>
                <el-button v-else-if="scope.row.status === 'IN_TRADE'" link type="primary" @click="openTradeResult">登记交易结果</el-button>
                <span v-else class="muted-text">—</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <EmptyState v-else title="队列暂时为空" description="买家提交意向后，会按照提交时间出现在这里。" />
      </AppCard>
      <p v-if="activeSection === 'queue'" class="privacy-note dashboard-note">买家姓名和电话仅在卖家工作台中展示。</p>
      </template>
    </div>
  </div>

  <AppDialog v-model="productDialogVisible" :title="workbench.product ? '编辑商品' : '发布商品'" width="600px">
    <template #title>{{ workbench.product ? '编辑商品' : '发布商品' }}</template>
    <el-form ref="productFormRef" :model="productForm" :rules="productRules" label-position="top">
      <el-form-item label="商品名称" prop="name"><el-input v-model="productForm.name" maxlength="80" show-word-limit placeholder="例如：机械键盘" /></el-form-item>
      <el-form-item label="商品价格（元）" prop="price"><el-input v-model="productForm.price" placeholder="请输入价格，最多两位小数"><template #prefix>¥</template></el-input></el-form-item>
      <el-form-item label="商品描述"><el-input v-model="productForm.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="选填，介绍商品成色、配置或交易说明" /></el-form-item>
      <el-form-item label="商品图片（选填）">
        <div class="image-editor">
          <input ref="imageInputRef" class="visually-hidden" type="file" accept="image/jpeg,image/png" @change="onImageChange">
          <el-button :disabled="imageRows.length >= 1" @click="imageInputRef?.click()">选择图片</el-button>
          <div v-if="imageRows.length" class="image-preview-grid">
            <div v-for="(image, index) in imageRows" :key="image.key" class="image-preview-item">
              <img :src="image.url" :alt="'商品图片 ' + (index + 1)">
              <button type="button" :aria-label="'移除第 ' + (index + 1) + ' 张图片'" @click="removeImage(index)">×</button>
            </div>
          </div>
          <small class="field-hint">最多 1 张 JPG/PNG，不超过 5 MB。更换图片请先移除原图。</small>
        </div>
      </el-form-item>
    </el-form>
    <template #footer><el-button @click="closeProductDialog">取消</el-button><el-button type="primary" :loading="savingProduct" @click="saveProduct">保存商品</el-button></template>
  </AppDialog>

  <AppDialog v-model="intentActionDialogVisible" :title="intentAction === 'VOID' ? '作废这条意向' : '将买家排到队尾'" width="480px">
    <p class="dialog-lead" v-if="selectedIntent">正在处理 <strong>{{ selectedIntent.name }}</strong> 的购买意向。</p>
    <p class="dialog-lead">{{ intentAction === 'VOID' ? '作废后，买家无法再使用当前口令码查询或操作。' : '该买家将保留原有口令码并排到队尾，其他买家位置会相应前移。' }}</p>
    <template #footer><el-button @click="intentActionDialogVisible = false">取消</el-button><el-button type="primary" :loading="processingIntent" @click="confirmIntentAction">{{ intentAction === 'VOID' ? '确认作废' : '确认排到队尾' }}</el-button></template>
  </AppDialog>

  <AppDialog v-model="tradeResultDialogVisible" title="登记交易结果" width="500px">
    <p class="dialog-lead">请根据线下交易情况选择结果。交易失败时，还需要确定这条意向作废或重新排到队尾。</p>
    <div class="trade-result-options">
      <button class="trade-result-option success-option" type="button" :class="{ selected: tradeResult.success }" @click="tradeResult.success = true"><strong>交易成功</strong><span>商品成交下架，其他排队意向标记为交易失败</span></button>
      <button class="trade-result-option failed-option" type="button" :class="{ selected: tradeResult.success === false }" @click="tradeResult.success = false"><strong>交易失败</strong><span>选择作废该意向，或保留口令码重新排队</span></button>
    </div>
    <el-form v-if="tradeResult.success === false" label-position="top" class="disposition-form"><el-form-item label="失败意向的处理方式"><el-radio-group v-model="tradeResult.disposition"><el-radio value="VOID">作废意向</el-radio><el-radio value="REQUEUE">排到队尾</el-radio></el-radio-group></el-form-item></el-form>
    <template #footer><el-button @click="tradeResultDialogVisible = false">取消</el-button><el-button type="primary" :loading="savingTradeResult" :disabled="tradeResult.success === null" @click="saveTradeResult">确认登记</el-button></template>
  </AppDialog>

  <AppDialog v-model="delistDialogVisible" title="确认商品下架" width="460px">
    <p class="dialog-lead">下架后买家不能继续提交意向；所有等待中的口令码将立即失效。你之后可以重新发布商品。</p>
    <template #footer><el-button @click="delistDialogVisible = false">暂不下架</el-button><el-button type="danger" :loading="delisting" @click="delistProduct">确认下架</el-button></template>
  </AppDialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppCard from '../components/common/AppCard.vue'
import AppDialog from '../components/common/AppDialog.vue'
import EmptyState from '../components/common/EmptyState.vue'
import PageHeader from '../components/common/PageHeader.vue'
import ProductGallery from '../components/common/ProductGallery.vue'
import StatusBadge from '../components/common/StatusBadge.vue'
import { apiErrorMessage } from '../api/http'
import { sellerApi } from '../api/sellerApi'
import { formatDate, formatPrice } from '../utils/format'
import { productLabel } from '../utils/status'

const router = useRouter()
const activeSection = ref('overview')
const sectionMeta = {
  overview: { title: '概览', description: '查看当前商品、排队人数和交易状态。' },
  product: { title: '商品管理', description: '发布或编辑当前商品。' },
  queue: { title: '购买意向', description: '按排队顺序处理购买意向并登记交易结果。' },
}
const loading = ref(true)
const workbench = ref({ product: null, intents: [], waitingCount: 0, activeIntent: null })
const productDialogVisible = ref(false)
const intentActionDialogVisible = ref(false)
const tradeResultDialogVisible = ref(false)
const delistDialogVisible = ref(false)
const savingProduct = ref(false)
const processingIntent = ref(false)
const savingTradeResult = ref(false)
const delisting = ref(false)
const productFormRef = ref()
const productForm = reactive({ name: '', price: '', description: '' })
const productRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入商品价格', trigger: 'blur' }, { pattern: /^(?:0|[1-9]\d*)(?:\.\d{1,2})?$/, message: '请输入最多两位小数的价格', trigger: 'blur' }],
}
const imageRows = ref([])
const imageInputRef = ref()
const selectedIntent = ref(null)
const intentAction = ref('VOID')
const tradeResult = reactive({ success: null, disposition: 'VOID' })

async function refresh() {
  loading.value = true
  try { const { data } = await sellerApi.workbench(); workbench.value = data }
  catch (error) {
    if ([401, 403].includes(error.response?.status)) router.replace('/seller/login')
    else ElMessage.error(apiErrorMessage(error, '无法加载工作台'))
  } finally { loading.value = false }
}
function openProductDialog() {
  const current = workbench.value.product
  Object.assign(productForm, { name: current?.name || '', price: current?.price ? String(current.price) : '', description: current?.description || '' })
  releaseTemporaryPreviews()
  imageRows.value = (current?.images || []).slice(0, 1).map((url) => ({ key: url, url, file: null }))
  productDialogVisible.value = true
}
function onImageChange(event) {
  const files = Array.from(event.target.files || [])
  const remaining = 1 - imageRows.value.length
  if (files.length > remaining) ElMessage.warning('商品图片最多上传 1 张')
  for (const file of files.slice(0, remaining)) {
    if (!['image/jpeg', 'image/png'].includes(file.type)) { ElMessage.warning('只支持 JPG 或 PNG 图片'); continue }
    if (file.size > 5 * 1024 * 1024) { ElMessage.warning('单张图片不能超过 5 MB'); continue }
    const url = URL.createObjectURL(file)
    imageRows.value.push({ key: url, url, file })
  }
  event.target.value = ''
}
function removeImage(index) {
  const [removed] = imageRows.value.splice(index, 1)
  if (removed?.file) URL.revokeObjectURL(removed.url)
}
function releaseTemporaryPreviews() {
  imageRows.value.forEach((image) => { if (image.file) URL.revokeObjectURL(image.url) })
}
function closeProductDialog() {
  productDialogVisible.value = false
  releaseTemporaryPreviews()
  imageRows.value = []
}
async function saveProduct() {
  const valid = await productFormRef.value?.validate().catch(() => false)
  if (!valid) return
  const price = Number(productForm.price)
  if (!Number.isFinite(price) || price <= 0) { ElMessage.warning('商品价格必须大于 0'); return }
  savingProduct.value = true
  try {
    const images = await Promise.all(imageRows.value.map(async (image) => {
      if (!image.file) return image.url
      return (await sellerApi.uploadImage(image.file)).data.url
    }))
    const payload = { name: productForm.name.trim(), price, description: productForm.description.trim(), images }
    if (workbench.value.product) await sellerApi.updateProduct(payload)
    else await sellerApi.createProduct(payload)
    ElMessage.success('商品信息已保存')
    productDialogVisible.value = false
    releaseTemporaryPreviews()
    imageRows.value = []
    await refresh()
  } catch (error) { ElMessage.error(apiErrorMessage(error, '保存商品失败')) }
  finally { savingProduct.value = false }
}
function openIntentAction(intent, action) { selectedIntent.value = intent; intentAction.value = action; intentActionDialogVisible.value = true }
async function confirmIntentAction() {
  if (!selectedIntent.value) return
  processingIntent.value = true
  try {
    await sellerApi.processIntent(selectedIntent.value.id, { action: intentAction.value })
    ElMessage.success(intentAction.value === 'VOID' ? '意向已作废' : '已排到队尾，原口令码继续有效')
    intentActionDialogVisible.value = false
    await refresh()
  } catch (error) { ElMessage.error(apiErrorMessage(error, '处理意向失败')) }
  finally { processingIntent.value = false }
}
async function startTrade() {
  try {
    await sellerApi.startTrade()
    ElMessage.success('已开始处理队首买家')
    await refresh()
  } catch (error) { ElMessage.error(apiErrorMessage(error, '暂时无法开始交易')) }
}
function openTradeResult() { tradeResult.success = null; tradeResult.disposition = 'VOID'; tradeResultDialogVisible.value = true }
async function saveTradeResult() {
  const active = workbench.value.activeIntent
  if (!active || tradeResult.success === null) return
  savingTradeResult.value = true
  try {
    await sellerApi.finishTrade(active.id, { success: tradeResult.success, disposition: tradeResult.success ? null : tradeResult.disposition })
    ElMessage.success(tradeResult.success ? '交易已标记成功，商品已下架' : (tradeResult.disposition === 'REQUEUE' ? '交易失败，买家已排到队尾' : '交易失败，意向已作废'))
    tradeResultDialogVisible.value = false
    await refresh()
  } catch (error) { ElMessage.error(apiErrorMessage(error, '登记交易结果失败')) }
  finally { savingTradeResult.value = false }
}
async function delistProduct() {
  delisting.value = true
  try { await sellerApi.delistProduct(); delistDialogVisible.value = false; ElMessage.success('商品已下架，等待意向口令码已失效'); await refresh() }
  catch (error) { ElMessage.error(apiErrorMessage(error, '下架失败')) }
  finally { delisting.value = false }
}
async function logout() {
  try { await sellerApi.logout() } catch { /* 登录会话可能已经超时 */ }
  router.replace('/seller/login')
}
onMounted(refresh)
</script>
