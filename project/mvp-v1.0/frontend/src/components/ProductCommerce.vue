<template>
  <section class="product-commerce">
    <div v-if="!studio" class="commerce-actions">
      <el-button :disabled="busy || product.status !== 'ON_SALE' || product.stock < 1" @click="addToCart">加入购物车</el-button>
      <el-button :disabled="busy" @click="favorite">收藏商品</el-button>
      <el-button v-if="settings?.artisanName" @click="artisanOpen = true">匠人主页</el-button>
    </div>

    <el-button v-if="!studio && settings?.customizable" type="primary"
      :disabled="product.status !== 'ON_SALE' || product.stock < 1"
      @click="router.push({ name: 'customization', query: { product: product.id } })">开始定制</el-button>
    <p v-if="studio && settingsLoading">正在加载定制设置…</p>
    <el-alert v-else-if="studio && settingsError" :title="settingsError" type="error" :closable="false" />
    <el-alert v-else-if="studio && settings && !settings.customizable" title="该商品已关闭在线定制，请返回选择其他商品。" type="info" :closable="false" />
    <el-button v-if="studio && settings?.artisanName" @click="artisanOpen = true">匠人主页</el-button>
    <div v-if="studio && settings?.customizable" class="custom-workbench">
      <h3>在线定制工作台</h3>
      <p>从卖家提供的选项中定制，确认预览后生成一件订单。</p>
      <div class="designer-layout">
      <el-form label-position="top">
        <el-form-item label="模板">
          <select v-model="design.template" aria-label="模板">
            <option v-for="option in options.templates" :key="option" :value="option">{{ templateLabels[option] }}</option>
          </select>
        </el-form-item>
        <el-form-item v-if="options.allowText" label="定制文字">
          <el-input v-model="design.text" :placeholder="options.textHint || '填写你想定制的文字'" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="图案">
          <select v-model="design.motif" aria-label="图案">
            <option v-for="option in options.motifs" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </el-form-item>
        <el-form-item label="颜色">
          <select v-model="design.color" aria-label="颜色">
            <option v-for="option in options.colors" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </el-form-item>
      </el-form>
      <div><h3>实时预览</h3><GiftPreview :design="design" /></div>
      </div>
      <el-button @click="fillExample">填入示例方案</el-button>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { commerceApi, newOrderRequestId } from '../api/commerceApi'
import { apiErrorMessage } from '../api/http'
import { clearCustomerSession, customerSession } from '../stores/customerSession'
import { formatPrice } from '../utils/format'
import { defaultOptions, templateLabels } from '../utils/customizationOptions'
import AppDialog from './common/AppDialog.vue'
import GiftPreview from './GiftPreview.vue'

const { product, studio } = defineProps({ product: { type: Object, required: true }, studio: Boolean })
const route = useRoute()
const router = useRouter()
const busy = ref(false)
const settings = ref(null)
const options = computed(() => settings.value?.options || defaultOptions)
const settingsLoading = ref(true)
const settingsError = ref('')
const artisanOpen = ref(false)
const confirmOpen = ref(false)
const confirmed = ref(null)
const design = reactive({ template: 'classic', text: '', motif: 'none', color: 'ink' })
const draftKey = `custom-design-${product.id}`
if (studio) {
  try {
    const saved = JSON.parse(sessionStorage.getItem(draftKey))
    if (saved && ['template', 'motif', 'color', 'text'].every(key => typeof saved[key] === 'string') && saved.text.length <= 60) {
      const { template, motif, color, text } = saved
      Object.assign(design, { template, motif, color, text })
    }
  } catch { /* 草稿不可用时使用默认方案 */ }
}
watch(design, value => {
  if (studio) {
    try { sessionStorage.setItem(draftKey, JSON.stringify(value)) } catch { /* 草稿存储不可用时仍可编辑 */ }
  }
})
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

function applyOptions() {
  const { templates, motifs, colors, allowText } = options.value
  if (!templates.includes(design.template)) design.template = templates[0]
  if (!motifs.some(choice => choice.value === design.motif)) design.motif = motifs[0].value
  if (!colors.some(choice => choice.value === design.color)) design.color = colors[0].value
  if (!allowText) design.text = ''
}

function fillExample() {
  const { templates, motifs, colors, allowText } = options.value
  Object.assign(design, {
    template: templates.at(-1),
    text: allowText ? '毕业快乐' : '',
    motif: motifs.find(choice => choice.value !== 'none')?.value || 'none',
    color: colors.at(-1).value,
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
    try { sessionStorage.removeItem(draftKey) } catch { /* 不影响已生成订单 */ }
    ElMessage.success('定制订单已生成')
    router.push('/customer/shop-orders')
  })
}

onMounted(async () => {
  try {
    settings.value = (await commerceApi.customization(product.id)).data
    if (studio && settings.value.customizable) applyOptions()
  }
  catch (error) { settingsError.value = apiErrorMessage(error, '定制设置暂不可用') }
  finally { settingsLoading.value = false }
})
</script>

<style scoped>
.product-commerce, .custom-workbench { display: grid; gap: 14px; margin-top: 20px; }
.commerce-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.commerce-actions :deep(.el-button) { margin-left: 0; }
.custom-workbench { border-top: 1px solid #e1e5ec; padding-top: 16px; }
.custom-workbench h3, .custom-workbench p { margin: 0; }
.custom-workbench select { padding: 8px; border: 1px solid #ccd4e1; border-radius: 4px; }
.designer-layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 28px; }
@media (max-width: 680px) { .designer-layout { grid-template-columns: 1fr; } }
.artisan-bio { white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
