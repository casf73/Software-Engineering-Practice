<template>
  <section class="seller-customization">
    <h3>定制与匠人资料</h3>
    <p v-if="loading">正在读取设置…</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-form v-if="ready" label-position="top">
      <el-form-item><el-checkbox v-model="form.customizable">启用在线定制</el-checkbox></el-form-item>
      <el-form-item label="匠人名称"><el-input v-model="form.artisanName" maxlength="60" /></el-form-item>
      <el-form-item label="匠人介绍"><el-input v-model="form.artisanBio" type="textarea" maxlength="500" show-word-limit /></el-form-item>
      <el-button :loading="saving" @click="save">保存定制设置</el-button>
    </el-form>
  </section>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { commerceApi } from '../api/commerceApi'
import { apiErrorMessage } from '../api/http'

const props = defineProps({ productId: { type: Number, required: true } })
const form = reactive({ customizable: false, artisanName: '', artisanBio: '' })
const loading = ref(true)
const ready = ref(false)
const error = ref('')
const saving = ref(false)

onMounted(async () => {
  try {
    Object.assign(form, (await commerceApi.sellerCustomization(props.productId)).data)
    ready.value = true
  } catch (e) { error.value = apiErrorMessage(e) }
  finally { loading.value = false }
})

async function save() {
  if (saving.value) return
  saving.value = true
  error.value = ''
  try {
    Object.assign(form, (await commerceApi.configure(props.productId, { ...form })).data)
    ElMessage.success('定制设置已保存')
  } catch (e) { error.value = apiErrorMessage(e) }
  finally { saving.value = false }
}
</script>
<style scoped>
.seller-customization { margin-top: 20px; padding-top: 12px; border-top: 1px solid #e1e5ec; }
</style>
