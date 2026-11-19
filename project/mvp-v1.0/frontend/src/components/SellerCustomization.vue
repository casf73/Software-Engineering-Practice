<template>
  <section class="seller-customization">
    <h3>定制与匠人资料</h3>
    <p v-if="loading">正在读取设置…</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-form v-if="ready" label-position="top">
      <el-form-item><el-checkbox v-model="form.customizable">启用在线定制</el-checkbox></el-form-item>
      <el-form-item label="匠人名称"><el-input v-model="form.artisanName" maxlength="60" /></el-form-item>
      <el-form-item label="匠人介绍"><el-input v-model="form.artisanBio" type="textarea" maxlength="500" show-word-limit /></el-form-item>
      <template v-if="form.customizable && form.options">
        <h4>买家可选内容</h4>
        <el-form-item label="开放模板">
          <el-checkbox v-for="template in templates" :key="template.value"
            :model-value="form.options.templates.includes(template.value)"
            @change="enabled => toggleTemplate(template.value, enabled)">{{ template.label }}</el-checkbox>
        </el-form-item>
        <fieldset v-for="group in choiceGroups" :key="group.key" class="option-group">
          <legend>{{ group.label }}</legend>
          <p>{{ group.help }}</p>
          <div v-for="(choice, index) in form.options[group.key]" :key="index" class="option-row">
            <el-input v-model="choice.label" :aria-label="group.label + '名称 ' + (index + 1)" placeholder="买家看到的名称" maxlength="20" />
            <el-input v-model="choice.value" :aria-label="group.label + '值 ' + (index + 1)" :placeholder="group.placeholder" :maxlength="group.maxLength" />
            <el-button :disabled="form.options[group.key].length === 1" @click="form.options[group.key].splice(index, 1)">移除</el-button>
          </div>
          <el-button :disabled="form.options[group.key].length >= 12" @click="form.options[group.key].push({ label: '', value: '' })">新增{{ group.label }}</el-button>
        </fieldset>
        <el-form-item><el-checkbox v-model="form.options.allowText">允许买家填写定制文字</el-checkbox></el-form-item>
        <el-form-item v-if="form.options.allowText" label="定制文字提示">
          <el-input v-model="form.options.textHint" maxlength="100" placeholder="例如：请输入要刻印的姓名或祝福语" />
        </el-form-item>
      </template>
      <el-button type="primary" :loading="saving" @click="save">保存定制设置</el-button>
    </el-form>
  </section>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { commerceApi } from '../api/commerceApi'
import { apiErrorMessage } from '../api/http'
import { templates } from '../utils/customizationOptions'

const props = defineProps({ productId: { type: Number, required: true } })
const form = reactive({ customizable: false, artisanName: '', artisanBio: '', options: null })
const choiceGroups = [
  { key: 'motifs', label: '图案', help: '填写自定义符号，如 ☕ 或 ✦。系统保留值：none（无）、heart（♥）、star（★）、leaf（❧）。', placeholder: '例如：☕', maxLength: 12 },
  { key: 'colors', label: '配色', help: '填写六位十六进制颜色。系统保留值：ink（墨蓝）、rose（玫瑰红）、forest（森林绿）。', placeholder: '例如：#21583d', maxLength: 7 },
]

function toggleTemplate(value, enabled) {
  const selected = new Set(form.options.templates)
  if (enabled) selected.add(value)
  else selected.delete(value)
  form.options.templates = [...selected]
}
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
.option-group { min-width: 0; border: 1px solid var(--el-border-color); border-radius: 4px; padding: 12px; margin: 0 0 16px; }
.option-group p { margin: 0 0 12px; color: var(--el-text-color-secondary); }
.option-row { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.option-row .el-input { flex: 1 1 160px; }
</style>
