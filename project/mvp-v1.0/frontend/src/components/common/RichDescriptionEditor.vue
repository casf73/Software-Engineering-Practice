<template>
  <div class="rich-editor">
    <div class="editor-toolbar">
      <el-button size="small" @click="wrap('strong')">加粗</el-button>
      <el-button size="small" @click="wrap('em')">斜体</el-button>
      <el-button size="small" @click="wrap('h3')">小标题</el-button>
      <el-button size="small" @click="wrap('p')">段落</el-button>
      <el-button size="small" @click="wrap('ul', '<li>', '</li>')">列表</el-button>
      <el-button size="small" @click="insert('<br>')">换行</el-button>
    </div>
    <textarea ref="input" :value="modelValue" rows="7" maxlength="20000" class="rich-source" aria-label="商品富文本描述" placeholder="输入描述，选中文字后点击格式按钮；也可以填写受支持的 HTML。" @input="$emit('update:modelValue', $event.target.value)" />
    <small>支持段落、标题、加粗、斜体、列表和描述图片；不接受脚本、样式或外部嵌入。</small>
    <details><summary>预览描述</summary><div class="rich-description" v-html="safeDescription(modelValue)" /></details>
  </div>
</template>
<script setup>
import { ref, nextTick } from 'vue'
import { safeDescription } from '../../utils/richText'
const props = defineProps({ modelValue: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue'])
const input = ref()
function insert(text, start = input.value?.selectionStart ?? props.modelValue.length, end = input.value?.selectionEnd ?? start) {
  emit('update:modelValue', props.modelValue.slice(0, start) + text + props.modelValue.slice(end))
  nextTick(() => { input.value?.focus(); input.value?.setSelectionRange(start + text.length, start + text.length) })
}
function wrap(tag, before = '', after = '') {
  const start = input.value.selectionStart, end = input.value.selectionEnd
  insert(`<${tag}>${before}${props.modelValue.slice(start, end) || '内容'}${after}</${tag}>`, start, end)
}
function insertImage(url) { if (/^\/uploads\/[a-zA-Z0-9_-]+\.(png|jpg)$/.test(url)) insert(`<img src="${url}">`) }
defineExpose({ insertImage })
</script>
