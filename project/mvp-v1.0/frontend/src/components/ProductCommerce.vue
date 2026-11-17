<template>
  <section class="product-commerce">
    <div class="commerce-actions">
      <el-button :disabled="busy || product.status !== 'ON_SALE' || product.stock < 1" @click="addToCart">加入购物车</el-button>
      <el-button :disabled="busy" @click="favorite">收藏商品</el-button>
      <el-button v-if="settings?.artisanName" @click="artisanOpen = true">匠人主页</el-button>
    </div>

    <div v-if="settings?.customizable" class="custom-workbench">
      <h3>在线定制工作台</h3>
      <p>选模板、填文字、挑图案，确认预览后生成一件定制订单。</p>
      <el-form label-position="top">
        <el-form-item label="模板">
          <select v-model="design.template" aria-label="模板"><option value="classic">经典留白</option><option value="modern">现代简约</option><option value="craft">手作纸纹</option></select>
        </el-form-item>
        <el-form-item label="定制文字"><el-input v-model="design.text" maxlength="60" show-word-limit /></el-form-item>
        <el-form-item label="图案">
          <select v-model="design.motif" aria-label="图案"><option value="none">无</option><option value="heart">爱心</option><option value="star">星星</option><option value="leaf">枝叶</option></select>
        </el-form-item>
        <el-form-item label="颜色">
          <select v-model="design.color" aria-label="颜色"><option value="ink">墨蓝</option><option value="rose">玫瑰红</option><option value="forest">森林绿</option></select>
        </el-form-item>
      </el-form>
      <GiftPreview :design="design" />
      <el-button type="primary" :disabled="busy || product.status !== 'ON_SALE' || product.stock < 1 || (!design.text.trim() && design.motif === 'none')" @click="confirmDesign">
        确认定制方案
      </el-button>
    </div>

    <AppDialog v-model="confirmOpen" title="确认定制订单">
      <p>{{ product.name }} · 1 件 · ¥{{ formatPrice(product.price) }}</p>
      <GiftPreview v-if="confirmed" :design="confirmed" />
      <template #footer>
        <el-button :disabled="busy" @click="confirmOpen = false">返回修改</el-button>
        <el-button type="primary" :loading="busy" @click="submitDesign">生成定制订单</el-button>
      </template>
    </AppDialog>

    <AppDialog v-model="artisanOpen" :title="settings?.artisanName || '匠人主页'">
      <p class="artisan-bio">{{ settings?.artisanBio || '匠人暂未填写介绍。' }}</p>
      <p>作品：{{ product.name }}</p>
      <p>定制订单生成后，可在“订单”里查看备货制作与发货进度。</p>
    </AppDialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { commerceApi, newOrderRequestId } from '../api/commerceApi'
import { apiErrorMessage } from '../api/http'
import { clearCustomerSession, customerSession } from '../stores/customerSession'
import { formatPrice } from '../utils/format'
import AppDialog from './common/AppDialog.vue'
import GiftPreview from './GiftPreview.vue'

const { product } = defineProps({ product: { type: Object, required: true } })
const route = useRoute()
const router = useRouter()
const busy = ref(false)
const settings = ref(null)
const artisanOpen = ref(false)
const confirmOpen = ref(false)
const confirmed = ref(null)
const design = reactive({ template: 'classic', text: '', motif: 'none', color: 'ink' })
let requestId
let confirmedSignature

function requireLogin() {
  if (customerSession.customer) return true
  router.push({ name: 'customer-login', query: { redirect: route.fullPath } })
  return false
}

async function act(action) {
  if (busy.value || !requireLogin()) return
  busy.value = true
  try { await action() } catch (error) {
    if (error.response?.status === 401) {
      clearCustomerSession()
      requireLogin()
    } else ElMessage.error(apiErrorMessage(error))
  } finally { busy.value = false }
}

function addToCart() {
  return act(async () => {
    const { data } = await commerceApi.cart()
    const row = data.find(x => x.productId === product.id)
    await commerceApi.quantity(product.id, (row?.quantity || 0) + 1)
    ElMessage.success('已加入购物车')
  })
}

function favorite() {
  return act(async () => {
    await commerceApi.favorite(product.id)
    ElMessage.success('已收藏')
  })
}

function confirmDesign() {
  if (!requireLogin()) return
  const signature = JSON.stringify(design)
  if (signature !== confirmedSignature) requestId = newOrderRequestId()
  confirmedSignature = signature
  confirmed.value = { ...design }
  confirmOpen.value = true
}

function submitDesign() {
  return act(async () => {
    await commerceApi.customOrder(product.id, confirmed.value, requestId)
    confirmOpen.value = false
    ElMessage.success('定制订单已生成')
    router.push('/customer/shop-orders')
  })
}

onMounted(async () => {
  try { settings.value = (await commerceApi.customization(product.id)).data }
  catch (error) { if (error.response?.status !== 404) ElMessage.error(apiErrorMessage(error)) }
})
</script>

<style scoped>
.product-commerce, .custom-workbench { display: grid; gap: 14px; margin-top: 20px; }
.commerce-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.commerce-actions :deep(.el-button) { margin-left: 0; }
.custom-workbench { border-top: 1px solid #e1e5ec; padding-top: 16px; }
.custom-workbench h3, .custom-workbench p { margin: 0; }
.custom-workbench select { padding: 8px; border: 1px solid #ccd4e1; border-radius: 4px; }
.artisan-bio { white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
